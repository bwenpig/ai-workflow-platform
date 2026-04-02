import React, { useEffect, useState, useRef } from 'react';

export interface LogEntry {
  id: string;
  timestamp?: string;
  level: 'info' | 'warn' | 'error' | 'success';
  message: string;
  source?: string;
}

interface LogPanelProps {
  executionId?: string;
}

const LEVEL_COLORS = {
  info: '#00d4ff',
  warn: '#ffa502',
  error: '#ff4757',
  success: '#2ed573'
};

export const LogPanel: React.FC<LogPanelProps> = ({ executionId }) => {
  const [logs, setLogs] = useState<LogEntry[]>([
    { id: '1', timestamp: '14:25:00', level: 'info', message: '系统启动完成', source: 'system' },
    { id: '2', timestamp: '14:25:01', level: 'success', message: '后端服务连接成功', source: 'api' },
    { id: '3', timestamp: '14:25:02', level: 'info', message: '工作流加载完成', source: 'workflow' },
  ]);
  const logEndRef = useRef<HTMLDivElement>(null);

  // 连接SSE获取实时日志
  useEffect(() => {
    if (!executionId) return;
    
    const eventSource = new EventSource(
      `http://localhost:8080/api/v1/executions/${executionId}/logs/stream`
    );

    eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        const newLog: LogEntry = {
          id: data.id || Date.now().toString(),
          timestamp: new Date().toLocaleTimeString(),
          level: data.level?.toLowerCase() || 'info',
          message: data.message || data.msg || 'Log message',
          source: data.nodeId || 'system'
        };
        setLogs(prev => [...prev.slice(-50), newLog]); // 最多保留50条
      } catch (e) {
        console.error('Parse log error:', e);
      }
    };

    eventSource.onerror = () => {
      eventSource.close();
    };

    return () => {
      eventSource.close();
    };
  }, [executionId]);

  return (
    <div style={{ 
      display: 'flex', 
      flexDirection: 'column', 
      height: '100%', 
      background: '#0d0d1a',
      color: '#c9d1d9',
      fontFamily: "'JetBrains Mono', monospace",
      fontSize: '12px'
    }}>
      {/* 标题栏 */}
      <div style={{ 
        padding: '8px 12px', 
        borderBottom: '1px solid #30363d',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center'
      }}>
        <span style={{ fontWeight: 'bold' }}>📋 Console</span>
        <span style={{ color: '#8b949e', fontSize: '11px' }}>
          {executionId ? '🟢 实时' : '⚪ 离线'}
        </span>
      </div>
      
      {/* 日志列表 */}
      <div style={{ flex: 1, overflow: 'auto', padding: '4px 0' }}>
        {logs.map((log, index) => (
          <div 
            key={index}
            style={{
              padding: '2px 12px',
              borderLeft: `3px solid ${LEVEL_COLORS[log.level] || LEVEL_COLORS.info}`,
              display: 'flex',
              gap: '8px'
            }}
          >
            <span style={{ color: '#484f58', whiteSpace: 'nowrap' }}>
              {log.timestamp}
            </span>
            <span style={{ 
              color: LEVEL_COLORS[log.level] || LEVEL_COLORS.info,
              fontWeight: 'bold',
              minWidth: '50px'
            }}>
              {log.level.toUpperCase()}
            </span>
            {log.source && (
              <span style={{ color: '#ffa502' }}>[{log.source}]</span>
            )}
            <span style={{ flex: 1 }}>{log.message}</span>
          </div>
        ))}
        <div ref={logEndRef} />
      </div>
    </div>
  );
};