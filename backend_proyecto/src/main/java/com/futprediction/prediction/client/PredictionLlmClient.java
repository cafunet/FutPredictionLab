package com.futprediction.prediction.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futprediction.prediction.client.dto.LlmApiResponse;
import com.futprediction.prediction.client.dto.LlmBracketSimulationRequest;
import com.futprediction.prediction.client.dto.LlmBracketSimulationResponse;
import com.futprediction.prediction.client.dto.LlmBracketTeamInput;
import com.futprediction.prediction.client.dto.LlmCreateTeamRequest;
import com.futprediction.prediction.client.dto.LlmPredictionRequest;
import com.futprediction.prediction.client.dto.LlmPredictionResponse;
import com.futprediction.prediction.client.dto.LlmTeamResponse;
import com.futprediction.prediction.config.PredictionLlmProperties;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class PredictionLlmClient {

    private final RestClient restClient;
    private final PredictionLlmProperties properties;
    private final ObjectMapper objectMapper;

    public PredictionLlmClient(
            RestClient predictionRestClient, PredictionLlmProperties properties, ObjectMapper objectMapper) {
        this.restClient = predictionRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public LlmPredictionResponse generatePrediction(
            String homeTeamName,
            String homeCountry,
            String awayTeamName,
            String awayCountry,
            Integer homeFifaRank,
            Integer awayFifaRank,
            String matchPhase,
            String stadium,
            String homeGroup,
            String awayGroup) {
        if (!properties.enabled()) {
            throw new IllegalStateException("El servicio de predicciones LLM está deshabilitado");
        }

        ensureTeamReady(homeTeamName, homeCountry);
        ensureTeamReady(awayTeamName, awayCountry);

        String base = properties.baseUrl().replaceAll("/$", "");
        try {
            LlmApiResponse<LlmPredictionResponse> response = restClient
                    .post()
                    .uri(base + "/api/predictions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new LlmPredictionRequest(
                            homeTeamName,
                            awayTeamName,
                            homeCountry,
                            awayCountry,
                            homeFifaRank,
                            awayFifaRank,
                            matchPhase,
                            stadium,
                            homeGroup,
                            awayGroup))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.data() == null) {
                throw new IllegalStateException("El servicio LLM no devolvió datos de predicción");
            }
            return response.data();
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "Error del servicio LLM (" + ex.getStatusCode().value() + "): " + extractMessage(ex), ex);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "No se puede conectar al servicio LLM en " + properties.baseUrl(), ex);
        }
    }

    public LlmBracketSimulationResponse simulateBracket(List<LlmBracketTeamInput> teams) {
        if (!properties.enabled()) {
            throw new IllegalStateException("El servicio de predicciones LLM está deshabilitado");
        }

        String base = properties.baseUrl().replaceAll("/$", "");
        try {
            LlmApiResponse<LlmBracketSimulationResponse> response = restClient
                    .post()
                    .uri(base + "/api/bracket/simulate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new LlmBracketSimulationRequest(teams))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.data() == null) {
                throw new IllegalStateException("El servicio LLM no devolvió datos del bracket");
            }
            return response.data();
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "Error del servicio LLM bracket (" + ex.getStatusCode().value() + "): " + extractMessage(ex), ex);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "No se puede conectar al servicio LLM en " + properties.baseUrl(), ex);
        }
    }

    private void ensureTeamReady(String teamName, String country) {
        Long teamId = findTeamId(teamName).orElseGet(() -> createTeam(teamName, country));
        refreshStatistics(teamId);
    }

    private java.util.Optional<Long> findTeamId(String teamName) {
        String base = properties.baseUrl().replaceAll("/$", "");
        try {
            LlmApiResponse<List<LlmTeamResponse>> response = restClient
                    .get()
                    .uri(base + "/api/teams")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.data() == null) {
                return java.util.Optional.empty();
            }

            return response.data().stream()
                    .filter(t -> t.name() != null && t.name().equalsIgnoreCase(teamName.trim()))
                    .map(LlmTeamResponse::id)
                    .findFirst();
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "No se pudo consultar equipos en el servicio LLM (" + properties.baseUrl() + ")", ex);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "No se puede conectar al servicio LLM en "
                            + properties.baseUrl()
                            + ". Si el backend corre en Docker, usa host.docker.internal:8080",
                    ex);
        }
    }

    private Long createTeam(String teamName, String country) {
        String base = properties.baseUrl().replaceAll("/$", "");
        String pais = (country != null && !country.isBlank()) ? country.trim() : teamName.trim();
        LlmApiResponse<LlmTeamResponse> response = restClient
                .post()
                .uri(base + "/api/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LlmCreateTeamRequest(teamName.trim(), pais))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || response.data() == null || response.data().id() == null) {
            throw new IllegalStateException("No se pudo registrar el equipo en el servicio LLM: " + teamName);
        }
        return response.data().id();
    }

    private void refreshStatistics(Long teamId) {
        String base = properties.baseUrl().replaceAll("/$", "");
        try {
            restClient
                    .get()
                    .uri(base + "/api/statistics/{teamId}", teamId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            // Si ya existían estadísticas (409/500 duplicado), el motor LLM puede seguir.
            if (ex.getStatusCode().value() >= 500) {
                return;
            }
            throw new IllegalStateException(
                    "No se pudieron generar estadísticas para el equipo (id=" + teamId + ")", ex);
        }
    }

    private String extractMessage(RestClientResponseException ex) {
        try {
            var node = objectMapper.readTree(ex.getResponseBodyAsString());
            if (node.has("message")) {
                return node.get("message").asText();
            }
        } catch (Exception ignored) {
            // use default below
        }
        return ex.getStatusText();
    }
}
