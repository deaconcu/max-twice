/**
 * 所有 QueryKey 集中定义
 *
 * 规则：
 * - all: 该模块所有缓存的根 key，invalidate 时可以一次性让整个模块失效
 * - 具体 key: 包含参数，精确匹配某一条缓存
 */

export const commentKeys = {
  all: ['comments'] as const,
  basic: (id: number) => ['comments', 'basic', id] as const,
  context: (id: number) => ['comments', 'context', id] as const,
  list: (objectId: number, objectType: number) =>
    ['comments', 'list', objectId, objectType] as const,
  replies: (id: number) => ['comments', 'replies', id] as const,
}

export const homeKeys = {
  all: ['home'] as const,
  page: () => ['home', 'page'] as const,
}

export const statsKeys = {
  all: ['stats'] as const,
  heatmap: (userId: number, months: number) => ['stats', 'heatmap', userId, months] as const,
  allTime: (userId: number) => ['stats', 'allTime', userId] as const,
  history: (userId: number, days: number) => ['stats', 'history', userId, days] as const,
  today: (userId: number) => ['stats', 'today', userId] as const,
  platform: () => ['stats', 'platform'] as const,
}

export const userKeys = {
  all: ['users'] as const,
  current: () => ['users', 'current'] as const,
  detail: (username: string) => ['users', 'detail', username] as const,
  posts: (userId: number, cursor?: string) => ['users', 'posts', userId, cursor] as const,
  myPosts: (cursor?: string, type?: number, state?: number) =>
    ['users', 'me', 'posts', cursor, type, state] as const,
  roadmaps: (userId: number, cursor?: string) => ['users', 'roadmaps', userId, cursor] as const,
  myRoadmaps: (cursor?: string, state?: number) =>
    ['users', 'me', 'roadmaps', cursor, state] as const,
  followees: (userId: number) => ['users', 'followees', userId] as const,
  subscriptions: (userId: number) => ['users', 'subscriptions', userId] as const,
}

export const courseKeys = {
  all: ['courses'] as const,
  detail: (id: number) => ['courses', 'detail', id] as const,
  list: (mainCategory?: number, subCategory?: number, cursor?: string) =>
    ['courses', 'list', mainCategory, subCategory, cursor] as const,
  hot: () => ['courses', 'hot'] as const,
  subcourses: (parentId: number) => ['courses', 'subcourses', parentId] as const,
  search: (name: string) => ['courses', 'search', name] as const,
}

export const postKeys = {
  all: ['posts'] as const,
  detail: (id: number) => ['posts', 'detail', id] as const,
  nodeList: (nodeId: number, cursor?: string) => ['posts', 'node', nodeId, cursor] as const,
  byIds: (ids: number[]) => ['posts', 'byIds', ids] as const,
}

export const roleKeys = {
  all: ['roles'] as const,
  detail: (id: number) => ['roles', 'detail', id] as const,
  list: (cursor?: string, mainCategory?: number, subCategory?: number) =>
    ['roles', 'list', cursor, mainCategory, subCategory] as const,
  hot: () => ['roles', 'hot'] as const,
  search: (keyword: string) => ['roles', 'search', keyword] as const,
  roadmaps: (roleId: number) => ['roles', 'roadmaps', roleId] as const,
}

export const roadmapKeys = {
  all: ['roadmaps'] as const,
  detail: (id: number) => ['roadmaps', 'detail', id] as const,
  roleRoadmaps: (roleId: number) => ['roadmaps', 'role', roleId] as const,
}

export const progressKeys = {
  all: ['progress'] as const,
  courseProgress: (courseId: number) => ['progress', 'course', courseId] as const,
  allCourses: (state?: string, cursor?: string) => ['progress', 'courses', state, cursor] as const,
  roadmapProgress: (roadmapId: number) => ['progress', 'roadmap', roadmapId] as const,
  allRoadmaps: (state?: string, cursor?: string) =>
    ['progress', 'roadmaps', state, cursor] as const,
  nodeStatus: (nodeId: number) => ['progress', 'node', nodeId] as const,
  learningRoadmapsByRole: (roleId: number) => ['progress', 'role', roleId, 'roadmaps'] as const,
}

