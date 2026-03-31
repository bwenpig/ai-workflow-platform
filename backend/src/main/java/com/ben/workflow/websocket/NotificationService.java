package com.ben.workflow.websocket;

import java.util.Map;

/**
 * 通知服务接口
 */
public interface NotificationService {

    void notifyExecutionStart(String executionId);

    void notifyNodeStart(String executionId, String nodeId);

    void notifyNodeComplete(String executionId, String nodeId, Map<String, Object> result);

    void notifyExecutionComplete(String executionId, Map<String, Object> outputs);

    void notifyExecutionFailed(String executionId, String errorMessage);

    void notifyProgress(String executionId, String nodeId, int progress, String message);
}
