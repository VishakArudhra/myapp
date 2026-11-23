package com.lab.myapp;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET / shows what is running where. The values come from two places:
 *  - buildVersion:     baked into the jar by Maven (Jenkins build number)
 *  - everything else:  /opt/myapp/shared/application.properties, written by Chef
 * If buildVersion and deployedVersion disagree, the deploy went wrong.
 */
@RestController
public class InfoController {

    private final ObjectProvider<BuildProperties> build;
    private final String environment;
    private final String deployedVersion;
    private final String greeting;
    private final String apiKey;

    public InfoController(ObjectProvider<BuildProperties> build,
                          @Value("${app.environment:local}") String environment,
                          @Value("${app.deployed-version:none}") String deployedVersion,
                          @Value("${app.greeting:Hello}") String greeting,
                          @Value("${app.api-key:}") String apiKey) {
        this.build = build;
        this.environment = environment;
        this.deployedVersion = deployedVersion;
        this.greeting = greeting;
        this.apiKey = apiKey;
    }

    @GetMapping("/")
    public Map<String, Object> info() {
        BuildProperties props = build.getIfAvailable();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("app", "myapp");
        body.put("greeting", greeting);
        body.put("environment", environment);
        body.put("buildVersion", props != null ? props.getVersion() : "dev");
        body.put("deployedVersion", deployedVersion);
        body.put("host", hostname());
        body.put("apiKeyConfigured", !apiKey.isBlank()); // never echo the secret itself
        return body;
    }

    private static String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
