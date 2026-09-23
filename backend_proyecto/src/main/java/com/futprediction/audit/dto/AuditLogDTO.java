package com.futprediction.audit.dto;

public record AuditLogDTO(
        String id,
        String userId,
        String userEmail,
        String userName,
        String action,
        String entity,
        String entityId,
        String detail,
        String auditSignature,
        String date) {}
