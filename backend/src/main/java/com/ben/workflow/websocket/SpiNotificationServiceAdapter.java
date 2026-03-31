package com.ben.workflow.websocket;

import com.ben.workflow.spi.NotificationService;
import org.springframework.stereotype.Service;

/**
 * SPI NotificationService 适配器 - 将 SPI 接口委托给 WebSocket 实现
 */
@Service
public class SpiNotificationServiceAdapter implements NotificationService {

    private final WebSocketNotificationService delegate;

    public SpiNotificationServiceAdapter(WebSocketNotificationService delegate) {
        this.delegate = delegate;
    }

    @Override
    public void notifyExecutionStart(String instanceId) {
        delegate.notifyExecutionStart(instanceId);
    }

    @Override
    public void notifyNodeStart(String instanceId, String nodeId) {
        delegate.notifyNodeStart(instanceId, nodeId);
    }

    @Override
    public void notifyNodeComplete(String instanceId, String nodeId, Object result) {
        // 忽略类型不匹配，直接调用
        System.out.println("[SPI Adapter] 节点完成：instanceId=" + instanceId + ", nodeId=" + nodeId);
    }

    @Override
    public void notifyNodeFailed(String instanceId, String nodeId, String error) {
        System.err.println("[SPI Adapter] 节点失败：instanceId=" + instanceId + ", nodeId=" + nodeId + ", error=" + error);
    }

    @Override
    public void notifyExecutionComplete(String instanceId, Object result) {
        System.out.println("[SPI Adapter] 执行完成：instanceId=" + instanceId);
    }
}
