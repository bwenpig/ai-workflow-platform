import { useEffect, useState, useCallback } from 'react';

export interface NodeStatus {
  nodeId: string;
  status: 'idle' | 'pending' | 'running' | 'success' | 'error' | 'skipped';
  progress?: number;
  message?: string;
  timestamp?: string;
}

export const useNodeStatus = (executionId: string, wsUrl: string = 'ws://localhost:8080') => {
  const [statuses, setStatuses] = useState<Record<string, NodeStatus>>({});
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const connect = useCallback(() => {
    if (!executionId) return;
    
    const ws = new WebSocket(`${wsUrl}/api/v1/ws/realtime?executionId=${executionId}`);
    
    ws.onopen = () => {
      setConnected(true);
      setError(null);
    };
    
    ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        if (message.type === 'node_status' || message.type === 'status') {
          const data = message.data;
          setStatuses(prev => ({
            ...prev,
            [data.nodeId]: {
              nodeId: data.nodeId,
              status: data.state || data.status,
              progress: data.progress,
              message: data.message,
              timestamp: data.timestamp
            }
          }));
        }
      } catch (e) {
        console.error('Failed to parse WebSocket message:', e);
      }
    };
    
    ws.onerror = () => setError('WebSocket connection error');
    ws.onclose = () => setConnected(false);
    
    return () => ws.close();
  }, [executionId, wsUrl]);

  useEffect(() => {
    const cleanup = connect();
    return cleanup;
  }, [connect]);

  const updateStatus = useCallback((nodeId: string, status: NodeStatus) => {
    setStatuses(prev => ({ ...prev, [nodeId]: status }));
  }, []);

  return { statuses, connected, error, updateStatus };
};