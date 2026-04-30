import React, { useState, useEffect } from 'react'
import { Routes, Route, useNavigate, useLocation, Navigate } from 'react-router-dom'
import { Layout, Menu, Avatar, Dropdown, Badge, Space, message } from 'antd'
import {
  MessageOutlined,
  FolderOutlined,
  UploadOutlined,
  SettingOutlined,
  DashboardOutlined,
  BellOutlined,
  UserOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SearchOutlined,
} from '@ant-design/icons'
import Dashboard from './pages/Dashboard'
import Chat from './pages/Chat'
import KnowledgeBase from './pages/KnowledgeBase'
import FileUpload from './pages/FileUpload'
import Settings from './pages/Settings'
import Login from './pages/Login'
import { authService } from './services/auth'
import styles from './App.module.css'

const { Sider, Header, Content } = Layout

const menuItems = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: '工作台' },
  { key: '/chat', icon: <MessageOutlined />, label: '智能问答' },
  { key: '/knowledge', icon: <FolderOutlined />, label: '知识库管理' },
  { key: '/upload', icon: <UploadOutlined />, label: '文件上传' },
  { key: '/settings', icon: <SettingOutlined />, label: '系统设置' },
]

const userMenuItems = [
  { key: 'profile', icon: <UserOutlined />, label: '个人中心' },
  { key: 'logout', icon: <LogoutOutlined />, label: '退出登录' },
]

function ProtectedRoute({ children }) {
  const isAuthenticated = authService.isAuthenticated()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  return children
}

function AppLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()

  const user = authService.getCurrentUser()

  const handleMenuClick = ({ key }) => {
    if (key === 'logout') {
      authService.logout()
      navigate('/login')
    } else if (key === 'profile') {
      message.info('个人中心功能开发中')
    } else {
      navigate(key)
    }
  }

  const handleGlobalSearch = (value) => {
    if (value.trim()) {
      navigate('/knowledge', { state: { searchKeyword: value } })
      message.info(`搜索: ${value}`)
    }
  }

  return (
    <Layout className={styles.layout}>
      <Sider trigger={null} collapsible collapsed={collapsed} className={styles.sider} theme="light">
        <div className={styles.logo}>
          {!collapsed ? (
            <div className={styles.logoText}>
              <span className={styles.logoIcon}>RAG</span>
              <span className={styles.logoTitle}>知识库系统</span>
            </div>
          ) : (
            <span className={styles.logoIcon}>RAG</span>
          )}
        </div>
        <Menu
          theme="light"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={handleMenuClick}
          className={styles.menu}
        />
      </Sider>
      <Layout>
        <Header className={styles.header}>
          <div className={styles.headerLeft}>
            <span
              className={styles.trigger}
              onClick={() => setCollapsed(!collapsed)}
            >
              {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            </span>
            <div className={styles.searchBar}>
              <SearchOutlined className={styles.searchIcon} />
              <input 
                type="text" 
                placeholder="全局搜索知识库..." 
                className={styles.searchInput}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    handleGlobalSearch(e.target.value)
                  }
                }}
              />
            </div>
          </div>
          <div className={styles.headerRight}>
            <Space size={24}>
              <Badge count={5}>
                <BellOutlined style={{ fontSize: 18, cursor: 'pointer' }} />
              </Badge>
              <Dropdown menu={{ items: userMenuItems, onClick: handleMenuClick }} placement="bottomRight">
                <Space style={{ cursor: 'pointer' }}>
                  <Avatar size={36} style={{ backgroundColor: '#1890ff' }}>
                    {user.username ? user.username.charAt(0).toUpperCase() : 'U'}
                  </Avatar>
                  <span style={{ color: '#333' }}>{user.username || '用户'}</span>
                </Space>
              </Dropdown>
            </Space>
          </div>
        </Header>
        <Content className={styles.content}>
          <div className={styles.contentInner}>
            <Routes>
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/chat" element={<Chat />} />
              <Route path="/knowledge" element={<KnowledgeBase />} />
              <Route path="/upload" element={<FileUpload />} />
              <Route path="/settings" element={<Settings />} />
            </Routes>
          </div>
        </Content>
      </Layout>
    </Layout>
  )
}

function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/*"
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      />
    </Routes>
  )
}

export default App
