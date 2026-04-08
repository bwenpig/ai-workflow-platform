import { memo, useCallback } from 'react'
import { Handle, Position, NodeProps, useReactFlow } from '@xyflow/react'
import { Card, Tag, Typography, Button, Progress, Space } from 'antd'
import { 
  CheckCircleOutlined, 
  CloseCircleOutlined,
  SyncOutlined,
  ClockCircleOutlined,
  ReloadOutlined,
  PlayCircleOutlined,
} from '@ant-design/icons'
import { useWorkflowStore, NodeStatus } from '../store/useWorkflowStore'

const { Text } = Typography

export interface TaskNodeData {
  label: string
  taskType?: 'script' | 'api' | 'llm' | 'image' | 'video'
  status?: NodeStatus
  executionTime?: number
  progress?: number
  retryCount?: number
  error?: string
}

function TaskNodeComponent({ id, data, selected }: NodeProps) {
  const { setNodes } = useReactFlow()
  const nodeExecutionInfo = useWorkflowStore((state) => 
    state.executionState?.nodeStates[id]
  )
  const updateNodeStatus = useWorkflowStore((state) => state.updateNodeStatus)
  const retryNode = useWorkflowStore((state) => state.retryNode)
  
  const status = nodeExecutionInfo?.status || data.status || 'pending'
  const executionTime = nodeExecutionInfo?.executionTime || data.executionTime
  const progress = nodeExecutionInfo?.result?.progress || data.progress || 0
  const retryCount = nodeExecutionInfo?.retryCount || data.retryCount || 0
  const error = nodeExecutionInfo?.error || data.error
  
  // 状态颜色映射 (F028 - 状态颜色)
  const getStatusColor = useCallback(() => {
    switch (status) {
      case 'success': return '#52c41a'  // 绿色
      case 'failed': return '#ff4d4f'   // 红色
      case 'running': return '#faad14'  // 黄色
      case 'skipped': return '#d9d9d9'  // 灰色
      default: return '#d9d9d9'         // 灰色
    }
  }, [status])
  
  // 边框样式 (F028 - 边框颜色表示状态)
  const getBorderStyle = useCallback(() => ({
    borderColor: selected ? '#1890ff' : getStatusColor(),
    borderWidth: selected ? 2 : 2,
    borderStyle: 'solid',
    transition: 'border-color 0.2s ease',
  }), [selected, status])
  
  // 处理重试 (F034 - 重试按钮)
  const handleRetry = useCallback(async (e: React.MouseEvent) => {
    e.stopPropagation()
    await retryNode(id)
  }, [id, retryNode])
  
  // 获取状态图标
  const getStatusIcon = useCallback(() => {
    switch (status) {
      case 'success':
        return <CheckCircleOutlined style={{ color: '#52c41a' }} />
      case 'failed':
        return <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
      case 'running':
        return <SyncOutlined spin style={{ color: '#faad14' }} />
      case 'skipped':
        return <ClockCircleOutlined style={{ color: '#999' }} />
      default:
        return <ClockCircleOutlined style={{ color: '#999' }} />
    }
  }, [status])
  
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
        <div style={{ fontSize: 18, marginRight: 8 }}>{getStatusIcon()}</div>
        <div style={{ fontWeight: 600, flex: 1, fontSize: 14 }}>
          {data.label || '任务节点'}
        </div>
        <Tag color={getStatusColor()} style={{ fontSize: 11 }}>
          {status}
        </Tag>
      </div>
      
      {/* 任务类型 */}
      {data.taskType && (
        <div style={{ marginBottom: 8 }}>
          <Tag 
            color={
              data.taskType === 'script' ? 'blue' :
              data.taskType === 'api' ? 'cyan' :
              data.taskType === 'llm' ? 'purple' :
              data.taskType === 'image' ? 'orange' :
              data.taskType === 'video' ? 'red' : 'default'
            }
            style={{ fontSize: 11 }}
          >
            {data.taskType.toUpperCase()}
          </Tag>
        </div>
      )}
      
      {/* 进度条 (F029 - 实时状态) */}
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
      
      {/* 执行时间显示 (F033 - 执行时间) */}
      {executionTime !== undefined && (
        <div style={{ fontSize: 11, color: '#999', marginBottom: 4 }}>
          ⏱️ 耗时：{executionTime}ms
        </div>
      )}
      
      {/* 重试次数 */}
      {retryCount > 0 && (
        <div style={{ fontSize: 11, color: '#faad14', marginBottom: 4 }}>
          🔄 重试：{retryCount} 次
        </div>
      )}
      
      {/* 错误信息 */}
      {error && status === 'failed' && (
        <div 
          style={{ 
            fontSize: 11, 
            color: '#ff4d4f', 
            backgroundColor: '#fff1f0',
            padding: '4px 8px',
            borderRadius: 4,
            marginBottom: 8,
            maxHeight: 60,
            overflow: 'hidden',
          }}
        >
          ⚠️ {error.slice(0, 100)}{error.length > 100 ? '...' : ''}
        </div>
      )}
      
      {/* 重试按钮 (F034 - 失败节点可点击重试) */}
      {status === 'failed' && (
        <Button
          size="small"
          type="primary"
          icon={<ReloadOutlined />}
          onClick={handleRetry}
          block
          danger
        >
          重试
        </Button>
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

export const TaskNode = memo(TaskNodeComponent)
