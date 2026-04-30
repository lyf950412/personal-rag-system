import React, { useState, useEffect } from 'react'
import { Row, Col, Card, Statistic, Table, Tag, Progress, Spin } from 'antd'
import {
  FileTextOutlined,
  QuestionCircleOutlined,
  CloudUploadOutlined,
  TeamOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
} from '@ant-design/icons'
import { dashboardApi, documentApi, knowledgeBaseApi } from '../services'
import styles from './Dashboard.module.css'

function Dashboard() {
  const [loading, setLoading] = useState(true)
  const [stats, setStats] = useState({
    knowledgeBaseCount: 0,
    documentCount: 0,
    vectorCount: 0,
    chatCount: 0,
  })
  const [recentDocs, setRecentDocs] = useState([])
  const [knowledgeBases, setKnowledgeBases] = useState([])

  useEffect(() => {
    fetchData()
  }, [])

  const fetchData = async () => {
    setLoading(true)
    try {
      const [statsRes, docsRes, kbsRes] = await Promise.all([
        dashboardApi.getStats(),
        documentApi.getRecent(5),
        knowledgeBaseApi.getAll(),
      ])
      
      setStats(statsRes.data || statsRes)
      setRecentDocs(docsRes.data || [])
      setKnowledgeBases(kbsRes.data || [])
    } catch (error) {
      console.error('获取数据失败:', error)
      setStats({
        knowledgeBaseCount: 0,
        documentCount: 0,
        vectorCount: 0,
        chatCount: 8542,
      })
      setRecentDocs([])
      setKnowledgeBases([])
    } finally {
      setLoading(false)
    }
  }

  const statsData = [
    { title: '知识库总数', value: stats.knowledgeBaseCount, icon: <FileTextOutlined />, color: '#1677ff', suffix: '个', trend: 8.5 },
    { title: '文档总数', value: stats.documentCount, icon: <FileTextOutlined />, color: '#52c41a', suffix: '篇', trend: 12.3 },
    { title: '问答次数', value: stats.chatCount, icon: <QuestionCircleOutlined />, color: '#faad14', suffix: '次', trend: -3.2 },
    { title: '向量总数', value: stats.vectorCount, icon: <CloudUploadOutlined />, color: '#722ed1', suffix: '条', trend: 5.7 },
  ]

  const docColumns = [
    { title: '文档名称', dataIndex: 'name', key: 'name', render: (text) => <span className={styles.docName}>{text}</span> },
    { title: '类型', dataIndex: 'type', key: 'type', render: (type) => <Tag color="blue">{type}</Tag> },
    { title: '大小', dataIndex: 'fileSize', key: 'fileSize' },
    { title: '上传时间', dataIndex: 'uploadTime', key: 'uploadTime' },
    {
      title: '知识库',
      dataIndex: 'knowledgeBaseName',
      key: 'knowledgeBaseName',
      render: (name) => name || '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => (
        <Tag color={status === '已处理' ? 'success' : status === '处理中' ? 'processing' : 'default'}>
          {status}
        </Tag>
      ),
    },
  ]

  const kbStats = knowledgeBases.map((kb) => ({
    name: kb.name,
    count: kb.docCount || 0,
    progress: kb.vectorCount && kb.docCount ? Math.round((kb.vectorCount / (kb.docCount * 10)) * 100) : 0,
    color: ['#1677ff', '#52c41a', '#faad14', '#722ed1'][knowledgeBases.indexOf(kb) % 4],
  }))

  if (loading) {
    return (
      <div className={styles.loading}>
        <Spin size="large" />
      </div>
    )
  }

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h1 className={styles.title}>工作台</h1>
        <p className={styles.subtitle}>欢迎回来！您当前拥有 {stats.knowledgeBaseCount} 个知识库</p>
      </div>

      <Row gutter={[16, 16]} className={styles.statsRow}>
        {statsData.map((stat, index) => (
          <Col xs={24} sm={12} lg={6} key={index}>
            <Card className={styles.statCard} hoverable>
              <div className={styles.statHeader}>
                <div className={styles.statIcon} style={{ backgroundColor: stat.color + '15', color: stat.color }}>
                  {stat.icon}
                </div>
                <div className={styles.statTrend}>
                  {stat.trend > 0 ? (
                    <span className={styles.trendUp}><ArrowUpOutlined /> {stat.trend}%</span>
                  ) : (
                    <span className={styles.trendDown}><ArrowDownOutlined /> {Math.abs(stat.trend)}%</span>
                  )}
                </div>
              </div>
              <Statistic
                title={stat.title}
                value={stat.value}
                suffix={stat.suffix}
                valueStyle={{ fontSize: 28, fontWeight: 600 }}
              />
            </Card>
          </Col>
        ))}
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={16}>
          <Card title="最近上传文档" className={styles.card}>
            <Table
              columns={docColumns}
              dataSource={recentDocs}
              pagination={false}
              size="small"
              locale={{ emptyText: '暂无文档数据' }}
            />
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card title="知识库统计" className={styles.card}>
            {kbStats.length > 0 ? (
              <div className={styles.kbList}>
                {kbStats.map((kb, index) => (
                  <div key={index} className={styles.kbItem}>
                    <div className={styles.kbInfo}>
                      <span className={styles.kbName}>{kb.name}</span>
                      <span className={styles.kbCount}>{kb.count} 篇文档</span>
                    </div>
                    <Progress
                      percent={kb.progress}
                      strokeColor={kb.color}
                      showInfo={false}
                      size="small"
                    />
                  </div>
                ))}
              </div>
            ) : (
              <div style={{ textAlign: 'center', color: '#8c8c8c', padding: '20px 0' }}>
                暂无知识库数据
              </div>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default Dashboard
