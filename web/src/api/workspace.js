import { del, get, post, put } from './http'

export const fetchWorkspaces = () => get('/api/workspaces')

export const fetchWorkspaceDetail = (id) => get(`/api/workspaces/${id}`)

export const createWorkspace = (data) => post('/api/workspaces', data)

export const updateWorkspace = (id, data) => put(`/api/workspaces/${id}`, data)

export const deleteWorkspace = (id) => del(`/api/workspaces/${id}`)

export const addWorkspaceItem = (id, data) => post(`/api/workspaces/${id}/items`, data)

export const updateWorkspaceItem = (id, itemId, data) =>
  put(`/api/workspaces/${id}/items/${itemId}`, data)

export const deleteWorkspaceItem = (id, itemId) =>
  del(`/api/workspaces/${id}/items/${itemId}`)

export const reorderWorkspaceItems = (id, itemIds) =>
  put(`/api/workspaces/${id}/items/order`, { itemIds })

export const unbindWorkspaceGroup = (id, itemId) =>
  post(`/api/workspaces/${id}/items/${itemId}/unbind`)

// --- 编辑区存档 ---

export const fetchArchives = () => get('/api/archives')

export const fetchArchiveDetail = (id) => get(`/api/archives/${id}`)

export const saveArchive = (data) => post('/api/archives', data)

export const importArchive = (payload) => post('/api/archives/import', payload)

export const restoreArchive = (id, workspaceId) =>
  post(`/api/archives/${id}/restore`, { workspaceId })

export const deleteArchive = (id) => del(`/api/archives/${id}`)
