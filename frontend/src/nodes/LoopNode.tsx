import { memo } from 'react'
import { Handle, Position } from '@xyflow/react'
import { CheckCircleOutlined, CloseCircleOutlined, LoadingOutlined } from '@ant-design/icons'

interface LoopNodeData {
  label?: string;
  status?: string;
  config?: {
    items?: any[];
    itemVar?: string;
  };
}

export const LoopNode = memo(({ data }: { data: LoopNodeData }) => {
  const items = data?.config?.items
  const itemVar = data?.config?.itemVar || 'item'
  const count = Array.isArray(items) ? items.length : 0
  
  // 状态颜色
  const borderColor = 
    data.status === 'SUCCESS' ? '#52c41a' : 
    data.status === 'FAILED' ? '#ff4d4f' : 
    data.status === 'RUNNING' ? '#faad14' : '#9C27B0'
  
  return (
    <div 
      className="loop-node"
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
        background: '#f3e5f5',
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
      }}>
        <span className="node-icon">🔄</span>
        <span className="node-label" style={{ fontWeight: 600 }}>{data.label || '循环处理'}</span>
        {data.status && (
          <span style={{ marginLeft: 'auto' }}>
            {data.status === 'SUCCESS' && <CheckCircleOutlined style={{ color: '#52c41a' }} />}
            {data.status === 'FAILED' && <CloseCircleOutlined style={{ color: '#ff4d4f' }} />}
            {data.status === 'RUNNING' && <LoadingOutlined spin style={{ color: '#faad14' }} />}
          </span>
        )}
      </div>
      <div className="node-body" style={{ padding: '12px' }}>
        <span className="node-count" style={{ 
          display: 'block',
          fontSize: '12px', 
          color: count > 0 ? '#9C27B0' : '#999',
          fontWeight: count > 0 ? 500 : 400
        }}>
          {count > 0 ? `${count} 次循环` : '未配置'}
        </span>
        <span className="node-var" style={{ 
          display: 'block',
          fontSize: '11px', 
          color: '#999',
          marginTop: '4px'
        }}>变量: {itemVar}</span>
      </div>
      
      <Handle type="target" position={Position.Left} style={{ background: '#9C27B0' }} />
      <Handle type="source" position={Position.Right} style={{ background: '#9C27B0' }} />
    </div>
  )
})

LoopNode.displayName = 'LoopNode'