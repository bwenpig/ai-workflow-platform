import { useEffect, useState, useRef, useCallback } from 'react';
import { Client, IMessage } from '@stomp/stompjs';

export interface WebSocketMessage {
  type: string;
  executionId: string;
  nodeId?: string;
  status?: string;
  progress?: number;
  message: string;
  data?: any;
  timestamp: string;
}

export interface UseWorkflowWebSocketOptions {
  executionId: string | null;
  enabled?: boolean;
  pollingInterval?: number; // 轮询间隔（毫秒），设为 0 禁用轮询
  onExecutionStart?: (msg: WebSocketMessage) => void;
  onNodeStart?: (msg: WebSocketMessage) => void;
  onNodeComplete?: (msg: WebSocketMessage) => void;
  onExecutionComplete?: (msg: WebSocketMessage) => void;
  onExecutionFailed?: (msg: WebSocketMessage) => void;
  onProgress?: (msg: WebSocketMessage) => void;
  onError?: (error: Error) => void;
}

/**
 * WebSocket Hook - 支持轮询降级方案
 * 
 * Phase 1: 使用 3 秒轮询（默认）
 * Phase 2: WebSocket 实时推送（当 WebSocket 可用时）
 */
export function useWorkflowWebSocket({
  executionId,
  enabled = true,
  pollingInterval = 3000, // 默认 3 秒轮询
  onExecutionStart,
  onNodeStart,
  onNodeComplete,
  onExecutionComplete,
  onExecutionFailed,
  onProgress,
  onError,
}: UseWorkflowWebSocketOptions) {
  const [connected, setConnected] = useState(false);
  const [usePolling, setUsePolling] = useState(true); // 默认使用轮询
  const [messages, setMessages] = useState<WebSocketMessage[]>([]);
  const [lastPollTime, setLastPollTime] = useState<number | null>(null);
  
  const clientRef = useRef<Client | null>(null);
  const pollIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const pollCountRef = useRef(0);
  const MAX_POLL_COUNT = 100; // 最大轮询次数

  // 处理消息
  const handleMessage = useCallback((msg: WebSocketMessage) => {
    setMessages((prev) => [...prev, msg]);
    setLastPollTime(Date.now());

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
  }, [onExecutionStart, onNodeStart, onNodeComplete, onExecutionComplete, onExecutionFailed, onProgress]);

  // 轮询函数
  const pollExecutionStatus = useCallback(async () => {
    if (!executionId || !enabled) return;

    try {
      pollCountRef.current += 1;
      
      if (pollCountRef.current > MAX_POLL_COUNT) {
        console.warn('达到最大轮询次数，停止轮询');
        stopPolling();
        return;
      }

      const response = await fetch(`http://localhost:8080/api/v1/executions/${executionId}`);
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      
      const data = await response.json();
      
      // 转换为 WebSocket 消息格式
      const msg: WebSocketMessage = {
        type: 'EXECUTION_PROGRESS',
        executionId,
        status: data.status,
        timestamp: new Date().toISOString(),
        message: `执行状态：${data.status}`,
        data: {
          nodeStates: data.nodeStates,
          outputs: data.outputs,
        },
      };
      
      handleMessage(msg);

      // 检查是否完成
      if (data.status === 'SUCCESS' || data.status === 'FAILED' || data.status === 'CANCELLED') {
        const completeMsg: WebSocketMessage = {
          type: data.status === 'SUCCESS' ? 'EXECUTION_COMPLETE' : 'EXECUTION_FAILED',
          executionId,
          status: data.status,
          timestamp: new Date().toISOString(),
          message: `执行${data.status === 'SUCCESS' ? '完成' : '失败'}`,
          data: {
            nodeStates: data.nodeStates,
            outputs: data.outputs,
            error: data.error,
          },
        };
        handleMessage(completeMsg);
        stopPolling();
      }
    } catch (error) {
      console.error('轮询失败:', error);
      onError?.(error as Error);
    }
  }, [executionId, enabled, handleMessage, onError]);

  // 停止轮询
  const stopPolling = useCallback(() => {
    if (pollIntervalRef.current) {
      clearInterval(pollIntervalRef.current);
      pollIntervalRef.current = null;
    }
  }, []);

  // 启动轮询
  const startPolling = useCallback(() => {
    stopPolling();
    pollCountRef.current = 0;
    
    if (pollingInterval > 0) {
      // 立即执行一次
      pollExecutionStatus();
      
      // 然后定期轮询
      pollIntervalRef.current = setInterval(pollExecutionStatus, pollingInterval);
    }
  }, [pollingInterval, pollExecutionStatus, stopPolling]);

  // 连接 WebSocket
  const connectWebSocket = useCallback(() => {
    if (!executionId || !enabled) return;

    const client = new Client({
      brokerURL: `ws://localhost:8080/ws-native`,
      reconnectDelay: 5000,
      debug: (str) => console.log('STOMP:', str),
    });

    client.onConnect = () => {
      console.log('WebSocket 已连接');
      setConnected(true);
      setUsePolling(false);

      // 订阅执行状态
      const subscription = client.subscribe(
        `/topic/executions/${executionId}`,
        (message: IMessage) => {
          const msg: WebSocketMessage = JSON.parse(message.body);
          handleMessage(msg);
        }
      );

      clientRef.current = client;

      return () => {
        subscription.unsubscribe();
      };
    };

    client.onStompError = (frame) => {
      console.error('STOMP 错误:', frame);
      setConnected(false);
      setUsePolling(true); // WebSocket 失败时降级到轮询
      onError?.(new Error(`STOMP 错误：${frame.headers.message}`));
    };

    client.onWebSocketClose = () => {
      console.log('WebSocket 连接关闭');
      setConnected(false);
      setUsePolling(true); // WebSocket 断开时降级到轮询
    };

    client.activate();
  }, [executionId, enabled, handleMessage, onError]);

  // 断开 WebSocket
  const disconnect = useCallback(() => {
    if (clientRef.current) {
      clientRef.current.deactivate();
      clientRef.current = null;
    }
    stopPolling();
    setConnected(false);
  }, [stopPolling]);

  // 初始化连接
  useEffect(() => {
    if (!executionId || !enabled) {
      disconnect();
      return;
    }

    // Phase 1: 使用轮询
    if (usePolling) {
      startPolling();
    } else {
      // Phase 2: 使用 WebSocket
      connectWebSocket();
    }

    return () => {
      disconnect();
    };
  }, [executionId, enabled, usePolling, startPolling, connectWebSocket, disconnect]);

  // 切换模式
  const switchToWebSocket = useCallback(() => {
    setUsePolling(false);
  }, []);

  const switchToPolling = useCallback(() => {
    setUsePolling(true);
  }, []);

  return {
    connected,
    usePolling,
    messages,
    lastPollTime,
    switchToWebSocket,
    switchToPolling,
    disconnect,
  };
}

export default useWorkflowWebSocket;
