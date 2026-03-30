import { useState } from 'react';
import WorkflowCanvas from './components/WorkflowCanvas';

/**
 * 主应用
 */
function App() {
  const [view, setView] = useState<'canvas' | 'list'>('canvas');

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
            <button
              onClick={() => setView('canvas')}
              style={{
                background: view === 'canvas' ? '#4a4a6a' : 'transparent',
                border: 'none',
                color: 'white',
                padding: '5px 15px',
                borderRadius: '4px',
                cursor: 'pointer',
              }}
            >
              📋 画布编辑器
            </button>
            <button
              onClick={() => setView('list')}
              style={{
                background: view === 'list' ? '#4a4a6a' : 'transparent',
                border: 'none',
                color: 'white',
                padding: '5px 15px',
                borderRadius: '4px',
                cursor: 'pointer',
              }}
            >
              📁 工作流列表
            </button>
          </nav>
        </div>
        <div style={{ fontSize: '12px', color: '#888' }}>
          Ben's AI Lab © 2026
        </div>
      </header>

      {/* 主内容区 */}
      <main style={{ flex: 1, overflow: 'hidden' }}>
        {view === 'canvas' ? (
          <WorkflowCanvas />
        ) : (
          <div style={{ padding: '20px' }}>
            <h2>工作流列表</h2>
            <p>功能开发中...</p>
          </div>
        )}
      </main>
    </div>
  );
}

export default App;
