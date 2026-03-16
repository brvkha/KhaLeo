package com.khaleo.flashcard.contract.deploy;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DeployWorkflowContractTest {

    @Test
    void shouldEncodeMainPushTriggerAndFailureOnAnyTarget() throws Exception {
        String workflow = Files.readString(Path.of("../.github/workflows/deploy-backend.yml"));

        assertThat(workflow).contains("push:");
        assertThat(workflow).contains("main");
        assertThat(workflow).contains("aws s3 cp");
        assertThat(workflow).contains("aws ssm send-command");
        assertThat(workflow).contains("failed_targets");
        assertThat(workflow).contains("exit 1");
    }
}
