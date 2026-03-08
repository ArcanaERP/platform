package com.arcanaerp.platform.inventory.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class InventoryReversalFingerprintTestSupport {

    private InventoryReversalFingerprintTestSupport() {}

    static String fingerprintForReversalRequest(String reason, String adjustedBy) {
        String canonicalRequest = reason + "\n" + adjustedBy.toLowerCase();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonicalRequest.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm not available", exception);
        }
    }
}
