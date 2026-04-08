import React, { memo } from 'react'
import { Handle, Position, NodeProps } from '@xyflow/react'
import { Card } from 'antd'

const ConditionalNode: React.FC<NodeProps> = ({ data, selected }) => {
  const { expression = '', value } = data

  return (
    <Card 
      size="small" 
      style={{ 
        width: 150, 
        borderColor: selected ? '#1890ff' : '#d9d9d9',
        borderWidth: selected ? 2 : 1 
      }}
    >
      <div style={{ fontWeight: 'bold', marginBottom: 8 }}>🔀 条件判断</div>
      <div style={{ fontSize: 12, color: '#666' }}>
        <div>表达式: <code>{expression || '未设置'}</code></div>
        <div>值: {value !== undefined ? String(value) : '-'}</div>
      </div>
      <Handle type="target" position={Position.Top} />
      <Handle type="source" position={Position.Bottom} id="true" style={{ left: '30%' }} />
      <Handle type="source" position={Position.Bottom} id="false" style={{ left: '70%' }} />
    </Card>
  )
}

export default memo(ConditionalNode)
