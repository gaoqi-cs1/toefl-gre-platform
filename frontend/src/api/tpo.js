import service from '@/utils/request'

/**
 * 获取全部TPO列表
 */
export function getTpoListApi(){
  return service.get('/api/tpo/list')
}

/**
 * 根据tpoId获取section列表
 * @param {Number} tpoId
 */
export function getSectionByTpoIdApi(tpoId){
  return service.get(`/api/tpo/section/${tpoId}`)
}

/**
 * 根据sectionId获取题目列表
 * @param {Number} sectionId
 */
export function getQuestionListApi(sectionId){
  return service.get(`/api/question/list/${sectionId}`)
}

/**
 * 提交答案
 * @param {Object} data {questionId,userAnswer,costTime}
 */
export function submitAnswerApi(data){
  return service.post('/api/record/submit', data)
}

/**
 * 添加错题
 * @param {Number} qId
 */
export function addWrongApi(qId){
  return service.post(`/api/wrong/add/${qId}`)
}

/**
 * 获取错题列表
 */
export function getWrongListApi(){
  return service.get('/api/wrong/list')
}

/**
 * 删除错题
 * @param {Number} qId
 */
export function removeWrongApi(qId){
  return service.delete(`/api/wrong/remove/${qId}`)
}
