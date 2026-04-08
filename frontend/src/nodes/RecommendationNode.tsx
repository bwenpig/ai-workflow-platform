import { memo } from 'react'
import { Handle, Position } from '@xyflow/react'
import { CheckCircleOutlined, CloseCircleOutlined, LoadingOutlined, UserOutlined } from '@ant-design/icons'

interface RecommendationNodeData {
  label?: string
  status?: string
  config?: {
    model?: string
    userProfile?: {
      profession?: string
      businessFocus?: string
      interests?: string[]
    }
  }
}

export const RecommendationNode = memo(({ data }: { data: RecommendationNodeData }) => {
  const model = data?.config?.model || 'hunyuan-2.0-instruct'
  const userProfile = data?.config?.userProfile || {
    profession: 'Java 工程师 + 技术 Leader',
    businessFocus: 'AI 生图、AI 生视频',
    interests: ['数码爱好者', '游戏爱好者', '爱狗人士', '业余拳击运动'],
  }

  const borderColor =
    data.status === 'SUCCESS' ? '#52c41a' :
    data.status === 'FAILED' ? '#ff4d4f' :
    data.status === 'RUNNING' ? '#faad14' : '#f59e0b'

  return (
    <div
      className="recommendation-node"
      style={{
        border: `2px solid ${borderColor}`,
        borderRadius: '12px',
        minWidth: '220px',
        background: '#fff',
        overflow: 'hidden',
        boxShadow: '0 2px 8px rgba(245, 158, 11, 0.15)',
      }}
    >
      <div
        className="node-header"
        style={{
          padding: '10px 12px',
          background: 'linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%)',
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
        }}
      >
        <span style={{ fontSize: '16px' }}>🎯</span>
        <span className="node-label" style={{ fontWeight: 600, color: '#92400e' }}>
          {data.label || 'LLM 智能推荐'}
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
        <span
          style={{
            background: '#fef3c7',
            padding: '2px 8px',
            borderRadius: '4px',
            fontSize: '12px',
            color: '#d97706',
            display: 'inline-block',
            marginBottom: '8px',
          }}
        >
          🤖 {model}
        </span>

        {/* 用户画像摘要 */}
        <div style={{ marginTop: '4px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '4px', marginBottom: '4px' }}>
            <UserOutlined style={{ fontSize: '11px', color: '#9ca3af' }} />
            <span style={{ fontSize: '11px', color: '#9ca3af' }}>用户画像</span>
          </div>
          <div style={{ fontSize: '11px', color: '#6b7280', lineHeight: '1.4' }}>
            <div style={{
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
              maxWidth: '200px',
            }} title={userProfile.profession}>
              👔 {userProfile.profession}
            </div>
            <div style={{
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
              maxWidth: '200px',
            }} title={userProfile.businessFocus}>
              🎯 {userProfile.businessFocus}
            </div>
          </div>

          {/* 兴趣标签 */}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '3px', marginTop: '6px' }}>
            {(userProfile.interests || []).slice(0, 4).map((interest: string, idx: number) => (
              <span
                key={idx}
                style={{
                  background: '#fef3c7',
                  padding: '1px 5px',
                  borderRadius: '3px',
                  fontSize: '10px',
                  color: '#92400e',
                }}
              >
                {interest}
              </span>
            ))}
          </div>
        </div>
      </div>

      <Handle type="target" position={Position.Left} style={{ background: '#f59e0b' }} />
      <Handle type="source" position={Position.Right} style={{ background: '#f59e0b' }} />
    </div>
  )
})

RecommendationNode.displayName = 'RecommendationNode'
