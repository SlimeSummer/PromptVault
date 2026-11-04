import { del, get, post, put } from './http'

export function fetchTags(params = {}) {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) {
      query.append(key, value)
    }
  })
  return get(`/api/tags?${query.toString()}`)
}

export const fetchTagStats = () => get('/api/tags/stats')

export function fetchTagSuggestions(keyword, limit = 8) {
  const query = new URLSearchParams({ keyword, limit })
  return get(`/api/tags/suggest?${query.toString()}`)
}

export const createTag = (data) => post('/api/tags', data)

export const updateTag = (id, data) => put(`/api/tags/${id}`, data)

export const deleteTag = (id) => del(`/api/tags/${id}`)

export const toggleTagFavorite = (id) => post(`/api/tags/${id}/favorite`)

export const batchImportTags = (data) => post('/api/tags/batch', data)
