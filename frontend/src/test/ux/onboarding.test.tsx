import { render, screen, fireEvent } from '@testing-library/react'
import { describe, test, expect, beforeEach, vi } from 'vitest'
import WorkflowCanvas from '@/components/WorkflowCanvas'

describe('F044 - 新手引导', () => {
  beforeEach(() => {
    // 清除本地存储中的引导状态
    localStorage.clear()
    render(<WorkflowCanvas />)
  })

  test('应首次访问时显示欢迎引导', () => {
    // 首次访问应显示欢迎对话框
    expect(screen.getByText(/欢迎|新手引导|开始使用/)).toBeInTheDocument()
  })

  test('应显示引导步骤指示器', () => {
    // 应显示步骤指示器（如 1/5）
    expect(screen.getByTestId('onboarding-progress')).toBeInTheDocument()
  })

  test('应显示画布介绍', () => {
    // 引导应介绍画布功能
    expect(screen.getByText(/画布|拖拽|节点/)).toBeInTheDocument()
  })

  test('应显示节点面板介绍', () => {
    // 引导应介绍节点面板
    expect(screen.getByText(/节点面板|添加节点/)).toBeInTheDocument()
  })

  test('应显示连线操作介绍', () => {
    // 引导应介绍如何连线
    expect(screen.getByText(/连线|连接|端口/)).toBeInTheDocument()
  })

  test('应显示运行操作介绍', () => {
    // 引导应介绍如何运行工作流
    expect(screen.getByText(/运行|执行|播放/)).toBeInTheDocument()
  })

  test('应支持跳过引导', async () => {
    const skipButton = screen.getByText(/跳过|Skip/)
    fireEvent.click(skipButton)
    
    // 引导应关闭
    expect(screen.queryByText(/欢迎|新手引导/)).not.toBeInTheDocument()
  })

  test('应支持上一步/下一步导航', async () => {
    // 下一步
    const nextButton = screen.getByText(/下一步|Next/)
    fireEvent.click(nextButton)
    
    // 应显示下一步内容
    expect(screen.getByTestId('onboarding-step-2')).toBeInTheDocument()
    
    // 上一步
    const prevButton = screen.getByText(/上一步|Previous/)
    fireEvent.click(prevButton)
    
    // 应返回上一步
    expect(screen.getByTestId('onboarding-step-1')).toBeInTheDocument()
  })

  test('应支持完成引导', async () => {
    // 连续点击下一步直到完成
    const nextButton = screen.getByText(/下一步|Next|完成|Finish/)
    
    // 模拟多步引导
    for (let i = 0; i < 5; i++) {
      if (nextButton) {
        fireEvent.click(nextButton)
      }
    }
    
    // 引导应完成
    expect(screen.queryByText(/下一步/)).not.toBeInTheDocument()
  })

  test('应记住用户已完成引导', async () => {
    // 完成引导
    const finishButton = screen.getByText(/完成|Finish/)
    fireEvent.click(finishButton)
    
    // 重新渲染组件
    render(<WorkflowCanvas />)
    
    // 不应再显示引导
    expect(screen.queryByText(/欢迎|新手引导/)).not.toBeInTheDocument()
  })

  test('应支持重置引导', async () => {
    // 完成引导
    const finishButton = screen.getByText(/完成|Finish/)
    fireEvent.click(finishButton)
    
    // 在设置中重置引导
    const settingsButton = screen.getByTestId('settings-button')
    fireEvent.click(settingsButton)
    
    const resetOnboarding = screen.getByText(/重置引导|Reset/)
    fireEvent.click(resetOnboarding)
    
    // 重新渲染应再次显示引导
    render(<WorkflowCanvas />)
    expect(screen.getByText(/欢迎|新手引导/)).toBeInTheDocument()
  })

  test('应高亮当前引导步骤的目标元素', async () => {
    // 引导应高亮目标元素
    const highlightedElement = screen.getByTestId('onboarding-highlight')
    expect(highlightedElement).toBeInTheDocument()
  })

  test('应显示引导提示气泡', async () => {
    // 应显示提示气泡
    expect(screen.getByTestId('onboarding-tooltip')).toBeInTheDocument()
  })

  test('应支持键盘导航引导', async () => {
    // 按 Enter 键继续
    fireEvent.keyDown(document, { key: 'Enter' })
    
    // 应进入下一步
    expect(screen.getByTestId('onboarding-step-2')).toBeInTheDocument()
  })

  test('应支持按 Esc 关闭引导', async () => {
    fireEvent.keyDown(document, { key: 'Escape' })
    
    // 引导应关闭
    expect(screen.queryByText(/欢迎|新手引导/)).not.toBeInTheDocument()
  })

  test('应显示引导进度条', async () => {
    const progressBar = screen.getByTestId('onboarding-progress-bar')
    expect(progressBar).toBeInTheDocument()
  })

  test('应支持点击遮罩层关闭引导', async () => {
    const overlay = screen.getByTestId('onboarding-overlay')
    fireEvent.click(overlay)
    
    // 引导应关闭
    expect(screen.queryByText(/欢迎|新手引导/)).not.toBeInTheDocument()
  })

  test('应显示交互式引导示例', async () => {
    // 引导应包含交互式示例
    expect(screen.getByText(/尝试|点击|拖拽/)).toBeInTheDocument()
  })

  test('应支持查看引导视频', async () => {
    const videoButton = screen.getByText(/视频教程|Video/)
    fireEvent.click(videoButton)
    
    // 应显示视频播放器
    expect(screen.getByTestId('onboarding-video')).toBeInTheDocument()
  })

  test('应支持查看文档链接', async () => {
    const docsLink = screen.getByText(/文档|Docs|帮助/)
    fireEvent.click(docsLink)
    
    // 应打开文档链接
    expect(window.open).toHaveBeenCalled()
  })

  test('应显示快捷键提示', async () => {
    // 引导应包含常用快捷键
    expect(screen.getByText(/Ctrl|Cmd|快捷键/)).toBeInTheDocument()
  })

  test('应支持多语言引导', async () => {
    // 切换语言
    const languageSelect = screen.getByTestId('language-select')
    fireEvent.change(languageSelect, { target: { value: 'en' } })
    
    // 引导应切换为英文
    expect(screen.getByText(/Welcome|Get Started/)).toBeInTheDocument()
  })

  test('应显示示例工作流', async () => {
    // 引导应展示示例工作流
    expect(screen.getByText(/示例|Example|模板/)).toBeInTheDocument()
  })

  test('应支持加载示例工作流', async () => {
    const loadExample = screen.getByText(/加载示例|Load Example/)
    fireEvent.click(loadExample)
    
    // 应加载示例工作流
    expect(screen.getByTestId('canvas')).toBeInTheDocument()
  })

  test('应显示常见问题提示', async () => {
    // 引导应包含常见问题
    expect(screen.getByText(/常见问题|FAQ/)).toBeInTheDocument()
  })

  test('应支持联系支持', async () => {
    const contactSupport = screen.getByText(/联系支持|Contact Support/)
    fireEvent.click(contactSupport)
    
    // 应显示联系方式
    expect(screen.getByText(/邮箱|邮件|Email/)).toBeInTheDocument()
  })

  test('应显示引导完成奖励', async () => {
    // 完成引导后显示奖励
    const finishButton = screen.getByText(/完成|Finish/)
    fireEvent.click(finishButton)
    
    expect(screen.getByText(/恭喜|完成|🎉/)).toBeInTheDocument()
  })

  test('应支持反馈引导体验', async () => {
    const feedbackButton = screen.getByText(/反馈|Feedback/)
    fireEvent.click(feedbackButton)
    
    // 应显示反馈表单
    expect(screen.getByTestId('feedback-form')).toBeInTheDocument()
  })

  test('应记录引导完成情况', async () => {
    const finishButton = screen.getByText(/完成|Finish/)
    fireEvent.click(finishButton)
    
    // 本地存储应记录完成状态
    const onboardingState = localStorage.getItem('onboarding-completed')
    expect(onboardingState).toBe('true')
  })

  test('应支持分角色引导（新手/进阶）', async () => {
    // 选择角色类型
    const beginnerOption = screen.getByText(/新手|Beginner/)
    fireEvent.click(beginnerOption)
    
    // 应显示适合新手的引导内容
    expect(screen.getByText(/基础|入门|Basic/)).toBeInTheDocument()
  })

  test('应支持跳过特定步骤', async () => {
    const skipStepButton = screen.getByText(/跳过此步|Skip Step/)
    fireEvent.click(skipStepButton)
    
    // 应进入下一步
    expect(screen.getByTestId('onboarding-step-2')).toBeInTheDocument()
  })

  test('应显示步骤缩略图', async () => {
    // 应显示步骤缩略图导航
    expect(screen.getByTestId('onboarding-thumbnails')).toBeInTheDocument()
  })

  test('应支持重新播放当前步骤', async () => {
    const replayButton = screen.getByText(/重播|Replay/)
    fireEvent.click(replayButton)
    
    // 应重新播放当前步骤
    expect(screen.getByTestId('onboarding-step-1')).toBeInTheDocument()
  })
})
