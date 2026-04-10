import { useState, useEffect } from 'react'
import { Input, Button, Space, Select, message } from 'antd'

const { Option } = Select
const { TextArea } = Input

interface ConditionalConfig {
  expression?: string;
  operator?: string;
  value?: string;
  label?: string;
}

interface ConditionalConfigPanelProps {
  node: {
    id: string;
    data: ConditionalConfig;
  } | null;
  onClose: () => void;
  onSave: (data: ConditionalConfig) => void;
}

/**
 * 条件判断节点配置面板
 */
export default function ConditionalConfigPanel({ node, onClose, onSave }: ConditionalConfigPanelProps) {
  const [mode, setMode] = useState<'simple' | 'expression'>('simple')
  const [variable, setVariable] = useState('')
  const [operator, setOperator] = useState('==')
  const [targetValue, setTargetValue] = useState('')
  const [expression, setExpression] = useState('')

  useEffect(() => {
    if (node) {
      const expr = node.data.expression || ''
      const val = node.data.value || ''
      const op = node.data.operator || '=='

      // 如果有 operator 字段，说明是简单模式
      if (op || (node.data.value !== undefined && !expr)) {
        setMode('simple')
        setVariable(expr || node.data.label || '')
        setOperator(op)
        setTargetValue(val)
        setExpression('')
      } else {
        setMode('expression')
        setExpression(expr)
        setVariable('')
        setOperator('==')
        setTargetValue('')
      }
    }
  }, [node])

  if (!node) return null

  const handleSave = () => {
    if (mode === 'expression') {
      if (!expression.trim()) {
        message.warning('请输入条件表达式')
        return
      }
      onSave({
        expression: expression.trim(),
        operator: undefined,
        value: undefined,
      })
    } else {
      if (!variable.trim()) {
        message.warning('请输入变量名')
        return
      }
      onSave({
        expression: variable.trim(),
        operator,
        value: targetValue,
      })
    }
  }

  return (
    <div style={styles.panel}>
      <h3 style={styles.title}>🔀 条件判断配置</h3>

      {/* 模式切换 */}
      <div style={styles.section}>
        <label style={styles.label}>配置模式</label>
        <Select
          value={mode}
          onChange={(v) => setMode(v as 'simple' | 'expression')}
          style={{ width: '100%' }}
          options={[
            { value: 'simple', label: '📝 简单模式（变量 操作符 值）' },
            { value: 'expression', label: '💻 表达式模式（自由编写）' },
          ]}
        />
      </div>

      {mode === 'simple' ? (
        <>
          {/* 变量名 */}
          <div style={styles.section}>
            <label style={styles.label}>变量 / 表达式</label>
            <Input
              value={variable}
              onChange={(e) => setVariable(e.target.value)}
              placeholder="如: data.status, result.count"
              allowClear
            />
          </div>

          {/* 操作符 */}
          <div style={styles.section}>
            <label style={styles.label}>操作符</label>
            <Select
              value={operator}
              onChange={setOperator}
              style={{ width: '100%' }}
              options={[
                { value: '==', label: '== 等于' },
                { value: '!=', label: '!= 不等于' },
                { value: '>', label: '> 大于' },
                { value: '>=', label: '>= 大于等于' },
                { value: '<', label: '< 小于' },
                { value: '<=', label: '<= 小于等于' },
                { value: 'contains', label: 'contains 包含' },
                { value: 'in', label: 'in 在列表中' },
                { value: 'exists', label: 'exists 存在' },
              ]}
            />
          </div>

          {/* 目标值 */}
          <div style={styles.section}>
            <label style={styles.label}>目标值</label>
            <Input
              value={targetValue}
              onChange={(e) => setTargetValue(e.target.value)}
              placeholder="比较的目标值"
              allowClear
            />
          </div>

          {/* 预览 */}
          <div style={styles.preview}>
            <span style={styles.previewLabel}>条件预览:</span>
            <code style={styles.previewCode}>
              {variable || '...'} {operator} {targetValue || '...'}
            </code>
          </div>
        </>
      ) : (
        <>
          {/* 自由表达式 */}
          <div style={styles.section}>
            <label style={styles.label}>条件表达式</label>
            <TextArea
              value={expression}
              onChange={(e) => setExpression(e.target.value)}
              placeholder="如: data.status == 'success' and result.count > 0"
              rows={4}
              style={{ fontFamily: 'monospace', fontSize: '13px' }}
            />
          </div>

          {/* 提示 */}
          <div style={styles.hint}>
            <strong>支持的操作符:</strong> ==, !=, &gt;, &lt;, &gt;=, &lt;=, and, or, not, in, contains
          </div>
        </>
      )}

      {/* Actions */}
      <div style={styles.actions}>
        <Button type="primary" onClick={handleSave}>
          保存配置
        </Button>
        <Button onClick={onClose}>取消</Button>
      </div>
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  panel: {
    padding: '16px',
    display: 'flex',
    flexDirection: 'column',
    gap: '16px',
  },
  title: {
    margin: '0 0 8px 0',
    color: '#fa8c16',
    fontSize: '18px',
    fontWeight: 600,
  },
  section: {
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
  },
  label: {
    fontSize: '14px',
    fontWeight: 500,
    color: '#333',
  },
  preview: {
    background: '#f5f5f5',
    borderRadius: '6px',
    padding: '10px 12px',
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
  },
  previewLabel: {
    fontSize: '12px',
    color: '#999',
  },
  previewCode: {
    fontSize: '13px',
    color: '#333',
    fontFamily: 'monospace',
  },
  hint: {
    fontSize: '12px',
    color: '#999',
    background: '#fafafa',
    borderRadius: '6px',
    padding: '8px 12px',
  },
  actions: {
    display: 'flex',
    gap: '8px',
    marginTop: '8px',
  },
}
