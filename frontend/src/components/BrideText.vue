<template>
    <div class="art-text-container">
      <span class="character sa">冴</span>
      <span class="character e">え</span>
      <span class="character na">な</span>
      <span class="character i">い</span>
      
      <!-- 重点突出的"花嫁"部分 -->
      <span class="character hanayome-wrapper">
        <span class="hanayome">花</span>
        <span class="hanayome">嫁</span>
        <!-- 樱花装饰元素 -->
        <span class="sakura-decor sakura-1">🌸</span>
        <span class="sakura-decor sakura-2">🌸</span>
        <span class="sakura-decor sakura-3">🌸</span>
      </span>
      
      <span class="character no">の</span>
      <span class="character sodate">育</span>
      <span class="character te">て</span>
      <span class="character kata">かた</span>
    </div>
  </template>
  
  <script setup>
  // 可以添加一些樱花动画逻辑
  import { onMounted } from 'vue'
  
  onMounted(() => {
    // 可以在这里添加樱花飘落的动画效果
  })
  </script>
  
  <style scoped>
  .art-text-container {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-wrap: wrap;
    font-family: 'Noto Sans JP', sans-serif;
    top: 0px;
    left: 160px;
  position: absolute;
  }
  
  .character {
    display: inline-block;
    margin: 0 2px;
    transition: all 0.3s ease;
    position: relative;
  }
  
  /* 普通字符样式 */
  .sa { transform: rotate(-5deg); color: #fc0f07; font-size: 1.8rem; }
  .e { transform: translateY(3px); color: orange }
  .na { transform: rotate(3deg); color: #f7df09; font-size: 1.6rem;}
  .i { transform: translateY(-2px); color: #11fa4b; font-size: 1.6rem; }
  .no { transform: rotate(2deg); color: #1fd9fa; font-size: 1.7rem; }
  .sodate { transform: rotate(-3deg); color: #2f20fd; font-size: 1.8rem; }
  .te { transform: translateY(2px); color: #8e19ee; font-size: 1.6rem; }
  .kata { transform: rotate(5deg); color: #f025df; font-size: 1.7rem; }
  
  /* 重点突出的"花嫁"样式 */
  .hanayome-wrapper {
    position: relative;
    margin: 0 8px;
    display: inline-flex;
  }
  
  .hanayome {
    font-size: 3.6rem; /* 其他字的两倍大小 */
    font-weight: 900;
    color: #ff6b9e;
    text-shadow: 2px 2px 4px rgba(216, 21, 174, 0.5);
    background: linear-gradient(to bottom, #ff8fab, #ff6b9e);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    padding: 0 5px;
    margin: 0 -2px;
    border-radius: 30%;
    transform: rotate(-5deg);
    position: relative;
    z-index: 2;
  }
  
  /* 樱花装饰元素 */
  .sakura-decor {
    position: absolute;
    font-size: 1.5rem;
    z-index: 1;
    opacity: 0.8;
    animation: float 5s infinite ease-in-out;
  }
  
  .sakura-1 {
    top: -5px;
    left: 15px;
    animation-delay: 0s;
  }
  
  .sakura-2 {
    bottom: -10px;
    right: 45px;
    animation-delay: 0.5s;
  }
  
  .sakura-3 {
    top: 0;
    right: 5px;
    animation-delay: 1s;
  }
  
  @keyframes float {
    0%, 100% { transform: translateY(0) rotate(0deg); }
    50% { transform: translateY(-8px) rotate(10deg); }
  }
  
  /* 响应式调整 */
  @media (max-width: 1280px) {
    .character {
      font-size: 1.2rem !important;
    }
    .hanayome {
      font-size: 2.4rem !important;
    }
  }
  </style>