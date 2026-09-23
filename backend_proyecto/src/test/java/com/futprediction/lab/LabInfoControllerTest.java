package com.futprediction.lab;

import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LabInfoControllerTest {
    @ParameterizedTest
    @ValueSource(strings = {"DEV", "QA", "PDN"})
    void cambiaElAmbientePeroConservaLaIdentidadDelArtefacto(String environment) throws Exception {
        var metadata = new Properties();
        metadata.setProperty("release", "build-42");
        metadata.setProperty("commit", "commit-de-prueba");
        var controller = new LabInfoController(environment, Optional.of(new BuildProperties(metadata)));
        MockMvcBuilders.standaloneSetup(controller).build().perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.environment").value(environment))
                .andExpect(jsonPath("$.version").value("build-42"))
                .andExpect(jsonPath("$.commit").value("commit-de-prueba"));
    }

    @Test
    void unaEjecucionSinEmpaquetarNoInventaUnCommit() throws Exception {
        var controller = new LabInfoController("LOCAL", Optional.empty());
        MockMvcBuilders.standaloneSetup(controller).build().perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("development"))
                .andExpect(jsonPath("$.commit").value("unknown"));
    }
}
