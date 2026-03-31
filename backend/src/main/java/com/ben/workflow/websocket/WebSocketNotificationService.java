package com.ben.workflow.websocket;

import com.ben.workflow.spi.NotificationService;
import org.springframework.stereotype.Service;
import java.util.Map;

/**
 * WebSocket 通知服务 (简化版 - 使用日志输出)
 * TODO: 后续可以添加真正的 WebSocket 支持
 */
@Service
public class WebSocketNotificationService implements NotificationService {

    @Override
    public void notifyExecutionStart(String instanceId) {
        System.out.println("[WebSocket] 执行开始：instanceId=" + instanceId);
    }

    @Override
    public void notifyNodeStart(String instanceId, String nodeId) {
        System.out.println("[WebSocket] 节点开始：instanceId=" + instanceId + ", nodeId=" + nodeId);
    }

    @Override
    public void notifyNodeComplete(String instanceId, String nodeId, Object result) {
        System.out.println("[WebSocket] 节点完成：instanceId=" + instanceId + ", nodeId=" + nodeId);
    }

    @Override
    public void notifyNodeFailed(String instanceId, String nodeId, String error) {
        System.err.println("[WebSocket] 节点失败：instanceId=" + instanceId + ", nodeId=" + nodeId + ", error=" + error);
    }

    @Override
    public void notifyExecutionComplete(String instanceId, Object result) {
        System.out.println("[WebSocket] 执行完成：instanceId=" + instanceId);
    }
}
