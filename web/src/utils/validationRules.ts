import {
  USER_VALIDATION,
  COMMENT_VALIDATION,
  COURSE_VALIDATION,
  POST_VALIDATION,
  PROFESSION_VALIDATION,
  CARD_VALIDATION,
  DECK_VALIDATION,
  MESSAGE_VALIDATION,
  ROADMAP_VALIDATION,
} from '@/types/validation'

// ========== 用户相关验证规则 ==========

export const emailRules = [
  (v: string) => !!v || '邮箱不能为空',
  (v: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v) || '请输入有效的邮箱地址',
  (v: string) =>
    v.length <= USER_VALIDATION.EMAIL_MAX_LENGTH ||
    `邮箱长度不能超过 ${USER_VALIDATION.EMAIL_MAX_LENGTH} 个字符`,
]

export const passwordRules = [
  (v: string) => !!v || '密码不能为空',
  (v: string) =>
    v.length >= USER_VALIDATION.PASSWORD_MIN_LENGTH ||
    `密码至少需要 ${USER_VALIDATION.PASSWORD_MIN_LENGTH} 个字符`,
  (v: string) =>
    v.length <= USER_VALIDATION.PASSWORD_MAX_LENGTH ||
    `密码不能超过 ${USER_VALIDATION.PASSWORD_MAX_LENGTH} 个字符`,
]

export const usernameRules = [
  (v: string) => !!v || '用户名不能为空',
  (v: string) =>
    v.length >= USER_VALIDATION.USERNAME_MIN_LENGTH ||
    `用户名至少需要 ${USER_VALIDATION.USERNAME_MIN_LENGTH} 个字符`,
  (v: string) =>
    v.length <= USER_VALIDATION.USERNAME_MAX_LENGTH ||
    `用户名不能超过 ${USER_VALIDATION.USERNAME_MAX_LENGTH} 个字符`,
]

export const biographyRules = [
  (v: string) =>
    !v ||
    v.length <= USER_VALIDATION.BIOGRAPHY_MAX_LENGTH ||
    `个人简介不能超过 ${USER_VALIDATION.BIOGRAPHY_MAX_LENGTH} 个字符`,
]

export const phoneRules = [
  (v: string) =>
    !v ||
    v.length <= USER_VALIDATION.PHONE_MAX_LENGTH ||
    `手机号长度不能超过 ${USER_VALIDATION.PHONE_MAX_LENGTH} 个字符`,
]

// ========== 评论相关验证规则 ==========

export const commentRules = [
  (v: string) => {
    if (!v?.trim()) return true
    if (v.trim().length < COMMENT_VALIDATION.CONTENT_MIN_LENGTH) {
      return `评论内容至少需要 ${COMMENT_VALIDATION.CONTENT_MIN_LENGTH} 个字符`
    }
    if (v.length > COMMENT_VALIDATION.CONTENT_MAX_LENGTH) {
      return `评论内容不能超过 ${COMMENT_VALIDATION.CONTENT_MAX_LENGTH} 个字符`
    }
    return true
  },
]

// ========== 课程相关验证规则 ==========

export const courseNameRules = [
  (v: string) => !!v || '课程名称不能为空',
  (v: string) =>
    v.length >= COURSE_VALIDATION.NAME_MIN_LENGTH ||
    `课程名称至少需要 ${COURSE_VALIDATION.NAME_MIN_LENGTH} 个字符`,
  (v: string) =>
    v.length <= COURSE_VALIDATION.NAME_MAX_LENGTH ||
    `课程名称不能超过 ${COURSE_VALIDATION.NAME_MAX_LENGTH} 个字符`,
]

export const courseDescriptionRules = [
  (v: string) => !!v || '课程描述不能为空',
  (v: string) =>
    v.length >= COURSE_VALIDATION.DESCRIPTION_MIN_LENGTH ||
    `课程描述至少需要 ${COURSE_VALIDATION.DESCRIPTION_MIN_LENGTH} 个字符`,
  (v: string) =>
    v.length <= COURSE_VALIDATION.DESCRIPTION_MAX_LENGTH ||
    `课程描述不能超过 ${COURSE_VALIDATION.DESCRIPTION_MAX_LENGTH} 个字符`,
]

export const categoryRules = [(v: any) => !!v || '请选择分类']

// ========== 帖子相关验证规则 ==========

export const postContentRules = [
  (v: string) => !!v || '帖子内容不能为空',
  (v: string) =>
    v.length >= POST_VALIDATION.CONTENT_MIN_LENGTH ||
    `帖子内容至少需要 ${POST_VALIDATION.CONTENT_MIN_LENGTH} 个字符`,
  (v: string) =>
    v.length <= POST_VALIDATION.CONTENT_MAX_LENGTH ||
    `帖子内容不能超过 ${POST_VALIDATION.CONTENT_MAX_LENGTH} 个字符`,
]

// ========== 专业相关验证规则 ==========

