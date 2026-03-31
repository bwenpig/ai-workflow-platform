import { useState, useEffect } from 'react'
import Editor from '@monaco-editor/react'
import { Input, Button, Space, Divider } from 'antd'

const { TextArea } = Input

interface PythonNodeData {
  script?: string;
  timeout?: number;
  dependencies?: string[];
  description?: string;
}

interface PythonConfigPanelProps {
  node: {
    id: string;
    data: PythonNodeData;
  } | null;
  onClose: () => void;
  onSave: (data: PythonNodeData) => void;
}

/**
 * Python 节点配置面板
 */
export default function PythonConfigPanel({ node, onClose, onSave }: PythonConfigPanelProps) {
  const [script, setScript] = useState('')
  const [timeout, setTimeout] = useState(30)
  const [dependencies, setDependencies] = useState('')
  const [description, setDescription] = useState('')

  useEffect(() => {
    if (node) {
      setScript(node.data.script || '')
      setTimeout(node.data.timeout || 30)
      setDependencies(node.data.dependencies?.join('\n') || '')
      setDescription(node.data.description || '')
    }
  }, [node])

  if (!node) return null

  const handleSave = () => {
    onSave({
      script,
      timeout,
      dependencies: dependencies.split('\n').filter(d => d.trim()),
      description,
    })
  }

  const handleEditorChange = (value?: string) => {
    setScript(value || '')
  }

  return (
    <div className="python-config-panel" style={styles.panel}>
      <h3 style={styles.title}>🐍 Python 脚本配置</h3>
      
      <div style={styles.section}>
        <label style={styles.label}>脚本描述</label>
        <Input
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="简要描述这个节点的功能..."
          style={styles.input}
        />
      </div>

      <div style={styles.section}>
        <label style={styles.label}>Python 代码</label>
        <div style={styles.editorWrapper}>
          <Editor
            height="300px"
            language="python"
            value={script}
            onChange={handleEditorChange}
            theme="vs-dark"
            options={{
              minimap: { enabled: false },
              fontSize: 14,
              automaticLayout: true,
              scrollBeyondLastLine: false,
              lineNumbers: 'on',
              renderWhitespace: 'selection',
              suggestOnTriggerCharacters: true,
              quickSuggestions: true,
              tabSize: 4,
              wordWrap: 'on',
            }}
          />
        </div>
      </div>
      
      <div style={styles.section}>
        <label style={styles.label}>超时时间（秒）</label>
        <Input
          type="number"
          value={timeout}
          onChange={(e) => setTimeout(parseInt(e.target.value) || 30)}
          style={styles.input}
          min={1}
          max={300}
        />
      </div>

      <div style={styles.section}>
        <label style={styles.label}>依赖包（每行一个）</label>
        <TextArea
          value={dependencies}
          onChange={(e) => setDependencies(e.target.value)}
          placeholder="requests&#10;pandas&#10;numpy"
          style={styles.textarea}
          rows={4}
        />
        <p style={styles.hint}>常用包：requests, pandas, numpy, Pillow, opencv-python</p>
      </div>

      <Divider />

      <Space style={styles.actions}>
        <Button type="primary" onClick={handleSave}>
          保存配置
        </Button>
        <Button onClick={onClose}>
          取消
        </Button>
      </Space>
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
    color: '#306998',
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
  input: {
    width: '100%',
  },
  textarea: {
    width: '100%',
    fontFamily: 'monospace',
    fontSize: '13px',
  },
  editorWrapper: {
    border: '1px solid #d9d9d9',
    borderRadius: '6px',
    overflow: 'hidden',
  },
  hint: {
    fontSize: '12px',
    color: '#999',
    margin: '4px 0 0 0',
  },
  actions: {
    marginTop: '8px',
  },
}
