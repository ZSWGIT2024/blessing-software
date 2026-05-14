import { reactive } from 'vue'

/**
 * Unified OSS upload composable — ALL files direct PUT to OSS (no backend proxy)
 */
export function useOssUpload() {
  const files = reactive([])
  let idCounter = 0

  const tk = () => localStorage.getItem('accessToken') || ''

  const upload = async (fileList, opts = {}) => {
    for (const file of fileList) {
      const id = 'uf_' + (++idCounter) + '_' + Date.now()
      const entry = reactive({
        id, name: file.name, size: file.size, progress: 0, eta: 0,
        status: 'uploading', fileUrl: null, error: null,
        fileObj: file, controller: null
      })
      files.push(entry)
      try {
        const result = await uploadDirect(file, (p) => { entry.progress = p }, entry)
        entry.status = 'success'; entry.progress = 100
        entry.fileUrl = result.fileUrl
        if (opts.onFileComplete) opts.onFileComplete(entry)
      } catch (e) {
        entry.status = e.name === 'CanceledError' || e.message === 'cancelled' ? 'cancelled' : 'failed'
        if (entry.status === 'failed') entry.error = e.message || 'Upload failed'
      }
    }
  }

  const uploadDirect = async (file, onProgress, entry) => {
    const sigRes = await fetch('/api/oss/upload/signature', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + tk() },
      body: JSON.stringify({ fileName: file.name, fileSize: file.size, folder: 'media' })
    })
    const sigData = await sigRes.json()
    if (sigData.code !== 0) throw new Error(sigData.msg || 'Signature failed')

    const { host, objectName, signedUrl } = sigData.data

    return new Promise((resolve, reject) => {
      const ctrl = new AbortController()
      if (entry) entry.controller = ctrl
      const xhr = new XMLHttpRequest()
      xhr.open('PUT', signedUrl)
      xhr.setRequestHeader('Content-Type', file.type || 'application/octet-stream')
      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) onProgress(Math.round(e.loaded * 100 / e.total))
      }
      xhr.onload = () => {
        if (xhr.status === 200 || xhr.status === 204) {
          resolve({ fileUrl: host + '/' + objectName, fileSize: file.size, objectName, fileName: file.name })
        } else reject(new Error('OSS ' + xhr.status))
      }
      xhr.onerror = () => reject(new Error('Network error'))
      xhr.onabort = () => reject(new DOMException('cancelled', 'CanceledError'))
      ctrl.signal.addEventListener('abort', () => xhr.abort())
      xhr.send(file)
    })
  }

  const cancel = (fid) => { const e = files.find(f => f.id === fid); if (e?.controller) e.controller.abort() }
  const retry = (fid) => { const i = files.findIndex(f => f.id === fid); if (i === -1 || !files[i].fileObj) return; const f = files[i]; files.splice(i, 1); upload([f.fileObj]) }
  const remove = (fid) => { const i = files.findIndex(f => f.id === fid); if (i !== -1) files.splice(i, 1) }
  const clear = () => files.splice(0, files.length)

  return { files, upload, cancel, retry, remove, clear }
}
