import dayjs from 'dayjs'

export const ACTIVITY_STATUS = {
  0: { text: '草稿', type: 'info' },
  1: { text: '待审核', type: 'warning' },
  2: { text: '报名中', type: 'success' },
  3: { text: '已驳回', type: 'danger' },
  4: { text: '已取消', type: 'info' },
  5: { text: '已结束', type: '' }
}

export const REG_STATUS = {
  1: { text: '已报名', type: 'success' },
  2: { text: '已签到', type: 'primary' },
  3: { text: '已取消', type: 'info' },
  4: { text: '爽约', type: 'danger' },
  5: { text: '待审核', type: 'warning' }
}

export const ROLE_TEXT = {
  STUDENT: '学生',
  ORGANIZER: '组织者',
  ADMIN: '管理员'
}

export const NOTICE_TYPE_TEXT = {
  AUDIT: '审核结果',
  ACTIVITY: '活动通知',
  SYSTEM: '系统消息'
}

export function activityStatusText (status) {
  return (ACTIVITY_STATUS[status] || {}).text || '未知'
}

export function activityStatusType (status) {
  return (ACTIVITY_STATUS[status] || {}).type || ''
}

export function regStatusText (status) {
  return (REG_STATUS[status] || {}).text || '未知'
}

export function regStatusType (status) {
  return (REG_STATUS[status] || {}).type || ''
}

export function fmtDateTime (value) {
  if (!value) return '-'
  return dayjs(value).format('YYYY-MM-DD HH:mm')
}

export function fmtDate (value) {
  if (!value) return '-'
  return dayjs(value).format('YYYY-MM-DD')
}

/** 距离某时间还剩多久，返回"2 天 3 小时""35 分钟""已结束" */
export function remainText (target) {
  if (!target) return ''
  const diff = dayjs(target).diff(dayjs(), 'minute')
  if (diff <= 0) return '已结束'
  const days = Math.floor(diff / 1440)
  const hours = Math.floor((diff % 1440) / 60)
  const minutes = diff % 60
  if (days > 0) return `${days} 天 ${hours} 小时`
  if (hours > 0) return `${hours} 小时 ${minutes} 分钟`
  return `${minutes} 分钟`
}

/** 报名截止时间是否已过 */
export function signupExpired (deadline) {
  if (!deadline) return false
  return dayjs().isAfter(dayjs(deadline))
}
