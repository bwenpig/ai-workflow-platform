import React, { memo } from 'react'
import { Handle, Position, NodeProps } from '@xyflow/react'
import { Card, Tag } from 'antd'
import { MailOutlined } from '@ant-design/icons'

export interface EmailNodeData {
  label?: string
  to?: string | string[]
  cc?: string | string[]
  subject?: string
  body?: string
  from?: string
  attachments?: string[]
}

const EmailNode: React.FC<NodeProps> = ({ data, selected }) => {
  const {
    to = '',
    subject = '',
    from = '',
  } = data as EmailNodeData

  const recipientCount = Array.isArray(to) ? to.length : (to ? 1 : 0)
  const truncatedSubject = subject.length > 18 ? subject.slice(0, 18) + '...' : subject

  return (
    <Card
      size="small"
      style={{
        width: 160,
        borderColor: selected ? '#1890ff' : '#d48806',
        borderWidth: selected ? 2 : 1,
      }}
    >
      <div style={{ fontWeight: 'bold', marginBottom: 8, display: 'flex', alignItems: 'center', gap: 4 }}>
        <MailOutlined style={{ color: '#d48806' }} />
        <span>📧 邮件发送</span>
      </div>
      <div style={{ fontSize: 12, color: '#666' }}>
        <div>
          收件人: <Tag color="orange" style={{ fontSize: 11 }}>{recipientCount} 人</Tag>
        </div>
        <div style={{
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
          maxWidth: 120,
        }}>
          主题: {truncatedSubject || '未设置'}
        </div>
        {from && (
          <div style={{
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
            maxWidth: 120,
          }}>
            发件人: {from}
          </div>
        )}
      </div>
      <Handle type="target" position={Position.Top} />
      <Handle type="source" position={Position.Bottom} />
    </Card>
  )
}

export default memo(EmailNode)
