import React, { memo } from 'react'
import { Handle, Position, NodeProps } from '@xyflow/react'
import { Card } from 'antd'

const LoopNode: React.FC<NodeProps> = ({ data, selected }) => {
  const { items = [], itemVar = 'item', concurrency = 1 } = data
  const itemCount = Array.isArray(items) ? items.length : 0

  return (
    <Card 
      size="small" 
      style={{ 
        width: 200, 
        borderColor: selected ? '#1890ff' : '#d9d9d9',
        borderWidth: selected ? 2 : 1 
      }}
    >
      <div style={{ fontWeight: 'bold', marginBottom: 8 }}>🔄 循环处理</div>
      <div style={{ fontSize: 12, color: '#666' }}>
        <div>项目数: <strong>{itemCount}</strong></div>
        <div>变量: {itemVar}</div>
        <div>并发: {concurrency}</div>
      </div>
      <Handle type="target" position={Position.Top} />
      <Handle type="source" position={Position.Bottom} />
    </Card>
  )
}

export default memo(LoopNode)
