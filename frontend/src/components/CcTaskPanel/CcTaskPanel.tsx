import React, { useState, useEffect, useRef } from 'react';
import { Tag, Progress, Button, Space, Badge } from 'antd';
import {
  ReloadOutlined,
  ClockCircleOutlined,
  LoadingOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  PauseCircleOutlined,
  DownOutlined,
  RightOutlined,
} from '@ant-design/icons';


const API_BASE = 'http://localhost:8080/api/v1/cc-tasks';

interface CcTask {
  id: string;
  title: string;
  description: string;
  agentId: string;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  progress: number;
  currentStep: string;
  logs: string[];
  createdAt: string;
  updatedAt: string;
  completedAt: string | null;
  errorMessage: string | null;
}

const STATUS_CONFIG: Record<string, { color: string; icon: React.ReactNode; label: string }> = {
  PENDING:    { color: '#8c8c8c', icon: <ClockCircleOutlined />, label: '等待中' },
  RUNNING:    { color: '#1677ff', icon: <LoadingOutlined spin />, label: '运行中' },
  COMPLETED:  { color: '#52c41a', icon: <CheckCircleOutlined />, label: '已完成' },
  FAILED:     { color: '#ff4d4f', icon: <CloseCircleOutlined />, label: '失败' },
  CANCELLED:  { color: '#8c8c8c', icon: <PauseCircleOutlined />, label: '已取消' },
};

/**
 * CC 任务追踪面板
 * 插件式组件，不耦合工作流逻辑
 */
