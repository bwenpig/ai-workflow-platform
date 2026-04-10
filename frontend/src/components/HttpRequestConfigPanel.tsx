import { useState, useEffect } from 'react'
import { Input, Select, Button, Space, message } from 'antd'
import { SaveOutlined } from '@ant-design/icons'

const { TextArea } = Input
const { Option } = Select

interface HttpRequestConfig {
  url: string
  method: string
  headers: Record<string, string>
  body?: string
  timeout?: number
}

interface HttpRequestConfigPanelProps {
  node: {
    id: string
    data: {
      label: string
      config?: Partial<HttpRequestConfig>
    }
  }
  onClose: () => void
  onSave: (data: HttpRequestConfig) => void
}

/**
 * HTTP 请求节点配置面板
 */
export default function HttpRequestConfigPanel({ node, onClose, onSave }: HttpRequestConfigPanelProps) {
  const [url, setUrl] = useState('')
  const [method, setMethod] = useState('GET')
  const [headersText, setHeadersText] = useState('')
  const [body, setBody] = useState('')
  const [timeout, setTimeout] = useState(5000)

  useEffect(() => {
    const cfg = node.data.config || {}
    setUrl(cfg.url || 'https://api.example.com')
    setMethod(cfg.method || 'GET')
    setHeadersText(cfg.headers ? JSON.stringify(cfg.headers, null, 2) : '{}')
    setBody(cfg.body || '')
    setTimeout(cfg.timeout || 5000)
  }, [node])

  const handleSave = () => {
    if (!url.trim()) {
      message.warning('请输入 URL')
      return
    }
    let parsedHeaders: Record<string, string> = {}
    try {
      parsedHeaders = headersText ? JSON.parse(headersText) : {}
    } catch {
      message.error('Headers JSON 格式错误')
      return
    }

    const config: HttpRequestConfig = {
      url: url.trim(),
      method,
      headers: parsedHeaders,
      body: body || undefined,
      timeout,
    }
    onSave(config)
    message.success('HTTP 请求配置已保存')
  }

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <div>
        <div style={{ fontWeight: 500, marginBottom: 8 }}>🌐 URL 地址</div>
        <Input
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          placeholder="https://api.example.com/endpoint"
        />
      </div>

      <div>
        <div style={{ fontWeight: 500, marginBottom: 8 }}>📮 请求方法</div>
        <Select
          value={method}
          onChange={setMethod}
          style={{ width: '100%' }}
        >
          <Option value="GET">GET</Option>
          <Option value="POST">POST</Option>
          <Option value="PUT">PUT</Option>
          <Option value="DELETE">DELETE</Option>
          <Option value="PATCH">PATCH</Option>
        </Select>
      </div>

      <div>
        <div style={{ fontWeight: 500, marginBottom: 8 }}>📋 Headers (JSON)</div>
        <TextArea
          value={headersText}
          onChange={(e) => setHeadersText(e.target.value)}
          placeholder='{"Content-Type": "application/json"}'
          rows={3}
          style={{ fontFamily: 'monospace', fontSize: 13 }}
        />
      </div>

      {method !== 'GET' && (
        <div>
          <div style={{ fontWeight: 500, marginBottom: 8 }}>📦 Request Body</div>
          <TextArea
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder='{"key": "value"}'
            rows={4}
            style={{ fontFamily: 'monospace', fontSize: 13 }}
          />
        </div>
      )}

      <div>
        <div style={{ fontWeight: 500, marginBottom: 8 }}>⏱️ 超时（ms）</div>
        <Input
          type="number"
          value={timeout}
          onChange={(e) => setTimeout(Number(e.target.value) || 5000)}
          min={1000}
          max={60000}
          step={1000}
        />
      </div>

      <Button type="primary" icon={<SaveOutlined />} onClick={handleSave} block>
        保存配置
      </Button>
    </Space>
  )
}