export const professionNameRules = [
  (v: string) => !!v || '专业名称不能为空',
  (v: string) =>
    v.length >= PROFESSION_VALIDATION.NAME_MIN_LENGTH ||
    `专业名称至少需要 ${PROFESSION_VALIDATION.NAME_MIN_LENGTH} 个字符`,
  (v: string) =>
    v.length <= PROFESSION_VALIDATION.NAME_MAX_LENGTH ||
    `专业名称不能超过 ${PROFESSION_VALIDATION.NAME_MAX_LENGTH} 个字符`,
]

export const professionDescriptionRules = [
  (v: string) => !!v || '专业描述不能为空',
  (v: string) =>
    v.length >= PROFESSION_VALIDATION.DESCRIPTION_MIN_LENGTH ||
    `专业描述至少需要 ${PROFESSION_VALIDATION.DESCRIPTION_MIN_LENGTH} 个字符`,
  (v: string) =>
    v.length <= PROFESSION_VALIDATION.DESCRIPTION_MAX_LENGTH ||
    `专业描述不能超过 ${PROFESSION_VALIDATION.DESCRIPTION_MAX_LENGTH} 个字符`,
]

// ========== 记忆卡片相关验证规则 ==========

export const cardFrontRules = [
  (v: string) => !!v || '卡片正面不能为空',
  (v: string) =>
    v.length >= CARD_VALIDATION.FRONT_MIN_LENGTH ||
    `卡片正面至少需要 ${CARD_VALIDATION.FRONT_MIN_LENGTH} 个字符`,
  (v: string) =>
    v.length <= CARD_VALIDATION.FRONT_MAX_LENGTH ||
    `卡片正面不能超过 ${CARD_VALIDATION.FRONT_MAX_LENGTH} 个字符`,
]

export const cardBackRules = [
  (v: string) => !!v || '卡片背面不能为空',
  (v: string) =>
    v.length >= CARD_VALIDATION.BACK_MIN_LENGTH ||
    `卡片背面至少需要 ${CARD_VALIDATION.BACK_MIN_LENGTH} 个字符`,
  (v: string) =>
    v.length <= CARD_VALIDATION.BACK_MAX_LENGTH ||
    `卡片背面不能超过 ${CARD_VALIDATION.BACK_MAX_LENGTH} 个字符`,
]

export const deckTitleRules = [
  (v: string) => !!v || '卡片组标题不能为空',
  (v: string) =>
    v.length >= DECK_VALIDATION.TITLE_MIN_LENGTH ||
    `卡片组标题至少需要 ${DECK_VALIDATION.TITLE_MIN_LENGTH} 个字符`,
  (v: string) =>
    v.length <= DECK_VALIDATION.TITLE_MAX_LENGTH ||
    `卡片组标题不能超过 ${DECK_VALIDATION.TITLE_MAX_LENGTH} 个字符`,
]

export const deckDescriptionRules = [
  (v: string) =>
    !v ||
    v.length <= DECK_VALIDATION.DESCRIPTION_MAX_LENGTH ||
    `卡片组描述不能超过 ${DECK_VALIDATION.DESCRIPTION_MAX_LENGTH} 个字符`,
]

// ========== 消息相关验证规则 ==========

export const messageContentRules = [
  (v: string) => !!v || '消息内容不能为空',
  (v: string) =>
    v.length >= MESSAGE_VALIDATION.CONTENT_MIN_LENGTH ||
    `消息内容至少需要 ${MESSAGE_VALIDATION.CONTENT_MIN_LENGTH} 个字符`,
  (v: string) =>
    v.length <= MESSAGE_VALIDATION.CONTENT_MAX_LENGTH ||
    `消息内容不能超过 ${MESSAGE_VALIDATION.CONTENT_MAX_LENGTH} 个字符`,
]

// ========== 路线图相关验证规则 ==========

export const roadmapContentRules = [
  (v: string) => !!v || '路线图内容不能为空',
  (v: string) =>
    v.length >= ROADMAP_VALIDATION.CONTENT_MIN_LENGTH ||
    `路线图内容至少需要 ${ROADMAP_VALIDATION.CONTENT_MIN_LENGTH} 个字符`,
  (v: string) =>
    v.length <= ROADMAP_VALIDATION.CONTENT_MAX_LENGTH ||
    `路线图内容不能超过 ${ROADMAP_VALIDATION.CONTENT_MAX_LENGTH} 个字符`,
]

export const roadmapDescriptionRules = [
  (v: string) => !!v || '路线图描述不能为空',
  (v: string) =>
    v.length >= ROADMAP_VALIDATION.DESCRIPTION_MIN_LENGTH ||
    `路线图描述至少需要 ${ROADMAP_VALIDATION.DESCRIPTION_MIN_LENGTH} 个字符`,
  (v: string) =>
    v.length <= ROADMAP_VALIDATION.DESCRIPTION_MAX_LENGTH ||
    `路线图描述不能超过 ${ROADMAP_VALIDATION.DESCRIPTION_MAX_LENGTH} 个字符`,
]
