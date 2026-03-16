package com.khaleo.flashcard.contract.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LoggingConfigurationContractTest {

    @Test
    void shouldDefineAsyncSplunkAppenderAndJsonConsoleLogging() throws Exception {
        String logback = Files.readString(Path.of("src/main/resources/logback-spring.xml"));
        assertThat(logback).contains("CONSOLE_JSON");
        assertThat(logback).contains("HttpEventCollectorLogbackAppender");
        assertThat(logback).contains("ASYNC_SPLUNK");
        assertThat(logback).contains("neverBlock");
        assertThat(logback).contains("app.observability.splunk.hec-url");
        assertThat(logback).contains("app.observability.splunk.hec-token");

        String appConfig = Files.readString(Path.of("src/main/resources/application.yml"));
        assertThat(appConfig).contains("app:");
        assertThat(appConfig).contains("observability:");
        assertThat(appConfig).contains("splunk:");
        assertThat(appConfig).contains("newrelic:");
    }
}
