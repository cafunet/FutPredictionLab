package com.futprediction.admin.dto;

public record AdminPredictionViewDTO(
        String id,
        String userName,
        String userEmail,
        String match,
        String predicted,
        String result,
        String status,
        String date,
        double confidence) {}
