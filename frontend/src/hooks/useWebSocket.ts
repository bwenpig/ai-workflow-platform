import { useEffect, useState } from 'react';
import { Client, IMessage } from '@stomp/stompjs';

interface WebSocketMessage {
  type: string;
  executionId: string;
  nodeId?: string;
  status?: string;
  progress?: number;
  message: string;
  data?: any;
  timestamp: string;
}

interface UseWebSocketOptions {
  executionId: string | null;
  onExecutionStart?: (msg: WebSocketMessage) => void;
  onNodeStart?: (msg: WebSocketMessage) => void;
  onNodeComplete?: (msg: WebSocketMessage) => void;
  onExecutionComplete?: (msg: WebSocketMessage) => void;
  onExecutionFailed?: (msg: WebSocketMessage) => void;
  onProgress?: (msg: WebSocketMessage) => void;
}

/**
 * WebSocket Hook - 订阅执行状态推送
 */
export function useWorkflowWebSocket({
  executionId,
  onExecutionStart,
  onNodeStart,
  onNodeComplete,
  onExecutionComplete,
  onExecutionFailed,
  onProgress,
}: UseWebSocketOptions) {
  const [connected, setConnected] = useState(false);
  const [messages, setMessages] = useState<WebSocketMessage[]>([]);

  useEffect(() => {
    if (!executionId) return;

    const client = new Client({
      brokerURL: `ws://localhost:8080/ws-native`,
      reconnectDelay: 5000,
      debug: (str) => console.log('STOMP:', str),
    });

    client.onConnect = () => {
      console.log('WebSocket 已连接');
      setConnected(true);

      // 订阅执行状态
      const subscription = client.subscribe(
        `/topic/executions/${executionId}`,
        (message: IMessage) => {
          const msg: WebSocketMessage = JSON.parse(message.body);
          console.log('收到消息:', msg);
          
          setMessages((prev) => [...prev, msg]);

          // 回调处理
          switch (msg.type) {
            case 'EXECUTION_START':
              onExecutionStart?.(msg);
              break;
            case 'NODE_START':
              onNodeStart?.(msg);
              break;
            case 'NODE_COMPLETE':
              onNodeComplete?.(msg);
              break;
            case 'EXECUTION_COMPLETE':
              onExecutionComplete?.(msg);
              break;
            case 'EXECUTION_FAILED':
              onExecutionFailed?.(msg);
              break;
            case 'EXECUTION_PROGRESS':
              onProgress?.(msg);
              break;
          }
        }
      );

      return () => {
        subscription.unsubscribe();
      };
    };

    client.onStompError = (frame) => {
      console.error('STOMP 错误:', frame);
      setConnected(false);
    };

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [executionId]);

  return { connected, messages };
}
