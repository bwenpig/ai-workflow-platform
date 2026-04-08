import { useState, useEffect } from 'react';
import { Modal, Form, Select, Input, message, Typography, Space, Tag } from 'antd';
import { ClockCircleOutlined } from '@ant-design/icons';
import { workflowApi, Workflow } from '../api/workflowApi';
import { schedulerApi } from '../api/schedulerApi';

const { Text } = Typography;

interface Props {
  open: boolean;
  onClose: () => void;
  onCreated: () => void;
}

/** 常用 cron 预设 */
const CRON_PRESETS = [
  { label: '每分钟', value: '0 * * * * *', desc: '每分钟执行一次' },
  { label: '每5分钟', value: '0 */5 * * * *', desc: '每5分钟执行一次' },
  { label: '每小时', value: '0 0 * * * *', desc: '每小时整点执行' },
  { label: '每天 9:00', value: '0 0 9 * * *', desc: '每天上午9点执行' },
  { label: '每天 18:00', value: '0 0 18 * * *', desc: '每天下午6点执行' },
  { label: '工作日 9:00', value: '0 0 9 * * MON-FRI', desc: '周一至周五上午9点' },
  { label: '每周一 10:00', value: '0 0 10 * * MON', desc: '每周一上午10点' },
  { label: '每月1号 0:00', value: '0 0 0 1 * *', desc: '每月1号零点执行' },
];

export default function CreateSchedulerJobModal({ open, onClose, onCreated }: Props) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const [loadingWorkflows, setLoadingWorkflows] = useState(false);

  useEffect(() => {
    if (open) {
      loadWorkflows();
      form.resetFields();
    }
  }, [open]);

  const loadWorkflows = async () => {
    setLoadingWorkflows(true);
    try {
      const data = await workflowApi.listWorkflows();
      setWorkflows(data);
    } catch (e) {
      message.error('加载工作流列表失败');
    } finally {
      setLoadingWorkflows(false);
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      await schedulerApi.createJob({
        workflowId: values.workflowId,
        cronExpression: values.cronExpression,
        description: values.description,
      });
      message.success('定时任务创建成功');
      onCreated();
      onClose();
    } catch (e: any) {
      if (e?.errorFields) return; // form validation error
      message.error(e?.message || '创建失败');
    } finally {
      setLoading(false);
    }
  };

  const handlePresetSelect = (value: string) => {
    form.setFieldValue('cronExpression', value);
  };

  return (
    <Modal
      title={
        <Space>
          <ClockCircleOutlined />
          <span>新建定时任务</span>
        </Space>
      }
      open={open}
      onOk={handleSubmit}
      onCancel={onClose}
      confirmLoading={loading}
      okText="创建"
      cancelText="取消"
      width={520}
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item
          name="workflowId"
          label="选择工作流"
          rules={[{ required: true, message: '请选择工作流' }]}
        >
          <Select
            placeholder="请选择要定时执行的工作流"
            loading={loadingWorkflows}
            showSearch
            optionFilterProp="label"
            options={workflows.map(wf => ({
              label: `${wf.name}${wf.description ? ' - ' + wf.description : ''}`,
              value: wf.id,
            }))}
          />
        </Form.Item>

        <Form.Item label="快捷选择">
          <Space wrap>
            {CRON_PRESETS.map(preset => (
              <Tag
                key={preset.value}
                color="blue"
                style={{ cursor: 'pointer' }}
                onClick={() => handlePresetSelect(preset.value)}
              >
                {preset.label}
              </Tag>
            ))}
          </Space>
        </Form.Item>

        <Form.Item
          name="cronExpression"
          label="Cron 表达式"
          rules={[{ required: true, message: '请输入 Cron 表达式' }]}
          extra={
            <Text type="secondary" style={{ fontSize: 12 }}>
              格式：秒 分 时 日 月 星期（Spring 6位 cron），例如 0 0 9 * * * 表示每天9点
            </Text>
          }
        >
          <Input placeholder="0 0 9 * * *" style={{ fontFamily: 'monospace' }} />
        </Form.Item>

        <Form.Item name="description" label="描述（可选）">
          <Input.TextArea placeholder="任务描述" rows={2} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
