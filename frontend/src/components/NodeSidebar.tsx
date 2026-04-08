import { useState, useMemo } from 'react';
import { Input } from 'antd';
import {
  SearchOutlined,
  RobotOutlined,
  ThunderboltOutlined,
  ExportOutlined,
  RightOutlined,
  DownOutlined,
  MailOutlined,
} from '@ant-design/icons';
import './NodeSidebar.css';

// ===== 节点定义 =====
export interface NodeDefinition {
  type: string;
  label: string;
  icon: string;
  description: string;
  category: string;
}

export const NODE_DEFINITIONS: NodeDefinition[] = [
  // 基础节点
  { type: 'input', label: '输入节点', icon: '📝', description: '工作流数据入口', category: 'basic' },
  { type: 'output', label: '输出节点', icon: '📤', description: '工作流数据出口', category: 'basic' },
  // AI 模型
  { type: 'model', label: '模型节点', icon: '🎨', description: 'AI 生图/生视频模型', category: 'ai' },
  { type: 'llm', label: 'LLM 节点', icon: '🤖', description: '大语言模型调用', category: 'ai' },
  // 逻辑控制
  { type: 'conditional', label: '条件判断', icon: '🔀', description: '根据条件分支执行', category: 'logic' },
  { type: 'loop', label: '循环处理', icon: '🔄', description: '循环遍历数据集合', category: 'logic' },
  // 数据处理
  { type: 'python', label: 'Python 脚本', icon: '🐍', description: '自定义 Python 代码', category: 'data' },
  { type: 'process', label: '处理节点', icon: '⚡', description: '通用数据处理', category: 'data' },
  { type: 'http_request', label: 'HTTP 请求', icon: '🌐', description: '调用外部 API 接口', category: 'data' },
  // 集成
  { type: 'email', label: '邮件发送', icon: '📧', description: '通过 SMTP 发送邮件', category: 'integration' },
  { type: 'wx_push', label: '微信推送', icon: '💬', description: '通过微信推送消息', category: 'integration' },
  // 数据清洗
  { type: 'etl', label: 'ETL 数据清洗', icon: '🔄', description: '归一化多数据源内容格式', category: 'data' },
  // AI 推荐
  { type: 'llm_recommendation', label: 'LLM 智能推荐', icon: '🎯', description: '结合用户画像的个性化推荐', category: 'ai' },
];

interface CategoryConfig {
  key: string;
  label: string;
  icon: React.ReactNode;
}

const CATEGORIES: CategoryConfig[] = [
  { key: 'basic', label: '基础节点', icon: <ExportOutlined /> },
  { key: 'ai', label: 'AI 模型', icon: <RobotOutlined /> },
  { key: 'logic', label: '逻辑控制', icon: <span>🔀</span> },
  { key: 'data', label: '数据处理', icon: <ThunderboltOutlined /> },
  { key: 'integration', label: '集成服务', icon: <MailOutlined /> },
];

// ===== 拖拽启动 =====
function onDragStart(e: React.DragEvent, nodeType: string) {
  e.dataTransfer.setData('application/reactflow', nodeType);
  e.dataTransfer.effectAllowed = 'move';
}

// ===== Props =====
interface NodeSidebarProps {
  collapsed?: boolean;
  onToggle?: () => void;
  onAddNode?: (type: string, label: string, icon: string) => void;
}

export default function NodeSidebar({ collapsed, onToggle, onAddNode }: NodeSidebarProps) {
  const [search, setSearch] = useState('');
  const [expandedCategories, setExpandedCategories] = useState<Record<string, boolean>>({
    basic: true,
    ai: true,
    logic: true,
    data: true,
    integration: true,
  });

  // 搜索过滤
  const filteredNodes = useMemo(() => {
    if (!search.trim()) return NODE_DEFINITIONS;
    const q = search.toLowerCase();
    return NODE_DEFINITIONS.filter(
      (n) => n.label.toLowerCase().includes(q) || n.description.toLowerCase().includes(q) || n.type.toLowerCase().includes(q)
    );
  }, [search]);

  // 按分类分组
  const grouped = useMemo(() => {
    const map: Record<string, NodeDefinition[]> = {};
    for (const cat of CATEGORIES) map[cat.key] = [];
    for (const node of filteredNodes) {
      if (map[node.category]) map[node.category].push(node);
    }
    return map;
  }, [filteredNodes]);

  const toggleCategory = (key: string) => {
    setExpandedCategories((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  if (collapsed) {
    return (
      <div className="node-sidebar node-sidebar--collapsed" onClick={onToggle}>
        <div className="node-sidebar__expand-btn">
          <RightOutlined />
        </div>
      </div>
    );
  }

  return (
    <div className="node-sidebar">
      {/* 标题 */}
      <div className="node-sidebar__header">
        <span className="node-sidebar__title">🧩 组件库</span>
        {onToggle && (
          <button className="node-sidebar__collapse-btn" onClick={onToggle} title="收起面板">
            ✕
          </button>
        )}
      </div>

      {/* 搜索框 */}
      <div className="node-sidebar__search">
        <Input
          prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
          placeholder="搜索节点..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          allowClear
          size="small"
        />
      </div>

      {/* 节点分类列表 */}
      <div className="node-sidebar__list">
        {CATEGORIES.map((cat) => {
          const nodes = grouped[cat.key] || [];
          if (nodes.length === 0) return null;
          const expanded = expandedCategories[cat.key];

          return (
            <div key={cat.key} className="node-sidebar__category">
              <div className="node-sidebar__category-header" onClick={() => toggleCategory(cat.key)}>
                <span className="node-sidebar__category-arrow">
                  {expanded ? <DownOutlined /> : <RightOutlined />}
                </span>
                <span className="node-sidebar__category-icon">{cat.icon}</span>
                <span className="node-sidebar__category-label">{cat.label}</span>
                <span className="node-sidebar__category-count">{nodes.length}</span>
              </div>

              {expanded && (
                <div className="node-sidebar__category-items">
                  {nodes.map((node) => (
                    <div
                      key={node.type}
                      className="node-sidebar__item"
                      draggable
                      onDragStart={(e) => onDragStart(e, node.type)}
                      onClick={() => onAddNode?.(node.type, node.label, node.icon)}
                      title={`拖拽或点击添加「${node.label}」`}
                    >
                      <span className="node-sidebar__item-icon">{node.icon}</span>
                      <div className="node-sidebar__item-info">
                        <div className="node-sidebar__item-label">{node.label}</div>
                        <div className="node-sidebar__item-desc">{node.description}</div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          );
        })}

        {filteredNodes.length === 0 && (
          <div className="node-sidebar__empty">
            没有匹配的节点
          </div>
        )}
      </div>
    </div>
  );
}
