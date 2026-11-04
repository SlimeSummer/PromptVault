import { get, post, put, del } from './http'

export function fetchGroups(scope = '', loraId = null, keyword = '') {
  const query = new URLSearchParams()
  if (scope) query.append('scope', scope)
  if (loraId != null) query.append('loraId', loraId)
  if (keyword) query.append('keyword', keyword)
  const qs = query.toString()
  return get(`/api/groups${qs ? '?' + qs : ''}`)
}

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
