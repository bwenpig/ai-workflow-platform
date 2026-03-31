import { memo, useCallback } from 'react'
import { Handle, Position, NodeProps, useReactFlow } from '@xyflow/react'
import { Card, Tag, Typography, Space } from 'antd'
import { 
  MergeCellsOutlined, 
  CheckCircleOutlined, 
  CloseCircleOutlined,
  SyncOutlined 
} from '@ant-design/icons'
import { useWorkflowStore, NodeStatus } from '../store/useWorkflowStore'

const { Text } = Typography

export interface MergeNodeData {
  label: string
  mergeStrategy?: 'all' | 'any' | 'first'
  inputCount?: number
  status?: NodeStatus
  executionTime?: number
  completedInputs?: number
  totalInputs?: number
}

function MergeNodeComponent({ id, data, selected }: NodeProps) {
  const { setNodes } = useReactFlow()
  const nodeExecutionInfo = useWorkflowStore((state) => 
    state.executionState?.nodeStates[id]
  )
  
  const status = nodeExecutionInfo?.status || data.status || 'pending'
  const executionTime = nodeExecutionInfo?.executionTime || data.executionTime
  const completedInputs = nodeExecutionInfo?.result?.completedInputs || data.completedInputs || 0
  const totalInputs = data.totalInputs || data.inputCount || 2
  
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
  
  // 处理合并策略变更
  const handleMergeStrategyChange = useCallback((value: 'all' | 'any' | 'first') => {
    setNodes((nodes) =>
      nodes.map((node) => {
        if (node.id === id) {
          return {
            ...node,
            data: {
              ...node.data,
              mergeStrategy: value,
            },
          }
        }
        return node
      })
    )
  }, [id, setNodes])
  
  // 合并策略标签
  const getMergeStrategyLabel = (strategy?: string) => {
    switch (strategy) {
      case 'all': return '等待全部'
      case 'any': return '任一完成'
      case 'first': return '首个完成'
      default: return '等待全部'
    }
  }
  
  return (
    <Card
      size="small"
      style={{
        width: 200,
        ...getBorderStyle(),
        backgroundColor: status === 'running' ? '#fffbe6' : '#e6f7ff',
        boxShadow: selected 
          ? `0 4px 12px ${getStatusColor()}40` 
          : '0 2px 8px rgba(0,0,0,0.1)',
        transition: 'all 0.2s ease',
      }}
    >
      {/* 节点头部 */}
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: 12 }}>
        <MergeCellsOutlined 
          style={{ 
            fontSize: 18, 
            marginRight: 8, 
            color: getStatusColor() 
          }} 
        />
        <div style={{ fontWeight: 600, flex: 1, fontSize: 14 }}>
          {data.label || '合并节点'}
        </div>
        {status && (
          <span>
            {status === 'success' && <CheckCircleOutlined style={{ color: '#52c41a' }} />}
            {status === 'failed' && <CloseCircleOutlined style={{ color: '#ff4d4f' }} />}
            {status === 'running' && <SyncOutlined spin style={{ color: '#faad14' }} />}
          </span>
        )}
      </div>
      
      {/* 合并策略 */}
      <div style={{ marginBottom: 8 }}>
        <Text type="secondary" style={{ fontSize: 12 }}>汇聚策略</Text>
        <div style={{ marginTop: 4 }}>
          <Tag 
            color="blue" 
            style={{ fontSize: 12, cursor: 'pointer' }}
            onClick={() => {
              const strategies: Array<'all' | 'any' | 'first'> = ['all', 'any', 'first']
              const currentIndex = strategies.indexOf(data.mergeStrategy || 'all')
              const nextStrategy = strategies[(currentIndex + 1) % strategies.length]
              handleMergeStrategyChange(nextStrategy)
            }}
          >
            {getMergeStrategyLabel(data.mergeStrategy)}
          </Tag>
        </div>
      </div>
      
      {/* 输入完成状态 (F026 - 合并节点汇聚输入) */}
      {status === 'running' && (
        <div style={{ marginBottom: 8 }}>
          <Text type="secondary" style={{ fontSize: 12 }}>
            已汇聚 {completedInputs}/{totalInputs}
          </Text>
          <div style={{ marginTop: 4, display: 'flex', gap: 4 }}>
            {Array.from({ length: totalInputs }).map((_, idx) => (
              <div
                key={idx}
                style={{
                  width: 20,
                  height: 20,
                  borderRadius: 4,
                  backgroundColor: idx < completedInputs ? '#52c41a' : '#d9d9d9',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 10,
                  color: '#fff',
                }}
              >
                {idx + 1}
              </div>
            ))}
          </div>
        </div>
      )}
      
      {/* 执行时间显示 (F033) */}
      {executionTime !== undefined && (
        <div style={{ fontSize: 11, color: '#999' }}>
          ⏱️ {executionTime}ms
        </div>
      )}
      
      {/* 多个输入端口 (F026 - 多输入汇聚) */}
      {Array.from({ length: Math.max(2, totalInputs) }).map((_, idx) => (
        <Handle
          key={`input-${idx}`}
          type="target"
          position={Position.Top}
          id={`input-${idx}`}
          style={{ 
            background: '#555', 
            width: 10, 
            height: 10,
            border: '2px solid #fff',
            left: `${20 + (idx * (60 / Math.max(2, totalInputs)))}%`,
          }}
        />
      ))}
      
      {/* 单一输出端口 */}
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

export const MergeNode = memo(MergeNodeComponent)
