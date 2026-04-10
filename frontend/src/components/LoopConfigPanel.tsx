import { useState, useEffect, useCallback } from 'react'
import { Input, Button, Space, Tag, message } from 'antd'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons'

const { TextArea } = Input

interface LoopConfig {
  items?: string[];
  itemVar?: string;
  indexVar?: string;
  maxIterations?: number;
  concurrency?: number;
  label?: string;
}

interface LoopConfigPanelProps {
  node: {
    id: string;
    data: LoopConfig;
  } | null;
  onClose: () => void;
  onSave: (data: LoopConfig) => void;
}

/**
 * 循环处理节点配置面板
 */
export default function LoopConfigPanel({ node, onClose, onSave }: LoopConfigPanelProps) {
  const [itemsInput, setItemsInput] = useState('')
  const [itemVar, setItemVar] = useState('item')
  const [indexVar, setIndexVar] = useState('index')
  const [maxIterations, setMaxIterations] = useState(100)
  const [concurrency, setConcurrency] = useState(1)

  useEffect(() => {
    if (node) {
      const items = node.data.items || []
      setItemsInput(items.join('\n'))
      setItemVar(node.data.itemVar || 'item')
      setIndexVar(node.data.indexVar || 'index')
      setMaxIterations(node.data.maxIterations || 100)
      setConcurrency(node.data.concurrency || 1)
    }
  }, [node])

  if (!node) return null

  const getItemsArray = (): string[] => {
    return itemsInput
      .split('\n')
      .map((line) => line.trim())
      .filter((line) => line.length > 0)
  }

  const handleSave = () => {
    const items = getItemsArray()
    if (items.length === 0) {
      message.warning('请至少添加一个循环项')
      return
    }
    if (!itemVar.trim()) {
      message.warning('请输入循环变量名')
      return
    }

    onSave({
      items,
      itemVar: itemVar.trim(),
      indexVar: indexVar.trim() || 'index',
      maxIterations,
      concurrency,
    })
  }

  const items = getItemsArray()

  return (
    <div style={styles.panel}>
      <h3 style={styles.title}>🔄 循环处理配置</h3>

      {/* 循环项 */}
      <div style={styles.section}>
        <label style={styles.label}>
          循环项 (每行一个)
          <span style={styles.itemCount}>
            共 {items.length} 项
          </span>
        </label>
        <TextArea
          value={itemsInput}
          onChange={(e) => setItemsInput(e.target.value)}
          placeholder={'item1\nitem2\nitem3'}
          rows={6}
          style={{ fontFamily: 'monospace', fontSize: '13px' }}
        />

        {/* 已添加的项标签 */}
        {items.length > 0 && (
          <div style={styles.tagContainer}>
            {items.slice(0, 20).map((item, i) => (
              <Tag key={i} color="purple" style={{ marginBottom: '4px' }}>
                {item}
              </Tag>
            ))}
            {items.length > 20 && <Tag color="default">+{items.length - 20} more</Tag>}
          </div>
        )}
      </div>

      {/* 变量名 */}
      <div style={styles.row}>
        <div style={{ flex: 1 }}>
          <label style={styles.label}>循环变量名</label>
          <Input
            value={itemVar}
            onChange={(e) => setItemVar(e.target.value)}
            placeholder="item"
          />
        </div>
        <div style={{ flex: 1, marginLeft: '12px' }}>
          <label style={styles.label}>索引变量名</label>
          <Input
            value={indexVar}
            onChange={(e) => setIndexVar(e.target.value)}
            placeholder="index"
          />
        </div>
      </div>

      {/* 高级选项 */}
      <div style={styles.row}>
        <div style={{ flex: 1 }}>
          <label style={styles.label}>最大迭代次数</label>
          <Input
            type="number"
            value={maxIterations}
            onChange={(e) => setMaxIterations(parseInt(e.target.value) || 100)}
            min={1}
            max={10000}
          />
        </div>
        <div style={{ flex: 1, marginLeft: '12px' }}>
          <label style={styles.label}>并发数</label>
          <Input
            type="number"
            value={concurrency}
            onChange={(e) => setConcurrency(parseInt(e.target.value) || 1)}
            min={1}
            max={10}
          />
        </div>
      </div>

      {/* 提示 */}
      <div style={styles.hint}>
        💡 在循环体内使用 <code>{'{{ ' + itemVar + ' }}</code>} 访问当前项，
        <code>{'{{ ' + (indexVar || 'index') + ' }}</code>} 访问索引
      </div>

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
    color: '#722ed1',
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
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  itemCount: {
    fontSize: '12px',
    color: '#999',
    fontWeight: 400,
  },
  row: {
    display: 'flex',
    alignItems: 'flex-start',
  },
  tagContainer: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: '4px',
  },
  hint: {
    fontSize: '12px',
    color: '#666',
    background: '#f9f0ff',
    borderRadius: '6px',
    padding: '8px 12px',
  },
  actions: {
    display: 'flex',
    gap: '8px',
    marginTop: '8px',
  },
}
