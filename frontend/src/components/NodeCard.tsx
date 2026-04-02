import React, { memo } from 'react'

interface NodeCardProps {
  id: string
  type: string
  data: {
    label: string
    icon?: string
    status?: 'running' | 'success' | 'error'
  }
  selected?: boolean
}

export const NodeCard = memo(({ id, type, data, selected }: NodeCardProps) => {
  const statusClass = data.status ? `node-status ${data.status}` : ''
  
  return (
    <div 
      className={`node-content ${type === 'input' ? 'node-input' : 
        type === 'model' ? 'node-model' :
        type === 'llm' ? 'node-llm' :
        type === 'python' ? 'node-python' :
        type === 'process' ? 'node-process' :
        type === 'output' ? 'node-output' : ''}`}
      data-testid={`node-${id}`}
    >
      <div className="node-header">
        <span>{data.icon || '⚙️'}</span>
        <span>{data.label}</span>
      </div>
      <div className="node-label">
        {type === 'python' && 'Python 脚本节点'}
        {type === 'model' && 'AI 模型节点'}
        {type === 'llm' && '大语言模型'}
        {type === 'input' && '数据输入'}
        {type === 'output' && '数据输出'}
        {type === 'process' && '数据处理'}
      </div>
      {data.status && <div className={statusClass} />}
    </div>
  )
})

NodeCard.displayName = 'NodeCard'
