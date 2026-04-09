import { useCallback, useState, useEffect, useRef } from 'react';
import {
  ReactFlow,
  Controls,
  Background,
  MiniMap,
  useNodesState,
  useEdgesState,
  useReactFlow,
  addEdge,
  Connection,
  Edge,
  Node,
  Handle,
  Position,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import '../styles/WorkflowCanvas.css';
import { ConfigProvider, Button, Card, Input, Select, Space, Divider, Empty, message, Tag } from 'antd';
import {
  PlayCircleOutlined,
  SettingOutlined,
  AppstoreOutlined,
  RobotOutlined,
  ThunderboltOutlined,
  ExportOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  LoadingOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import NodeSidebar from './NodeSidebar';
import ResultPreview from './ResultPreview';
import PythonConfigPanel from './PythonConfigPanel';
import { PythonNode } from '../nodes/PythonNode';
import { HttpRequestNode } from '../nodes/HttpRequestNode';
import { ConditionalNode } from '../nodes/ConditionalNode';
import { LoopNode } from '../nodes/LoopNode';
import { LLMNode } from '../nodes/LLMNode';
import WxPushNode from '../workflow/nodes/WxPushNode';
import { EtlNode } from '../nodes/EtlNode';
import { RecommendationNode } from '../nodes/RecommendationNode';

const { TextArea } = Input;
const { Option, OptGroup } = Select;

// 简化的节点组件 - 参考 Dify/n8n 设计
function SimpleNode({ data, id }: any) {
  const nodeType = data.type || 'simple';
  
  // 节点类型颜色映射
  const typeColors: Record<string, string> = {
    input: '#10b981',      // 绿色
    model: '#8b5cf6',      // 紫色
    llm: '#f59e0b',        // 橙色
    python: '#3b82f6',     // 蓝色
    process: '#ef4444',    // 红色
    output: '#06b6d4',     // 青色
  };
  
  const headerColors: Record<string, string> = {
    input: 'linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%)',
    model: 'linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%)',
    llm: 'linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%)',
    python: 'linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%)',
    process: 'linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%)',
    output: 'linear-gradient(135deg, #ecfeff 0%, #cffafe 100%)',
  };
  
  const borderColor = typeColors[nodeType] || (
    data.status === 'SUCCESS' ? '#52c41a' : 
    data.status === 'FAILED' ? '#ff4d4f' : 
    data.status === 'RUNNING' ? '#faad14' : '#d9d9d9'
  );
  
  const headerBg = headerColors[nodeType] || '#f8fafc';
  
  return (
    <div 
      data-testid={`node-${id}`}
      className="react-flow__node-simple"
      style={{
        padding: '0',
        borderRadius: '12px',
        minWidth: '180px',
        boxShadow: '0 2px 12px rgba(0,0,0,0.08)',
        border: `2px solid ${borderColor}`,
        overflow: 'hidden',
      }}>
      <Handle type="target" position={Position.Top} style={{ zIndex: 1 }} />
      
      {/* 节点头部 - 带颜色编码 */}
      <div style={{
        padding: '10px 12px',
        background: headerBg,
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        fontWeight: 600,
        fontSize: '13px',
        color: '#1e293b',
      }}>
        <span style={{ fontSize: '16px' }}>{data.icon}</span>
        <span>{data.label}</span>
        {data.status && (
          <span style={{ marginLeft: 'auto', fontSize: '14px' }}>
            {data.status === 'SUCCESS' && <CheckCircleOutlined style={{ color: '#10b981' }} />}
            {data.status === 'FAILED' && <CloseCircleOutlined style={{ color: '#ef4444' }} />}
            {data.status === 'RUNNING' && <LoadingOutlined style={{ color: '#3b82f6' }} />}
          </span>
        )}
      </div>
      
      {/* 节点内容 */}
      <div style={{ padding: '12px' }}>
        <div style={{ fontSize: '12px', color: '#64748b' }}>
          {nodeType === 'python' && 'Python 脚本节点'}
          {nodeType === 'model' && 'AI 模型节点'}
          {nodeType === 'llm' && '大语言模型'}
          {nodeType === 'input' && '数据输入'}
          {nodeType === 'output' && '数据输出'}
          {nodeType === 'process' && '数据处理'}
          {nodeType === 'wx_push' && '微信推送'}
        </div>
      </div>

      <Handle type="source" position={Position.Bottom} style={{ zIndex: 1 }} />
    </div>
  );
}

// 注意：nodeTypes 必须在组件外部定义，避免每次渲染重新创建导致 ReactFlow 重新挂载
const nodeTypes = {
  http_request: HttpRequestNode,
  conditional: ConditionalNode,
  loop: LoopNode,
  simple: SimpleNode,
  python_script: PythonNode,
  llm: LLMNode,
  wx_push: WxPushNode,
  etl: EtlNode,
  llm_recommendation: RecommendationNode,
};

const initialNodes: Node[] = [
  { id: '1', type: 'simple', position: { x: 250, y: 50 }, data: { label: '输入节点', icon: '📝' } },
  { id: '2', type: 'simple', position: { x: 250, y: 200 }, data: { label: '可灵模型', icon: '🎨' } },
  { id: '3', type: 'simple', position: { x: 250, y: 350 }, data: { label: '输出节点', icon: '📤' } },
];

const initialEdges: Edge[] = [
  { id: 'e1-2', source: '1', target: '2' },
  { id: 'e2-3', source: '2', target: '3' },
];

interface WorkflowCanvasProps {
  onExecutionStart?: (executionId: string) => void;
}

export default function WorkflowCanvas({ onExecutionStart }: WorkflowCanvasProps) {
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);
  
  const [selectedNode, setSelectedNode] = useState<Node | null>(null);
  const [showResultPreview, setShowResultPreview] = useState(false);
  const [isExecuting, setIsExecuting] = useState(false);
  const [executionResult, setExecutionResult] = useState<any>(null);
  const [currentWorkflowId, setCurrentWorkflowId] = useState<string | null>(null);
  const [executionId, setExecutionId] = useState<string | null>(null);
  const [nodeResults, setNodeResults] = useState<Record<string, any>>({});
  const [workflowList, setWorkflowList] = useState<any[]>([]);
  const [selectedWorkflow, setSelectedWorkflow] = useState<string>('');

  // 加载工作流列表
  const loadWorkflowList = async () => {
    try {
      const res = await fetch('http://localhost:8080/api/v1/workflows');
      const data = await res.json();
      setWorkflowList(data);
    } catch (e) {
      console.error('加载工作流列表失败', e);
    }
  };

  // 加载工作流详情
  const loadWorkflow = async (id: string) => {
    try {
      const res = await fetch(`http://localhost:8080/api/v1/workflows/${id}`);
      const workflow = await res.json();
      
      if (workflow.nodes && workflow.nodes.length > 0) {
        // 图标映射
        const iconMap: Record<string, string> = {
          http_request: '🌐',
          conditional: '🔀',
          loop: '🔄',
          python_script: '🐍',
          simple: '📝',
          llm: '🤖',
          wx_push: '💬',
          etl: '🔄',
          llm_recommendation: '🎯',
          link_scraper: '🔗',
        };

        // 自动布局：计算节点位置
        const hasPositions = workflow.nodes.some((n: any) => n.position);
        const loadedNodes = workflow.nodes.map((n: any, index: number) => {
          const nodeType = getNodeType(n.type);
          const label = n.config?.label || n.nodeId;
          
          // 如果后端没有位置，自动垂直排列
          let position = n.position;
          if (!position) {
            position = { x: 250, y: 50 + index * 180 };
          }
          
          return {
            id: n.nodeId,
            type: nodeType,
            position,
            data: {
              label: label,
              icon: iconMap[nodeType] || '📝',
              type: n.type,
              config: n.config || {},
            },
          };
        });
        
        const loadedEdges = (workflow.edges || []).map((e: any, i: number) => ({
          id: e.id || `e${i}`,
          source: e.source,
          target: e.target,
          sourceHandle: e.sourceHandle,
          targetHandle: e.targetHandle,
        }));
        
        setNodes(loadedNodes);
        setEdges(loadedEdges);
        setCurrentWorkflowId(id);
        message.success(`已加载工作流: ${workflow.name || '未命名'} (${loadedNodes.length} 节点)`);
      }
    } catch (e) {
      console.error('加载工作流失败', e);
      message.error('加载工作流失败');
    }
  };

  useEffect(() => {
    loadWorkflowList();
  }, []);

  const pollIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const pollCountRef = useRef(0);

  const onConnect = useCallback(
    (params: Connection) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  );

  // 节点类型映射（支持大小写）
  const getNodeType = (type: string): string => {
    const normalizedType = type.toLowerCase();
    const typeMap: Record<string, string> = {
      python: 'python_script',
      python_script: 'python_script',
      http_request: 'http_request',
      conditional: 'conditional',
      loop: 'loop',
      input: 'simple',
      output: 'simple',
      model: 'simple',
      llm: 'llm',
      process: 'simple',
      etl: 'etl',
      llm_recommendation: 'llm_recommendation',
      wx_push: 'wx_push',
      link_scraper: 'http_request', // link_scraper 用 HTTP 节点渲染
    };
    return typeMap[normalizedType] || 'simple';
  };

  // 获取默认节点数据
  const getDefaultNodeData = (type: string, label: string, icon: string) => {
    const baseData = { label, icon, type, config: {} };
    
    const defaultConfigs: Record<string, any> = {
      http_request: { url: 'https://api.example.com', method: 'GET', timeout: 5000, headers: {} },
      conditional: { expression: '', value: '' },
      loop: { items: [], itemVar: 'item', indexVar: 'index', concurrency: 1, maxIterations: 100 },
      model: { modelProvider: '', prompt: '' },
      llm: { model: 'qwen-plus', systemPrompt: '你是一个有用的 AI 助手', userPrompt: '', temperature: 0.7, maxTokens: 2048 },
      etl: { sources: [] },
      llm_recommendation: { model: 'hunyuan-2.0-instruct', userProfile: { profession: 'Java 工程师 + 技术 Leader', businessFocus: 'AI 生图、AI 生视频', interests: ['数码爱好者', '游戏爱好者', '爱狗人士', '业余拳击运动'] } },
    };
    
    return { ...baseData, ...defaultConfigs[type] };
  };

  const addNode = (type: string, label: string, icon: string = label.split(' ')[0]) => {
    const newNode: Node = {
      id: `node-${Date.now()}`,
      type: getNodeType(type),
      position: { x: Math.random() * 400 + 50, y: Math.random() * 400 + 50 },
      data: getDefaultNodeData(type, label, icon),
    };
    setNodes((nds) => [...nds, newNode]);
  };

  const onNodeClick = useCallback((event: React.MouseEvent, node: Node) => {
    setSelectedNode(node);
  }, []);

  const updateNodeStatus = (nodeId: string, status: string, result?: any) => {
    setNodes((nds) =>
      nds.map((node) => {
        if (node.id === nodeId) {
          return { ...node, data: { ...node.data, status } };
        }
        return node;
      })
    );
    
    if (result) {
      setNodeResults((prev) => ({
        ...prev,
        [nodeId]: { status, result, timestamp: new Date().toISOString() },
      }));
    }
  };

  const pollExecutionStatus = async (execId: string) => {
    try {
      const response = await fetch(`http://localhost:8080/api/v1/executions/${execId}`);
      if (!response.ok) return;
      
      const status = await response.json();
      pollCountRef.current += 1;
      
      if (pollCountRef.current > 10) {
        if (pollIntervalRef.current) clearInterval(pollIntervalRef.current);
        pollIntervalRef.current = null;
        setIsExecuting(false);
        message.error('执行超时，请重试');
        return;
      }
      
      if (status.nodeStates) {
        Object.entries(status.nodeStates).forEach(([nodeId, nodeState]: [string, any]) => {
          if (nodeState.status === 'SUCCESS') updateNodeStatus(nodeId, 'SUCCESS', nodeState.result);
          else if (nodeState.status === 'FAILED') updateNodeStatus(nodeId, 'FAILED');
          else if (nodeState.status === 'RUNNING') updateNodeStatus(nodeId, 'RUNNING');
        });
      }
      
      if (status.status === 'SUCCESS' || status.status === 'FAILED') {
        if (pollIntervalRef.current) {
          clearInterval(pollIntervalRef.current);
          pollIntervalRef.current = null;
        }
        setIsExecuting(false);
        setExecutionResult({ executionId: execId, status: status.status, nodeStates: status.nodeStates });
        setShowResultPreview(true);
      }
    } catch (error) {
      console.error('轮询失败:', error);
    }
  };

  const handleExecute = async () => {
    setIsExecuting(true);
    pollCountRef.current = 0;
    setNodeResults({});
    setNodes((nds) => nds.map((node) => ({ ...node, data: { ...node.data, status: undefined } })));
    
    try {
      const workflowData = {
        name: '测试工作流',
        description: '从画布创建的工作流',
        nodes: nodes.map(node => ({
          nodeId: node.id,
          type: node.type?.toUpperCase() || 'DEFAULT',
          position: node.position,
          config: node.data.config || {},
          modelProvider: node.data.config?.modelProvider || undefined,
        })),
        edges: edges.map(edge => ({ ...edge, dataType: 'text' })),
      };

      const saveResponse = await fetch('http://localhost:8080/api/v1/workflows', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-User-Id': 'ben-test' },
        body: JSON.stringify(workflowData),
      });
      const workflow = await saveResponse.json();
      setCurrentWorkflowId(workflow.id);

      const executeResponse = await fetch(`http://localhost:8080/api/v1/workflows/${workflow.id}/execute`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-User-Id': 'ben-test' },
        body: JSON.stringify({}),
      });
      const executeResult = await executeResponse.json();
      setExecutionId(executeResult.executionId);
      
      // 通知父组件执行已开始
      if (onExecutionStart && executeResult.executionId) {
        onExecutionStart(executeResult.executionId);
      }

      pollIntervalRef.current = setInterval(() => {
        pollExecutionStatus(executeResult.executionId);
      }, 2000);

    } catch (error) {
      console.error('执行失败:', error);
      setIsExecuting(false);
      message.error('执行失败：' + (error as Error).message);
    }
  };

  useEffect(() => {
    return () => {
      if (pollIntervalRef.current) clearInterval(pollIntervalRef.current);
    };
  }, []);

  const handleConfigChange = (key: string, value: any) => {
    if (!selectedNode) return;
    const newConfig = { ...selectedNode.data.config, [key]: value };
    setNodes((nds) =>
      nds.map((node) => {
        if (node.id === selectedNode.id) {
          return { ...node, data: { ...node.data, config: newConfig } };
        }
        return node;
      })
    );
    setSelectedNode({ ...selectedNode, data: { ...selectedNode.data, config: newConfig } });
  };

  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: '#1890ff',
          borderRadius: 8,
        },
      }}
    >
      <div style={{ width: '100vw', height: '100vh', display: 'flex', flexDirection: 'column', background: '#f0f2f5' }}>
        {/* 左侧画布区域 */}
        <div style={{ flex: 1, position: 'relative', minHeight: 0 }}>
          {/* 工作流选择器 */}
          <Card
            size="small"
            style={{
              position: 'absolute',
              top: 12,
              left: '240px',
              zIndex: 1000,
              width: '280px',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Select
                style={{ flex: 1 }}
                placeholder="选择工作流"
                value={selectedWorkflow || undefined}
                onChange={(val) => {
                  setSelectedWorkflow(val);
                  if (val) loadWorkflow(val);
                }}
                options={workflowList.map((w) => ({ label: w.name || '未命名', value: w.id }))}
                allowClear
                showSearch
                filterOption={(input, option) =>
                  (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
                }
                onClear={() => {
                  setSelectedWorkflow('');
                  setCurrentWorkflowId(null);
                }}
                virtual={false}
                listHeight={500}
              />
              <Button 
                icon={<ReloadOutlined />} 
                onClick={loadWorkflowList}
                title="刷新列表"
              />
              <Button 
                icon={<ExportOutlined />} 
                onClick={() => {
                  if (!currentWorkflowId) {
                    message.warning('请先选择一个工作流');
                    return;
                  }
                  const workflow = workflowList.find(w => w.id === currentWorkflowId);
                  if (!workflow) {
                    message.error('未找到工作流数据');
                    return;
                  }
                  const blob = new Blob([JSON.stringify(workflow, null, 2)], { type: 'application/json' });
                  const url = URL.createObjectURL(blob);
                  const a = document.createElement('a');
                  a.href = url;
                  a.download = `${workflow.name || 'workflow'}.json`;
                  a.click();
                  URL.revokeObjectURL(url);
                  message.success('导出成功');
                }}
                title="导出工作流"
              />
              <Button 
                icon={<AppstoreOutlined />} 
                onClick={() => {
                  const input = document.createElement('input');
                  input.type = 'file';
                  input.accept = '.json';
                  input.onchange = async (e: any) => {
                    const file = e.target.files[0];
                    if (!file) return;
                    try {
                      const text = await file.text();
                      const workflow = JSON.parse(text);
                      const res = await fetch('http://localhost:8080/api/v1/workflows', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(workflow)
                      });
                      if (res.ok) {
                        message.success('导入成功');
                        loadWorkflowList();
                      } else {
                        message.error('导入失败');
                      }
                    } catch (err) {
                      message.error('解析文件失败');
                    }
                  };
                  input.click();
                }}
                title="导入工作流"
              />
            </div>
          </Card>

          {/* 左侧节点分类面板 - 参考 Dify/LangFlow */}
          <NodeSidebar
            onAddNode={(type, label, icon) => addNode(type, label, icon)}
          />

          {/* 画布 */}
          <div data-testid="canvas" className="canvas-container" style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0 }}>
            <ReactFlow
              nodes={nodes}
              edges={edges}
              onNodesChange={onNodesChange}
              onEdgesChange={onEdgesChange}
              onConnect={onConnect}
              onNodeClick={onNodeClick}
              onInit={(instance) => {
                // 初始化后自动适应视图
                instance.fitView({ padding: 0.2 });
              }}
              onDragOver={(event) => event.preventDefault()}
              onDrop={(event) => {
                event.preventDefault();
                const type = event.dataTransfer.getData('application/reactflow');
                if (!type) return;
                const reactFlowBounds = event.currentTarget.getBoundingClientRect();
                const position = {
                  x: event.clientX - reactFlowBounds.left - 100,
                  y: event.clientY - reactFlowBounds.top - 25,
                };
                const labelMap: Record<string, string> = {
                  input: '输入节点', model: '模型节点', llm: 'LLM 节点',
                  python: 'Python 脚本', process: '处理节点', output: '输出节点',
                  http_request: 'HTTP 请求', conditional: '条件判断', loop: '循环处理',
                  etl: 'ETL 数据清洗', llm_recommendation: 'LLM 智能推荐'
                };
                addNode(type, labelMap[type] || type);
              }}
              nodeTypes={nodeTypes}
              style={{ background: 'transparent' }}
              data-testid="react-flow"
              fitView={{ padding: 0.2, duration: 300 }}
            >
              <Controls />
              <MiniMap
                nodeColor={(node) => {
                  if (node.data?.status === 'SUCCESS') return '#52c41a';
                  if (node.data?.status === 'FAILED') return '#ff4d4f';
                  if (node.data?.status === 'RUNNING') return '#faad14';
                  return '#999';
                }}
                style={{ width: 120, height: 80 }}
              />
              <Background variant="dots" gap={12} size={1} />
            </ReactFlow>
          </div>
        </div>

        {/* 右侧信息面板 */}
        <div style={{
          width: '450px',
          background: '#fff',
          borderLeft: '1px solid #e8e8e8',
          padding: '20px',
          overflowY: 'auto',
        }}>
          {/* 执行按钮 - 参考 Dify 设计 */}
          <Button
            type="primary"
            size="large"
            icon={isExecuting ? <LoadingOutlined /> : <PlayCircleOutlined />}
            onClick={handleExecute}
            disabled={isExecuting}
            style={{
              width: '100%',
              marginBottom: '24px',
              height: '50px',
              fontSize: '16px',
              fontWeight: 600,
              border: 'none',
              borderRadius: '8px',
              cursor: isExecuting ? 'not-allowed' : 'pointer',
              background: isExecuting ? '#cbd5e1' : 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)',
              color: isExecuting ? '#64748b' : 'white',
              boxShadow: isExecuting ? 'none' : '0 4px 12px rgba(59, 130, 246, 0.4)',
            }}
          >
            {isExecuting ? '执行中...' : '执行工作流'}
          </Button>

          {/* 选中节点的详细信息 */}
          {selectedNode ? (
            <Space direction="vertical" style={{ width: '100%' }} size="middle">
              {/* 节点配置 */}
              {selectedNode.type === 'python_script' ? (
                <PythonConfigPanel
                  node={selectedNode}
                  onClose={() => setSelectedNode(null)}
                  onSave={(data) => {
                    setNodes((nds) =>
                      nds.map((node) => {
                        if (node.id === selectedNode.id) {
                          return { ...node, data: { ...node.data, ...data } };
                        }
                        return node;
                      })
                    );
                    message.success('Python 脚本配置已保存');
                  }}
                />
              ) : selectedNode.type === 'http_request' ? (
                <Card
                  title={<><SettingOutlined /> 🌐 HTTP 请求 - 配置</>}
                  size="small"
                >
                  <Space direction="vertical" style={{ width: '100%' }}>
                    <div>
                      <div style={{ fontWeight: 500, marginBottom: 4 }}>URL 地址</div>
                      <Input
                        value={selectedNode.data.config?.url || ''}
                        onChange={(e) => handleConfigChange('url', e.target.value)}
                        placeholder="https://api.example.com"
                      />
                    </div>
                    <div>
                      <div style={{ fontWeight: 500, marginBottom: 4 }}>请求方法</div>
                      <Select
                        value={selectedNode.data.config?.method || 'GET'}
                        onChange={(v) => handleConfigChange('method', v)}
                        options={[{label:'GET',value:'GET'},{label:'POST',value:'POST'},{label:'PUT',value:'PUT'},{label:'DELETE',value:'DELETE'}]}
                        style={{ width: '100%' }}
                      />
                    </div>
                    <Button type="primary" onClick={() => message.success('配置已保存')}>保存配置</Button>
                  </Space>
                </Card>
              ) : selectedNode.type === 'conditional' ? (
                <Card
                  title={<><SettingOutlined /> 🔀 条件判断 - 配置</>}
                  size="small"
                >
                  <Space direction="vertical" style={{ width: '100%' }}>
                    <div>
                      <div style={{ fontWeight: 500, marginBottom: 4 }}>条件表达式</div>
                      <Input
                        value={selectedNode.data.config?.expression || ''}
                        onChange={(e) => handleConfigChange('expression', e.target.value)}
                        placeholder="status_code == 200"
                      />
                    </div>
                    <div>
                      <div style={{ fontWeight: 500, marginBottom: 4 }}>比较值</div>
                      <Input
                        value={selectedNode.data.config?.value || ''}
                        onChange={(e) => handleConfigChange('value', e.target.value)}
                        placeholder="200"
                      />
                    </div>
                    <Button type="primary" onClick={() => message.success('配置已保存')}>保存配置</Button>
                  </Space>
                </Card>
              ) : selectedNode.type === 'loop' ? (
                <Card
                  title={<><SettingOutlined /> 🔄 循环处理 - 配置</>}
                  size="small"
                >
                  <Space direction="vertical" style={{ width: '100%' }}>
                    <div>
                      <div style={{ fontWeight: 500, marginBottom: 4 }}>循环项 (JSON 数组)</div>
                      <TextArea
                        value={JSON.stringify(selectedNode.data.config?.items || [], null, 2)}
                        onChange={(e) => {
                          try {
                            const parsed = JSON.parse(e.target.value);
                            handleConfigChange('items', parsed);
                          } catch {}
                        }}
                        placeholder='["a", "b", "c"]'
                        rows={3}
                      />
                    </div>
                    <div>
                      <div style={{ fontWeight: 500, marginBottom: 4 }}>循环变量名</div>
                      <Input
                        value={selectedNode.data.config?.itemVar || 'item'}
                        onChange={(e) => handleConfigChange('itemVar', e.target.value)}
                        placeholder="item"
                      />
                    </div>
                    <Button type="primary" onClick={() => message.success('配置已保存')}>保存配置</Button>
                  </Space>
                </Card>
              ) : selectedNode.type === 'llm' ? (
                <Card
                  title={<><SettingOutlined /> <RobotOutlined /> LLM 节点 - 配置</>}
                  size="small"
                >
                  <Space direction="vertical" style={{ width: '100%' }}>
                    <div>
                      <div style={{ fontWeight: 500, marginBottom: 4 }}>🤖 模型选择</div>
                      <Select
                        value={selectedNode.data.config?.model || 'qwen-plus'}
                        onChange={(v) => handleConfigChange('model', v)}
                        style={{ width: '100%' }}
                      >
                        <OptGroup label="⭐ Qwen 系列（推荐）">
                          <Option value="qwen-plus">Qwen-Plus</Option>
                          <Option value="qwen-max">Qwen-Max</Option>
                          <Option value="qwen-turbo">Qwen-Turbo</Option>
                          <Option value="qwen-long">Qwen-Long</Option>
                        </OptGroup>
                        <OptGroup label="🚀 MiniMax">
                          <Option value="minimax-m2">MiniMax-M2</Option>
                          <Option value="minimax-m1">MiniMax-M1</Option>
                        </OptGroup>
                        <OptGroup label="🌙 Kimi">
                          <Option value="kimi-latest">Kimi</Option>
                          <Option value="kimi-plus">Kimi Plus</Option>
                        </OptGroup>
                        <OptGroup label="🌍 OpenAI">
                          <Option value="gpt-4">GPT-4</Option>
                          <Option value="gpt-4-turbo">GPT-4 Turbo</Option>
                          <Option value="gpt-3.5-turbo">GPT-3.5 Turbo</Option>
                        </OptGroup>
                        <OptGroup label="🤖 Anthropic">
                          <Option value="claude-3-opus">Claude 3 Opus</Option>
                          <Option value="claude-3-sonnet">Claude 3 Sonnet</Option>
                          <Option value="claude-3-haiku">Claude 3 Haiku</Option>
                        </OptGroup>
                      </Select>
                    </div>
                    <div>
                      <div style={{ fontWeight: 500, marginBottom: 4 }}>📋 系统提示词</div>
                      <TextArea
                        value={selectedNode.data.config?.systemPrompt || '你是一个有用的 AI 助手'}
                        onChange={(e) => handleConfigChange('systemPrompt', e.target.value)}
                        placeholder="设置 AI 的角色和行为"
                        rows={2}
                      />
                    </div>
                    <div>
                      <div style={{ fontWeight: 500, marginBottom: 4 }}>💬 用户提示词</div>
                      <TextArea
                        value={selectedNode.data.config?.userPrompt || ''}
                        onChange={(e) => handleConfigChange('userPrompt', e.target.value)}
                        placeholder="输入用户提示词，支持变量如 {{input}}"
                        rows={3}
                      />
                      <div style={{ fontSize: '11px', color: '#999', marginTop: 4 }}>
                        提示: 使用 {"{{变量名}}"} 引用上游节点输出
                      </div>
                    </div>
                    <div>
                      <div style={{ fontWeight: 500, marginBottom: 4 }}>🌡️ 温度 (Temperature)</div>
                      <Input
                        type="number"
                        value={selectedNode.data.config?.temperature ?? 0.7}
                        onChange={(e) => handleConfigChange('temperature', parseFloat(e.target.value) || 0.7)}
                        min={0}
                        max={2}
                        step={0.1}
                      />
                      <div style={{ fontSize: '11px', color: '#999', marginTop: 4 }}>
                        控制输出随机性: 0=确定性, 2=高随机性
                      </div>
                    </div>
                    <div>
                      <div style={{ fontWeight: 500, marginBottom: 4 }}>📏 最大 Tokens</div>
                      <Input
                        type="number"
                        value={selectedNode.data.config?.maxTokens ?? 2048}
                        onChange={(e) => handleConfigChange('maxTokens', parseInt(e.target.value) || 2048)}
                        min={256}
                        max={32768}
                        step={256}
                      />
                    </div>
                    <Button type="primary" onClick={() => message.success('配置已保存')}>保存配置</Button>
                  </Space>
                </Card>
              ) : (
              <Card
                title={<><SettingOutlined /> {selectedNode.data.icon} {selectedNode.data.label} - 配置</>}
                size="small"
                bordered
              >
                <Space direction="vertical" style={{ width: '100%' }} size="middle">
                  {/* 模型选择下拉框 - 仅模型节点显示 */}
                  {selectedNode.data.type === 'model' && (
                    <div>
                      <div style={{ fontWeight: 500, marginBottom: '8px' }}>🎨 模型提供商</div>
                      <Select
                        value={selectedNode.data.config?.modelProvider || undefined}
                        onChange={(value) => handleConfigChange('modelProvider', value)}
                        placeholder="请选择模型提供商"
                        style={{ width: '100%' }}
                        allowClear
                      >
                        <OptGroup label="🎬 视频生成">
                          <Option value="kling">🎬 可灵 (Kling)</Option>
                          <Option value="seedance">🎬 即梦 (Seedance)</Option>
                        </OptGroup>
                        <OptGroup label="🖼️ 图片生成">
                          <Option value="wan">🎨 海螺 (Wan)</Option>
                          <Option value="nanobanana">🎨 NanoBanana</Option>
                        </OptGroup>
                      </Select>
                    </div>
                  )}

                  <div>
                    <div style={{ fontWeight: 500, marginBottom: '8px' }}>📝 提示词</div>
                    <TextArea
                      value={selectedNode.data.config?.prompt || ''}
                      onChange={(e) => handleConfigChange('prompt', e.target.value)}
                      placeholder="输入提示词"
                      rows={3}
                      showCount
                      maxLength={1000}
                    />
                  </div>

                  {['kling', 'seedance'].includes(selectedNode.data.config?.modelProvider) && (
                    <>
                      <div>
                        <div style={{ fontWeight: 500, marginBottom: '8px' }}>⏱️ 视频时长（秒）</div>
                        <Input
                          type="number"
                          value={selectedNode.data.config?.duration || 5}
                          onChange={(e) => handleConfigChange('duration', parseInt(e.target.value) || 5)}
                          min={1}
                          max={30}
                        />
                      </div>
                      <div>
                        <div style={{ fontWeight: 500, marginBottom: '8px' }}>🎬 帧率（FPS）</div>
                        <Select
                          value={selectedNode.data.config?.fps || 24}
                          onChange={(value) => handleConfigChange('fps', value)}
                          style={{ width: '100%' }}
                        >
                          <Option value={24}>24 FPS（电影标准）</Option>
                          <Option value={30}>30 FPS（电视标准）</Option>
                          <Option value={60}>60 FPS（高帧率）</Option>
                        </Select>
                      </div>
                    </>
                  )}

                  {['wan', 'nanobanana'].includes(selectedNode.data.config?.modelProvider) && (
                    <div>
                      <div style={{ fontWeight: 500, marginBottom: '8px' }}>🖼️ 图片尺寸</div>
                      <Select
                        value={selectedNode.data.config?.size || '1024x1024'}
                        onChange={(value) => handleConfigChange('size', value)}
                        style={{ width: '100%' }}
                      >
                        <Option value="512x512">512×512</Option>
                        <Option value="1024x1024">1024×1024（标准）</Option>
                        <Option value="1024x1536">1024×1536（竖版）</Option>
                        <Option value="1536x1024">1536×1024（横版）</Option>
                      </Select>
                    </div>
                  )}

                  {selectedNode.data.label?.includes('LLM') && (
                    <>
                      <div>
                        <div style={{ fontWeight: 500, marginBottom: '8px' }}>🤖 模型选择</div>
                        <Select
                          value={selectedNode.data.config?.model || 'qwen-plus'}
                          onChange={(value) => handleConfigChange('model', value)}
                          style={{ width: '100%' }}
                        >
                          <OptGroup label="⭐ Qwen 系列（推荐）">
                            <Option value="qwen-plus">Qwen-Plus</Option>
                            <Option value="qwen-max">Qwen-Max</Option>
                            <Option value="qwen-turbo">Qwen-Turbo</Option>
                            <Option value="qwen-long">Qwen-Long</Option>
                          </OptGroup>
                          <OptGroup label="🚀 MiniMax">
                            <Option value="minimax-m2">MiniMax-M2</Option>
                            <Option value="minimax-m1">MiniMax-M1</Option>
                          </OptGroup>
                          <OptGroup label="🌙 Kimi">
                            <Option value="kimi-latest">Kimi</Option>
                            <Option value="kimi-plus">Kimi Plus</Option>
                          </OptGroup>
                        </Select>
                      </div>
                      <div>
                        <div style={{ fontWeight: 500, marginBottom: '8px' }}>📋 系统提示词</div>
                        <TextArea
                          value={selectedNode.data.config?.systemPrompt || '你是一个有用的 AI 助手'}
                          onChange={(e) => handleConfigChange('systemPrompt', e.target.value)}
                          placeholder="设置 AI 的角色和行为"
                          rows={2}
                        />
                      </div>
                      <div>
                        <div style={{ fontWeight: 500, marginBottom: '8px' }}>🌡️ 温度</div>
                        <Input
                          type="number"
                          value={selectedNode.data.config?.temperature || 0.7}
                          onChange={(e) => handleConfigChange('temperature', parseFloat(e.target.value) || 0.7)}
                          min={0}
                          max={2}
                          step={0.1}
                        />
                      </div>
                      <div>
                        <div style={{ fontWeight: 500, marginBottom: '8px' }}>📏 最大 Tokens</div>
                        <Input
                          type="number"
                          value={selectedNode.data.config?.maxTokens || 2048}
                          onChange={(e) => handleConfigChange('maxTokens', parseInt(e.target.value) || 2048)}
                          min={256}
                          max={32768}
                          step={256}
                        />
                      </div>
                    </>
                  )}
                </Space>
                <Divider style={{ margin: '16px 0 12px' }} />
                <div style={{ textAlign: 'center', color: '#52c41a', fontSize: '13px' }}>
                  <CheckCircleOutlined /> 配置已自动保存
                </div>
              </Card>
              )}

              {/* 执行结果 */}
              <Card
                title={<><CheckCircleOutlined /> {selectedNode.data.icon} {selectedNode.data.label} - 执行结果</>}
                size="small"
                bordered
              >
                {nodeResults[selectedNode.id] ? (
                  <Space direction="vertical" style={{ width: '100%' }} size="middle">
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span>状态:</span>
                      {nodeResults[selectedNode.id].status === 'SUCCESS' && (
                        <Tag color="success" icon={<CheckCircleOutlined />}>成功</Tag>
                      )}
                      {nodeResults[selectedNode.id].status === 'FAILED' && (
                        <Tag color="error" icon={<CloseCircleOutlined />}>失败</Tag>
                      )}
                      {nodeResults[selectedNode.id].status === 'RUNNING' && (
                        <Tag color="warning" icon={<LoadingOutlined />}>执行中</Tag>
                      )}
                    </div>
                    
                    {nodeResults[selectedNode.id].result && (
                      <div>
                        {nodeResults[selectedNode.id].result.type === 'video' && (
                          <div>
                            <div style={{ color: '#666', marginBottom: '8px' }}>
                              🎬 视频 | {nodeResults[selectedNode.id].result.duration}秒 | {nodeResults[selectedNode.id].result.fps} FPS
                            </div>
                            {nodeResults[selectedNode.id].result.previewUrl && (
                              <img src={nodeResults[selectedNode.id].result.previewUrl} alt="预览" style={{ width: '100%', borderRadius: '8px' }} />
                            )}
                            {nodeResults[selectedNode.id].result.url && (
                              <Button type="primary" href={nodeResults[selectedNode.id].result.url} target="_blank" block style={{ marginTop: '8px' }}>
                                🔗 查看视频
                              </Button>
                            )}
                          </div>
                        )}
                        {nodeResults[selectedNode.id].result.type === 'image' && (
                          <div>
                            <div style={{ color: '#666', marginBottom: '8px' }}>
                              🖼️ 图片 | {nodeResults[selectedNode.id].result.width}×{nodeResults[selectedNode.id].result.height}
                            </div>
                            {nodeResults[selectedNode.id].result.url && (
                              <img src={nodeResults[selectedNode.id].result.url} alt="结果" style={{ width: '100%', borderRadius: '8px' }} />
                            )}
                            {nodeResults[selectedNode.id].result.url && (
                              <Button type="primary" href={nodeResults[selectedNode.id].result.url} target="_blank" block style={{ marginTop: '8px' }}>
                                🔗 查看大图
                              </Button>
                            )}
                          </div>
                        )}
                        {/* 通用 JSON 结果展示 */}
                        {(!nodeResults[selectedNode.id].result.type || (nodeResults[selectedNode.id].result.type !== 'video' && nodeResults[selectedNode.id].result.type !== 'image')) && (
                          <div style={{ background: '#f5f5f5', borderRadius: '8px', padding: '12px', maxHeight: '300px', overflow: 'auto' }}>
                            <pre style={{ margin: 0, fontSize: '12px', color: '#333' }}>
                              {JSON.stringify(nodeResults[selectedNode.id].result, null, 2)}
                            </pre>
                          </div>
                        )}
                      </div>
                    )}
                  </Space>
                ) : (
                  <Empty description="暂无执行结果" image={Empty.PRESENTED_IMAGE_SIMPLE}>
                    <div style={{ color: '#999', fontSize: '13px' }}>点击上方按钮执行工作流</div>
                  </Empty>
                )}
              </Card>
            </Space>
          ) : (
            <Card style={{ marginTop: '20px', textAlign: 'center' }}>
              <Empty description="点击左侧节点卡片，查看该节点的配置和执行结果" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            </Card>
          )}
        </div>

        {/* 执行结果预览弹窗 */}
        {showResultPreview && executionResult && (
          <ResultPreview
            result={executionResult}
            onClose={() => {
              setShowResultPreview(false);
              setExecutionResult(null);
            }}
          />
        )}
      </div>
    </ConfigProvider>
  );
}
