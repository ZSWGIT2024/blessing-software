<template>
  <!-- Video Background with Fallback -->
  <div class="video-container">
    <video 
      ref="videoBg"
      class="video-background" 
      autoplay 
      muted 
      loop
      playsinline
      :poster="fallbackImage"
    >
      <source :src="sakuraVideo" type="video/mp4">
      <!-- Fallback image if video can't load -->
      <img :src="fallbackImage" alt="Sakura background" class="fallback-image">
    </video>
  </div>

  <!-- Logo -->
  <img 
    class="logo" 
    :src="logoImage" 
    alt="Company Logo"
    @load="handleImageLoad"
    @error="handleImageError"
  >

  <!-- Stylized Text -->
  <div class="art-text-container">
    <div class="art-text-group">
      <span class="kanji small left">天</span>
      <span class="kanji main">恵</span>
      <span class="kanji small right">下</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import sakuraVideo from '@/assets/videos/sakura1.mp4'
import logoImage from '@/assets/images/惠天下LOGO.png'
import fallbackImage from '@/assets/images/sakura-fallback.jpg'

const videoBg = ref(null)

// Handle video autoplay with fallback
const initVideo = () => {
  if (videoBg.value) {
    const playPromise = videoBg.value.play()
    
    if (playPromise !== undefined) {
      playPromise
        .then(() => {
          console.log('Video autoplay started')
        })
        .catch(error => {
          console.warn('Autoplay prevented:', error)
          videoBg.value.controls = true
        })
    }
  }
}

// Image loading handlers
const handleImageLoad = () => {}

const handleImageError = () => {
  console.error('Failed to load logo image')
}

onMounted(() => {
  initVideo()
  
  // Intersection observer for video play/pause
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting && videoBg.value) {
        videoBg.value.play().catch(e => console.log("Playback error:", e))
      } else if (videoBg.value) {
        videoBg.value.pause()
      }
    })
  }, { threshold: 0.1 })

  if (videoBg.value) {
    observer.observe(videoBg.value)
  }

  return () => {
    if (videoBg.value) {
      observer.unobserve(videoBg.value)
    }
  }
})
</script>

<style scoped>
/* Video Container */
.video-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  z-index: -1;
}

.video-background {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}

.fallback-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* Logo */
.logo {
  position: absolute;
  top: 10px;
  left: 5px;
  width: 150px;
  height: auto;
  opacity: 0.9;
  transition: opacity 0.3s ease;
  z-index: 10;
}

.logo:hover {
  opacity: 1;
}

/* Art Text */
.art-text-container {
  position: fixed;
  top: 30px;
  left: 30px;
  z-index: 100;
}

.art-text-group {
  display: inline-block;
  position: relative;
  filter: drop-shadow(0 0 10px rgba(255, 105, 180, 0.5));
  padding: 20px;
}

.kanji {
  font-family: 'Nico Moji', 'Noto Sans JP', sans-serif;
  font-weight: 900;
  line-height: 1;
  display: inline-block;
  position: relative;
}

.main {
  font-size: 3rem;
  color: #f02c94;
  margin: 0 0.3rem;
  text-shadow:
    3px 3px 0 #ff69b4,
    6px 6px 0 #ffb6c1,
    9px 9px 0 #fff0f5;
  transform: rotate(-3deg);
  z-index: 3;
  left: -13px;
  top: -13px;
}

.small {
  font-size: 2rem;
  color: #ff92b6;
  text-shadow:
    2px 2px 0 #66e3ec,
    4px 4px 0 #17ec70;
  margin-bottom: 1rem;
  z-index: 2;
  left: -9px;
  top: -10px;
}

.left {
  transform: rotate(-8deg) translateY(0.5rem);
  margin-right: -1rem;
}

.right {
  transform: rotate(5deg) translateY(0.5rem);
  margin-left: -1rem;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .logo {
    width: 100px;
    top: 15px;
    left: 15px;
  }

  .art-text-container {
    top: 20px;
    left: 60px;
  }

  .main {
    font-size: 2rem;
  }

  .small {
    font-size: 1.5rem;
  }
}
</style>