import service from '@/utils/request'
/**
 * 登录接口封装
 * @param {Object} data {username,password}
 */
export function loginApi(data){
  return service.post('/api/user/login',data)
}
/**
 * 注册接口封装
 * @param {Object} data {username,password}
 */
export function registerApi(data){
  return service.post('/api/user/register',data)
}
// 获取当前登录人信息
export function getUserInfo() {
  return service.get('/api/user/info')
}
// 用户列表
export function getUserList() {
  return service.get('/api/user/list')
}