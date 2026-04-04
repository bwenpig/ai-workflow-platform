import { memo } from 'react'
import { Handle, Position } from '@xyflow/react'
import { CheckCircleOutlined, CloseCircleOutlined, LoadingOutlined } from '@ant-design/icons'

interface ConditionalNodeData {
  label?: string;
  status?: string;
  config?: {
    expression?: string;
  };
}

export const ConditionalNode = memo(({ data }: { data: ConditionalNodeData }) => {
  const expression = data?.config?.expression || '未配置'
  
  // 状态颜色
  const borderColor = 
    data.status === 'SUCCESS' ? '#52c41a' : 
    data.status === 'FAILED' ? '#ff4d4f' : 
    data.status === 'RUNNING' ? '#faad14' : '#FF9800'
  
  return (
    <div 
      className="conditional-node"
      style={{
        border: `2px solid ${borderColor}`,
        borderRadius: '12px',
        minWidth: '180px',
        background: '#fff',
        overflow: 'hidden',
      }}
    >
      <div className="node-header" style={{ 
        padding: '10px 12px', 
        background: '#fff7e6',
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
      }}>
        <span className="node-icon">🔀</span>
        <span className="node-label" style={{ fontWeight: 600 }}>{data.label || '条件判断'}</span>
        {data.status && (
          <span style={{ marginLeft: 'auto' }}>
            {data.status === 'SUCCESS' && <CheckCircleOutlined style={{ color: '#52c41a' }} />}
            {data.status === 'FAILED' && <CloseCircleOutlined style={{ color: '#ff4d4f' }} />}
            {data.status === 'RUNNING' && <LoadingOutlined spin style={{ color: '#faad14' }} />}
          </span>
        )}
      </div>
      <div className="node-body" style={{ padding: '12px' }}>
        <span className="node-expression" style={{ 
          display: 'block',
          fontSize: '12px', 
          color: '#666',
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap'
        }} title={expression}>
          {expression}
        </span>
      </div>
      
      <Handle type="target" position={Position.Left} style={{ background: '#FF9800' }} />
      <Handle type="source" position={Position.Right} id="true" style={{ background: '#52c41a' }} title="True" />
      <Handle type="source" position={Position.Right} id="false" style={{ background: '#ff4d4f' }} title="False" />
    </div>
  )
})

ConditionalNode.displayName = 'ConditionalNode'