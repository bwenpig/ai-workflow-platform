import { useState } from 'react';

interface ExecutionResult {
  executionId: string;
  status: string;
  nodeStates: Record<string, {
    status: string;
    result?: any;
    errorMessage?: string;
  }>;
}

interface ResultPreviewProps {
  result: ExecutionResult | null;
  onClose: () => void;
}

/**
 * 执行结果预览组件
 */
export default function ResultPreview({ result, onClose }: ResultPreviewProps) {
  const [selectedNode, setSelectedNode] = useState<string | null>(null);

  if (!result) return null;

  const getNodeResult = () => {
    if (!selectedNode || !result.nodeStates[selectedNode]) return null;
    return result.nodeStates[selectedNode].result;
  };

  const renderResult = () => {
    const nodeResult = getNodeResult();
    if (!nodeResult) return <p>暂无结果</p>;

    // 视频结果
    if (nodeResult.type === 'video') {
      return (
        <div>
          <video
            src={nodeResult.url}
            controls
            style={{ width: '100%', borderRadius: '8px', background: '#000' }}
          />
          <div style={{ marginTop: '15px' }}>
            <p><strong>时长:</strong> {nodeResult.duration} 秒</p>
            <p><strong>帧率:</strong> {nodeResult.fps} FPS</p>
            <p><strong>下载:</strong> <a href={nodeResult.url} target="_blank" rel="noopener noreferrer">点击下载</a></p>
          </div>
        </div>
      );
    }

    // 图片结果
    if (nodeResult.type === 'image') {
      return (
        <div>
          <img
            src={nodeResult.url}
            alt="生成结果"
            style={{ width: '100%', borderRadius: '8px' }}
          />
          <div style={{ marginTop: '15px' }}>
            <p><strong>尺寸:</strong> {nodeResult.width} × {nodeResult.height}</p>
            <p><strong>下载:</strong> <a href={nodeResult.url} target="_blank" rel="noopener noreferrer">点击下载</a></p>
          </div>
        </div>
      );
    }

    // 其他类型
    return (
      <pre style={{
        background: '#f5f5f5',
        padding: '15px',
        borderRadius: '8px',
        overflow: 'auto',
      }}>
        {JSON.stringify(nodeResult, null, 2)}
      </pre>
    );
  };

  return (
    <div style={styles.overlay}>
      <div style={styles.panel}>
        <div style={styles.header}>
          <h3 style={styles.title}>执行结果</h3>
          <button onClick={onClose} style={styles.closeButton}>×</button>
        </div>

        <div style={styles.content}>
          {/* 执行状态概览 */}
          <div style={styles.section}>
            <h4 style={styles.sectionTitle}>执行状态</h4>
            <div style={{
              display: 'inline-block',
              padding: '5px 15px',
              borderRadius: '20px',
              fontSize: '14px',
              fontWeight: 600,
              background: result.status === 'SUCCESS' ? '#d4edda' :
                         result.status === 'FAILED' ? '#f8d7da' : '#fff3cd',
              color: result.status === 'SUCCESS' ? '#155724' :
                     result.status === 'FAILED' ? '#721c24' : '#856404',
            }}>
              {result.status === 'SUCCESS' && '✅ 成功'}
              {result.status === 'FAILED' && '❌ 失败'}
              {result.status === 'RUNNING' && '⏳ 执行中'}
              {result.status === 'PENDING' && '⏸️ 等待中'}
            </div>
            <p style={{ marginTop: '10px', fontSize: '13px', color: '#666' }}>
              执行 ID: {result.executionId}
            </p>
          </div>

          {/* 节点状态列表 */}
          <div style={styles.section}>
            <h4 style={styles.sectionTitle}>节点执行结果</h4>
            <div style={styles.nodeList}>
              {Object.entries(result.nodeStates).map(([nodeId, state]) => (
                <div
                  key={nodeId}
                  onClick={() => setSelectedNode(nodeId)}
                  style={{
                    ...styles.nodeItem,
                    background: selectedNode === nodeId ? '#f0f0f0' : 'white',
                  }}
                >
                  <div style={styles.nodeInfo}>
                    <span style={styles.nodeId}>{nodeId}</span>
                    <span style={{
                      ...styles.nodeStatus,
                      background: state.status === 'SUCCESS' ? '#d4edda' :
                                 state.status === 'FAILED' ? '#f8d7da' : '#fff3cd',
                      color: state.status === 'SUCCESS' ? '#155724' :
                             state.status === 'FAILED' ? '#721c24' : '#856404',
                    }}>
                      {state.status}
                    </span>
                  </div>
                  {state.result && (
                    <div style={styles.nodeResult}>
                      {state.result.type === 'video' && '🎬 视频'}
                      {state.result.type === 'image' && '🖼️ 图片'}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* 选中节点的详细结果 */}
          {selectedNode && (
            <div style={styles.section}>
              <h4 style={styles.sectionTitle}>
                节点详情：{selectedNode}
              </h4>
              <div style={styles.resultContainer}>
                {renderResult()}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  overlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    background: 'rgba(0, 0, 0, 0.5)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 2000,
  },
  panel: {
    background: 'white',
    borderRadius: '12px',
    width: '700px',
    maxHeight: '80vh',
    display: 'flex',
    flexDirection: 'column',
    boxShadow: '0 10px 40px rgba(0,0,0,0.2)',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '20px',
    borderBottom: '1px solid #e0e0e0',
  },
  title: {
    margin: 0,
    fontSize: '18px',
    fontWeight: 600,
  },
  closeButton: {
    background: 'none',
    border: 'none',
    fontSize: '24px',
    cursor: 'pointer',
    color: '#666',
  },
  content: {
    flex: 1,
    overflow: 'auto',
    padding: '20px',
  },
  section: {
    marginBottom: '25px',
  },
  sectionTitle: {
    margin: '0 0 10px 0',
    fontSize: '14px',
    fontWeight: 600,
    color: '#333',
  },
  nodeList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
  },
  nodeItem: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '12px',
    border: '1px solid #e0e0e0',
    borderRadius: '8px',
    cursor: 'pointer',
    transition: 'background 0.2s',
  },
  nodeInfo: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
  },
  nodeId: {
    fontWeight: 500,
    fontSize: '14px',
  },
  nodeStatus: {
    padding: '3px 10px',
    borderRadius: '12px',
    fontSize: '12px',
    fontWeight: 600,
  },
  nodeResult: {
    fontSize: '13px',
    color: '#666',
  },
  resultContainer: {
    background: '#f9f9f9',
    padding: '15px',
    borderRadius: '8px',
  },
};
