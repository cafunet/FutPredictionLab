package com.futprediction.standings.dto;

public record StandingDTO(
        String teamId,
        String name,
        String pais,
        String banderaUrl,
        int pts,
        int pj,
        int pg,
        int pe,
        int pp,
        int gf,
        int gc,
        int dg,
        boolean provisional,
        boolean manualOverride,
        String modifiedBy,
        String sealSignature,
        String modifiedAt) {}
