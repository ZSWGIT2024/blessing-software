<template>
  <div class="agreement">
    <label>
      <input type="checkbox" :checked="checked" @change="toggleCheck">
      我已阅读并同意
      <a href="#" @click.prevent="openAgreement('user')">《用户协议》</a>
      和
      <a href="#" @click.prevent="openAgreement('privacy')">《隐私政策》</a>
    </label>
  </div>

  <!-- Teleport 到 body，完全脱离 LoginModal 的层叠上下文 -->
  <Teleport to="body">
    <div v-if="showDialog" class="agreement-overlay" @click.self="showDialog = false">
      <div class="agreement-panel">
        <div class="agreement-header">
          <h3>{{ dialogTitle }}</h3>
          <button class="close-btn" @click="showDialog = false">&times;</button>
        </div>
        <div class="agreement-body" v-html="dialogContent"></div>
        <div class="agreement-footer">
          <button class="confirm-btn" @click="showDialog = false">我已阅读并同意</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue'])

const checked = ref(false)
const showDialog = ref(false)
const dialogTitle = ref('')
const dialogContent = ref('')

const toggleCheck = () => {
  checked.value = !checked.value
  emit('update:modelValue', checked.value)
}

const openAgreement = (type) => {
  if (type === 'user') {
    dialogTitle.value = '用户协议'
    dialogContent.value = userAgreementText
  } else {
    dialogTitle.value = '隐私政策'
    dialogContent.value = privacyPolicyText
  }
  showDialog.value = true
}

// ==================== 协议文本（硬编码） ====================

const userAgreementText = `<div style="line-height:1.8;font-size:14px;">
<p><strong>更新日期：2026年5月11日</strong></p>
<p><strong>生效日期：2026年5月11日</strong></p>

<h4>一、总则</h4>
<p>1.1 欢迎使用惠天下（以下简称"本平台"）。本协议是您与本平台之间关于使用本平台服务所订立的协议。</p>
<p>1.2 您在注册过程中勾选"我已阅读并同意《用户协议》"并完成注册，即表示您已充分阅读、理解并接受本协议的全部内容。</p>
<p>1.3 本平台有权根据需要不时修订本协议。修订后的协议一经发布即生效，取代原协议。您继续使用本平台服务即视为接受修订后的协议。</p>

<h4>二、账号注册与管理</h4>
<p>2.1 您注册时须提供真实、准确的手机号或邮箱，并设置安全的密码。每个手机号或邮箱仅可注册一个账号。</p>
<p>2.2 您应对账号和密码的安全性负责，不得将账号出借、转让或提供给他人使用。因账号保管不善导致的一切后果由您自行承担。</p>
<p>2.3 如发现账号被盗用或存在安全风险，您应立即通知本平台。本平台有权对可疑账号采取暂停服务等措施。</p>
<p>2.4 您连续12个月未登录本平台，本平台有权注销您的账号。</p>

<h4>三、用户行为规范</h4>
<p>3.1 您在使用本平台服务时，应遵守中华人民共和国相关法律法规，不得利用本平台从事以下行为：</p>
<ul>
  <li>发布、传播反对宪法所确定的基本原则的信息；</li>
  <li>发布、传播危害国家安全、泄露国家秘密、颠覆国家政权、破坏国家统一的信息；</li>
  <li>发布、传播损害国家荣誉和利益的信息；</li>
  <li>发布、传播煽动民族仇恨、民族歧视，破坏民族团结的信息；</li>
  <li>发布、传播破坏国家宗教政策，宣扬邪教和封建迷信的信息；</li>
  <li>发布、传播谣言，扰乱社会秩序，破坏社会稳定的信息；</li>
  <li>发布、传播淫秽、色情、赌博、暴力、凶杀、恐怖或者教唆犯罪的信息；</li>
  <li>侮辱或者诽谤他人，侵害他人合法权益的；</li>
  <li>发布、传播含有法律、行政法规禁止的其他内容的信息。</li>
</ul>
<p>3.2 您上传的作品（图片、视频等）应为您拥有合法权利的内容，不得侵犯他人的知识产权、肖像权、隐私权等合法权益。</p>

<h4>四、知识产权</h4>
<p>4.1 您在本平台上传的作品，其知识产权仍归您所有。您授予本平台在全球范围内免费、非独家、可再许可的使用权，以便本平台提供、推广和改进服务。</p>
<p>4.2 本平台的名称、标识、源代码、界面设计等知识产权归本平台所有，未经许可不得使用。</p>
<p>4.3 如您认为本平台上的内容侵犯了您的知识产权，请通过客服渠道联系我们，我们将依法处理。</p>

<h4>五、VIP会员服务</h4>
<p>5.1 本平台提供月度、季度、年度及终身VIP会员服务，具体价格和权益以购买页面为准。</p>
<p>5.2 VIP会员服务购买后不支持退款，除非因本平台原因导致服务无法正常提供。</p>
<p>5.3 VIP会员到期后未续费，相关权益自动终止。</p>

<h4>六、免责声明</h4>
<p>6.1 本平台按"现状"提供服务和内容，不作任何明示或默示的保证，包括但不限于适销性、特定用途适用性和非侵权的保证。</p>
<p>6.2 本平台不对以下情况导致的损失承担责任：不可抗力、网络故障、黑客攻击、计算机病毒、政府管制等。</p>
<p>6.3 本平台有权随时修改、暂停或终止部分或全部服务，无需事先通知。</p>

<h4>七、违约责任</h4>
<p>7.1 如您违反本协议约定，本平台有权采取以下一项或多项措施：警告、限制功能、暂停服务、永久封禁账号、追究法律责任。</p>
<p>7.2 因您的违约行为给本平台或第三方造成损失的，您应承担全部赔偿责任。</p>

<h4>八、其他</h4>
<p>8.1 本协议的解释、效力及争议的解决，适用中华人民共和国法律。</p>
<p>8.2 如本协议的任何条款被认定为无效或不可执行，该条款应在必要的最小范围内被限制或删除，其余条款继续有效。</p>
<p>8.3 如您对本协议有任何疑问，请联系客服邮箱：2977025339@qq.com</p>
</div>`

