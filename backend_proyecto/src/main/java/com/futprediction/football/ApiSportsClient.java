package com.futprediction.football;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApiSportsClient {
    private static final String BASE_URL = "https://v3.football.api-sports.io";
    private final RestClient client;
    private final String apiKey;

    @Autowired
    public ApiSportsClient(@Value("${API_SPORTS_KEY:}") String apiKey) {
        this(apiKey, httpBuilder());
    }

    // Permite probar las peticiones sin contactar al proveedor real.
    ApiSportsClient(String apiKey, RestClient.Builder builder) {
        this.apiKey = apiKey.trim();
        this.client = builder.baseUrl(BASE_URL)
                .defaultHeader("x-apisports-key", this.apiKey).build();
    }

    private static RestClient.Builder httpBuilder() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().requestFactory(factory);
    }

    public JsonNode teams(String search) {
        if (search == null || search.trim().length() < 2 || search.trim().length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La busqueda debe tener entre 2 y 120 caracteres.");
        }
        return fetch("/teams", "search", search.trim());
    }

    public JsonNode squad(long team) {
        validateTeam(team);
        return fetch("/players/squads", "team", Long.toString(team));
    }

    public JsonNode coaches(long team) {
        validateTeam(team);
        return fetch("/coachs", "team", Long.toString(team));
    }

    private void validateTeam(long team) {
        if (team <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El equipo debe tener un ID positivo.");
        }
    }

    private JsonNode fetch(String path, String parameter, String value) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La consulta de plantillas no esta configurada.");
        }
        try {
            JsonNode body = client.get()
                    .uri(uri -> uri.path(path).queryParam(parameter, "{value}").build(value))
                    .retrieve().body(JsonNode.class);
            if (body == null || !body.path("response").isArray()
                    || body.path("errors").size() > 0) {
                throw providerUnavailable();
            }
            // Angular solo necesita response. No se reenvian encabezados ni errores del proveedor.
            return JsonNodeFactory.instance.objectNode().set("response", body.get("response"));
        } catch (RestClientException ex) {
            throw providerUnavailable();
        }
    }

    private ResponseStatusException providerUnavailable() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "No fue posible consultar las plantillas. Intenta nuevamente mas tarde.");
    }
}
