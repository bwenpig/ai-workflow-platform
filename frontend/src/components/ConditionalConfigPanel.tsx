import { useState, useEffect } from 'react'
import { Input, Select, Button, Space, message } from 'antd'
import { SaveOutlined } from '@ant-design/icons'

const { TextArea } = Input
const { Option } = Select

interface ConditionalConfig {
  expression: string
  value: string
  operator: string
}

interface ConditionalConfigPanelProps {
  node: {
    id: string
    data: {
      label: string
      config?: Partial<ConditionalConfig>
    }
  }
  onClose: () => void
  onSave: (data: ConditionalConfig) => void
}

const OPERATORS = [
  { label: '== 等于', value: '==' },
  { label: '!= 不等于', value: '!=' },
  { label: '> 大于', value: '>' },
  { label: '>= 大于等于', value: '>=' },
  { label: '< 小于', value: '<' },
  { label: '<= 小于等于', value: '<=' },
  { label: 'contains 包含', value: 'contains' },
  { label: 'in 在列表中', value: 'in' },
]

/**
 * 条件判断节点配置面板
 */
export default function ConditionalConfigPanel({ node, onClose, onSave }: ConditionalConfigPanelProps) {
  const [expression, setExpression] = useState('')
  const [value, setValue] = useState('')
  const [operator, setOperator] = useState('==')

  useEffect(() => {
    const cfg = node.data.config || {}
    setExpression(cfg.expression || '')
    setValue(cfg.value || '')
    setOperator(cfg.operator || '==')
  }, [node])

  const handleSave = () => {
    if (!expression.trim()) {
      message.warning('请输入条件表达式')
      return
    }
    const config: ConditionalConfig = {
      expression: expression.trim(),
      value: value.trim(),
      operator,
    }
    onSave(config)
    message.success('条件判断配置已保存')
  }

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <div>
        <div style={{ fontWeight: 500, marginBottom: 8 }}>📐 条件表达式</div>
        <Input
          value={expression}
          onChange={(e) => setExpression(e.target.value)}
          placeholder="例如: data.status, result.count"
        />
      </div>

      <div>
        <div style={{ fontWeight: 500, marginBottom: 8 }}>⚙️ 比较操作符</div>
        <Select
          value={operator}
          onChange={setOperator}
          style={{ width: '100%' }}
        >
          {OPERATORS.map((op) => (
            <Option key={op.value} value={op.value}>{op.label}</Option>
          ))}
        </Select>
      </div>

      <div>
        <div style={{ fontWeight: 500, marginBottom: 8 }}>🎯 目标值</div>
        <Input
          value={value}
          onChange={(e) => setValue(e.target.value)}
          placeholder="例如: success, 100"
        />
      </div>

      {/* 条件预览 */}
      <div style={{
        background: '#f6f8fa',
        borderRadius: 6,
        padding: '10px 14px',
        fontSize: 13,
        fontFamily: 'monospace',
        color: '#333',
      }}>
        <span style={{ color: '#999', fontSize: 11, display: 'block', marginBottom: 4 }}>预览</span>
        {expression || 'expression'} {operator} {value || 'value'}
      </div>

      <Button type="primary" icon={<SaveOutlined />} onClick={handleSave} block>
        保存配置
      </Button>
    </Space>
  )
}
