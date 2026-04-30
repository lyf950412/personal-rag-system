import React, { useState, useEffect } from 'react'
import { Upload, Button, Select, Tag, Progress, Card, Row, Col, Steps, message, Alert, List } from 'antd'
import {
  InboxOutlined,
  FileTextOutlined,
  PictureOutlined,
  AudioOutlined,
  VideoCameraOutlined,
  FileExcelOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  LoadingOutlined,
  InfoCircleOutlined,
  ReloadOutlined,
} from '@ant-design/icons'
import { useLocation } from 'react-router-dom'
import { documentApi, knowledgeBaseApi } from '../services'
import styles from './FileUpload.module.css'

const { Dragger } = Upload

const supportedFormats = [
  { type: '文档', icon: <FileTextOutlined />, formats: ['PDF', 'Word', 'TXT', 'Markdown'], color: '#1677ff', maxSize: '100MB' },
  { type: '表格', icon: <FileExcelOutlined />, formats: ['Excel', 'CSV'], color: '#52c41a', maxSize: '50MB' },
  { type: '图片', icon: <PictureOutlined />, formats: ['PNG', 'JPG', 'JPEG', 'GIF', 'WEBP'], color: '#faad14', maxSize: '50MB' },
  { type: '音频', icon: <AudioOutlined />, formats: ['MP3', 'WAV', 'AAC'], color: '#722ed1', maxSize: '500MB' },
  { type: '视频', icon: <VideoCameraOutlined />, formats: ['MP4', 'AVI', 'MOV'], color: '#eb2f96', maxSize: '500MB' },
]

