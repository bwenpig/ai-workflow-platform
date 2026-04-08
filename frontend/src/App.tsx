import { useState } from 'react';
import WorkflowCanvas from './components/WorkflowCanvas';
import { LogPanel } from './components/LogPanel/LogPanel';
import { NodeStatusBadge } from './components/NodeStatus/NodeStatusBadge';
import { useNodeStatusStore } from './stores/nodeStatusStore';
import SchedulerPage from './components/SchedulerPage';

// 导入主题样式
import './styles/theme-dark.css';

/**
 * 主应用
 */
function App() {
  const [view, setView] = useState<'canvas' | 'list' | 'scheduler'>('canvas');
  const [showLog, setShowLog] = useState(true);
  const { statuses } = useNodeStatusStore();
  
  // 工作流列表状态
  const [workflows, setWorkflows] = useState<any[]>([]);
  const [currentExecutionId, setCurrentExecutionId] = useState<string | null>(null);

  // 获取工作流列表
  const loadWorkflows = async () => {
    const res = await fetch('http://localhost:8080/api/v1/projects');
    const data = await res.json();
    setWorkflows(data);
  };

  // 执行工作流 - 使用正确的API端点
  const executeWorkflow = async (workflowId: string) => {
    const res = await fetch(`http://localhost:8080/api/v1/workflows/${workflowId}/execute`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ inputs: {} })
    });
    const result = await res.json();
    const execId = result.executionId;
    if (execId) {
      setCurrentExecutionId(execId);
      setShowLog(true); // 自动显示日志面板
      alert(`工作流已启动！执行ID: ${execId}`);
    } else {
      alert(result.message || '执行失败');
    }
  };

  const navButtonStyle = (active: boolean) => ({
    background: active ? '#4a4a6a' : 'transparent',
    border: 'none',
    color: 'white',
    padding: '5px 15px',
    borderRadius: '4px',
    cursor: 'pointer' as const,
  });

  return (
    <div style={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* 顶部导航栏 */}
      <header style={{
        height: '50px',
        background: '#1a1a2e',
        color: 'white',
        display: 'flex',
        alignItems: 'center',
        padding: '0 20px',
        justifyContent: 'space-between',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
          <h1 style={{ margin: 0, fontSize: '18px' }}>🚀 AI Workflow Platform</h1>
          <nav style={{ display: 'flex', gap: '10px' }}>
            <button onClick={() => setView('canvas')} style={navButtonStyle(view === 'canvas')}>
              📋 画布编辑器
            </button>
            <button
              onClick={() => { setView('list'); loadWorkflows(); }}
              style={navButtonStyle(view === 'list')}
            >
              📁 工作流列表
            </button>
            <button onClick={() => setView('scheduler')} style={navButtonStyle(view === 'scheduler')}>
              ⏰ 定时任务
            </button>
          </nav>
        </div>
        
        {/* 节点状态徽章 */}
        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
          {Object.entries(statuses).map(([nodeId, status]) => (
            <NodeStatusBadge key={nodeId} status={status.status} label={nodeId} />
          ))}
        </div>
        
        <div style={{ fontSize: '12px', color: '#888' }}>
          Ben's AI Lab © 2026
        </div>
      </header>

      {/* 主内容区 + 日志面板 */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        {/* 工作流画布 */}
        <main style={{ flex: 1, overflow: 'hidden' }}>
          {view === 'canvas' ? (
            <WorkflowCanvas onExecutionStart={(execId) => {
              setCurrentExecutionId(execId);
              setShowLog(true);
            }} />
          ) : view === 'scheduler' ? (
            <div style={{ height: '100%', overflow: 'auto', background: '#0d1117' }}>
              <SchedulerPage />
            </div>
          ) : (
            <div style={{ padding: '20px', color: 'white' }}>
              <h2>工作流列表</h2>
              <button onClick={loadWorkflows} style={{
                background: '#4a4a6a',
                border: 'none',
                color: 'white',
                padding: '8px 16px',
                borderRadius: '4px',
                cursor: 'pointer',
                marginBottom: '15px',
              }}>🔄 刷新</button>
              {workflows.length === 0 ? (
                <p>暂无工作流，点击刷新加载</p>
              ) : (
                workflows.map(wf => (
                  <div key={wf.id} style={{ 
                    margin: '10px 0', 
                    padding: '15px', 
                    background: '#30363d',
                    borderRadius: '6px',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                  }}>
                    <div>
                      <div style={{ fontWeight: 'bold', marginBottom: '5px' }}>{wf.name}</div>
                      {wf.description && (
                        <div style={{ fontSize: '12px', color: '#888' }}>{wf.description}</div>
                      )}
                      <div style={{ fontSize: '11px', color: '#5a5a6e', marginTop: '5px' }}>
                        📍 {(wf.nodes || []).length} 个节点 | 🔗 {(wf.edges || []).length} 条连线
                      </div>
                    </div>
                    <button 
                      onClick={() => executeWorkflow(wf.id)}
                      style={{
                        background: '#238636',
                        border: 'none',
                        color: 'white',
                        padding: '8px 16px',
                        borderRadius: '4px',
                        cursor: 'pointer',
                      }}
                    >
                      ▶ 执行
                    </button>
                  </div>
                ))
              )}
            </div>
          )}
        </main>
        
        {/* 日志面板 - 可折叠 */}
        {showLog && view !== 'scheduler' && (
          <div style={{ 
            height: '180px', 
            background: '#0d0d1a', 
            borderTop: '1px solid #30363d',
            overflow: 'hidden'
          }}>
            <LogPanel executionId={currentExecutionId ?? undefined} />
          </div>
        )}
      </div>

      {/* 日志面板切换按钮 */}
      {view !== 'scheduler' && (
        <button
          onClick={() => setShowLog(!showLog)}
          style={{
            position: 'fixed',
            bottom: showLog ? '190px' : '10px',
            right: '10px',
            background: '#30363d',
            border: 'none',
            color: 'white',
            padding: '8px 12px',
            borderRadius: '4px',
            cursor: 'pointer',
            zIndex: 1000,
          }}
        >
          {showLog ? '📋 隐藏日志' : '📋 显示日志'}
        </button>
      )}
    </div>
  );
}

export default App;
