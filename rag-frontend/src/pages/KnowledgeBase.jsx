import React, { useState, useEffect } from 'react'
import { Table, Button, Input, Tag, Space, Modal, Form, Select, Popconfirm, Card, Row, Col, Progress, message, Spin, Drawer } from 'antd'
import {
  PlusOutlined,
  SearchOutlined,
  EditOutlined,
  DeleteOutlined,
  FolderOutlined,
  FileTextOutlined,
  EyeOutlined,
  UploadOutlined,
  SettingOutlined,
  ReloadOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { knowledgeBaseApi, documentApi } from '../services'
import styles from './KnowledgeBase.module.css'

function KnowledgeBase() {
  const [searchText, setSearchText] = useState('')
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const [knowledgeBases, setKnowledgeBases] = useState([])
  const [editingRecord, setEditingRecord] = useState(null)
  const [form] = Form.useForm()
  const [selectedKB, setSelectedKB] = useState(null)
  const [kbDocuments, setKBDocuments] = useState([])
  const [drawerVisible, setDrawerVisible] = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    fetchKnowledgeBases()
  }, [])

  const fetchKnowledgeBases = async () => {
    setLoading(true)
    try {
      const response = await knowledgeBaseApi.getAll()
      setKnowledgeBases(response.data || response)
    } catch (error) {
      console.error('获取知识库失败:', error)
      message.error('获取知识库列表失败')
    } finally {
      setLoading(false)
    }
  }

  const handleCreate = () => {
    setEditingRecord(null)
    form.resetFields()
    setIsModalOpen(true)
  }

  const handleEdit = (record) => {
    setEditingRecord(record)
    form.setFieldsValue(record)
    setIsModalOpen(true)
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      
      if (editingRecord) {
        await knowledgeBaseApi.update(editingRecord.id, values)
        message.success('知识库更新成功')
      } else {
        await knowledgeBaseApi.create(values)
        message.success('知识库创建成功')
      }
      
      setIsModalOpen(false)
      form.resetFields()
      fetchKnowledgeBases()
    } catch (error) {
      console.error('保存失败:', error)
      message.error('保存失败')
    }
  }

  const handleDelete = async (id) => {
    try {
      await knowledgeBaseApi.delete(id)
      message.success('删除成功')
      fetchKnowledgeBases()
    } catch (error) {
      console.error('删除失败:', error)
      message.error('删除失败')
    }
  }

  const handleSearch = async (value) => {
    if (!value.trim()) {
      fetchKnowledgeBases()
      return
    }
    
    setLoading(true)
    try {
      const response = await knowledgeBaseApi.search(value)
      setKnowledgeBases(response.data || response)
    } catch (error) {
      console.error('搜索失败:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleView = async (record) => {
    setSelectedKB(record)
    setDrawerVisible(true)
    
    try {
      const response = await documentApi.getByKnowledgeBase(record.id)
      setKBDocuments(response.data || response || [])
    } catch (error) {
      console.error('获取知识库文档失败:', error)
      setKBDocuments([])
    }
  }

  const handleUpload = (record) => {
    navigate('/upload', { state: { selectedKB: record } })
  }

  const handleDeleteDocument = async (docId) => {
    try {
      await documentApi.delete(docId)
      message.success('文档删除成功')
      const response = await documentApi.getByKnowledgeBase(selectedKB.id)
      setKBDocuments(response.data || response || [])
      fetchKnowledgeBases()
    } catch (error) {
      console.error('删除文档失败:', error)
      message.error('删除文档失败')
    }
  }

  const columns = [
    {
      title: '知识库名称',
      dataIndex: 'name',
      key: 'name',
      render: (text, record) => (
        <div className={styles.kbNameCell}>
          <FolderOutlined className={styles.folderIcon} />
          <div>
            <span className={styles.kbName}>{text}</span>
            <span className={styles.kbDesc}>{record.description}</span>
          </div>
        </div>
      ),
    },
    {
      title: '文档数',
      dataIndex: 'docCount',
      key: 'docCount',
      render: (count, record) => (
        <div>
          <div>{count || 0} 篇</div>
          <div className={styles.vectorCount}>{record.vectorCount || 0} 向量</div>
        </div>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => (
        <Tag color={status === '正常' ? 'success' : 'processing'}>{status}</Tag>
      ),
    },
    {
      title: '负责人',
      dataIndex: 'owner',
      key: 'owner',
      render: (owner) => owner || '-',
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      render: (date) => date ? new Date(date).toLocaleDateString() : '-',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleView(record)}>查看</Button>
          <Button type="link" size="small" icon={<UploadOutlined />} onClick={() => handleUpload(record)}>上传</Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>编辑</Button>
          <Popconfirm title="确定删除此知识库吗？" okText="确定" cancelText="取消" onConfirm={() => handleDelete(record.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const kbCards = knowledgeBases.map((kb, index) => ({
    ...kb,
    color: ['#1677ff', '#52c41a', '#faad14', '#722ed1'][index % 4],
    progress: kb.vectorCount && kb.docCount ? Math.round((kb.vectorCount / (kb.docCount * 10)) * 100) : 0,
  }))

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <div className={styles.headerLeft}>
          <h2 className={styles.title}>知识库管理</h2>
          <Tag color="blue">共 {knowledgeBases.length} 个知识库</Tag>
        </div>
        <div className={styles.headerRight}>
          <Input.Search
            placeholder="搜索知识库..."
            prefix={<SearchOutlined />}
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            onSearch={handleSearch}
            className={styles.searchInput}
            allowClear
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            创建知识库
          </Button>
          <Button icon={<ReloadOutlined />} onClick={fetchKnowledgeBases} loading={loading}>
            刷新
          </Button>
        </div>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '50px 0' }}>
          <Spin size="large" />
        </div>
      ) : (
        <>
          <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
            {kbCards.map((kb) => (
              <Col xs={24} sm={12} lg={6} key={kb.id}>
                <Card className={styles.kbCard} hoverable>
                  <div className={styles.kbCardHeader}>
                    <FolderOutlined style={{ fontSize: 24, color: kb.color }} />
                    <Tag color={kb.status === '正常' ? 'success' : 'processing'}>{kb.status}</Tag>
                  </div>
                  <h3 className={styles.kbCardTitle}>{kb.name}</h3>
                  <p className={styles.kbCardDesc}>{kb.description || '暂无描述'}</p>
                  <div className={styles.kbCardStats}>
                    <div className={styles.statItem}>
                      <FileTextOutlined />
                      <span>{kb.docCount || 0} 篇文档</span>
                    </div>
                    <div className={styles.statItem}>
                      <span>{kb.vectorCount || 0} 向量</span>
                    </div>
                  </div>
                  <Progress percent={kb.progress} size="small" strokeColor={kb.color} />
                  <div className={styles.kbCardActions}>
                    <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleView(kb)}>查看</Button>
                    <Button type="link" size="small" icon={<UploadOutlined />} onClick={() => handleUpload(kb)}>上传</Button>
                    <Button type="link" size="small" icon={<SettingOutlined />} onClick={() => handleEdit(kb)}>设置</Button>
                  </div>
                </Card>
              </Col>
            ))}
          </Row>

          <Card title="详细列表" className={styles.tableCard}>
            <Table
              columns={columns}
              dataSource={knowledgeBases}
              rowKey={(record) => record.id || `kb-${record.name}`}
              pagination={{ pageSize: 10 }}
              size="middle"
              locale={{ emptyText: '暂无知识库数据' }}
            />
          </Card>
        </>
      )}

      <Modal
        title={editingRecord ? '编辑知识库' : '创建知识库'}
        open={isModalOpen}
        onOk={handleSubmit}
        onCancel={() => setIsModalOpen(false)}
        okText={editingRecord ? '更新' : '创建'}
        cancelText="取消"
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="知识库名称"
            rules={[{ required: true, message: '请输入知识库名称' }]}
          >
            <Input placeholder="请输入知识库名称" />
          </Form.Item>
          <Form.Item
            name="description"
            label="描述"
            rules={[{ required: true, message: '请输入知识库描述' }]}
          >
            <Input.TextArea rows={3} placeholder="请输入知识库描述" />
          </Form.Item>
          <Form.Item
            name="owner"
            label="负责人"
            rules={[{ required: true, message: '请选择负责人' }]}
          >
            <Select placeholder="请选择负责人">
              <Select.Option value="产品团队">产品团队</Select.Option>
              <Select.Option value="技术团队">技术团队</Select.Option>
              <Select.Option value="行政团队">行政团队</Select.Option>
              <Select.Option value="HR团队">HR团队</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="tags" label="标签">
            <Select mode="tags" placeholder="请输入标签" />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <FolderOutlined style={{ color: '#1677ff' }} />
            <span>{selectedKB?.name || '知识库详情'}</span>
          </div>
        }
        placement="right"
        width={600}
        open={drawerVisible}
        onClose={() => setDrawerVisible(false)}
        extra={
          <Button type="primary" icon={<UploadOutlined />} onClick={() => {
            setDrawerVisible(false)
            handleUpload(selectedKB)
          }}>
            上传文档
          </Button>
        }
      >
        {selectedKB && (
          <div>
            <Card size="small" style={{ marginBottom: 16 }}>
              <p><InfoCircleOutlined style={{ marginRight: 8 }} /><strong>描述：</strong>{selectedKB.description || '暂无描述'}</p>
              <p><strong>负责人：</strong>{selectedKB.owner || '-'}</p>
              <p><strong>文档数：</strong>{selectedKB.docCount || 0} 篇</p>
              <p><strong>向量数：</strong>{selectedKB.vectorCount || 0} 条</p>
              <p><strong>状态：</strong><Tag color={selectedKB.status === '正常' ? 'success' : 'processing'}>{selectedKB.status}</Tag></p>
            </Card>
            
            <h4>文档列表</h4>
            {kbDocuments.length > 0 ? (
              <div style={{ maxHeight: 'calc(100vh - 400px)', overflowY: 'auto' }}>
                {kbDocuments.map((doc, index) => (
                  <Card key={doc.id || `doc-${index}`} size="small" style={{ marginBottom: 8 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <div>
                        <div style={{ fontWeight: 500 }}>{doc.name}</div>
                        <div style={{ fontSize: 12, color: '#8c8c8c' }}>
                          <Tag size="small">{doc.type}</Tag>
                          <span>{doc.fileSize}</span>
                          {doc.uploadTime && <span> · {new Date(doc.uploadTime).toLocaleDateString()}</span>}
                        </div>
                      </div>
                      <div>
                        <Tag color={doc.status === '已处理' ? 'success' : doc.status === '处理中' ? 'processing' : 'default'}>{doc.status}</Tag>
                        <Button type="link" danger size="small" onClick={() => handleDeleteDocument(doc.id)}>删除</Button>
                      </div>
                    </div>
                  </Card>
                ))}
              </div>
            ) : (
              <div style={{ textAlign: 'center', padding: '40px 0', color: '#8c8c8c' }}>
                <FileTextOutlined style={{ fontSize: 48, marginBottom: 16 }} />
                <p>暂无文档</p>
                <Button type="primary" icon={<UploadOutlined />} onClick={() => handleUpload(selectedKB)}>
                  上传第一个文档
                </Button>
              </div>
            )}
          </div>
        )}
      </Drawer>
    </div>
  )
}

export default KnowledgeBase
