<template>
    <div class="content-section sakura-bg">
      <h2>🌸 意见建议</h2>
      <div class="feedback-container">
        <div class="feedback-form">
          <div class="form-group">
            <label for="feedback-type">反馈类型</label>
            <select id="feedback-type" v-model="feedback.type">
              <option value="suggestion">建议</option>
              <option value="bug">错误报告</option>
              <option value="complaint">投诉</option>
              <option value="other">其他</option>
            </select>
          </div>
          
          <div class="form-group">
            <label for="feedback-content">反馈内容</label>
            <textarea 
              id="feedback-content" 
              v-model="feedback.content" 
              placeholder="请详细描述您的反馈内容..."
              rows="6"
            ></textarea>
          </div>
          
          <div class="form-group">
            <label>上传截图 (可选)</label>
            <div class="upload-area" @click="triggerFileInput">
              <div v-if="!feedback.image" class="upload-prompt">
                <span class="upload-icon">📁</span>
                <span>点击上传截图</span>
              </div>
              <img v-else :src="feedback.imagePreview" class="upload-preview" />
              <input 
                type="file" 
                ref="fileInput" 
                @change="handleFileUpload" 
                accept="image/*"
                style="display: none;"
              />
            </div>
          </div>
          
          <div class="form-group">
            <label for="contact-method">联系方式</label>
            <input 
              id="contact-method" 
              type="text" 
              v-model="feedback.contact" 
              placeholder="邮箱/QQ/微信等 (选填)"
            />
          </div>
          
          <button class="submit-button" @click="submitFeedback">提交反馈</button>
        </div>
        
        <div class="feedback-history" v-if="history.length > 0">
          <h3>历史反馈</h3>
          <div class="history-item" v-for="item in history" :key="item.id">
            <div class="history-header">
              <span class="history-type">{{ formatType(item.type) }}</span>
              <span class="history-date">{{ item.date }}</span>
              <span class="history-status" :class="item.status">{{ item.status }}</span>
            </div>
            <div class="history-content">{{ item.content }}</div>
          </div>
        </div>
      </div>
    </div>
  </template>
  
  <script>
  export default {
    data() {
      return {
        feedback: {
          type: 'suggestion',
          content: '',
          image: null,
          imagePreview: '',
          contact: ''
        },
        history: [
          {
            id: 1,
            type: 'suggestion',
            content: '希望能增加更多二次元风格的主题',
            date: '2023-04-15',
            status: '已处理'
          },
          {
            id: 2,
            type: 'bug',
            content: '个人主页在移动端显示不正常',
            date: '2023-05-02',
            status: '处理中'
          }
        ]
      }
    },
    methods: {
      triggerFileInput() {
        this.$refs.fileInput.click()
      },
      handleFileUpload(event) {
        const file = event.target.files[0]
        if (file) {
          this.feedback.image = file
          this.feedback.imagePreview = URL.createObjectURL(file)
        }
      },
      formatType(type) {
        const types = {
          suggestion: '建议',
          bug: '错误',
          complaint: '投诉',
          other: '其他'
        }
        return types[type] || type
      },
      submitFeedback() {
        if (!this.feedback.content) {
          alert('请填写反馈内容')
          return
        }
        
        const newFeedback = {
          id: this.history.length + 1,
          type: this.feedback.type,
          content: this.feedback.content,
          date: new Date().toISOString().split('T')[0],
          status: '待处理'
        }
        
        this.history.unshift(newFeedback)
        
        // 重置表单
        this.feedback = {
          type: 'suggestion',
          content: '',
          image: null,
          imagePreview: '',
          contact: ''
        }
        
        alert('反馈已提交，感谢您的支持！')
      }
    }
  }
  </script>
  
  <style scoped>
  .feedback-container {
    display: flex;
    gap: 30px;
  }
  
  .feedback-form {
    flex: 1;
    background: rgba(255, 255, 255, 0.8);
    border-radius: 15px;
    padding: 20px;
  }
  
  .form-group {
    margin-bottom: 20px;
  }
  
  .form-group label {
    display: block;
    margin-bottom: 8px;
    color: #666;
    font-weight: bold;
  }
  
  .form-group select,
  .form-group input,
  .form-group textarea {
    width: 100%;
    padding: 10px;
    border: 1px solid #ffb6c1;
    border-radius: 5px;
    background: white;
  }
  
  .form-group textarea {
    resize: vertical;
  }
  
  .upload-area {
    border: 2px dashed #ffb6c1;
    border-radius: 5px;
    padding: 20px;
    text-align: center;
    cursor: pointer;
    transition: background 0.3s;
  }
  
  .upload-area:hover {
    background: #fff0f5;
  }
  
  .upload-prompt {
    display: flex;
    flex-direction: column;
    align-items: center;
    color: #888;
  }
  
  .upload-icon {
    font-size: 24px;
    margin-bottom: 5px;
  }
  
  .upload-preview {
    max-width: 100%;
    max-height: 200px;
    display: block;
    margin: 0 auto;
  }
  
  .submit-button {
    width: 100%;
    padding: 12px;
    background: #ff69b4;
    color: white;
    border: none;
    border-radius: 5px;
    font-weight: bold;
    cursor: pointer;
    transition: background 0.3s;
  }
  
  .submit-button:hover {
    background: #ff1493;
  }
  
  .feedback-history {
    flex: 1;
    background: rgba(255, 255, 255, 0.8);
    border-radius: 15px;
    padding: 20px;
    max-height: 600px;
    overflow-y: auto;
  }
  
  .history-item {
    margin-bottom: 15px;
    padding-bottom: 15px;
    border-bottom: 1px dashed #ffb6c1;
  }
  
  .history-item:last-child {
    border-bottom: none;
  }
  
  .history-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 10px;
    font-size: 14px;
  }
  
  .history-type {
    color: #ff69b4;
    font-weight: bold;
  }
  
  .history-date {
    color: #888;
  }
  
  .history-status {
    padding: 2px 8px;
    border-radius: 10px;
    font-size: 12px;
  }
  
  .history-status.已处理 {
    background: #e0ffe0;
    color: #2e8b57;
  }
  
  .history-status.处理中 {
    background: #fff0e0;
    color: #ff8c00;
  }
  
  .history-status.待处理 {
    background: #f0f0f0;
    color: #666;
  }
  
  .history-content {
    color: #333;
    line-height: 1.5;
  }
  </style>