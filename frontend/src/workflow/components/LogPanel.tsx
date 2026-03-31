import { memo, useMemo, useState, useCallback } from 'react'
import { Drawer, List, Input, Select, Space, Typography, Tag, Empty, Button } from 'antd'
import { 
  SearchOutlined, 
  FilterOutlined, 
  ClearOutlined,
  InfoCircleOutlined,
  WarningOutlined,
  CloseCircleOutlined,
  BugOutlined,
} from '@ant-design/icons'
import { useWorkflowStore, LogLevel } from '../store/useWorkflowStore'
import { FixedSizeList as VirtualList } from 'react-window'

const { Text } = Typography
const { Search } = Input

// 日志级别配置
const LOG_LEVEL_CONFIG: Record<LogLevel, { color: string; icon: any; label: string }> = {
  INFO: { color: '#1890ff', icon: InfoCircleOutlined, label: 'INFO' },
  WARN: { color: '#faad14', icon: WarningOutlined, label: 'WARN' },
  ERROR: { color: '#ff4d4f', icon: CloseCircleOutlined, label: 'ERROR' },
  DEBUG: { color: '#722ed1', icon: BugOutlined, label: 'DEBUG' },
}

// 日志项组件
const LogItem = memo(({ log, highlight }: { log: any; highlight: string }) => {
  const config = LOG_LEVEL_CONFIG[log.level as LogLevel]
  const Icon = config.icon
  
  // 高亮搜索关键词
  const highlightText = (text: string) => {
    if (!highlight) return text
    
    const parts = text.split(new RegExp(`(${highlight})`, 'gi'))
    return parts.map((part, i) => 
      part.toLowerCase() === highlight.toLowerCase() ? (
        <mark key={i} style={{ backgroundColor: '#fff566', padding: '0 2px' }}>{part}</mark>
      ) : part
    )
  }
  
  return (
    <div
      style={{
        padding: '12px 16px',
        borderBottom: '1px solid #f0f0f0',
        backgroundColor: log.level === 'ERROR' ? '#fff1f0' : 'transparent',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8 }}>
        <Icon style={{ color: config.color, fontSize: 14, marginTop: 2 }} />
        <div style={{ flex: 1 }}>
          <div style={{ marginBottom: 4 }}>
            <Tag color={config.color} style={{ fontSize: 11, marginRight: 8 }}>
              {config.label}
            </Tag>
            <Text type="secondary" style={{ fontSize: 11 }}>
              {new Date(log.timestamp).toLocaleTimeString()}
            </Text>
            {log.nodeId && (
              <Tag style={{ fontSize: 11, marginLeft: 8 }}>
                节点：{log.nodeId.slice(0, 8)}
              </Tag>
            )}
          </div>
          <div style={{ fontSize: 13, color: '#333' }}>
            {highlightText(log.message)}
          </div>
          {log.details && (
            <div 
              style={{ 
                fontSize: 11, 
                color: '#666', 
                marginTop: 4,
                backgroundColor: '#f5f5f5',
                padding: '8px',
                borderRadius: 4,
                fontFamily: 'monospace',
              }}
            >
              {JSON.stringify(log.details, null, 2)}
            </div>
          )}
        </div>
      </div>
    </div>
  )
})

// 虚拟列表行组件
const LogRow = ({ index, style, data }: any) => {
  const { logs, highlight } = data
  const log = logs[index]
  
  return (
    <div style={style}>
      <LogItem log={log} highlight={highlight} />
    </div>
  )
}

export interface LogPanelProps {
  open?: boolean
  onClose?: () => void
}

function LogPanelComponent({ open: propOpen, onClose }: LogPanelProps) {
  const {
    logDrawerOpen,
    setLogDrawerOpen,
    logFilter,
    setLogFilter,
    logSearchKeyword,
    setLogSearchKeyword,
    executionState,
    clearLogs,
  } = useWorkflowStore()
  
  const open = propOpen !== undefined ? propOpen : logDrawerOpen
  
  // 过滤和搜索日志 (F031 - 日志过滤，F032 - 日志搜索)
  const filteredLogs = useMemo(() => {
    if (!executionState) return []
    
    let logs = executionState.logs
    
    // 按级别过滤
    if (logFilter !== 'ALL') {
      logs = logs.filter(log => log.level === logFilter)
    }
    
    // 按关键词搜索
    if (logSearchKeyword) {
      logs = logs.filter(log => 
        log.message.toLowerCase().includes(logSearchKeyword.toLowerCase()) ||
        log.nodeId?.toLowerCase().includes(logSearchKeyword.toLowerCase())
      )
    }
    
    // 按时间倒序
    return logs.sort((a, b) => 
      new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
    )
  }, [executionState, logFilter, logSearchKeyword])
  
  // 处理关闭
  const handleClose = useCallback(() => {
    setLogDrawerOpen(false)
    onClose?.()
  }, [setLogDrawerOpen, onClose])
  
  // 处理清空日志
  const handleClearLogs = useCallback(() => {
    clearLogs()
  }, [clearLogs])
  
  // 日志级别选项
  const filterOptions = [
    { value: 'ALL', label: '全部' },
    { value: 'INFO', label: '信息' },
    { value: 'WARN', label: '警告' },
    { value: 'ERROR', label: '错误' },
    { value: 'DEBUG', label: '调试' },
  ]
  
  return (
    <Drawer
      title={
        <Space>
          <BugOutlined />
          <span>执行日志</span>
          {executionState?.logs.length > 0 && (
            <Tag color="blue">{executionState.logs.length} 条</Tag>
          )}
        </Space>
      }
      placement="right"
      open={open}
      onClose={handleClose}
      width={600}
      styles={{
        body: { padding: 0, display: 'flex', flexDirection: 'column' },
      }}
    >
      {/* 过滤和搜索工具栏 */}
      <div style={{ padding: '16px', borderBottom: '1px solid #f0f0f0' }}>
        <Space direction="vertical" size="small" style={{ width: '100%' }}>
          {/* 搜索框 (F032 - 关键词搜索) */}
          <Search
            placeholder="搜索日志内容..."
            value={logSearchKeyword}
            onChange={(e) => setLogSearchKeyword(e.target.value)}
            allowClear
            prefix={<SearchOutlined />}
            size="middle"
          />
          
          {/* 过滤选项 (F031 - 按级别过滤) */}
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <FilterOutlined style={{ color: '#999' }} />
            <Select
              value={logFilter}
              onChange={setLogFilter}
              size="small"
              style={{ width: 120 }}
              options={filterOptions}
            />
            <Button
              size="small"
              icon={<ClearOutlined />}
              onClick={handleClearLogs}
              disabled={!executionState?.logs.length}
            >
              清空
            </Button>
          </div>
        </Space>
      </div>
      
      {/* 日志列表 (F030 - Drawer + 虚拟列表) */}
      <div style={{ flex: 1, overflow: 'hidden' }}>
        {filteredLogs.length > 0 ? (
          <VirtualList
            height={window.innerHeight - 200}
            itemCount={filteredLogs.length}
            itemSize={100}
            itemData={{ logs: filteredLogs, highlight: logSearchKeyword }}
            width="100%"
          >
            {LogRow}
          </VirtualList>
        ) : (
          <Empty
            description={
              executionState?.logs.length
                ? '没有匹配的日志'
                : '暂无日志'
            }
            style={{ marginTop: 100 }}
          >
            {!executionState?.logs.length && (
              <Button type="primary" onClick={handleClose}>
                执行工作流查看日志
              </Button>
            )}
          </Empty>
        )}
      </div>
    </Drawer>
  )
}

export const LogPanel = memo(LogPanelComponent)