export const CcTaskPanel: React.FC = () => {
  const [tasks, setTasks] = useState<CcTask[]>([]);
  const [loading, setLoading] = useState(false);
  const [expandedKeys, setExpandedKeys] = useState<Set<string>>(new Set());
  const [autoRefresh, setAutoRefresh] = useState(true);
  const logRefs = useRef<Map<string, HTMLDivElement>>(new Map());

  // 轮询获取任务列表
  const fetchTasks = async () => {
    try {
      const res = await fetch(API_BASE);
      if (res.ok) {
        const data = await res.json();
        setTasks(data);
      }
    } catch (e) {
      console.error('Fetch cc tasks error:', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    setLoading(true);
    fetchTasks();
  }, []);

  // 自动轮询
  useEffect(() => {
    if (!autoRefresh) return;
    const timer = setInterval(fetchTasks, 3000);
    return () => clearInterval(timer);
  }, [autoRefresh]);

  // 日志自动滚动
  useEffect(() => {
    tasks.forEach(task => {
      const ref = logRefs.current.get(task.id);
      if (ref) {
        ref.scrollTop = ref.scrollHeight;
      }
    });
  }, [tasks]);

  const toggleExpand = (id: string) => {
    setExpandedKeys(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const activeCount = tasks.filter(
    t => t.status === 'PENDING' || t.status === 'RUNNING'
  ).length;

  const panelStyle: React.CSSProperties = {
    width: 420,
    maxHeight: '70vh',
    background: '#161b22',
    border: '1px solid #30363d',
    borderRadius: '8px',
    overflow: 'hidden',
    display: 'flex',
    flexDirection: 'column',
  };

  const headerStyle: React.CSSProperties = {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '12px 16px',
    borderBottom: '1px solid #30363d',
    background: '#1a1a2e',
  };

  return (
    <div style={panelStyle}>
      {/* 头部 */}
      <div style={headerStyle}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ color: '#fff', fontWeight: 'bold', fontSize: '14px' }}>
            🤖 CC 任务追踪
          </span>
          {activeCount > 0 && (
            <Badge count={activeCount} style={{ backgroundColor: '#1677ff' }} />
          )}
        </div>
        <Space size="small">
          <Button
            type="text"
            size="small"
            icon={<ReloadOutlined />}
            onClick={() => { setLoading(true); fetchTasks(); }}
            loading={loading}
            style={{ color: '#8b949e' }}
            title="刷新"
          />
          <Button
            type="text"
            size="small"
            onClick={() => setAutoRefresh(!autoRefresh)}
            style={{
              color: autoRefresh ? '#52c41a' : '#8b949e',
              fontSize: '11px',
            }}
            title={autoRefresh ? '自动刷新: 开' : '自动刷新: 关'}
          >
            {autoRefresh ? '🟢 3s' : '⚪ 3s'}
          </Button>
        </Space>
      </div>

      {/* 任务列表 */}
      <div style={{ flex: 1, overflow: 'auto', padding: '8px' }}>
        {tasks.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px 0', color: '#8b949e' }}>
            <div style={{ fontSize: '32px', marginBottom: '8px' }}>📭</div>
            <div style={{ fontSize: '13px' }}>暂无 CC 任务</div>
            <div style={{ fontSize: '11px', marginTop: '4px' }}>
              任务创建后会在此显示
            </div>
          </div>
        ) : (
          tasks.map(task => {
            const cfg = STATUS_CONFIG[task.status] || STATUS_CONFIG.PENDING;
            const isExpanded = expandedKeys.has(task.id);
            const hasLogs = task.logs && task.logs.length > 0;

            return (
              <div
                key={task.id}
                style={{
                  background: '#21262d',
                  borderRadius: '6px',
                  marginBottom: '8px',
                  border: `1px solid ${cfg.color}33`,
                  overflow: 'hidden',
                }}
              >
                {/* 任务卡片头部 */}
                <div
                  style={{
                    padding: '10px 12px',
                    cursor: 'pointer',
                    userSelect: 'none',
                  }}
                  onClick={() => toggleExpand(task.id)}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
                    {isExpanded ? <DownOutlined style={{ fontSize: '10px', color: '#8b949e' }} /> : <RightOutlined style={{ fontSize: '10px', color: '#8b949e' }} />}
                    <span style={{ color: cfg.color, fontSize: '16px' }}>{cfg.icon}</span>
                    <span style={{ color: '#e6edf3', fontWeight: 500, fontSize: '13px', flex: 1 }}>
                      {task.title}
                    </span>
                    <Tag
                      color={cfg.color}
                      style={{
                        margin: 0,
                        fontSize: '11px',
                        padding: '0 6px',
                        lineHeight: '18px',
                        background: `${cfg.color}18`,
                        border: `1px solid ${cfg.color}40`,
                        color: cfg.color,
                      }}
                    >
                      {cfg.label}
                    </Tag>
                  </div>

                  {/* 进度条 */}
                  <Progress
                    percent={task.progress}
                    size="small"
                    strokeColor={cfg.color}
                    trailColor="#30363d"
                    showInfo={false}
                    style={{ marginBottom: '4px' }}
                  />

                  {/* 底部信息 */}
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', color: '#8b949e' }}>
                    <span>{task.currentStep || task.agentId?.toUpperCase()}</span>
                    <span>
                      {hasLogs ? `📝 ${task.logs.length} 条日志` : '暂无日志'}
                    </span>
                  </div>
                </div>

                {/* 展开日志区域 */}
                {isExpanded && (
                  <div style={{ borderTop: '1px solid #30363d' }}>
                    {task.errorMessage && (
                      <div style={{
                        padding: '8px 12px',
                        background: '#2a1a1a',
                        color: '#ff7b72',
                        fontSize: '12px',
                        borderBottom: '1px solid #30363d',
                      }}>
                        ❌ {task.errorMessage}
                      </div>
                    )}
                    <div
                      ref={el => { if (el) logRefs.current.set(task.id, el); }}
                      style={{
                        maxHeight: 200,
                        overflow: 'auto',
                        padding: '8px 0',
                        background: '#0d1117',
                        fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
                        fontSize: '11px',
                      }}
                    >
                      {hasLogs ? (
                        task.logs.map((log, i) => (
                          <div
                            key={i}
                            style={{
                              padding: '2px 12px',
                              color: '#c9d1d9',
                              whiteSpace: 'pre-wrap',
                              wordBreak: 'break-all',
                              lineHeight: '1.6',
                            }}
                          >
                            {log}
                          </div>
                        ))
                      ) : (
                        <div style={{ padding: '8px 12px', color: '#484f58', textAlign: 'center' }}>
                          等待日志...
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
