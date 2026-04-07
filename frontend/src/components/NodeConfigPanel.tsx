import { useState, useEffect } from 'react';

interface NodeConfig {
  nodeId: string;
  type: string;
  modelProvider?: string;
  config: Record<string, any>;
}

interface NodeConfigPanelProps {
  node: NodeConfig | null;
  onClose: () => void;
  onSave: (config: NodeConfig) => void;
}

/**
 * 节点配置面板
 */
export default function NodeConfigPanel({ node, onClose, onSave }: NodeConfigPanelProps) {
  const [config, setConfig] = useState<Record<string, any>>({});

  useEffect(() => {
    if (node) {
      setConfig(node.config || {});
    }
  }, [node]);

  if (!node) return null;

  const handleSave = () => {
    onSave({ ...node, config });
  };

  const handleChange = (key: string, value: any) => {
    setConfig((prev) => ({ ...prev, [key]: value }));
  };

  // 当前选中的模型提供商（从 config 中读取）
  const selectedProvider = config.modelProvider || '';

  // 根据节点类型渲染不同配置项
  const renderConfigFields = () => {
    switch (node.type) {
      case 'input':
        return (
          <div>
            <label style={styles.label}>提示词</label>
            <textarea
              value={config.prompt || ''}
              onChange={(e) => handleChange('prompt', e.target.value)}
              style={styles.textarea}
              rows={4}
              placeholder="输入提示词..."
            />
          </div>
        );

      case 'model':
        return (
          <div>
            <label style={styles.label}>模型提供商</label>
            <select
              value={selectedProvider}
              onChange={(e) => handleChange('modelProvider', e.target.value)}
              style={styles.select}
            >
              <option value="">请选择模型</option>
              <optgroup label="🎬 视频生成">
                <option value="kling">🎬 可灵 (Kling)</option>
                <option value="seedance">🎬 即梦 (Seedance)</option>
              </optgroup>
              <optgroup label="🖼️ 图片生成">
                <option value="wan">🎨 海螺 (Wan)</option>
                <option value="nanobanana">🎨 NanoBanana</option>
              </optgroup>
            </select>

            {selectedProvider && (
              <>
                <label style={styles.label}>提示词</label>
                <textarea
                  value={config.prompt || ''}
                  onChange={(e) => handleChange('prompt', e.target.value)}
                  style={styles.textarea}
                  rows={3}
                  placeholder="描述你想生成的内容..."
                />

                {['kling', 'seedance'].includes(selectedProvider) && (
                  <>
                    <label style={styles.label}>视频时长 (秒)</label>
                    <input
                      type="number"
                      value={config.duration || 5}
                      onChange={(e) => handleChange('duration', parseInt(e.target.value))}
                      style={styles.input}
                      min={1}
                      max={30}
                    />

                    <label style={styles.label}>帧率 (FPS)</label>
                    <select
                      value={config.fps || 24}
                      onChange={(e) => handleChange('fps', parseInt(e.target.value))}
                      style={styles.select}
                    >
                      <option value={24}>24 FPS</option>
                      <option value={30}>30 FPS</option>
                      <option value={60}>60 FPS</option>
                    </select>
                  </>
                )}

                {['wan', 'nanobanana'].includes(selectedProvider) && (
                  <>
                    <label style={styles.label}>图片尺寸</label>
                    <select
                      value={config.size || '1024x1024'}
                      onChange={(e) => handleChange('size', e.target.value)}
                      style={styles.select}
                    >
                      <option value="512x512">512×512</option>
                      <option value="1024x1024">1024×1024</option>
                      <option value="1024x1536">1024×1536 (竖版)</option>
                      <option value="1536x1024">1536×1024 (横版)</option>
                    </select>
                  </>
                )}
              </>
            )}
          </div>
        );

      case 'process':
        return (
          <div>
            <label style={styles.label}>处理类型</label>
            <select
              value={config.processType || 'upscale'}
              onChange={(e) => handleChange('processType', e.target.value)}
              style={styles.select}
            >
              <option value="upscale">🔍 高清放大</option>
              <option value="style">🎨 风格转换</option>
              <option value="inpaint">✏️ 局部重绘</option>
              <option value="remove_bg">✂️ 去除背景</option>
            </select>
          </div>
        );

      case 'email':
        return (
          <div>
            <label style={styles.label}>收件人 (逗号分隔)</label>
            <input
              type="text"
              value={Array.isArray(config.to) ? config.to.join(', ') : (config.to || '')}
              onChange={(e) => handleChange('to', e.target.value.split(',').map((s: string) => s.trim()).filter(Boolean))}
              style={styles.input}
              placeholder="user@example.com, user2@example.com"
            />

            <label style={styles.label}>抄送 (可选, 逗号分隔)</label>
            <input
              type="text"
              value={Array.isArray(config.cc) ? config.cc.join(', ') : (config.cc || '')}
              onChange={(e) => handleChange('cc', e.target.value.split(',').map((s: string) => s.trim()).filter(Boolean))}
              style={styles.input}
              placeholder="cc@example.com"
            />

            <label style={styles.label}>邮件主题</label>
            <input
              type="text"
              value={config.subject || ''}
              onChange={(e) => handleChange('subject', e.target.value)}
              style={styles.input}
              placeholder="输入邮件主题..."
            />

            <label style={styles.label}>邮件正文 (支持 HTML)</label>
            <textarea
              value={config.body || ''}
              onChange={(e) => handleChange('body', e.target.value)}
              style={styles.textarea}
              rows={6}
              placeholder="输入邮件正文..."
            />

            <label style={styles.label}>发件人 (可选)</label>
            <input
              type="text"
              value={config.from || ''}
              onChange={(e) => handleChange('from', e.target.value)}
              style={styles.input}
              placeholder="sender@example.com"
            />

            <label style={styles.label}>附件路径 (可选, 逗号分隔)</label>
            <input
              type="text"
              value={Array.isArray(config.attachments) ? config.attachments.join(', ') : (config.attachments || '')}
              onChange={(e) => handleChange('attachments', e.target.value.split(',').map((s: string) => s.trim()).filter(Boolean))}
              style={styles.input}
              placeholder="/path/to/file1, /path/to/file2"
            />
          </div>
        );

      default:
        return <p style={styles.hint}>该节点类型暂无配置项</p>;
    }
  };

  return (
    <div style={styles.overlay}>
      <div style={styles.panel}>
        <div style={styles.header}>
          <h3 style={styles.title}>节点配置</h3>
          <button onClick={onClose} style={styles.closeButton}>×</button>
        </div>

        <div style={styles.content}>
          <div style={styles.section}>
            <h4 style={styles.sectionTitle}>基本信息</h4>
            <label style={styles.label}>节点 ID</label>
            <input
              type="text"
              value={node.nodeId}
              disabled
              style={{ ...styles.input, background: '#f0f0f0' }}
            />

            <label style={styles.label}>节点类型</label>
            <input
              type="text"
              value={node.type}
              disabled
              style={{ ...styles.input, background: '#f0f0f0' }}
            />
          </div>

          <div style={styles.section}>
            <h4 style={styles.sectionTitle}>参数配置</h4>
            {renderConfigFields()}
          </div>
        </div>

        <div style={styles.footer}>
          <button onClick={onClose} style={styles.cancelButton}>
            取消
          </button>
          <button onClick={handleSave} style={styles.saveButton}>
            保存
          </button>
        </div>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  overlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    background: 'rgba(0, 0, 0, 0.5)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 2000,
  },
  panel: {
    background: 'white',
    borderRadius: '12px',
    width: '500px',
    maxHeight: '80vh',
    display: 'flex',
    flexDirection: 'column',
    boxShadow: '0 10px 40px rgba(0,0,0,0.2)',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '20px',
    borderBottom: '1px solid #e0e0e0',
  },
  title: {
    margin: 0,
    fontSize: '18px',
    fontWeight: 600,
  },
  closeButton: {
    background: 'none',
    border: 'none',
    fontSize: '24px',
    cursor: 'pointer',
    color: '#666',
  },
  content: {
    flex: 1,
    overflow: 'auto',
    padding: '20px',
  },
  section: {
    marginBottom: '20px',
  },
  sectionTitle: {
    margin: '0 0 10px 0',
    fontSize: '14px',
    fontWeight: 600,
    color: '#333',
  },
  label: {
    display: 'block',
    marginBottom: '5px',
    fontSize: '13px',
    fontWeight: 500,
    color: '#555',
  },
  input: {
    width: '100%',
    padding: '8px 12px',
    border: '1px solid #ddd',
    borderRadius: '6px',
    fontSize: '14px',
    marginBottom: '15px',
    boxSizing: 'border-box',
  },
  textarea: {
    width: '100%',
    padding: '8px 12px',
    border: '1px solid #ddd',
    borderRadius: '6px',
    fontSize: '14px',
    marginBottom: '15px',
    resize: 'vertical',
    boxSizing: 'border-box',
  },
  select: {
    width: '100%',
    padding: '8px 12px',
    border: '1px solid #ddd',
    borderRadius: '6px',
    fontSize: '14px',
    marginBottom: '15px',
    boxSizing: 'border-box',
  },
  hint: {
    color: '#999',
    fontSize: '13px',
    fontStyle: 'italic',
  },
  footer: {
    display: 'flex',
    justifyContent: 'flex-end',
    gap: '10px',
    padding: '20px',
    borderTop: '1px solid #e0e0e0',
  },
  cancelButton: {
    padding: '8px 20px',
    border: '1px solid #ddd',
    background: 'white',
    borderRadius: '6px',
    cursor: 'pointer',
    fontSize: '14px',
  },
  saveButton: {
    padding: '8px 20px',
    border: 'none',
    background: '#4a4a6a',
    color: 'white',
    borderRadius: '6px',
    cursor: 'pointer',
    fontSize: '14px',
  },
};
