import { useState, useEffect, useCallback } from 'react';
import {
  Table, Button, Tag, Space, Popconfirm, message, Typography, Card, Tooltip, Empty,
} from 'antd';
import {
  PlusOutlined, PauseCircleOutlined, PlayCircleOutlined,
  DeleteOutlined, ReloadOutlined, ClockCircleOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { schedulerApi, SchedulerJob } from '../api/schedulerApi';
import CreateSchedulerJobModal from './CreateSchedulerJobModal';

const { Title, Text } = Typography;

/** 状态颜色映射 */
const STATUS_MAP: Record<string, { color: string; label: string }> = {
  RUNNING: { color: 'green', label: '运行中' },
  PAUSED: { color: 'orange', label: '已暂停' },
  COMPLETED: { color: 'default', label: '已完成' },
};

/** 格式化时间 */
function formatTime(iso: string | null): string {
  if (!iso) return '-';
  try {
    return new Date(iso).toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai' });
  } catch {
    return iso;
  }
}

export default function SchedulerPage() {
  const [jobs, setJobs] = useState<SchedulerJob[]>([]);
  const [loading, setLoading] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);

  const loadJobs = useCallback(async () => {
    setLoading(true);
    try {
      const data = await schedulerApi.listJobs();
      setJobs(data);
    } catch (e: any) {
      message.error('加载定时任务列表失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadJobs();
  }, [loadJobs]);

  const handlePause = async (id: string) => {
    try {
      await schedulerApi.pauseJob(id);
      message.success('已暂停');
      loadJobs();
    } catch (e: any) {
      message.error(e?.message || '操作失败');
    }
  };

  const handleResume = async (id: string) => {
    try {
      await schedulerApi.resumeJob(id);
      message.success('已恢复');
      loadJobs();
    } catch (e: any) {
      message.error(e?.message || '操作失败');
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await schedulerApi.deleteJob(id);
      message.success('已删除');
      loadJobs();
    } catch (e: any) {
      message.error(e?.message || '删除失败');
    }
  };

  const columns: ColumnsType<SchedulerJob> = [
    {
      title: '任务ID',
      dataIndex: 'id',
      key: 'id',
      width: 120,
      ellipsis: true,
      render: (id: string) => (
        <Tooltip title={id}>
          <Text copyable={{ text: id }} style={{ fontFamily: 'monospace', fontSize: 12 }}>
            {id.substring(0, 8)}...
          </Text>
        </Tooltip>
      ),
    },
    {
      title: '工作流',
      dataIndex: 'workflowName',
      key: 'workflowName',
      width: 180,
      ellipsis: true,
      render: (name: string, record: SchedulerJob) => (
        <Tooltip title={`ID: ${record.workflowId}`}>
          <span>{name || record.workflowId?.substring(0, 8)}</span>
        </Tooltip>
      ),
    },
    {
      title: 'Cron 表达式',
      dataIndex: 'cronExpression',
      key: 'cronExpression',
      width: 180,
      render: (cron: string) => (
        <Tag icon={<ClockCircleOutlined />} style={{ fontFamily: 'monospace' }}>
          {cron}
        </Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => {
        const s = STATUS_MAP[status] || { color: 'default', label: status };
        return <Tag color={s.color}>{s.label}</Tag>;
      },
    },
    {
      title: '下次执行时间',
      dataIndex: 'nextFireTime',
      key: 'nextFireTime',
      width: 180,
      render: formatTime,
    },
    {
      title: '上次执行时间',
      dataIndex: 'lastFireTime',
      key: 'lastFireTime',
      width: 180,
      render: formatTime,
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      width: 150,
      ellipsis: true,
      render: (desc: string) => desc || '-',
    },
    {
      title: '操作',
      key: 'actions',
      width: 180,
      fixed: 'right',
      render: (_: any, record: SchedulerJob) => (
        <Space size="small">
          {record.status === 'RUNNING' && (
            <Tooltip title="暂停">
              <Button
                type="text"
                size="small"
                icon={<PauseCircleOutlined />}
                onClick={() => handlePause(record.id)}
              />
            </Tooltip>
          )}
          {record.status === 'PAUSED' && (
            <Tooltip title="恢复">
              <Button
                type="text"
                size="small"
                icon={<PlayCircleOutlined style={{ color: '#52c41a' }} />}
                onClick={() => handleResume(record.id)}
              />
            </Tooltip>
          )}
          <Popconfirm
            title="确定删除此定时任务？"
            onConfirm={() => handleDelete(record.id)}
            okText="删除"
            cancelText="取消"
          >
            <Tooltip title="删除">
              <Button type="text" size="small" danger icon={<DeleteOutlined />} />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <Title level={4} style={{ margin: 0 }}>
            <ClockCircleOutlined style={{ marginRight: 8 }} />
            定时任务管理
          </Title>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={loadJobs} loading={loading}>
              刷新
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setShowCreateModal(true)}>
              新建定时任务
            </Button>
          </Space>
        </div>

        <Table
          columns={columns}
          dataSource={jobs}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10 }}
          scroll={{ x: 1200 }}
          locale={{
            emptyText: (
              <Empty description="暂无定时任务" image={Empty.PRESENTED_IMAGE_SIMPLE}>
                <Button type="primary" onClick={() => setShowCreateModal(true)}>
                  创建第一个定时任务
                </Button>
              </Empty>
            ),
          }}
        />
      </Card>

      <CreateSchedulerJobModal
        open={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onCreated={loadJobs}
      />
    </div>
  );
}
