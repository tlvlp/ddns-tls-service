package com.tlvlp.ddns.tls.service.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;

/**
 * Reads Docker Secrets before SpringBoot starts and makes them available as new environment variables.
 * The main use-case is to parse Docker Secrets.
 *
 * NOTE:
 * This class must be registered under resources/META-INF/spring.factories as an {@link EnvironmentPostProcessor}
 * eg.: org.springframework.boot.env.EnvironmentPostProcessor = RELATIVE.PATH.IN.PROJECT.SecretsLoader
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class SecretsLoader implements EnvironmentPostProcessor {

    private static Boolean loaded = false;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (loaded) {
            return;
        }
        System.out.printf("%n%nLoading file-based Docker secrets before the service starts.%n");

        Path dockerSecretsFolder = Path.of("/run/secrets/");

        try (Stream<Path> pathStream = Files.walk(dockerSecretsFolder, 2, FileVisitOption.FOLLOW_LINKS)) {
            Map<String, Object> secrets = pathStream
                    .filter(path -> path.toFile().isFile())
                    .collect(Collectors.toMap(path -> path.toFile().getName(), this::readFile));

            environment.getPropertySources()
                    .addAfter(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, new MapPropertySource("secrets", secrets));

            System.out.println("Secrets are now available under the following environment variables:");
            secrets.keySet().forEach(key -> System.out.printf("    %s%n", key));
            loaded = true;
        } catch (Exception e) {
            System.err.println(String.format("Unable to parse Docker Secrets from folder: %s", e));
        }
    }

    private String readFile(Path filePath) {
        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error while attempting to read Docker secret file: %s \n%s", filePath, e));
        }
    }

}