package com.futprediction.lab;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LabInfoController {
    private final String environment;
    private final String version;
    private final String commit;

    public LabInfoController(@Value("${APP_ENVIRONMENT:LOCAL}") String environment,
            Optional<BuildProperties> build) {
        this.environment = environment;
        this.version = build.map(value -> value.get("release")).orElse("development");
        this.commit = build.map(value -> value.get("commit")).orElse("unknown");
    }

    @GetMapping("/hello")
    public ReleaseInfo hello() {
        return new ReleaseInfo("Hola desde DevOps - FutPrediction", environment, version, commit);
    }

    public record ReleaseInfo(String message, String environment, String version, String commit) {}
}
