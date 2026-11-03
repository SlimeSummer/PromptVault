import { get, put } from './http'

export const fetchSettings = () => get('/api/settings')

export const saveSettings = (data) => put('/api/settings', data)

export const fetchTagCategories = () => get('/api/tags/categories')
