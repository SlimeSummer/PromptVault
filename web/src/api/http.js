const JSON_HEADERS = { 'Content-Type': 'application/json' }

async function request(url, options = {}) {
  const { headers = {}, ...rest } = options
  const merged = { ...JSON_HEADERS, ...headers }
  // 值为 undefined 的头直接去掉（postForm 用它让浏览器自动带 multipart boundary）
  Object.keys(merged).forEach((key) => {
    if (merged[key] === undefined) {
      delete merged[key]
    }
  })
  const response = await fetch(url, {
    ...rest,
    headers: merged
  })

  let body
  try {
    body = await response.json()
  } catch (e) {
    throw new Error(`请求失败（HTTP ${response.status}）`)
  }

  if (body.code !== 0) {
    throw new Error(body.message || '操作失败')
  }
  return body.data
}

export const get = (url) => request(url)

export const post = (url, data) =>
  request(url, { method: 'POST', body: JSON.stringify(data) })

export const put = (url, data) =>
  request(url, { method: 'PUT', body: JSON.stringify(data) })

export const del = (url) => request(url, { method: 'DELETE' })

export const postForm = (url, formData) =>
  request(url, {
    method: 'POST',
    body: formData,
    // multipart 的 Content-Type 由浏览器自动带 boundary，不能手动设
    headers: { 'Content-Type': undefined }
  })