const privacyPolicyText = `<div style="line-height:1.8;font-size:14px;">
<p><strong>更新日期：2026年5月11日</strong></p>
<p><strong>生效日期：2026年5月11日</strong></p>

<h4>引言</h4>
<p>惠天下（以下简称"我们"）深知个人信息对您的重要性。我们承诺按照法律法规和业界成熟的安全标准保护您的个人信息。本隐私政策旨在说明我们如何收集、使用、存储和共享您的个人信息。</p>

<h4>一、我们收集的信息</h4>
<p>1.1 <strong>账号信息：</strong>手机号、邮箱地址、用户名、头像、密码（加密存储）。</p>
<p>1.2 <strong>个人资料：</strong>昵称、性别、生日、个性签名、兴趣爱好、血型、星座等您主动填写的资料。</p>
<p>1.3 <strong>内容数据：</strong>您上传的图片、视频、评论、收藏、点赞等操作记录。</p>
<p>1.4 <strong>社交数据：</strong>好友关系、关注关系、聊天记录、群聊记录。</p>
<p>1.5 <strong>设备与日志：</strong>IP地址、设备型号、操作系统、浏览器类型、访问时间和页面、登录记录。</p>
<p>1.6 <strong>位置信息：</strong>通过IP地址获取的大致地理位置。</p>

<h4>二、我们如何使用信息</h4>
<p>2.1 提供、维护和改进我们的服务。</p>
<p>2.2 进行用户身份验证和安全防范。</p>
<p>2.3 向您发送服务相关通知（系统消息、好友申请等）。</p>
<p>2.4 分析用户行为以优化产品体验和内容推荐。</p>
<p>2.5 检测和防止欺诈、滥用和非法活动。</p>

<h4>三、信息存储</h4>
<p>3.1 您的个人信息存储于中华人民共和国境内。</p>
<p>3.2 我们仅在实现本政策所述目的所必需的时间内保留您的个人信息，法律法规另有规定的除外。</p>
<p>3.3 当您注销账号后，我们将在合理期限内删除或匿名化处理您的个人信息。</p>

<h4>四、信息共享</h4>
<p>4.1 我们不会将您的个人信息出售给任何第三方。</p>
<p>4.2 在以下情况下，我们可能共享您的信息：</p>
<ul>
  <li>获得您的明确同意；</li>
  <li>根据法律法规要求或行政、司法机关的强制性要求；</li>
  <li>与我们的关联公司共享（仅限为提供服务所必需）；</li>
  <li>为保护我们、您或其他用户的合法权益。</li>
</ul>

<h4>五、您的权利</h4>
<p>5.1 您可以在设置中查看、修改您的个人资料。</p>
<p>5.2 您可以随时删除您上传的内容（作品、评论等）。</p>
<p>5.3 您可以随时申请注销账号，我们将依法删除您的个人信息。</p>
<p>5.4 您可以撤回已同意的授权或关闭特定功能权限。</p>

<h4>六、信息安全</h4>
<p>6.1 我们采取加密传输（HTTPS）、密码哈希加盐存储（BCrypt）、访问控制等安全措施保护您的信息。</p>
<p>6.2 我们定期进行安全评估和漏洞扫描，及时修复安全漏洞。</p>
<p>6.3 如发生个人信息安全事件，我们将按照法律法规要求及时告知您。</p>

<h4>七、未成年人保护</h4>
<p>7.1 我们特别重视对未成年人个人信息的保护。如果您是未满18周岁的未成年人，请在监护人指导下使用本平台。</p>
<p>7.2 我们不会主动收集未成年人的个人信息。如发现误收集，我们将立即删除。</p>

<h4>八、政策更新</h4>
<p>8.1 我们可能会不时更新本隐私政策。更新后的政策将在本平台发布，并在重大变更时通过站内通知或邮件方式告知您。</p>
<p>8.2 如您继续使用本平台服务，即表示您同意更新后的隐私政策。</p>

<h4>九、联系我们</h4>
<p>如您对本隐私政策有任何疑问、意见或投诉，请联系：</p>
<p>客服邮箱：2977025339@qq.com</p>
<p>我们将在15个工作日内回复您的请求。</p>
</div>`
</script>

