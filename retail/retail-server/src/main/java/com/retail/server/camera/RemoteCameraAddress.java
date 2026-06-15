package com.retail.server.camera;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public record RemoteCameraAddress(String scheme, String host, int port, int cameraIndex) {

    public static final int DEFAULT_AGENT_PORT = 8765;

    public static Optional<RemoteCameraAddress> parse(String cameraNo) {
        if (cameraNo == null || cameraNo.isBlank()) {
            return Optional.empty();
        }

        String value = cameraNo.trim();
        Optional<RemoteCameraAddress> compact = parseCompact(value);
        if (compact.isPresent()) {
            return compact;
        }

        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (scheme == null) {
                return Optional.empty();
            }

            String normalizedScheme = scheme.toLowerCase();
            if ("agent".equals(normalizedScheme) || "remote".equals(normalizedScheme)) {
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : DEFAULT_AGENT_PORT;
                Integer cameraIndex = parseIndexFromPath(uri.getPath());
                return create(normalizedScheme, host, port, cameraIndex);
            }

            if ("http".equals(normalizedScheme) || "https".equals(normalizedScheme)) {
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : defaultPortFor(normalizedScheme);
                Integer cameraIndex = parseIndexFromPath(uri.getPath());
                return create(normalizedScheme, host, port, cameraIndex);
            }
        } catch (URISyntaxException | IllegalArgumentException ignored) {
            return Optional.empty();
        }

        return Optional.empty();
    }

    public String baseUrl() {
        String httpScheme = ("https".equals(scheme)) ? "https" : "http";
        return httpScheme + "://" + authority();
    }

    public String frameUrl() {
        return baseUrl() + "/api/agent/cameras/frame/" + cameraIndex;
    }

    public String streamUrl() {
        return baseUrl() + "/api/agent/cameras/stream/" + cameraIndex;
    }

    public String stopStreamUrl() {
        return streamUrl() + "/stop";
    }

    public String cameraNo() {
        return "agent:" + host + ":" + port + ":" + cameraIndex;
    }

    private String authority() {
        String normalizedHost = host.contains(":") && !host.startsWith("[")
                ? "[" + host + "]"
                : host;
        return normalizedHost + ":" + port;
    }

    private static Optional<RemoteCameraAddress> create(
            String scheme,
            String host,
            int port,
            Integer cameraIndex) {
        if (host == null || host.isBlank() || port < 1 || port > 65535 || cameraIndex == null || cameraIndex < 0) {
            return Optional.empty();
        }
        return Optional.of(new RemoteCameraAddress(scheme, host, port, cameraIndex));
    }

    private static int defaultPortFor(String scheme) {
        return "https".equals(scheme) ? 443 : 80;
    }

    private static Integer parseIndexFromPath(String path) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            return null;
        }

        String[] segments = path.split("/");
        for (int i = segments.length - 1; i >= 0; i--) {
            String segment = segments[i];
            if (segment == null || segment.isBlank() || "stop".equalsIgnoreCase(segment)) {
                continue;
            }
            try {
                return Integer.parseInt(segment);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Optional<RemoteCameraAddress> parseCompact(String value) {
        if (!value.regionMatches(true, 0, "agent:", 0, "agent:".length())
                || value.regionMatches(true, 0, "agent://", 0, "agent://".length())) {
            return Optional.empty();
        }

        String payload = value.substring("agent:".length());
        int lastColon = payload.lastIndexOf(':');
        if (lastColon < 1 || lastColon >= payload.length() - 1) {
            return Optional.empty();
        }

        Integer cameraIndex = parsePositiveInt(payload.substring(lastColon + 1));
        if (cameraIndex == null) {
            return Optional.empty();
        }

        String hostAndMaybePort = payload.substring(0, lastColon);
        int port = DEFAULT_AGENT_PORT;
        String host = hostAndMaybePort;

        int portColon = hostAndMaybePort.lastIndexOf(':');
        if (portColon > 0 && portColon < hostAndMaybePort.length() - 1) {
            Integer parsedPort = parsePositiveInt(hostAndMaybePort.substring(portColon + 1));
            if (parsedPort != null) {
                port = parsedPort;
                host = hostAndMaybePort.substring(0, portColon);
            }
        }

        return create("agent", host, port, cameraIndex);
    }

    private static Integer parsePositiveInt(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
