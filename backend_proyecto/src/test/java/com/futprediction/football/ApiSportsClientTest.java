package com.futprediction.football;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ApiSportsClientTest {
    private MockRestServiceServer server;
    private ApiSportsClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ApiSportsClient("clave-simulada-solo-para-tests", builder);
    }

    @Test
    void buscaEquiposConClaveEnServidorYDevuelveSoloLosDatos() {
        server.expect(requestTo("https://v3.football.api-sports.io/teams?search=Costa%20Rica"))
                .andExpect(header("x-apisports-key", "clave-simulada-solo-para-tests"))
                .andRespond(withSuccess("""
                        {"errors":[],"response":[{"team":{"id":29,"name":"Costa Rica"}}],
                         "parameters":{"search":"Costa Rica"},"internal":"no reenviar"}
                        """, MediaType.APPLICATION_JSON));
        var result = client.teams(" Costa Rica ");
        assertEquals(29, result.path("response").get(0).path("team").path("id").asInt());
        assertEquals(1, result.size());
        server.verify();
    }

    @Test
    void consultaJugadoresPorIdDeEquipo() {
        server.expect(requestTo("https://v3.football.api-sports.io/players/squads?team=29"))
                .andRespond(withSuccess("{\"errors\":[],\"response\":[{\"players\":[]}]}", MediaType.APPLICATION_JSON));
        assertTrue(client.squad(29).path("response").get(0).path("players").isArray());
        server.verify();
    }

    @Test
    void consultaEntrenadoresPorIdDeEquipo() {
        server.expect(requestTo("https://v3.football.api-sports.io/coachs?team=29"))
                .andRespond(withSuccess("{\"errors\":[],\"response\":[{\"name\":\"Entrenador de prueba\"}]}", MediaType.APPLICATION_JSON));
        assertEquals("Entrenador de prueba", client.coaches(29).path("response").get(0).path("name").asText());
        server.verify();
    }

    @Test
    void sinClaveDevuelveServicioNoDisponibleSinConsultarAlProveedor() {
        var withoutKey = new ApiSportsClient("", RestClient.builder());
        var error = assertThrows(ResponseStatusException.class, () -> withoutKey.teams("Colombia"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
    }

    @Test
    void rechazaParametrosInvalidosAntesDeHacerPeticiones() {
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class, () -> client.squad(0)).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class, () -> client.coaches(-1)).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class, () -> client.teams(" ")).getStatusCode());
        server.verify();
    }

    @Test
    void noExponeElMensajeDelProveedorCuandoFallaLaPeticion() {
        server.expect(requestTo("https://v3.football.api-sports.io/teams?search=Colombia"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("mensaje privado del proveedor").contentType(MediaType.TEXT_PLAIN));
        var error = assertThrows(ResponseStatusException.class, () -> client.teams("Colombia"));
        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
        assertFalse(error.getReason().contains("privado"));
        assertNull(error.getCause());
        server.verify();
    }

    @Test
    void detectaErroresDelProveedorAunqueRespondaHttp200() {
        server.expect(requestTo("https://v3.football.api-sports.io/teams?search=Colombia"))
                .andRespond(withSuccess("{\"errors\":{\"quota\":\"limit\"},\"response\":[]}", MediaType.APPLICATION_JSON));
        var error = assertThrows(ResponseStatusException.class, () -> client.teams("Colombia"));
        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
        server.verify();
    }
}
