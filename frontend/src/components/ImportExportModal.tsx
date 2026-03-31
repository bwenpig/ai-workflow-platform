/**
 * 导入导出对话框组件
 * F038: 覆盖确认 - 确认对话框
 */

import React, { useState, useRef } from 'react';
import { Modal, Upload, message, Button, Space, Typography, Tag } from 'antd';
import { UploadOutlined, DownloadOutlined, FileTextOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import type { UploadFile } from 'antd/es/upload/interface';
import {
  exportWorkflow,
  importWorkflowFromFile,
  getExportPreview,
  getFriendlyErrorMessage,
} from '../io';

const { Text } = Typography;

interface ImportExportModalProps {
  open: boolean;
  mode: 'import' | 'export';
  onClose: () => void;
  onImportSuccess?: () => void;
  onExportSuccess?: () => void;
}

/**
 * 导入导出对话框
 */
export const ImportExportModal: React.FC<ImportExportModalProps> = ({
  open,
  mode,
  onClose,
  onImportSuccess,
  onExportSuccess,
}) => {
  const [confirmLoading, setConfirmLoading] = useState(false);
  const [previewData, setPreviewData] = useState<any>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // 导出模式
  if (mode === 'export') {
    const handleExport = async () => {
      setConfirmLoading(true);
      
      try {
        const result = await exportWorkflow({ pretty: true });
        
        if (result.success) {
          message.success('导出成功');
          onExportSuccess?.();
          onClose();
        } else {
          message.error(result.error || '导出失败');
        }
      } catch (error) {
        message.error('导出失败');
      } finally {
        setConfirmLoading(false);
      }
    };

    // 获取预览数据
    React.useEffect(() => {
      if (open) {
        setPreviewData(getExportPreview());
      }
    }, [open]);

    return (
      <Modal
        title={
          <Space>
            <DownloadOutlined />
            <span>导出工作流</span>
          </Space>
        }
        open={open}
        onCancel={onClose}
        onOk={handleExport}
        confirmLoading={confirmLoading}
        okText="导出"
        cancelText="取消"
        width={500}
      >
        <div style={{ marginBottom: 16 }}>
          <Text strong>即将导出以下内容：</Text>
        </div>
        
        {previewData && (
          <div style={{ 
            padding: 16, 
            background: '#f5f5f5', 
            borderRadius: 4,
            marginBottom: 16 
          }}>
            <div style={{ marginBottom: 8 }}>
              <Tag color="blue">节点：{previewData.nodeCount} 个</Tag>
              <Tag color="green">连接：{previewData.edgeCount} 个</Tag>
            </div>
            
            {previewData.nodeCount === 0 ? (
              <div style={{ color: '#999', fontSize: 12 }}>
                <ExclamationCircleOutlined /> 当前工作流为空
              </div>
            ) : (
              <div style={{ fontSize: 12, color: '#666' }}>
                {previewData.nodes.slice(0, 5).map((node: any) => (
                  <div key={node.id} style={{ marginBottom: 4 }}>
                    • {node.label} ({node.type})
                  </div>
                ))}
                {previewData.nodes.length > 5 && (
                  <div style={{ color: '#999' }}>
                    ... 还有 {previewData.nodes.length - 5} 个节点
                  </div>
                )}
              </div>
            )}
          </div>
        )}
        
        <div style={{ fontSize: 12, color: '#999' }}>
          导出格式：JSON
          <br />
          文件名将自动生成：workflow-{Date.now()}.json
        </div>
      </Modal>
    );
  }

  // 导入模式
  const handleFileChange = async (info: { file: UploadFile }) => {
    const file = info.file.originFileObj;
    if (!file) return;

    setConfirmLoading(true);

    try {
      const result = await importWorkflowFromFile(file);

      if (result.success) {
        message.success('导入成功');
        onImportSuccess?.();
        onClose();
      } else {
        const errorMsg = result.errors 
          ? getFriendlyErrorMessage(result.errors)
          : result.error;
        message.error(errorMsg || '导入失败');
      }
    } catch (error) {
      message.error('导入失败');
    } finally {
      setConfirmLoading(false);
    }
  };

  return (
    <Modal
      title={
        <Space>
          <UploadOutlined />
          <span>导入工作流</span>
        </Space>
      }
      open={open}
      onCancel={onClose}
      footer={[
        <Button key="cancel" onClick={onClose}>
          取消
        </Button>,
      ]}
      width={500}
    >
      <div style={{ marginBottom: 16 }}>
        <Text strong>选择 JSON 文件：</Text>
      </div>

      <Upload
        accept=".json"
        maxCount={1}
        beforeUpload={() => false} // 阻止自动上传
        onChange={handleFileChange}
        showUploadList={false}
      >
        <Button icon={<UploadOutlined />} size="large" block>
          选择文件
        </Button>
      </Upload>

      <div style={{ marginTop: 16, padding: 12, background: '#e6f7ff', borderRadius: 4, fontSize: 12 }}>
        <ExclamationCircleOutlined style={{ color: '#1890ff', marginRight: 8 }} />
        支持格式：JSON
        <br />
        文件大小：最大 10MB
        <br />
        注意：导入将覆盖当前工作流
      </div>
    </Modal>
  );
};

export default ImportExportModal;
