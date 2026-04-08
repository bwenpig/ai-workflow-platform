import { memo, useCallback } from 'react'
import { Handle, Position, NodeProps, useReactFlow } from '@xyflow/react'
import { Card, Tag, Input, Select, Space, Typography } from 'antd'
import { BranchesOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons'
import { useWorkflowStore, NodeStatus } from '../store/useWorkflowStore'

const { Text } = Typography

export interface IfNodeData {
  label: string
  condition?: string
  operator?: 'equals' | 'notEquals' | 'contains' | 'greaterThan' | 'lessThan'
  variable?: string
  value?: string
  status?: NodeStatus
  executionTime?: number
}

function IfNodeComponent({ id, data, selected }: NodeProps) {
  const { setNodes } = useReactFlow()
  const updateNodeStatus = useWorkflowStore((state) => state.updateNodeStatus)
  const nodeExecutionInfo = useWorkflowStore((state) => 
    state.executionState?.nodeStates[id]
  )
  
  const status = nodeExecutionInfo?.status || data.status || 'pending'
  const executionTime = nodeExecutionInfo?.executionTime || data.executionTime
  
  // 状态颜色映射
  const getStatusColor = useCallback(() => {
    switch (status) {
      case 'success': return '#52c41a'
      case 'failed': return '#ff4d4f'
      case 'running': return '#faad14'
      case 'skipped': return '#d9d9d9'
      default: return '#d9d9d9'
    }
  }, [status])
  
  // 边框样式
  const getBorderStyle = useCallback(() => ({
    borderColor: selected ? '#1890ff' : getStatusColor(),
    borderWidth: selected ? 2 : 2,
    borderStyle: 'solid',
  }), [selected, status])
  
  // 处理条件变更
  const handleConditionChange = useCallback((field: string, value: any) => {
    setNodes((nodes) =>
      nodes.map((node) => {
        if (node.id === id) {
          return {
            ...node,
            data: {
              ...node.data,
              [field]: value,
            },
          }
        }
        return node
      })
    )
  }, [id, setNodes])
  
  return (
    <Card
      size="small"
      style={{
        width: 150,
        ...getBorderStyle(),
        backgroundColor: status === 'running' ? '#fffbe6' : '#fff',
        boxShadow: selected 
          ? `0 4px 12px ${getStatusColor()}40` 
          : '0 2px 8px rgba(0,0,0,0.1)',
        transition: 'all 0.2s ease',
      }}
    >
      {/* 节点头部 */}
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: 12 }}>
        <BranchesOutlined 
          style={{ 
            fontSize: 18, 
            marginRight: 8, 
            color: getStatusColor() 
          }} 
        />
        <div style={{ fontWeight: 600, flex: 1, fontSize: 14 }}>
          {data.label || '条件分支'}
        </div>
        {status && (
          <span>
            {status === 'success' && <CheckCircleOutlined style={{ color: '#52c41a' }} />}
            {status === 'failed' && <CloseCircleOutlined style={{ color: '#ff4d4f' }} />}
            {status === 'running' && <span style={{ color: '#faad14' }}>⏳</span>}
          </span>
        )}
      </div>
      
      {/* 条件配置 */}
      <Space direction="vertical" size="small" style={{ width: '100%', marginBottom: 8 }}>
        <div>
          <Text type="secondary" style={{ fontSize: 12 }}>变量</Text>
          <Input
            size="small"
            placeholder="例如：input.value"
            value={data.variable || ''}
            onChange={(e) => handleConditionChange('variable', e.target.value)}
            style={{ fontSize: 12 }}
          />
        </div>
        
        <div style={{ display: 'flex', gap: 4 }}>
          <Select
            size="small"
            value={data.operator || 'equals'}
            onChange={(value) => handleConditionChange('operator', value)}
            style={{ width: 80, fontSize: 11 }}
            options={[
              { value: 'equals', label: '=' },
              { value: 'notEquals', label: '≠' },
              { value: 'contains', label: '包含' },
              { value: 'greaterThan', label: '>' },
              { value: 'lessThan', label: '<' },
            ]}
          />
          <Input
            size="small"
            placeholder="值"
            value={data.value || ''}
            onChange={(e) => handleConditionChange('value', e.target.value)}
            style={{ fontSize: 12, flex: 1 }}
          />
        </div>
        
        {data.condition && (
          <div
            style={{
              fontSize: 11,
              backgroundColor: '#f5f5f5',
              padding: '4px 8px',
              borderRadius: 4,
              fontFamily: 'monospace',
              color: '#666',
            }}
          >
            {data.condition}
          </div>
        )}
      </Space>
      
      {/* 执行时间显示 (F033) */}
      {executionTime !== undefined && (
        <div style={{ fontSize: 11, color: '#999', marginTop: 8 }}>
          ⏱️ {executionTime}ms
        </div>
      )}
      
      {/* 输入端口 */}
      <Handle
        type="target"
        position={Position.Top}
        style={{ 
          background: '#555', 
          width: 10, 
          height: 10,
          border: '2px solid #fff',
        }}
      />
      
      {/* True 输出端口 (F024 - 多输出端口) */}
      <Handle
        type="source"
        position={Position.Right}
        id="true"
        style={{ 
          background: '#52c41a', 
          width: 10, 
          height: 10,
          border: '2px solid #fff',
          top: '40%',
        }}
      />
      <div 
        style={{ 
          position: 'absolute', 
          right: -35, 
          top: '35%',
          fontSize: 11,
          color: '#52c41a',
          fontWeight: 500,
        }}
      >
        True
      </div>
      
      {/* False 输出端口 (F024 - 多输出端口) */}
      <Handle
        type="source"
        position={Position.Right}
        id="false"
        style={{ 
          background: '#ff4d4f', 
          width: 10, 
          height: 10,
          border: '2px solid #fff',
          top: '70%',
        }}
      />
      <div 
        style={{ 
          position: 'absolute', 
          right: -35, 
          top: '65%',
          fontSize: 11,
          color: '#ff4d4f',
          fontWeight: 500,
        }}
      >
        False
      </div>
    </Card>
  )
}

export const IfNode = memo(IfNodeComponent)
