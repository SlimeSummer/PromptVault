const JSON_HEADERS = { 'Content-Type': 'application/json' }

async function request(url, options = {}) {
  const { headers = {}, ...rest } = options
  const response = await fetch(url, {
    ...rest,
    headers: { ...JSON_HEADERS, ...headers }
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
