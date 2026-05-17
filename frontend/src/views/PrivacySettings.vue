<template>
  <div class="content-section sakura-bg">
    <h2>🌸 个性设置</h2>
    <div class="privacy-settings">
      <div class="setting-section">
        <h3>个人资料可见性</h3>
        <div class="setting-item">
          <div class="setting-label">个人主页</div>
          <select v-model="privacy.profileVisibility" class="setting-control">
            <option value="public">公开</option>
            <option value="friends">仅好友</option>
            <option value="private">仅自己</option>
          </select>
        </div>
        <div class="setting-item">
          <div class="setting-label">动态</div>
          <select v-model="privacy.postVisibility" class="setting-control">
            <option value="public">公开</option>
            <option value="friends">仅好友</option>
            <option value="private">仅自己</option>
          </select>
        </div>
      </div>

      <div class="setting-section">
        <h3>消息设置</h3>
        <div class="setting-item">
          <div class="setting-label">接收陌生人私信</div>
          <label class="switch">
            <input type="checkbox" v-model="privacy.allowStrangerMessages">
            <span class="slider round"></span>
          </label>
        </div>
      </div>

      <button class="save-button" @click="saveSettings">保存设置</button>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      privacy: {
        profileVisibility: 'public',
        postVisibility: 'public',
        allowStrangerMessages: true
      }
    }
  },
  methods: {
    saveSettings() {
      alert('个性设置已保存！')
    }
  }
}
</script>

<style scoped>
.privacy-settings {
  background: rgba(255, 255, 255, 0.8);
  border-radius: 15px;
  padding: 20px;

}

.setting-section {
  margin-bottom: 25px;
  padding-bottom: 15px;
  border-bottom: 1px dashed #ffb6c1;
}

.setting-section:last-child {
  border-bottom: none;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 15px 0;
}

.setting-label {
  color: #666;
}

.setting-control {
  padding: 8px 12px;
  border: 1px solid #ffb6c1;
  border-radius: 5px;
  background: white;
  color: #666;
}

.switch {
  position: relative;
  display: inline-block;
  width: 50px;
  height: 24px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: .4s;
  border-radius: 24px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 16px;
  width: 16px;
  left: 4px;
  bottom: 4px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

input:checked+.slider {
  background-color: #ff69b4;
}

input:checked+.slider:before {
  transform: translateX(26px);
}

.save-button {
  display: block;
  width: 100%;
  padding: 12px;
  background: #ff69b4;
  color: white;
  border: none;
  border-radius: 5px;
  font-weight: bold;
  cursor: pointer;
  margin-top: 20px;
  transition: background 0.3s;
}

.save-button:hover {
  background: #ff1493;
}
</style>