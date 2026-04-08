import React, { memo } from 'react'
import { Handle, Position, NodeProps } from '@xyflow/react'
import { Card, Tag } from 'antd'

export interface WxPushNodeData {
  label?: string
  to?: string
  content?: string
  silent?: boolean
}

const WxPushNode: React.FC<NodeProps> = ({ data, selected }) => {
  const {
    to = '',
    content = '',
    silent = false,
  } = data as WxPushNodeData

  const truncatedContent = content.length > 20 ? content.slice(0, 20) + '...' : content
  const receiver = to || '当前用户'

  return (
    <Card
      size="small"
      style={{
        width: 160,
        borderColor: selected ? '#1890ff' : '#52c41a',
        borderWidth: selected ? 2 : 1,
      }}
    >
      <div style={{ fontWeight: 'bold', marginBottom: 8, display: 'flex', alignItems: 'center', gap: 4 }}>
        <span style={{ fontSize: 16 }}>💬</span>
        <span>微信推送</span>
      </div>
      <div style={{ fontSize: 12, color: '#666' }}>
        <div>
          接收者: <Tag color="green" style={{ fontSize: 11 }}>{receiver}</Tag>
        </div>
        <div style={{
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
          maxWidth: 120,
        }}>
          内容: {truncatedContent || '未设置'}
        </div>
        {silent && (
          <div>
            <Tag color="default" style={{ fontSize: 11 }}>🔇 静默</Tag>
          </div>
        )}
      </div>
      <Handle type="target" position={Position.Top} />
      <Handle type="source" position={Position.Bottom} />
    </Card>
  )
}

export default memo(WxPushNode)
