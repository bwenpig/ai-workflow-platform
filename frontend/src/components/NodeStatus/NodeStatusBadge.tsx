import React from 'react';

interface Props {
  status: 'idle' | 'pending' | 'running' | 'success' | 'error' | 'skipped';
  label?: string;
}

const COLORS = {
  idle: '#57606f',
  pending: '#ffa502', 
  running: '#00d4ff',
  success: '#2ed573',
  error: '#ff4757',
  skipped: '#6c6c80'
};

const LABELS = {
  idle: '空闲',
  pending: '等待',
  running: '运行中',
  success: '成功',
  error: '失败',
  skipped: '跳过'
};

export const NodeStatusBadge: React.FC<Props> = ({ status, label }) => {
  const color = COLORS[status];
  
  return (
    <span style={{ 
      color: color,
      border: `1px solid ${color}`,
      padding: '2px 8px',
      borderRadius: '4px',
      fontSize: '11px',
      display: 'inline-flex',
      alignItems: 'center',
      gap: '4px',
      background: `${color}15`
    }}>
      <span style={{
        width: '6px',
        height: '6px',
        borderRadius: '50%',
        background: color,
        animation: status === 'running' ? 'pulse 1s infinite' : 'none'
      }} />
      {label || LABELS[status]}
    </span>
  );
};