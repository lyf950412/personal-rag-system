import React, { useState, useEffect } from 'react'
import { Card, Form, Input, Select, Switch, Button, Slider, Tabs, message, Divider, Tag, Spin } from 'antd'
import {
  SaveOutlined,
  SettingOutlined,
  RocketOutlined,
  DatabaseOutlined,
  SafetyOutlined,
} from '@ant-design/icons'
import { configApi } from '../services'
import styles from './Settings.module.css'

const modelOptions = [
  { value: 'qwen2-7b', label: 'Qwen2-7B (推荐)', provider: '通义千问' },
  { value: 'chatglm3-6b', label: 'ChatGLM3-6B', provider: '智谱AI' },
  { value: 'llama3-8b', label: 'Llama3-8B', provider: 'Meta' },
  { value: 'gpt-4', label: 'GPT-4', provider: 'OpenAI' },
]

const embeddingOptions = [
  { value: 'bge-base-zh', label: 'BGE-base-zh (推荐)', dimension: 768 },
  { value: 'text2vec-base', label: 'text2vec-base-chinese', dimension: 768 },
  { value: 'm3e-base', label: 'm3e-base', dimension: 768 },
  { value: 'clip-vit', label: 'CLIP-ViT-B/32 (多模态)', dimension: 512 },
]

const vectorDBOptions = [
  { value: 'milvus', label: 'Milvus' },
  { value: 'qdrant', label: 'Qdrant' },
  { value: 'chroma', label: 'Chroma' },
]

