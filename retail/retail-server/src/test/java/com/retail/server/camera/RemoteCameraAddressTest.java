package com.retail.server.camera;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteCameraAddressTest {

    @Test
    void parsesCompactAgentCameraNo() {
        Optional<RemoteCameraAddress> parsed = RemoteCameraAddress.parse("agent:192.168.1.23:8765:0");

        assertTrue(parsed.isPresent());
        assertEquals("192.168.1.23", parsed.get().host());
        assertEquals(8765, parsed.get().port());
        assertEquals(0, parsed.get().cameraIndex());
        assertEquals("http://192.168.1.23:8765/api/agent/cameras/frame/0", parsed.get().frameUrl());
    }

    @Test
    void parsesAgentUriCameraNo() {
        Optional<RemoteCameraAddress> parsed = RemoteCameraAddress.parse("agent://camera-node.local:9000/2");

        assertTrue(parsed.isPresent());
        assertEquals("camera-node.local", parsed.get().host());
        assertEquals(9000, parsed.get().port());
        assertEquals(2, parsed.get().cameraIndex());
        assertEquals("agent:camera-node.local:9000:2", parsed.get().cameraNo());
    }

    @Test
    void ignoresLocalNumericCameraNo() {
        assertTrue(RemoteCameraAddress.parse("0").isEmpty());
    }
}
