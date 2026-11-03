import { del, get, post, put } from './http'

export function fetchLoras(keyword = '', sort = '', order = '') {
  const query = new URLSearchParams()
  if (keyword) {
    query.append('keyword', keyword)
  }
  if (sort) {
    query.append('sort', sort)
    query.append('order', order)
  }
  return get(`/api/loras?${query.toString()}`)
}

export const fetchLora = (id) => get(`/api/loras/${id}`)

export const createLora = (data) => post('/api/loras', data)

export const updateLora = (id, data) => put(`/api/loras/${id}`, data)

export const deleteLora = (id) => del(`/api/loras/${id}`)

export const fetchLoraPresets = (loraId) => get(`/api/loras/${loraId}/presets`)

export const createPreset = (data) => post('/api/presets', data)

export const fetchPresetDetail = (id) => get(`/api/presets/${id}`)

export const updatePreset = (id, data) => put(`/api/presets/${id}`, data)

export const deletePreset = (id) => del(`/api/presets/${id}`)

export const duplicatePreset = (id) => post(`/api/presets/${id}/duplicate`)

export const addPresetItem = (id, data) => post(`/api/presets/${id}/items`, data)

export const updatePresetItem = (id, itemId, data) =>
  put(`/api/presets/${id}/items/${itemId}`, data)

export const deletePresetItem = (id, itemId) => del(`/api/presets/${id}/items/${itemId}`)

export const reorderPresetItems = (id, itemIds) =>
  put(`/api/presets/${id}/items/order`, { itemIds })
