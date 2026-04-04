import { memo } from 'react'
import { Handle, Position } from '@xyflow/react'
import { CheckCircleOutlined, CloseCircleOutlined, LoadingOutlined } from '@ant-design/icons'

interface HttpRequestNodeData {
  label?: string;
  status?: string;
  config?: {
    url?: string;
    method?: string;
  };
}

export const HttpRequestNode = memo(({ data }: { data: HttpRequestNodeData }) => {
  const url = data?.config?.url || '未配置'
  const method = data?.config?.method || 'GET'
  
  // 状态颜色
  const borderColor = 
    data.status === 'SUCCESS' ? '#52c41a' : 
    data.status === 'FAILED' ? '#ff4d4f' : 
    data.status === 'RUNNING' ? '#faad14' : '#4CAF50'
  
  return (
    <div 
      className="http-request-node"
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
        background: '#f8fafc',
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
      }}>
        <span className="node-icon">🌐</span>
        <span className="node-label" style={{ fontWeight: 600 }}>{data.label || 'HTTP 请求'}</span>
        {data.status && (
          <span style={{ marginLeft: 'auto' }}>
            {data.status === 'SUCCESS' && <CheckCircleOutlined style={{ color: '#52c41a' }} />}
            {data.status === 'FAILED' && <CloseCircleOutlined style={{ color: '#ff4d4f' }} />}
            {data.status === 'RUNNING' && <LoadingOutlined spin style={{ color: '#faad14' }} />}
          </span>
        )}
      </div>
      <div className="node-body" style={{ padding: '12px' }}>
        <span className="node-method" style={{ 
          background: '#e6f7ff', 
          padding: '2px 8px', 
          borderRadius: '4px',
          fontSize: '12px',
          color: '#1890ff'
        }}>{method}</span>
        <span className="node-url" title={url} style={{ 
          display: 'block', 
          marginTop: '8px', 
          fontSize: '12px', 
          color: '#666',
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap'
        }}>{url}</span>
      </div>
      
      <Handle type="target" position={Position.Left} style={{ background: '#4CAF50' }} />
      <Handle type="source" position={Position.Right} style={{ background: '#4CAF50' }} />
    </div>
  )
})

HttpRequestNode.displayName = 'HttpRequestNode'