import { memo } from 'react'
import { Handle, Position } from '@xyflow/react'

interface PythonNodeData {
  label?: string;
  status?: string;
}

export const PythonNode = memo(({ data }: { data: PythonNodeData }) => {
  return (
    <div className="python-node">
      <div className="node-header">
        <span className="node-icon">🐍</span>
        <span className="node-label">{data.label || 'Python 脚本'}</span>
      </div>
      
      <Handle 
        type="target" 
        position={Position.Left} 
        className="node-handle"
        style={{ background: '#306998' }}
      />
      
      <Handle 
        type="source" 
        position={Position.Right} 
        className="node-handle"
        style={{ background: '#306998' }}
      />
    </div>
  )
})

PythonNode.displayName = 'PythonNode'