<style>
.agreement { margin: 10px 0; font-size: 0.9rem; color: #666; }
.agreement a { color: #1890ff; text-decoration: none; }
.agreement a:hover { color: #40a9ff; text-decoration: underline; }

/* overlay — 不透明遮罩，遮盖所有下层内容 */
.agreement-overlay {
  position: fixed; inset: 0;
  background: rgba(0, 0, 0, 0.85);
  display: flex; align-items: center; justify-content: center;
  z-index: 99999;
}

/* 面板 — 纯白不透明 */
.agreement-panel {
  background: #fff;
  border-radius: 12px;
  width: 660px;
  max-width: 92vw;
  max-height: 88vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.35);
}

.agreement-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid #eee;
  flex-shrink: 0;
}
.agreement-header h3 { margin: 0; font-size: 17px; color: #f1177d; }

.close-btn {
  width: 32px; height: 32px;
  border: none; background: #f5f5f5;
  font-size: 20px; line-height: 1;
  border-radius: 50%; cursor: pointer;
  color: #999;
  display: flex; align-items: center; justify-content: center;
}
.close-btn:hover { background: #ffebee; color: #ff4757; }

/* body — 可滚动区域 */
.agreement-body {
  flex: 1; overflow-y: auto;
  padding: 20px 24px;
  line-height: 1.8; font-size: 14px; color: #333;
}

.agreement-footer {
  padding: 14px 24px;
  border-top: 1px solid #eee;
  text-align: right;
  flex-shrink: 0;
}
.confirm-btn {
  padding: 10px 32px;
  background: linear-gradient(135deg, #fa94c3, #50b9b4);
  color: #fff; border: none; border-radius: 20px;
  font-size: 14px; cursor: pointer;
  transition: opacity 0.2s;
}
.confirm-btn:hover { opacity: 0.85; }
</style>
