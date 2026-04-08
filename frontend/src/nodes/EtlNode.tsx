import { memo } from 'react'
import { Handle, Position } from '@xyflow/react'
import { CheckCircleOutlined, CloseCircleOutlined, LoadingOutlined } from '@ant-design/icons'

interface EtlNodeData {
  label?: string
  status?: string
  config?: {
    sources?: string[]
  }
}

export const EtlNode = memo(({ data }: { data: EtlNodeData }) => {
  const sources = data?.config?.sources || []

  const borderColor =
    data.status === 'SUCCESS' ? '#52c41a' :
    data.status === 'FAILED' ? '#ff4d4f' :
    data.status === 'RUNNING' ? '#faad14' : '#06b6d4'

  return (
    <div
      className="etl-node"
      style={{
        border: `2px solid ${borderColor}`,
        borderRadius: '12px',
        minWidth: '200px',
        background: '#fff',
        overflow: 'hidden',
        boxShadow: '0 2px 8px rgba(6, 182, 212, 0.15)',
      }}
    >
      <div
        className="node-header"
        style={{
          padding: '10px 12px',
          background: 'linear-gradient(135deg, #ecfeff 0%, #cffafe 100%)',
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
        }}
      >
        <span style={{ fontSize: '16px' }}>🔄</span>
        <span className="node-label" style={{ fontWeight: 600, color: '#0e7490' }}>
          {data.label || 'ETL 数据清洗'}
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
        <div
          style={{
            fontSize: '12px',
            color: '#64748b',
            marginBottom: '8px',
          }}
        >
          归一化多数据源内容
        </div>

        {/* 统一字段预览 */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
          {['title', 'url', 'source', 'publishedAt', 'summary', 'tags'].map((field) => (
            <span
              key={field}
              style={{
                background: '#ecfeff',
                padding: '1px 6px',
                borderRadius: '4px',
                fontSize: '10px',
                color: '#0891b2',
              }}
            >
              {field}
            </span>
          ))}
        </div>

        {/* 数据源列表 */}
        {sources.length > 0 && (
          <div style={{ marginTop: '8px' }}>
            <span style={{ fontSize: '11px', color: '#9ca3af' }}>数据源:</span>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px', marginTop: '2px' }}>
              {sources.map((src: string, idx: number) => (
                <span
                  key={idx}
                  style={{
                    background: '#f0fdf4',
                    padding: '1px 6px',
                    borderRadius: '4px',
                    fontSize: '10px',
                    color: '#16a34a',
                  }}
                >
                  {src}
                </span>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* 多输入端口（左侧） */}
      <Handle type="target" position={Position.Left} style={{ background: '#06b6d4' }} />
      <Handle type="source" position={Position.Right} style={{ background: '#06b6d4' }} />
    </div>
  )
})

EtlNode.displayName = 'EtlNode'
