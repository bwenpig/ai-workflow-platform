import { memo, useCallback } from 'react'
import { Handle, Position, NodeProps, useReactFlow } from '@xyflow/react'
import { Card, Tag, Progress, Typography } from 'antd'
import { AppstoreOutlined, CheckCircleOutlined, CloseCircleOutlined, SyncOutlined } from '@ant-design/icons'
import { useWorkflowStore, NodeStatus } from '../store/useWorkflowStore'

const { Text } = Typography

export interface ParallelNodeData {
  label: string
  parallelCount?: number
  branchCount?: number
  status?: NodeStatus
  executionTime?: number
  progress?: number
}

function ParallelNodeComponent({ id, data, selected }: NodeProps) {
  const { setNodes } = useReactFlow()
  const nodeExecutionInfo = useWorkflowStore((state) => 
    state.executionState?.nodeStates[id]
  )
  
  const status = nodeExecutionInfo?.status || data.status || 'pending'
  const executionTime = nodeExecutionInfo?.executionTime || data.executionTime
  const progress = nodeExecutionInfo?.result?.progress || data.progress || 0
  
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
  
  // 处理并行数量变更
  const handleParallelCountChange = useCallback((value: number) => {
    setNodes((nodes) =>
      nodes.map((node) => {
        if (node.id === id) {
          return {
            ...node,
            data: {
              ...node.data,
              parallelCount: value,
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
        width: 200,
        ...getBorderStyle(),
        backgroundColor: status === 'running' ? '#fffbe6' : '#f6ffed',
        boxShadow: selected 
          ? `0 4px 12px ${getStatusColor()}40` 
          : '0 2px 8px rgba(0,0,0,0.1)',
        transition: 'all 0.2s ease',
      }}
    >
      {/* 节点头部 */}
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: 12 }}>
        <AppstoreOutlined 
          style={{ 
            fontSize: 18, 
            marginRight: 8, 
            color: getStatusColor() 
          }} 
        />
        <div style={{ fontWeight: 600, flex: 1, fontSize: 14 }}>
          {data.label || '并行执行'}
        </div>
        {status && (
          <span>
            {status === 'success' && <CheckCircleOutlined style={{ color: '#52c41a' }} />}
            {status === 'failed' && <CloseCircleOutlined style={{ color: '#ff4d4f' }} />}
            {status === 'running' && <SyncOutlined spin style={{ color: '#faad14' }} />}
          </span>
        )}
      </div>
      
      {/* 并行配置 */}
      <div style={{ marginBottom: 8 }}>
        <Text type="secondary" style={{ fontSize: 12 }}>并行分支数</Text>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginTop: 4 }}>
          <Tag color="green" style={{ fontSize: 12 }}>
            {data.parallelCount || data.branchCount || 2} 路并行
          </Tag>
          {status === 'running' && (
            <Tag color="processing" style={{ fontSize: 12 }}>
              执行中
            </Tag>
          )}
        </div>
      </div>
      
      {/* 进度条 (F025 - 并行执行状态) */}
      {status === 'running' && (
        <div style={{ marginBottom: 8 }}>
          <Progress 
            percent={progress} 
            size="small" 
            strokeColor="#faad14"
            format={(percent) => `${percent}%`}
          />
        </div>
      )}
      
      {/* 执行时间显示 (F033) */}
      {executionTime !== undefined && (
        <div style={{ fontSize: 11, color: '#999' }}>
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
      
      {/* 输出端口 */}
      <Handle
        type="source"
        position={Position.Bottom}
        style={{ 
          background: getStatusColor(), 
          width: 10, 
          height: 10,
          border: '2px solid #fff',
        }}
      />
    </Card>
  )
}

export const ParallelNode = memo(ParallelNodeComponent)
