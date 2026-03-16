package com.khaleo.flashcard.config.observability;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NewRelicPersistenceInstrumentation {

    public void recordRelationalWriteSuccess(String operation, String entityType) {
        log.info("event=new_relic_persistence_metric metric=relational_write_success operation={} entityType={} count=1",
                operation,
                entityType);
    }

    public void recordRelationalWriteFailure(String operation, String entityType, String reason) {
        log.warn("event=new_relic_persistence_metric metric=relational_write_failure operation={} entityType={} reason={} count=1",
                operation,
                entityType,
                reason);
    }

    public void recordAsyncActivityLogResult(String status, Map<String, String> attributes) {
        log.info("event=new_relic_async_activity_log metric=activity_log_publish status={} attributes={}",
                status,
                attributes);
    }

    public void recordDeadLetter(String logId, String reason) {
        log.error("event=new_relic_async_activity_log metric=activity_log_dead_letter logId={} reason={} count=1",
                logId,
                reason);
    }

    public void recordDeploymentDispatch(String commandId, String instanceId, String status) {
        log.info("event=new_relic_deploy metric=deployment_dispatch commandId={} instanceId={} status={} count=1",
                commandId,
                instanceId,
                status);
    }

    public void recordDeploymentResult(String commandId, String instanceId, String status) {
        log.info("event=new_relic_deploy metric=deployment_result commandId={} instanceId={} status={} count=1",
                commandId,
                instanceId,
                status);
    }

    public void recordAdminAuthorizationDenied(String path, String principalRole) {
        log.warn("event=new_relic_security metric=admin_authorization_denied path={} principalRole={} count=1",
                path,
                principalRole);
    }
}
