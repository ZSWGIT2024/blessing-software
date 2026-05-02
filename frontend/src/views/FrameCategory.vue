<template>
  <div class="frame-grid">
    <div 
      v-for="frame in frames" 
      :key="frame.id"
      class="frame-item"
      :class="{
        'frame-unlocked': frame.unlocked,
        'frame-using': frame.isUsing,
        'frame-locked': !frame.unlocked
      }"
      @click="$emit('select', frame)"
    >
      <img :src="frame.previewUrl" class="frame-preview" />
      <div class="frame-info">
        <div class="frame-name">{{ frame.name }}</div>
        <div v-if="!frame.unlocked" class="frame-requirement">
          {{ getUnlockRequirement(frame) }}
        </div>
        <el-tag v-if="frame.isUsing" type="success" size="small">使用中</el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps(['frames', 'currentFrame'])
defineEmits(['select'])

// 复用相同的解锁条件方法
const getUnlockRequirement = (frame) => {
  const reqs = []
  if (frame.requiredLevel > 0) reqs.push(`Lv${frame.requiredLevel}`)
  if (frame.requiredVipType ==1 ) reqs.push(`月度VIP会员解锁`)
  if (frame.requiredVipType ==2 ) reqs.push(`季度VIP会员解锁`)
  if (frame.requiredVipType ==3 ) reqs.push(`年度VIP会员解锁`)
  if (frame.requiredVipType ==4 ) reqs.push(`永久VIP会员解锁`)
  if (frame.requiredDays > 0) reqs.push(`累计登录${frame.requiredDays}天`)
  return `需满足: ${reqs.join(' 或 ')}`
}
</script>

<style scoped>
.frame-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 20px;
  padding: 15px;
}

.frame-item {
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
}

.frame-preview {
  width: 100%;
  aspect-ratio: 1;
  object-fit: contain;
  border: 2px solid transparent;
}

.frame-unlocked .frame-preview {
  border-color: #67c23a;
}

.frame-locked .frame-preview {
  filter: grayscale(80%);
  opacity: 0.7;
}

.frame-using {
  box-shadow: 0 0 10px #eb2a9a;
}

.frame-info {
  padding: 8px;
  text-align: center;
}

.frame-requirement {
  font-size: 12px;
  color: #f56c6c;
  margin-top: 4px;
}
</style>
