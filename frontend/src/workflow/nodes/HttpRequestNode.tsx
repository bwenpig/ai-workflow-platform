import React, { memo } from 'react'
import { Handle, Position, NodeProps } from '@xyflow/react'
import { Input, Card } from 'antd'

const HttpRequestNode: React.FC<NodeProps> = ({ data, selected }) => {
  const { url, method = 'GET', timeout = 5000 } = data

  return (
    <Card 
      size="small" 
      style={{ 
        width: 150, 
        borderColor: selected ? '#1890ff' : '#d9d9d9',
        borderWidth: selected ? 2 : 1 
      }}
    >
      <div style={{ fontWeight: 'bold', marginBottom: 8 }}>🌐 HTTP 请求</div>
      <div style={{ fontSize: 12, color: '#666' }}>
        <div>方法: <strong>{method}</strong></div>
        <div style={{ 
          overflow: 'hidden', 
          textOverflow: 'ellipsis', 
          whiteSpace: 'nowrap',
          maxWidth: 110 
        }}>
          URL: {url || '未设置'}
        </div>
        <div>超时: {timeout}ms</div>
      </div>
      <Handle type="target" position={Position.Top} />
      <Handle type="source" position={Position.Bottom} />
    </Card>
  )
}

export default memo(HttpRequestNode)
