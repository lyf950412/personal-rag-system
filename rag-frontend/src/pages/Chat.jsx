import React, { useState, useRef, useEffect } from 'react'
import { Input, Button, Select, Tag, Spin, Tooltip, Collapse, Avatar, message } from 'antd'
import {
  SendOutlined,
  PictureOutlined,
  AudioOutlined,
  VideoCameraOutlined,
  FileOutlined,
  CopyOutlined,
  LikeOutlined,
  DislikeOutlined,
  ReloadOutlined,
  BookOutlined,
  RobotOutlined,
  UserOutlined,
  ClearOutlined,
  ExclamationCircleOutlined,
  UploadOutlined,
} from '@ant-design/icons'
import { chatApi, knowledgeBaseApi } from '../services'
import styles from './Chat.module.css'

const { TextArea } = Input

function Chat() {
  const [messages, setMessages] = useState([])
  const [inputValue, setInputValue] = useState('')
  const [selectedKB, setSelectedKB] = useState('all')
  const [loading, setLoading] = useState(false)
  const [knowledgeBases, setKnowledgeBases] = useState([])
  const [sessionId] = useState(() => `session_${Date.now()}`)
  const [errorMessage, setErrorMessage] = useState(null)
  const [uploadingFile, setUploadingFile] = useState(null)
  const messagesEndRef = useRef(null)
  const fileInputRef = useRef(null)
  const [uploadType, setUploadType] = useState(null)
  const [retryCount, setRetryCount] = useState(0)

  useEffect(() => {
    fetchKnowledgeBases()
    fetchChatHistory()
  }, [sessionId])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const fetchKnowledgeBases = async () => {
    try {
      const response = await knowledgeBaseApi.getAll()
      const kbs = response.data || response || []
      setKnowledgeBases([
        { value: 'all', label: '全部知识库' },
        ...kbs.map((kb) => ({ value: kb.id, label: kb.name })),
      ])
    } catch (error) {
      console.error('获取知识库失败:', error)
      setKnowledgeBases([{ value: 'all', label: '全部知识库' }])
    }
  }

  const fetchChatHistory = async () => {
    try {
      const response = await chatApi.getHistory(sessionId)
      const history = response.data || []
      setMessages(history)
    } catch (error) {
      console.error('获取聊天历史失败:', error)
      setMessages([])
    }
  }

  const handleSend = async () => {
    const trimmedValue = inputValue.trim()
    if (!trimmedValue || loading) return

    if (trimmedValue.length > 2000) {
      message.warning('问题长度不能超过2000字符')
      return
    }

    setErrorMessage(null)
    const userMessage = {
      id: Date.now(),
      role: 'user',
      content: trimmedValue,
      timestamp: new Date().toISOString(),
    }

    const assistantMessageId = Date.now() + 1
    const assistantMessage = {
      id: assistantMessageId,
      role: 'assistant',
      content: '',
      timestamp: new Date().toISOString(),
      isStreaming: true,
    }

    setMessages((prev) => [...prev, userMessage, assistantMessage])
    setInputValue('')
    setLoading(true)

    let reader = null
    let fullContent = ''
    let abortController = null
    try {
      abortController = new AbortController()
      const response = await chatApi.chatStream(sessionId, trimmedValue, selectedKB === 'all' ? null : selectedKB, abortController.signal)
      
      if (!response.ok) {
        throw new Error('Stream request failed')
      }

      reader = response.body.getReader()
      const decoder = new TextDecoder()
      let eventName = ''
      let lastDataTime = Date.now()
      const STREAM_TIMEOUT = 30000
      let streamDone = false
      let readCount = 0
      const streamStartTime = Date.now()
      const MAX_STREAM_DURATION = 60000

      while (!streamDone) {
        readCount++
        const now = Date.now()
        
        if (now - streamStartTime > MAX_STREAM_DURATION) {
          console.error('Stream exceeded max duration of', MAX_STREAM_DURATION, 'ms')
          streamDone = true
          break
        }
        
        if (now - lastDataTime > STREAM_TIMEOUT) {
          console.error('Stream timeout: no data received for 30 seconds after', readCount, 'reads')
          streamDone = true
          break
        }

        const readPromise = reader.read()
        const timeoutPromise = new Promise((_, reject) => 
          setTimeout(() => reject(new Error('读取超时')), 5000)
        )

        let result
        try {
          result = await Promise.race([readPromise, timeoutPromise])
        } catch (readError) {
          console.warn('Read error, ending stream:', readError.message)
          streamDone = true
          break
        }
        
        const { done, value } = result
        
        if (done) {
          console.log('Stream reader done, total content length:', fullContent.length)
          streamDone = true
          break
        }

        lastDataTime = Date.now()
        const chunk = decoder.decode(value, { stream: true })
        const lines = chunk.split('\n')

        for (const line of lines) {
          if (line.startsWith('event:')) {
            eventName = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            const data = line.slice(5).trim()
            if (data === '[DONE]') {
              console.log('Received [DONE] signal')
              streamDone = true
              break
            }
            if (data && eventName === 'message') {
              fullContent += data
              setMessages((prev) =>
                prev.map((msg) =>
                  msg.id === assistantMessageId
                    ? { ...msg, content: fullContent, isStreaming: true }
                    : msg
                )
              )
              messagesEndRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
            } else if (data && eventName === 'error') {
              throw new Error(data.replace(/^Error:\s*/, ''))
            }
          } else if (line.trim() === '') {
            eventName = ''
          }
        }
      }

      console.log('Loop exited, streamDone:', streamDone, 'fullContent length:', fullContent.length)

      console.log('Stream finished, setting isStreaming to false')
      setMessages((prev) =>
        prev.map((msg) =>
          msg.id === assistantMessageId
            ? { ...msg, content: fullContent, isStreaming: false }
            : msg
        )
      )

      setRetryCount(0)
    } catch (error) {
      if (error.name === 'AbortError') {
        console.log('Stream aborted')
      } else {
        console.error('发送消息失败:', error)
        setErrorMessage(error.message || '发送消息失败，请重试')
      }
      
      setMessages((prev) =>
        prev.map((msg) =>
          msg.id === assistantMessageId
            ? { ...msg, content: fullContent || '响应失败，请重试', isStreaming: false }
            : msg
        )
      )
    } finally {
      try {
        if (abortController) {
          abortController.abort()
        }
      } catch (e) {
        console.error('Error during cleanup:', e)
      }
      console.log('Finally block executed, setting loading to false')
      setLoading(false)
    }
  }

  const handleRetry = async () => {
    const lastUserMessage = [...messages].reverse().find((m) => m.role === 'user')
    
    if (!lastUserMessage) {
      message.warning('没有可重试的消息')
      return
    }
    
    if (retryCount >= 3) {
      message.error('重试次数已达上限，请稍后再试')
      return
    }
    
    const msgIndex = messages.findIndex((m) => m.id === lastUserMessage.id)
    setMessages((prev) => prev.slice(0, msgIndex))
    setInputValue(lastUserMessage.content)
    message.info('已加载上一条问题，请点击发送按钮重试')
  }

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const handleFileUpload = (type) => {
    setUploadType(type)
    fileInputRef.current?.click()
  }

  const handleFileChange = (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    
    const maxSizes = {
      image: 50 * 1024 * 1024,
      audio: 500 * 1024 * 1024,
      video: 500 * 1024 * 1024,
      file: 100 * 1024 * 1024,
    }
    
    if (file.size > (maxSizes[uploadType] || 100 * 1024 * 1024)) {
      message.error(`文件大小超过限制 (${uploadType === 'image' ? '50MB' : uploadType === 'audio' || uploadType === 'video' ? '500MB' : '100MB'})`)
      return
    }
    
    setUploadingFile(file)
    const fileInfo = `[${uploadType === 'image' ? '图片' : uploadType === 'audio' ? '音频' : uploadType === 'video' ? '视频' : '文件'}] ${file.name} (${(file.size / 1024 / 1024).toFixed(2)} MB)`
    setInputValue((prev) => prev + (prev ? '\n' : '') + fileInfo)
    message.success(`已添加文件: ${file.name}`)
    setUploadingFile(null)
    
    e.target.value = ''
  }

  const handleRegenerate = (msgIndex) => {
    const userMsgIndex = messages.findIndex((m, i) => i > msgIndex && m.role === 'user')
    if (userMsgIndex !== -1) {
      const question = messages[userMsgIndex]?.content
      setInputValue(question)
      message.info('已加载上一条问题，请修改后发送')
    }
  }

  const handleLike = (msgId) => {
    message.success('感谢您的反馈！')
  }

  const handleDislike = (msgId) => {
    message.success('感谢您的反馈，我们会继续改进！')
  }

  const clearChat = async () => {
    try {
      await chatApi.clearSession(sessionId)
      setMessages([])
      message.success('对话已清空')
    } catch (error) {
      console.error('清空对话失败:', error)
      setMessages([])
    }
  }

  const handleCopy = (content) => {
    navigator.clipboard.writeText(content)
    message.success('已复制到剪贴板')
  }

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <div className={styles.headerLeft}>
          <h2 className={styles.title}>智能问答</h2>
          <Tag color="blue">支持文本/图片/音频/视频</Tag>
        </div>
        <div className={styles.headerRight}>
          <Select
            value={selectedKB}
            onChange={setSelectedKB}
            options={knowledgeBases}
            className={styles.kbSelect}
            placeholder="选择知识库"
            suffixIcon={<BookOutlined />}
          />
          <Tooltip title="清空对话">
            <Button icon={<ClearOutlined />} onClick={clearChat} />
          </Tooltip>
        </div>
      </div>

      {errorMessage && (
        <div className={styles.errorBanner}>
          <ExclamationCircleOutlined />
          <span>{errorMessage}</span>
          <Button size="small" onClick={handleRetry} icon={<ReloadOutlined />}>
            重试
          </Button>
        </div>
      )}

      <div className={styles.messagesContainer}>
        {messages.length === 0 && (
          <div className={styles.emptyState}>
            <RobotOutlined className={styles.emptyIcon} />
            <h3>开始智能问答</h3>
            <p>选择知识库范围，输入您的问题，AI将基于知识库内容为您生成准确回答</p>
            <div className={styles.suggestions}>
              <Tag className={styles.suggestionTag} onClick={() => setInputValue('产品有哪些核心功能？')}>产品有哪些核心功能？</Tag>
              <Tag className={styles.suggestionTag} onClick={() => setInputValue('如何部署RAG系统？')}>如何部署RAG系统？</Tag>
              <Tag className={styles.suggestionTag} onClick={() => setInputValue('支持哪些文件格式？')}>支持哪些文件格式？</Tag>
              <Tag className={styles.suggestionTag} onClick={() => setInputValue('系统性能指标如何？')}>系统性能指标如何？</Tag>
            </div>
          </div>
        )}

        {messages.map((msg) => (
          <div key={msg.id} className={`${styles.message} ${msg.role === 'user' ? styles.userMessage : styles.assistantMessage}`}>
            <Avatar
              className={styles.avatar}
              icon={msg.role === 'user' ? <UserOutlined /> : <RobotOutlined />}
              style={{ backgroundColor: msg.role === 'user' ? '#1677ff' : '#52c41a' }}
            />
            <div className={styles.messageContent}>
              <div className={styles.messageBubble}>
                <div className={styles.messageText}>
                  {msg.content}
                  {msg.isStreaming && <span className={styles.streamingCursor}>▍</span>}
                </div>
              </div>
              {msg.sources && msg.sources.length > 0 && (
                <Collapse className={styles.sources} size="small">
                  <Collapse.Panel header={`参考来源 (${msg.sources.length}个)`} key="sources">
                    {msg.sources.map((source, index) => (
                      <div key={index} className={styles.sourceItem}>
                        <FileOutlined className={styles.sourceIcon} />
                        <span className={styles.sourceTitle}>{source.title}</span>
                        <Tag color="blue">{source.page}</Tag>
                        <span className={styles.sourceScore}>相似度: {(source.score * 100).toFixed(0)}%</span>
                      </div>
                    ))}
                  </Collapse.Panel>
                </Collapse>
              )}
              <div className={styles.messageActions}>
                <span className={styles.timestamp}>
                  {new Date(msg.timestamp).toLocaleString()}
                </span>
                {msg.role === 'assistant' && (
                  <div className={styles.actionButtons}>
                    <Tooltip title="复制">
                      <Button type="text" size="small" icon={<CopyOutlined />} onClick={() => handleCopy(msg.content)} />
                    </Tooltip>
                    <Tooltip title="有用">
                      <Button type="text" size="small" icon={<LikeOutlined />} onClick={() => handleLike(msg.id)} />
                    </Tooltip>
                    <Tooltip title="无用">
                      <Button type="text" size="small" icon={<DislikeOutlined />} onClick={() => handleDislike(msg.id)} />
                    </Tooltip>
                    <Tooltip title="重新生成">
                      <Button type="text" size="small" icon={<ReloadOutlined />} onClick={() => handleRegenerate(messages.indexOf(msg))} />
                    </Tooltip>
                  </div>
                )}
              </div>
            </div>
          </div>
        ))}

        {loading && (
          <div className={`${styles.message} ${styles.assistantMessage}`}>
            <Avatar className={styles.avatar} icon={<RobotOutlined />} style={{ backgroundColor: '#52c41a' }} />
            <div className={styles.messageContent}>
              <div className={styles.messageBubble}>
                <Spin size="small" />
                <span className={styles.loadingText}>AI正在思考中...</span>
              </div>
            </div>
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      <div className={styles.inputArea}>
        <div className={styles.inputTools}>
          <Tooltip title="上传图片">
            <Button
              type="text"
              icon={<PictureOutlined />}
              onClick={() => handleFileUpload('image')}
            />
          </Tooltip>
          <Tooltip title="上传音频">
            <Button
              type="text"
              icon={<AudioOutlined />}
              onClick={() => handleFileUpload('audio')}
            />
          </Tooltip>
          <Tooltip title="上传视频">
            <Button
              type="text"
              icon={<VideoCameraOutlined />}
              onClick={() => handleFileUpload('video')}
            />
          </Tooltip>
          <Tooltip title="上传文件">
            <Button
              type="text"
              icon={<FileOutlined />}
              onClick={() => handleFileUpload('file')}
            />
          </Tooltip>
        </div>
        <div className={styles.inputWrapper}>
          <TextArea
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyDown={handleKeyPress}
            placeholder="输入您的问题，支持文本/图片/音频/视频多模态提问..."
            autoSize={{ minRows: 1, maxRows: 4 }}
            className={styles.textarea}
            maxLength={2000}
            showCount
          />
          <Button
            type="primary"
            icon={<SendOutlined />}
            onClick={handleSend}
            disabled={!inputValue.trim() || loading}
            className={styles.sendButton}
          />
        </div>
        <div className={styles.inputHint}>
          <span>按 Enter 发送，Shift + Enter 换行</span>
          <span>支持 PDF/Word/图片/音频/视频 等多模态内容</span>
        </div>
      </div>

      <input
        ref={fileInputRef}
        type="file"
        style={{ display: 'none' }}
        accept={uploadType === 'image' ? 'image/*' : uploadType === 'audio' ? 'audio/*' : uploadType === 'video' ? 'video/*' : '*/*'}
        onChange={handleFileChange}
      />
    </div>
  )
}

export default Chat
