import { memo } from 'react'
import { Handle, Position } from '@xyflow/react'
import { CheckCircleOutlined, CloseCircleOutlined, LoadingOutlined, RobotOutlined } from '@ant-design/icons'

interface LLMNodeData {
  label?: string;
  status?: string;
  config?: {
    model?: string;
    systemPrompt?: string;
    userPrompt?: string;
    temperature?: number;
    maxTokens?: number;
  };
}

export const LLMNode = memo(({ data }: { data: LLMNodeData }) => {
  const model = data?.config?.model || 'qwen-plus'
  const systemPrompt = data?.config?.systemPrompt || ''
  const userPrompt = data?.config?.userPrompt || ''
  
  // 状态颜色
  const borderColor = 
    data.status === 'SUCCESS' ? '#52c41a' : 
    data.status === 'FAILED' ? '#ff4d4f' : 
    data.status === 'RUNNING' ? '#faad14' : '#8b5cf6'
  
  // 模型名称映射
  const modelNames: Record<string, string> = {
    'qwen-plus': 'Qwen-Plus',
    'qwen-max': 'Qwen-Max',
    'qwen-turbo': 'Qwen-Turbo',
    'qwen-long': 'Qwen-Long',
    'qwen3.6-plus': 'Qwen3.6-Plus',
    'minimax-m2': 'MiniMax-M2',
    'minimax-m2.5': 'MiniMax-M2.5',
    'minimax-m1': 'MiniMax-M1',
    'kimi-k2.5': 'Kimi-K2.5',
    'kimi-latest': 'Kimi',
    'kimi-plus': 'Kimi Plus',
    'gpt-4': 'GPT-4',
    'gpt-3.5-turbo': 'GPT-3.5',
    'claude-3': 'Claude 3',
  }
  
  return (
    <div 
      className="llm-node"
      style={{
        border: `2px solid ${borderColor}`,
        borderRadius: '12px',
        minWidth: '200px',
        background: '#fff',
        overflow: 'hidden',
        boxShadow: '0 2px 8px rgba(139, 92, 246, 0.15)',
      }}
    >
      <div className="node-header" style={{ 
        padding: '10px 12px', 
        background: 'linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%)',
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
      }}>
        <RobotOutlined style={{ color: '#8b5cf6', fontSize: '16px' }} />
        <span className="node-label" style={{ fontWeight: 600, color: '#5b21b6' }}>
          {data.label || 'LLM 节点'}
        </span>
        {data.status && (
          <span style={{ marginLeft: 'auto' }}>
            {data.status === 'SUCCESS' && <CheckCircleOutlined style={{ color: '#52c41a' }} />}
            {data.status === 'FAILED' && <CloseCircleOutlined style={{ color: '#ff4d4f' }} />}
            {data.status === 'RUNNING' && <LoadingOutlined spin style={{ color: '#faad14' }} />}
          </span>
        )}
      </div>
      
      <div className="node-body" style={{ padding: '12px' }}>
        {/* 模型标签 */}
        <span className="node-model" style={{ 
          background: '#ede9fe', 
          padding: '2px 8px', 
          borderRadius: '4px',
          fontSize: '12px',
          color: '#7c3aed',
          display: 'inline-block',
          marginBottom: '8px'
        }}>
          {modelNames[model] || model}
        </span>
        
        {/* 提示词预览 */}
        {systemPrompt && (
          <div style={{ marginTop: '4px' }}>
            <span style={{ fontSize: '11px', color: '#9ca3af' }}>System:</span>
            <div style={{ 
              fontSize: '11px', 
              color: '#6b7280',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
              maxWidth: '180px'
            }} title={systemPrompt}>
              {systemPrompt}
            </div>
          </div>
        )}
        
        {userPrompt && (
          <div style={{ marginTop: '4px' }}>
            <span style={{ fontSize: '11px', color: '#9ca3af' }}>User:</span>
            <div style={{ 
              fontSize: '11px', 
              color: '#6b7280',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
              maxWidth: '180px'
            }} title={userPrompt}>
              {userPrompt}
            </div>
          </div>
        )}
        
        {!systemPrompt && !userPrompt && (
          <div style={{ 
            fontSize: '12px', 
            color: '#9ca3af',
            fontStyle: 'italic'
          }}>
            点击配置提示词
          </div>
        )}
        
        {/* 参数指示器 */}
        <div style={{ 
          display: 'flex', 
          gap: '8px', 
          marginTop: '8px',
          fontSize: '10px',
          color: '#9ca3af'
        }}>
          {data?.config?.temperature !== undefined && (
            <span title="Temperature">🌡️ {data.config.temperature}</span>
          )}
          {data?.config?.maxTokens !== undefined && (
            <span title="Max Tokens">📏 {data.config.maxTokens}</span>
          )}
        </div>
      </div>
      
      <Handle type="target" position={Position.Left} style={{ background: '#8b5cf6' }} />
      <Handle type="source" position={Position.Right} style={{ background: '#8b5cf6' }} />
    </div>
  )
})

LLMNode.displayName = 'LLMNode'