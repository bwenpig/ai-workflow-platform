package com.ben.workflow.spi;

public interface NotificationService {
    void notifyExecutionStart(String instanceId);
    void notifyNodeStart(String instanceId, String nodeId);
    void notifyNodeComplete(String instanceId, String nodeId, Object result);
    void notifyNodeFailed(String instanceId, String nodeId, String error);
    void notifyExecutionComplete(String instanceId, Object result);
}
