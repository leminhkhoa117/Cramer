package com.cramer.service;

import java.util.Map;
import java.util.UUID;

public interface AdminSpeakingService {
    Map<String, Object> regrade(Long sessionId, String mode, boolean force, UUID adminUserId, String reason, String ipAddress, String userAgent);
}
