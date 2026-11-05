import { get, post, put, del } from './http'

export function fetchGroups(scope = '', loraId = null, keyword = '', category = '') {
  const query = new URLSearchParams()
  if (scope) query.append('scope', scope)
  if (loraId != null) query.append('loraId', loraId)
  if (keyword) query.append('keyword', keyword)
  if (category) query.append('category', category)
  const qs = query.toString()
  return get(`/api/groups${qs ? '?' + qs : ''}`)
}

export const fetchGroupCategories = () => get('/api/groups/categories')

export const fetchGroupSuggestions = (keyword) =>
  get(`/api/groups/suggest?keyword=${encodeURIComponent(keyword)}&limit=10`)

export const fetchGroupDetail = (id) => get(`/api/groups/${id}`)

export const createGroup = (data) => post('/api/groups', data)

export const updateGroup = (id, data) => put(`/api/groups/${id}`, data)

export const deleteGroup = (id) => del(`/api/groups/${id}`)

export const duplicateGroup = (id) => post(`/api/groups/${id}/duplicate`)

// 条目

export const addGroupItem = (id, item) => post(`/api/groups/${id}/items`, item)

export const updateGroupItem = (id, itemId, item) => put(`/api/groups/${id}/items/${itemId}`, item)

export const deleteGroupItem = (id, itemId) => del(`/api/groups/${id}/items/${itemId}`)

export const reorderGroupItems = (id, itemIds) =>
  put(`/api/groups/${id}/items/order`, { itemIds })

export const importGroupItems = (id, sourceGroupId) =>
  post(`/api/groups/${id}/items/import`, { sourceGroupId })

// 引用关系
export const addGroupRef = (id, refGroupId, priority, sortOrder) =>
  post(`/api/groups/${id}/refs`, { refGroupId, priority, sortOrder })

export const updateGroupRef = (id, refId, priority, sortOrder) =>
  put(`/api/groups/${id}/refs/${refId}`, { priority, sortOrder })

export const removeGroupRef = (id, refId) =>
  del(`/api/groups/${id}/refs/${refId}`)
