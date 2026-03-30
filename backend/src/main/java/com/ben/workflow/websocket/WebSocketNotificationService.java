package com.ben.workflow.websocket;

import org.springframework.stereotype.Service;
import java.util.Map;

/**
 * WebSocket 通知服务 (简化版 - 使用日志输出)
 * TODO: 后续可以添加真正的 WebSocket 支持
 */
@Service
public class WebSocketNotificationService {

    public void notifyExecutionStart(String executionId) {
        System.out.println("[WebSocket] 执行开始：executionId=" + executionId);
    }

    public void notifyNodeStart(String executionId, String nodeId) {
        System.out.println("[WebSocket] 节点开始：executionId=" + executionId + ", nodeId=" + nodeId);
    }

    public void notifyNodeComplete(String executionId, String nodeId, Map<String, Object> result) {
        System.out.println("[WebSocket] 节点完成：executionId=" + executionId + ", nodeId=" + nodeId);
    }

    public void notifyExecutionComplete(String executionId, Map<String, Object> outputs) {
        System.out.println("[WebSocket] 执行完成：executionId=" + executionId);
    }

    public void notifyExecutionFailed(String executionId, String errorMessage) {
        System.err.println("[WebSocket] 执行失败：executionId=" + executionId + ", error=" + errorMessage);
    }

    public void notifyProgress(String executionId, String nodeId, int progress, String message) {
        System.out.println("[WebSocket] 进度更新：executionId=" + executionId + ", progress=" + progress + "%");
    }
}
