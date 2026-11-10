import { del, get, postForm } from './http'

export function fetchImages(refType, refId) {
  const query = new URLSearchParams({ refType, refId })
  return get(`/api/images?${query.toString()}`)
}

export function uploadImage(refType, refId, file) {
  const formData = new FormData()
  formData.append('refType', refType)
  formData.append('refId', refId)
  formData.append('file', file)
  return postForm('/api/images', formData)
}

export const deleteImage = (id) => del(`/api/images/${id}`)

export const imageUrl = (id) => `/api/images/${id}/raw`
