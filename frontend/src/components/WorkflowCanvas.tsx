import { useCallback, useState, useEffect, useRef } from 'react';
import {
  ReactFlow,
  Controls,
  Background,
  MiniMap,
  useNodesState,
  useEdgesState,
  addEdge,
  Connection,
  Edge,
  Node,
  NodeClickHandler,
  Handle,
  Position,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
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
} from '@ant-design/icons';
import ResultPreview from './ResultPreview';

const { TextArea } = Input;
const { Option, OptGroup } = Select;

// 简化的节点组件
function SimpleNode({ data, id }: any) {
  return (
    <div 
      data-testid={`node-${id}`}
      className="react-flow__node-simple"
      style={{
      padding: '16px 24px',
      borderRadius: '12px',
      background: data.status === 'SUCCESS' ? '#f6ffed' : 
                  data.status === 'FAILED' ? '#fff2f0' : 
                  data.status === 'RUNNING' ? '#fffbe6' : '#fff',
      border: `3px solid ${data.status === 'SUCCESS' ? '#52c41a' : 
                          data.status === 'FAILED' ? '#ff4d4f' : 
                          data.status === 'RUNNING' ? '#faad14' : '#d9d9d9'}`,
      minWidth: '200px',
      boxShadow: '0 2px 12px rgba(0,0,0,0.08)',
    }}>
      <Handle type="target" position={Position.Top} style={{ opacity: 0 }} />
      
      <div style={{ fontWeight: 600, fontSize: '15px', display: 'flex', alignItems: 'center', gap: '8px' }}>
        <span style={{ fontSize: '22px' }}>{data.icon}</span>
        <span>{data.label}</span>
        {data.status && (
          <span style={{ marginLeft: 'auto', fontSize: '18px' }}>
            {data.status === 'SUCCESS' && <CheckCircleOutlined style={{ color: '#52c41a' }} />}
            {data.status === 'FAILED' && <CloseCircleOutlined style={{ color: '#ff4d4f' }} />}
            {data.status === 'RUNNING' && <LoadingOutlined style={{ color: '#faad14' }} />}
          </span>
        )}
      </div>

      <Handle type="source" position={Position.Bottom} style={{ opacity: 0 }} />
    </div>
  );
}

const nodeTypes = {
  simple: SimpleNode,
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

export default function WorkflowCanvas() {
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);
  
  const [selectedNode, setSelectedNode] = useState<Node | null>(null);
  const [showResultPreview, setShowResultPreview] = useState(false);
  const [isExecuting, setIsExecuting] = useState(false);
  const [executionResult, setExecutionResult] = useState<any>(null);
  const [currentWorkflowId, setCurrentWorkflowId] = useState<string | null>(null);
  const [executionId, setExecutionId] = useState<string | null>(null);
  const [nodeResults, setNodeResults] = useState<Record<string, any>>({});

  const pollIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const pollCountRef = useRef(0);

  const onConnect = useCallback(
    (params: Connection) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  );

  const addNode = (type: string, label: string) => {
    const newNode: Node = {
      id: `node-${Date.now()}`,
      type: 'simple',
      position: { x: Math.random() * 400 + 50, y: Math.random() * 400 + 50 },
      data: { label, icon: label.split(' ')[0] },
    };
    setNodes((nds) => [...nds, newNode]);
  };

  const onNodeClick: NodeClickHandler = useCallback((event, node) => {
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
          modelProvider: node.data.label?.includes('可灵') ? 'kling' : 
                        node.data.label?.includes('万相') ? 'wan' :
                        node.data.label?.includes('Seedance') ? 'seedance' :
                        node.data.label?.includes('NanoBanana') ? 'nanobanana' : undefined,
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
      <div style={{ width: '100vw', height: '100vh', display: 'flex', background: '#f0f2f5' }}>
        {/* 左侧画布区域 */}
        <div style={{ flex: 1, position: 'relative' }}>
          {/* 顶部工具栏 */}
          <Card
            size="small"
            style={{
              position: 'absolute',
              top: 12,
              left: 12,
              zIndex: 1000,
              width: '200px',
            }}
          >
            <div style={{ fontWeight: 600, marginBottom: '12px', fontSize: '14px' }}>
              <AppstoreOutlined style={{ marginRight: '8px' }} />
              节点工具箱
            </div>
            <Space direction="vertical" style={{ width: '100%' }} size="small">
              <Button icon={<span>📝</span>} onClick={() => addNode('input', '输入节点')} block>
                输入节点
              </Button>
              <Button icon={<span>🎨</span>} onClick={() => addNode('model', '模型节点')} block>
                模型节点
              </Button>
              <Button icon={<RobotOutlined />} onClick={() => addNode('llm', 'LLM 节点')} block>
                LLM 节点
              </Button>
              <Button icon={<ThunderboltOutlined />} onClick={() => addNode('process', '处理节点')} block>
                处理节点
              </Button>
              <Button icon={<ExportOutlined />} onClick={() => addNode('output', '输出节点')} block>
                输出节点
              </Button>
            </Space>
          </Card>

          {/* 画布 */}
          <div data-testid="canvas" className="canvas-container" style={{ width: '100%', height: '100%' }}>
            <ReactFlow
              nodes={nodes}
              edges={edges}
              onNodesChange={onNodesChange}
              onEdgesChange={onEdgesChange}
              onConnect={onConnect}
              onNodeClick={onNodeClick}
              fitView
              nodeTypes={nodeTypes}
              style={{ background: '#fafafa' }}
              data-testid="react-flow"
            >
              <Controls />
              <MiniMap
                nodeStrokeColor={(n) => '#1890ff'}
                nodeColor={(n) => {
                  if (n.data?.status === 'SUCCESS') return '#f6ffed';
                  if (n.data?.status === 'FAILED') return '#fff2f0';
                  if (n.data?.status === 'RUNNING') return '#fffbe6';
                  return '#fff';
                }}
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
          {/* 执行按钮 */}
          <Button
            type="primary"
            size="large"
            icon={isExecuting ? <LoadingOutlined /> : <PlayCircleOutlined />}
            onClick={handleExecute}
            disabled={isExecuting}
            style={{ width: '100%', marginBottom: '24px', height: '50px', fontSize: '16px' }}
          >
            {isExecuting ? '执行中...' : '执行工作流'}
          </Button>

          {/* 选中节点的详细信息 */}
          {selectedNode ? (
            <Space direction="vertical" style={{ width: '100%' }} size="middle">
              {/* 节点配置 */}
              <Card
                title={<><SettingOutlined /> {selectedNode.data.icon} {selectedNode.data.label} - 配置</>}
                size="small"
                bordered
              >
                <Space direction="vertical" style={{ width: '100%' }} size="middle">
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

                  {(selectedNode.data.label?.includes('模型') || selectedNode.data.label?.includes('可灵')) && (
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

                  {(selectedNode.data.label?.includes('万相') || selectedNode.data.label?.includes('NanoBanana')) && (
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
