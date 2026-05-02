<template>
  <div class="confirm-dialog-overlay">
    <div class="confirm-dialog">
      <div class="dialog-message">{{ message }}</div>
      <div class="dialog-actions">
        <button class="confirm-btn" @click="$emit('confirm')">保存并退出</button>
        <button class="cancel-btn" @click="$emit('cancel')">不保存退出</button>
        <button class="close-btn" @click="$emit('close')">取消</button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    message: String
  }
}
</script>

<style scoped>
.confirm-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
}

.confirm-dialog {
  background: white;
  padding: 20px;
  border-radius: 10px;
  width: 400px;
  max-width: 90%;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.2);
}

.dialog-message {
  margin-bottom: 20px;
  font-size: 16px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.confirm-btn {
  background: #ff69b4;
  color: white;
}

.cancel-btn {
  background: #f0f0f0;
}

.close-btn {
  background: #f0f0f0;
}
</style>