function FileUpload() {
  const [selectedKB, setSelectedKB] = useState(null)
  const [knowledgeBases, setKnowledgeBases] = useState([])
  const [recentDocuments, setRecentDocuments] = useState([])
  const [loading, setLoading] = useState(false)
  const [uploading, setUploading] = useState(false)
  const location = useLocation()

  useEffect(() => {
    fetchKnowledgeBases()
    fetchRecentDocuments()
    
    if (location.state?.selectedKB) {
      setSelectedKB(location.state.selectedKB.id)
    }
  }, [location])

  const fetchKnowledgeBases = async () => {
    try {
      const response = await knowledgeBaseApi.getAll()
      const kbs = response.data || response || []
      setKnowledgeBases(kbs.map((kb) => ({ value: kb.id, label: kb.name })))
    } catch (error) {
      console.error('获取知识库失败:', error)
      message.error('获取知识库列表失败')
    }
  }

  const fetchRecentDocuments = async () => {
    setLoading(true)
    try {
      const response = await documentApi.getRecent(10)
      setRecentDocuments(response.data || response || [])
    } catch (error) {
      console.error('获取文档列表失败:', error)
    } finally {
      setLoading(false)
    }
  }

  const uploadProps = {
    name: 'file',
    multiple: true,
    showUploadList: false,
    beforeUpload: (file) => {
      if (!selectedKB) {
        message.warning('请先选择目标知识库')
        return Upload.LIST_IGNORE
      }
      return true
    },
    customRequest: async (options) => {
      const { file, onSuccess, onError, onProgress } = options
      
      setUploading(true)
      
      try {
        const formData = new FormData()
        formData.append('file', file)
        formData.append('knowledgeBaseId', selectedKB)
        
        await documentApi.upload(file, selectedKB)
        
        message.success(`${file.name} 上传成功`)
        onSuccess && onSuccess()
        fetchRecentDocuments()
      } catch (error) {
        console.error('上传失败:', error)
        message.error(`${file.name} 上传失败`)
        onError && onError(error)
      } finally {
        setUploading(false)
      }
    },
  }

  const getStatusIcon = (status) => {
    switch (status) {
      case '已处理':
        return <CheckCircleOutlined style={{ color: '#52c41a' }} />
      case '处理中':
      case '待处理':
        return <LoadingOutlined style={{ color: '#1677ff' }} />
      case '处理失败':
        return <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
      default:
        return null
    }
  }

  const getStatusColor = (status) => {
    switch (status) {
      case '已处理':
        return 'success'
      case '处理中':
      case '待处理':
        return 'processing'
      case '处理失败':
        return 'error'
      default:
        return 'default'
    }
  }

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h2 className={styles.title}>文件上传</h2>
        <Tag color="blue">支持多模态文件批量上传</Tag>
      </div>

      <Alert
        message="上传说明"
        description="支持PDF、Word、Excel、TXT、Markdown、图片、音频、视频等多种格式。文件上传后将自动进行解析并向量化处理，处理完成后可用于智能问答检索。"
        type="info"
        showIcon
        icon={<InfoCircleOutlined />}
        className={styles.alert}
      />

      <Card className={styles.uploadCard}>
        <div className={styles.uploadHeader}>
          <h3 className={styles.uploadTitle}>选择目标知识库</h3>
          <Select
            value={selectedKB}
            onChange={setSelectedKB}
            placeholder="请选择知识库"
            options={knowledgeBases}
            className={styles.kbSelect}
            style={{ minWidth: 200 }}
          />
        </div>

        <Dragger {...uploadProps} className={styles.dragger} disabled={!selectedKB}>
          <p className="ant-upload-drag-icon">
            <InboxOutlined style={{ fontSize: 48, color: '#1677ff' }} />
          </p>
          <p className="ant-upload-text">点击或拖拽文件到此区域上传</p>
          <p className="ant-upload-hint">支持批量上传，单文件大小限制请参考下方说明</p>
        </Dragger>

        <div className={styles.formatGuide}>
          <h4 className={styles.guideTitle}>支持的文件格式</h4>
          <Row gutter={[16, 16]}>
            {supportedFormats.map((format, index) => (
              <Col xs={24} sm={12} lg={8} xl={4} key={index}>
                <Card className={styles.formatCard} size="small">
                  <div className={styles.formatIcon} style={{ color: format.color }}>
                    {format.icon}
                  </div>
                  <h5 className={styles.formatType}>{format.type}</h5>
                  <div className={styles.formatList}>
                    {format.formats.map((f) => (
                      <Tag key={f} color={format.color} className={styles.formatTag}>{f}</Tag>
                    ))}
                  </div>
                  <span className={styles.maxSize}>最大 {format.maxSize}</span>
                </Card>
              </Col>
            ))}
          </Row>
        </div>
      </Card>

      <Card 
        title="上传记录" 
        className={styles.historyCard}
        extra={
          <Button 
            icon={<ReloadOutlined />} 
            onClick={fetchRecentDocuments}
            loading={loading}
          >
            刷新
          </Button>
        }
      >
        {loading ? (
          <div style={{ textAlign: 'center', padding: '20px 0' }}>
            <LoadingOutlined style={{ fontSize: 24 }} />
          </div>
        ) : recentDocuments.length > 0 ? (
          <List
            itemLayout="horizontal"
            dataSource={recentDocuments}
            renderItem={(item) => (
              <List.Item>
                <List.Item.Meta
                  avatar={getStatusIcon(item.status)}
                  title={item.name}
                  description={
                    <div className={styles.fileMeta}>
                      <Tag size="small">{item.type}</Tag>
                      <span>{item.fileSize}</span>
                      {item.knowledgeBaseName && <span>知识库: {item.knowledgeBaseName}</span>}
                    </div>
                  }
                />
                <Tag color={getStatusColor(item.status)}>{item.status}</Tag>
              </List.Item>
            )}
          />
        ) : (
          <div style={{ textAlign: 'center', color: '#8c8c8c', padding: '20px 0' }}>
            暂无上传记录
          </div>
        )}
      </Card>

      <Card title="处理流程" className={styles.processCard}>
        <Steps
          current={2}
          items={[
            { title: '文件上传', description: '上传原始文件' },
            { title: '内容解析', description: '提取文本/图像/音频内容' },
            { title: '分块处理', description: '按规则分割内容' },
            { title: '向量化', description: '生成embedding向量' },
            { title: '入库完成', description: '可用于检索问答' },
          ]}
          className={styles.steps}
        />
      </Card>
    </div>
  )
}

export default FileUpload