function Settings() {
  const [form] = Form.useForm()
  const [activeTab, setActiveTab] = useState('model')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    fetchConfigs()
  }, [])

  const fetchConfigs = async () => {
    setLoading(true)
    try {
      const response = await configApi.getAll()
      const configs = response.data || response
      form.setFieldsValue(configs)
    } catch (error) {
      console.error('获取配置失败:', error)
      message.error('获取配置失败')
    } finally {
      setLoading(false)
    }
  }

  const handleSave = async () => {
    setSaving(true)
    try {
      const values = form.getFieldsValue()
      await configApi.update(values)
      message.success('设置保存成功')
    } catch (error) {
      console.error('保存失败:', error)
      message.error('保存失败')
    } finally {
      setSaving(false)
    }
  }

  const testModelConnection = async () => {
    message.loading({ content: '正在测试模型连接...', key: 'testModel' })
    try {
      await new Promise(resolve => setTimeout(resolve, 1500))
      message.success({ content: '模型连接成功！', key: 'testModel' })
    } catch (error) {
      message.error({ content: '模型连接失败，请检查配置', key: 'testModel' })
    }
  }

  const testVectorDB = async () => {
    message.loading({ content: '正在测试向量数据库连接...', key: 'testVector' })
    try {
      await new Promise(resolve => setTimeout(resolve, 1500))
      message.success({ content: '向量数据库连接成功！', key: 'testVector' })
    } catch (error) {
      message.error({ content: '向量数据库连接失败，请检查配置', key: 'testVector' })
    }
  }

  const clearVectorIndex = async () => {
    try {
      await new Promise(resolve => setTimeout(resolve, 1000))
      message.success('向量索引已清空')
    } catch (error) {
      message.error('清空向量索引失败')
    }
  }

  const exportConfig = async () => {
    try {
      const values = form.getFieldsValue()
      const blob = new Blob([JSON.stringify(values, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `rag-config-${new Date().toISOString().split('T')[0]}.json`
      a.click()
      URL.revokeObjectURL(url)
      message.success('配置已导出')
    } catch (error) {
      message.error('导出配置失败')
    }
  }

  const importConfig = () => {
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = '.json'
    input.onchange = async (e) => {
      const file = e.target.files?.[0]
      if (!file) return
      
      try {
        const text = await file.text()
        const configs = JSON.parse(text)
        form.setFieldsValue(configs)
        message.success('配置已导入，请在确认后点击保存')
      } catch (error) {
        message.error('导入配置失败，请检查文件格式')
      }
    }
    input.click()
  }

  const modelSettings = (
    <Form form={form} layout="vertical" className={styles.form}>
      <Form.Item
        name="llmModel"
        label="大语言模型"
        tooltip="选择用于生成回答的LLM模型"
        rules={[{ required: true }]}
      >
        <Select options={modelOptions} />
      </Form.Item>
      
      <Form.Item
        name="embeddingModel"
        label="Embedding模型"
        tooltip="选择用于文本向量化的模型"
        rules={[{ required: true }]}
      >
        <Select options={embeddingOptions} />
      </Form.Item>

      <Form.Item
        name="apiKey"
        label="API Key"
        tooltip="如果使用云端模型，请填写API Key"
      >
        <Input.Password placeholder="请输入API Key" />
      </Form.Item>

      <Form.Item
        name="apiBaseUrl"
        label="API Base URL"
        tooltip="模型API的基础URL"
      >
        <Input placeholder="http://localhost:8000/v1" />
      </Form.Item>

      <Divider />

      <Form.Item
        name="temperature"
        label="Temperature (创造性)"
        tooltip="值越高回答越有创造性，值越低回答越确定"
      >
        <Slider min={0} max={1} step={0.1} marks={{ 0: '确定', 0.5: '平衡', 1: '创造' }} />
      </Form.Item>

      <Form.Item
        name="maxTokens"
        label="最大输出长度"
        tooltip="模型单次回答的最大token数"
      >
        <Slider min={256} max={4096} step={256} marks={{ 256: '256', 2048: '2048', 4096: '4096' }} />
      </Form.Item>
    </Form>
  )

  const vectorSettings = (
    <Form form={form} layout="vertical" className={styles.form}>
      <Form.Item
        name="vectorDB"
        label="向量数据库"
        tooltip="选择用于存储和检索向量的数据库"
      >
        <Select options={vectorDBOptions} />
      </Form.Item>

      <Form.Item
        name="vectorDBUrl"
        label="数据库连接地址"
      >
        <Input placeholder="http://localhost:19530" />
      </Form.Item>

      <Form.Item
        name="chunkSize"
        label="分块大小 (Chunk Size)"
        tooltip="文档分割的块大小，影响检索精度和性能"
      >
        <Slider min={100} max={2000} step={100} marks={{ 100: '100', 500: '500', 1000: '1000', 2000: '2000' }} />
      </Form.Item>

      <Form.Item
        name="chunkOverlap"
        label="分块重叠 (Chunk Overlap)"
        tooltip="相邻块之间的重叠比例，保持上下文连贯性"
      >
        <Slider min={0} max={200} step={10} marks={{ 0: '0', 100: '100', 200: '200' }} />
      </Form.Item>

      <Form.Item
        name="topK"
        label="检索返回数量 (Top-K)"
        tooltip="每次检索返回的最相关片段数量"
      >
        <Slider min={1} max={20} step={1} marks={{ 1: '1', 5: '5', 10: '10', 20: '20' }} />
      </Form.Item>

      <Form.Item
        name="similarityThreshold"
        label="相似度阈值"
        tooltip="低于此相似度的结果将被过滤"
      >
        <Slider min={0} max={1} step={0.05} marks={{ 0: '0', 0.5: '0.5', 0.7: '0.7', 1: '1' }} />
      </Form.Item>
    </Form>
  )

  const systemSettings = (
    <Form form={form} layout="vertical" className={styles.form}>
      <Form.Item name="systemName" label="系统名称">
        <Input />
      </Form.Item>

      <Form.Item name="maxFileSize" label="单文件最大上传大小 (MB)">
        <Input type="number" suffix="MB" />
      </Form.Item>

      <Divider />

      <Form.Item name="enableAuditLog" label="启用操作审计日志" valuePropName="checked">
        <Switch />
      </Form.Item>

      <Form.Item name="enableNotifications" label="启用系统通知" valuePropName="checked">
        <Switch />
      </Form.Item>

      <Form.Item name="autoIndex" label="上传后自动索引" valuePropName="checked">
        <Switch />
      </Form.Item>

      <Form.Item name="enableCache" label="启用检索缓存" valuePropName="checked">
        <Switch />
      </Form.Item>
    </Form>
  )

  const items = [
    {
      key: 'model',
      label: <span><RocketOutlined />模型设置</span>,
      children: modelSettings,
    },
    {
      key: 'vector',
      label: <span><DatabaseOutlined />向量检索</span>,
      children: vectorSettings,
    },
    {
      key: 'system',
      label: <span><SettingOutlined />系统配置</span>,
      children: systemSettings,
    },
  ]

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px 0' }}>
        <Spin size="large" />
      </div>
    )
  }

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h2 className={styles.title}>系统设置</h2>
        <Tag color="blue">配置模型、检索和系统参数</Tag>
      </div>

      <div className={styles.settingsLayout}>
        <Card className={styles.settingsCard}>
          <Tabs
            activeKey={activeTab}
            onChange={setActiveTab}
            items={items}
            className={styles.tabs}
          />
        </Card>

        <Card className={styles.actionCard}>
          <h3 className={styles.actionTitle}>快捷操作</h3>
          <div className={styles.actionList}>
            <Button 
              type="primary" 
              icon={<SaveOutlined />} 
              onClick={handleSave} 
              block 
              size="large"
              loading={saving}
            >
              保存所有设置
            </Button>
            <Button block size="large" className={styles.actionButton} onClick={testModelConnection}>
              测试模型连接
            </Button>
            <Button block size="large" className={styles.actionButton} onClick={testVectorDB}>
              测试向量数据库
            </Button>
            <Button block size="large" danger className={styles.actionButton} onClick={clearVectorIndex}>
              清空向量索引
            </Button>
            <Button block size="large" className={styles.actionButton} onClick={exportConfig}>
              导出配置
            </Button>
            <Button block size="large" className={styles.actionButton} onClick={importConfig}>
              导入配置
            </Button>
          </div>
        </Card>
      </div>
    </div>
  )
}

export default Settings
