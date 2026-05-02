<template>
    <div class="social-login">
      <div class="social-title">快捷登录</div>
      <div class="social-icons">
        <div class="social-icon" @mouseenter="showQR('wechat')" @mouseleave="hideQR">
          <span class="iconfont icon-a-weixin_huaban1"></span>
          <div v-if="showQRCode && currentType === 'wechat'" class="qr-code wechat">
            <img :src="currentQR" alt="微信二维码">
            <p>使用微信扫描二维码登录</p>
          </div>
        </div>
        <div class="social-icon" @mouseenter="showQR('qq')" @mouseleave="hideQR">
          <span class="iconfont icon-qq"></span>
          <div v-if="showQRCode && currentType === 'qq'" class="qr-code qq">
            <img :src="currentQR" alt="QQ二维码">
            <p>使用QQ扫描二维码登录</p>
          </div>
        </div>
      </div>
    </div>
  </template>
  
  <script>
  import wechatQR from '../assets/images/wechat-qr.jpg'
  import qqQR from '../assets/images/qq-qr.jpg'
  
  export default {
    data() {
      return {
        showQRCode: false,
        currentType: '',
        currentQR: ''
      }
    },
    methods: {
      showQR(type) {
        this.currentType = type
        this.currentQR = type === 'wechat' ? wechatQR : qqQR
        this.showQRCode = true
      },
      hideQR() {
        this.showQRCode = false
      }
    }
  }
  </script>
  
  <style scoped>
  .social-login {
    width: 100%;
    text-align: center;
    margin: 10px 0;
    position: relative;
  }
  
  .social-title {
    color: #666;
    font-size: 0.9rem;
    margin-bottom: 10px;
  }
  
  .social-icons {
    display: flex;
    justify-content: center;
    gap: 20px;
  }
  
  .social-icon {
    position: relative;
    width: 40px;
    height: 40px;
    background-color: #ddd8d8;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.3s;
  }
.iconfont.icon-qq {
    position: relative;
    width: 40px;
    height: 40px;
    font-size: 30px;
   color: #e655a5;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.3s;
  }
.iconfont.icon-a-weixin_huaban1{
    position: relative;
    width: 40px;
    height: 40px;
    font-size: 30px;
   color: #e655a5;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.3s;
  }
  
  .social-icon:hover {
    transform: scale(1.1);
  }
  
  .qr-code {
    position: absolute;
    background: rgb(238, 234, 237);
    padding: 10px;
    border-radius: 8px;
    box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
    z-index: 100;
    width: 100px;
  }
  
  .qr-code.wechat {
    left: -120px;
    top: -70px;
  }
  
  .qr-code.qq {
    right: -120px;
    top: -70px;
  }
  
  .qr-code img {
    width: 100%;
    height: auto;
  }
  
  .qr-code p {
    margin-top: 5px;
    font-size: 0.8rem;
    color: #666;
    text-align: center;
  }
  </style>