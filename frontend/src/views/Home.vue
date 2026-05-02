<template>
   
      <!-- 图片入口框 -->
      <div class="portal-wrapper">
        <ImagePortal 
          :initialImage="{ url: '/path/to/your/image.gif', alt: 'Gallery' }"
          :initialText="'画廊'"
        />
      </div>
      
  </template>
  
  <script setup>
  
  import ImagePortal from '@/components/ImagePortal.vue'
 

 
</script>
 
  <style scoped>
  
  .subtitle {
    font-size: 1.2rem;
    color: #7f8d8c;
  }
  
  .portal-wrapper {
    margin: 2rem 0;
    display: flex;
    justify-content: center;
  
  }
  
  .intro-section {
    margin-top: 4rem;
    padding: 2rem;
    background-color: #f8f9fa;
    border-radius: 12px;
  }
  
  .intro-section h2 {
    color: #2c3e50;
    margin-bottom: 1rem;
  }
  
  .intro-section p {
    color: #666;
    line-height: 1.6;
  }
  
  /* 响应式设计 */
  @media (max-width: 768px) {
    .title {
      font-size: 2rem;
    }
    
    .subtitle {
      font-size: 1rem;
    }
  }
  </style>