export const memoryKeys = {
  all: ['memory'] as const,
  reviewSummary: (state?: number) => ['memory', 'summary', state] as const,
  reviewQueue: (params: object) => ['memory', 'review', 'queue', params] as const,
  cardList: (courseId?: number) => ['memory', 'review', 'cards', courseId] as const,
  nextCard: (courseId?: number) => ['memory', 'review', 'next', courseId] as const,
  decksByNode: (nodeId: number) => ['memory', 'decks', 'node', nodeId] as const,
  deckDetail: (deckId: number) => ['memory', 'decks', 'detail', deckId] as const,
  deckDiff: (deckId: number) => ['memory', 'decks', 'diff', deckId] as const,
  cardDiff: (cardId: number) => ['memory', 'cards', 'diff', cardId] as const,
  postDecks: (postId: number, sortBy?: string) => ['memory', 'posts', postId, 'decks', sortBy] as const,
  postCreatorDeck: (postId: number, sortBy?: string) => ['memory', 'posts', postId, 'creator-deck', sortBy] as const,
  myPostDeck: (postId: number, sortBy?: string) => ['memory', 'posts', postId, 'my-deck', sortBy] as const,
  userDecks: (userId: number) => ['memory', 'users', userId, 'decks'] as const,
  myDecks: (state?: number) => ['memory', 'users', 'me', 'decks', state] as const,
  cardsByNode: (nodeId: number) => ['memory', 'cards', 'node', nodeId] as const,
}

export const messageKeys = {
  all: ['messages'] as const,
  byCategory: (category: number, cursor?: string) =>
    ['messages', 'category', category, cursor] as const,
  unreadCount: () => ['messages', 'unread-count'] as const,
}

export const searchKeys = {
  all: ['search'] as const,
  courses: (q: string) => ['search', 'courses', q] as const,
  nodes: (q: string) => ['search', 'nodes', q] as const,
  users: (q: string) => ['search', 'users', q] as const,
  roles: (q: string) => ['search', 'roles', q] as const,
  all_: (q: string) => ['search', 'all', q] as const,
}

export const systemKeys = {
  all: ['system'] as const,
  courseCategories: () => ['system', 'courseCategories'] as const,
  roleCategories: () => ['system', 'roleCategories'] as const,
  readonlyMode: () => ['system', 'readonlyMode'] as const,
}

export const bookmarkKeys = {
  all: ['bookmarks'] as const,
  list: (contentType: string, cursor?: string) =>
    ['bookmarks', contentType, cursor] as const,
}

export const upvoteKeys = {
  all: ['upvotes'] as const,
  status: (objectId: number, objectType: number) =>
    ['upvotes', 'status', objectId, objectType] as const,
}

export const imageKeys = {
  all: ['images'] as const,
  quota: () => ['images', 'quota'] as const,
  history: (limit: number) => ['images', 'history', limit] as const,
}

export const pageKeys = {
  all: ['pages'] as const,
  read: (nodeId?: number, courseId?: number, path?: string) =>
    ['pages', 'read', nodeId ?? null, courseId ?? null, path ?? null] as const,
  node: (nodeId: number) => ['pages', 'node', nodeId] as const,
  post: (params: object) => ['pages', 'post', params] as const,
}

export const adminKeys = {
  all: ['admin'] as const,
  contents: (contentType: string, state?: number) =>
    ['admin', 'contents', contentType, state] as const,
  users: (offsetId?: number) => ['admin', 'users', offsetId] as const,
  operationLogs: (query: object) => ['admin', 'operation-logs', query] as const,
  systemConfig: (part?: string) => ['admin', 'system', part] as const,
}
