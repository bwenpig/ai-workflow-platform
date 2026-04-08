import { memo, useCallback } from 'react'
import { Handle, Position, NodeProps, useReactFlow } from '@xyflow/react'
import { Card, Tag, Typography, Button, Collapse } from 'antd'
import { 
  AppstoreOutlined, 
  CheckCircleOutlined, 
  CloseCircleOutlined,
  SyncOutlined,
  UnorderedListOutlined,
  DownOutlined,
  RightOutlined,
} from '@ant-design/icons'
import { useWorkflowStore, NodeStatus } from '../store/useWorkflowStore'

const { Text } = Typography
const { Panel } = Collapse

export interface SubWorkflowNodeData {
  label: string
  subWorkflowId?: string
  subWorkflowName?: string
  status?: NodeStatus
  executionTime?: number
  collapsed?: boolean
}

function SubWorkflowNodeComponent({ id, data, selected }: NodeProps) {
  const { setNodes } = useReactFlow()
  const nodeExecutionInfo = useWorkflowStore((state) => 
    state.executionState?.nodeStates[id]
  )
  const expandedSubWorkflowId = useWorkflowStore((state) => state.expandedSubWorkflowId)
  const setExpandedSubWorkflowId = useWorkflowStore((state) => state.setExpandedSubWorkflowId)
  
  const status = nodeExecutionInfo?.status || data.status || 'pending'
  const executionTime = nodeExecutionInfo?.executionTime || data.executionTime
  const isExpanded = expandedSubWorkflowId === id
  
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
  
  // 处理展开/折叠
  const handleToggleExpand = useCallback(() => {
    if (isExpanded) {
      setExpandedSubWorkflowId(null)
    } else {
      setExpandedSubWorkflowId(id)
    }
    
    // 更新节点折叠状态
    setNodes((nodes) =>
      nodes.map((node) => {
        if (node.id === id) {
          return {
            ...node,
            data: {
              ...node.data,
              collapsed: !isExpanded,
            },
          }
        }
        return node
      })
    )
  }, [id, isExpanded, setExpandedSubWorkflowId, setNodes])
  
  return (
    <>
      <Card
        size="small"
        style={{
          width: 150,
          ...getBorderStyle(),
          backgroundColor: status === 'running' ? '#fffbe6' : '#f9f0ff',
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
            {data.label || '子工作流'}
          </div>
          {status && (
            <span>
              {status === 'success' && <CheckCircleOutlined style={{ color: '#52c41a' }} />}
              {status === 'failed' && <CloseCircleOutlined style={{ color: '#ff4d4f' }} />}
              {status === 'running' && <SyncOutlined spin style={{ color: '#faad14' }} />}
            </span>
          )}
        </div>
        
        {/* 子工作流信息 */}
        <div style={{ marginBottom: 8 }}>
          <Text type="secondary" style={{ fontSize: 12 }}>
            {data.subWorkflowName || '未命名子工作流'}
          </Text>
          {data.subWorkflowId && (
            <div style={{ marginTop: 4 }}>
              <Tag color="purple" style={{ fontSize: 11 }}>
                ID: {data.subWorkflowId.slice(0, 8)}...
              </Tag>
            </div>
          )}
        </div>
        
        {/* 展开/折叠按钮 (F027 - 展开折叠) */}
        <Button
          size="small"
          icon={isExpanded ? <UnorderedListOutlined /> : <DownOutlined />}
          onClick={handleToggleExpand}
          block
          style={{ marginBottom: 8 }}
        >
          {isExpanded ? '收起子流程' : '展开子流程'}
        </Button>
        
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
      
      {/* 展开的子工作流内容 (F027 - 嵌套工作流) */}
      {isExpanded && (
        <div
          style={{
            marginTop: 8,
            padding: '12px 16px',
            backgroundColor: '#fafafa',
            border: '1px dashed #d9d9d9',
            borderRadius: 8,
            minWidth: 150,
          }}
        >
          <div style={{ fontSize: 12, color: '#999', marginBottom: 8 }}>
            📋 子工作流节点预览
          </div>
          <div style={{ fontSize: 11, color: '#666' }}>
            <div>• 子节点 1: 数据预处理</div>
            <div>• 子节点 2: AI 模型调用</div>
            <div>• 子节点 3: 结果后处理</div>
          </div>
          <div style={{ marginTop: 8, textAlign: 'center' }}>
            <Button 
              size="small" 
              type="link"
              onClick={() => {
                // TODO: 导航到子工作流详情页面
                console.log('Navigate to sub-workflow:', data.subWorkflowId)
              }}
            >
              查看完整子工作流 →
            </Button>
          </div>
        </div>
      )}
    </>
  )
}

export const SubWorkflowNode = memo(SubWorkflowNodeComponent)
