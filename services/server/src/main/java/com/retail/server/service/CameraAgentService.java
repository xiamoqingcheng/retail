package com.retail.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.server.camera.RemoteCameraAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class CameraAgentService {

    private static final String DISCOVERY_MESSAGE = "RETAIL_CAMERA_DISCOVER";
    private static final String AGENT_AVAILABLE_PATH = "/api/agent/cameras/available";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${camera.agent.default-port:8765}")
    private Integer defaultPort;

    @Value("${camera.agent.discovery-port:8766}")
    private Integer discoveryPort;

    public CameraAgentService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<RemoteCameraAddress> parse(String cameraNo) {
        return RemoteCameraAddress.parse(cameraNo);
    }

    public String captureFrame(RemoteCameraAddress address) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(address.frameUrl(), Map.class);
            if (resp != null && resp.get("image_base64") != null) {
                return resp.get("image_base64").toString();
            }
        } catch (Exception e) {
            log.warn("Capture remote camera frame failed: cameraNo={}, error={}", address.cameraNo(), e.getMessage());
        }
        return null;
    }

    public boolean releaseStream(RemoteCameraAddress address) {
        if (address == null) {
            return false;
        }
        try {
            restTemplate.postForEntity(address.stopStreamUrl(), null, String.class);
            return true;
        } catch (Exception e) {
            log.warn("Release remote camera stream failed: cameraNo={}, error={}", address.cameraNo(), e.getMessage());
            return false;
        }
    }

    public Map<String, Object> listAvailable(String host, Integer port) {
        if (!StringUtils.hasText(host)) {
            return Map.of("available_indexes", List.of(), "cameras", List.of());
        }
        int resolvedPort = resolveAgentPort(port);
        String url = "http://" + host.trim() + ":" + resolvedPort + AGENT_AVAILABLE_PATH;
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        return normalizeAgentResponse(response, host.trim(), resolvedPort);
    }

    public List<Map<String, Object>> discoverAgents(Integer timeoutMillis) {
        int timeout = clamp(timeoutMillis == null ? 1500 : timeoutMillis, 300, 5000);
        int port = resolveDiscoveryPort();
        long deadline = System.nanoTime() + Duration.ofMillis(timeout).toNanos();
        List<Map<String, Object>> agents = new ArrayList<>();
        Map<String, Map<String, Object>> byAddress = new LinkedHashMap<>();

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setSoTimeout(150);

            byte[] payload = DISCOVERY_MESSAGE.getBytes(StandardCharsets.UTF_8);
            for (InetAddress target : broadcastTargets()) {
                try {
                    socket.send(new DatagramPacket(payload, payload.length, target, port));
                } catch (Exception ex) {
                    log.debug("Skip discovery target {}: {}", target, ex.getMessage());
                }
            }

            while (System.nanoTime() < deadline) {
                byte[] buffer = new byte[8192];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(packet);
                    String json = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    Map<String, Object> raw = objectMapper.readValue(json, new TypeReference<>() {
                    });
                    String remoteHost = packet.getAddress().getHostAddress();
                    int agentPort = numberValue(raw.get("port"), resolveAgentPort(null));
                    Map<String, Object> normalized = normalizeAgentResponse(raw, remoteHost, agentPort);
                    byAddress.put(remoteHost + ":" + agentPort, normalized);
                } catch (SocketTimeoutException ignored) {
                    // Continue until the total discovery timeout expires.
                } catch (Exception ex) {
                    log.debug("Ignore invalid camera agent discovery packet: {}", ex.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("Discover camera agents failed: {}", ex.getMessage());
        }

        agents.addAll(byAddress.values());
        return agents;
    }

    private Map<String, Object> normalizeAgentResponse(Map<String, Object> raw, String host, int port) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("service", raw == null ? "retail-camera-agent" : raw.getOrDefault("service", "retail-camera-agent"));
        normalized.put("version", raw == null ? "" : raw.getOrDefault("version", ""));
        normalized.put("host", host);
        normalized.put("port", port);

        List<Integer> indexes = extractIndexes(raw);
        normalized.put("available_indexes", indexes);

        List<Map<String, Object>> cameras = new ArrayList<>();
        for (Integer index : indexes) {
            Map<String, Object> camera = new LinkedHashMap<>();
            camera.put("id", "agent:" + host + ":" + port + ":" + index);
            camera.put("index", index);
            camera.put("name", "Remote Camera " + index);
            cameras.add(camera);
        }
        normalized.put("cameras", cameras);
        return normalized;
    }

    private List<Integer> extractIndexes(Map<String, Object> raw) {
        if (raw == null) {
            return List.of();
        }
        Object value = raw.get("available_indexes");
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<Integer> indexes = new ArrayList<>();
        for (Object item : values) {
            Integer index = numberValue(item, null);
            if (index != null && index >= 0 && !indexes.contains(index)) {
                indexes.add(index);
            }
        }
        return indexes;
    }

    private List<InetAddress> broadcastTargets() {
        List<InetAddress> targets = new ArrayList<>();
        try {
            targets.add(InetAddress.getByName("255.255.255.255"));
            targets.add(InetAddress.getLoopbackAddress());
        } catch (Exception ignored) {
        }

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }
                networkInterface.getInterfaceAddresses().forEach(address -> {
                    InetAddress localAddress = address.getAddress();
                    if (localAddress != null && localAddress.getHostAddress().indexOf(':') < 0 && !targets.contains(localAddress)) {
                        targets.add(localAddress);
                    }
                    InetAddress broadcast = address.getBroadcast();
                    if (broadcast != null && !targets.contains(broadcast)) {
                        targets.add(broadcast);
                    }
                });
            }
        } catch (Exception ex) {
            log.debug("List network broadcast targets failed: {}", ex.getMessage());
        }
        return targets.isEmpty() ? Collections.emptyList() : targets;
    }

    private int resolveAgentPort(Integer port) {
        if (port != null && port > 0 && port <= 65535) {
            return port;
        }
        return defaultPort == null ? RemoteCameraAddress.DEFAULT_AGENT_PORT : defaultPort;
    }

    private int resolveDiscoveryPort() {
        if (discoveryPort != null && discoveryPort > 0 && discoveryPort <= 65535) {
            return discoveryPort;
        }
        return 8766;
    }

    private static Integer numberValue(Object value, Integer fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
