
//导入request.js文件
import request from '@/utils/requests'

//提供调用注册接口的方法
export const userRegisterService = (registerData) => {
  //借助于urlsearchparams对象，将params对象中的参数转换为url编码的字符串
  const params = new URLSearchParams()
  for (let key in registerData) {
    params.append(key, registerData[key]);
  }
  return request.post('/user/register', params);
}

//提供调用根据用户名和密码登录接口的方法
export const userLoginService = (loginData) => {
  //借助于urlsearchparams对象，将params对象中的参数转换为url编码的字符串
  const params = new URLSearchParams()
  for (let key in loginData) {
    params.append(key, loginData[key]);
  }
  return request.post('/user/login', params);
}

//提供调用根据用户名和验证码登录接口的方法
export const userLoginByCodeService = (loginData) => {
   const params = new URLSearchParams()
  for (let key in loginData) {
    params.append(key, loginData[key]);
  }
  return request.post('/user/loginByCode', params)
}

//提供调用发送短信验证码接口的方法
export const sendSMSCodeService = (phone, type = 'login') => {
  return request.post('/user/sendSMSCode',
    new URLSearchParams({ phone, type }),
    {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    });
}

//根据id获取用户信息
export const getUserInfoByIdService = (id) => {
  return request.get(`/user/${id}`);
}

//获取用户详细信息的方法
export const userInfoService = () => {
  return request.get('/user/userInfo'
  )
}

//获取更新修改用户详细信息的方法
export const updateUserInfoService = (userInfoData) => {
  return request.put('/user/update', userInfoData);
}

//获取更新修改用户名的方法
export const updateUserNameService = (username) => {
  const params = new URLSearchParams();
  params.append('username', username);
  return request.patch('/user/updateUsername', params);
}

//提供用户更新头像的方法
export const useUpdateAvatarService = (avatarUrl) => {
  const formData = new FormData();
  formData.append('avatarUrl', avatarUrl);
  return request.patch('/user/updateAvatar', formData);
}

//提供用户更新密码的方法，
export const useUpdatePasswordService = (params) => {
  return request.patch('/user/updatePwd', 
    { new_pwd: params.newPassword, re_pwd: params.confirmNewPassword, code: params.code },
     {
    headers: {
      'Content-Type': 'application/json'  // 明确指定 JSON 格式  
    }
  });
}

//发送异步请求获取文章列表
export function getArticleList() {
  //同步等待服务器响应的结果，并返回。async函数会等待await后面的Promise对象执行完，并返回结果
  return request.get('/article/getAll');
}

//定义一个方法，用来发送异步请求，获取条件搜索的文章列表
export function searchArticleList(params) {
  return request.get('/article/search', {
    params: {
      categoryId: (Number)(params.categoryId),
      state: params.state
    }
  });
}
