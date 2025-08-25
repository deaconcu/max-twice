import apiClient from './apiClient';

async function chatWithGPT(message) {
}

export const userService = {
  login(formData) {
    return apiClient.post(`/login`, formData);
  },

  register(formData) {
    return apiClient.post('/user', formData);
  },

  validateMail(formData) {
    return apiClient.post('/user/validate', formData);
  },

  getSelf() {
    console.log("get /self");
    return apiClient.get('/self');
  },

  postSelf(name, biography) {
    const formData = new FormData();
    formData.append('name', name);
    formData.append('biography', biography);
    console.log("post /self, formData: " + new URLSearchParams(formData));
    return apiClient.post('/self', formData);
  },

  getSubscription(id) {
    console.log("get /user/subscription");
    return apiClient.get('/user/subscription?userId=' + id);
  },

  putSubscription(subscription) {
    const formData = new FormData();
    formData.append('subscription', subscription);
    console.log("post /user/subscription, formData: " + new URLSearchParams(formData));
    return apiClient.put('/user/subscription', formData);
  },

  getUser(id) {
    console.log("get /user/" + id);
    return apiClient.get('/user/' + id);
  },

  searchUser(name) {
    console.log("search /user/" + name);
    return apiClient.get('/user?name=' + name);
  },

  follow(id) {
    const formData = new FormData();
    formData.append('followeeId', id);
    console.log("post /user/follow: " + + new URLSearchParams(formData));
    return apiClient.post('/user/follow', formData);
  },

  unfollow(id) {
    return apiClient.delete('/user/follow', {
      params: { followeeId: id },
    })
  },

  getFolloweeList(followerId, lastCreateTime) {
    return apiClient.get('/user/followee', {
      params: { followerId: followerId, lastCreateTime: lastCreateTime },
    })
  },

};

export const learnService = {

  getSystem() {
    console.log("get system");
    return apiClient.get('/system')
  },

  // 获取课程分类数据
  getCourseCategories() {
    console.log("get course categories");
    return apiClient.get('/system', { 
      params: { part: 'courseCategories' } 
    });
  },

  // 获取职业类别数据
  getProfessionCategories() {
    console.log("get profession categories");
    // 暂时使用本地示例数据进行测试，等后端API准备就绪后替换为：
    // return apiClient.get('/system', { params: { part: 'professionCategories' } })
    return fetch('/sample-profession-categories.json').then(response => {
      if (!response.ok) {
        throw new Error('Failed to fetch profession categories');
      }
      return response.json().then(data => ({ data }));
    });
  },

  postSystem(config) {
    const formData = new FormData();
    formData.append('config', config);
    console.log("post /system, formData: " + new URLSearchParams(formData));
    return apiClient.post('/system', formData)
  },

  openAI(prompt, model) {
    let formData = new FormData();
    console.log("prompt: " + prompt);
    formData.append('prompt', prompt);
    formData.append('model', model);
    return apiClient.post('/openai', formData);
  },

  courseList(page) {
    return apiClient.get('/course/list', {
      params: { page: page },
    })
  },

  // 根据主分类和子分类获取课程列表
  getCoursesByCategory(mainCategory, subCategory) {
    return apiClient.get('/course/list', {
      params: { 
        mainCategory: mainCategory,
        subCategory: subCategory
      }
    })
  },

  // 根据状态获取课程列表（用于管理员审核）
  getCoursesByState(state, lastId = 0) {
    return apiClient.get('/course/list', {
      params: { 
        state: state,
        lastId: lastId
      }
    })
  },

  // 课程操作（通过、拒绝等）
  operateCourse(id, action, rejectedReason = null) {
    const formData = new FormData();
    formData.append('id', id);
    formData.append('action', action);
    if (rejectedReason) {
      formData.append('rejectedReason', rejectedReason);
    }
    return apiClient.post('/course/operate', formData);
  },

  // 更新课程信息
  updateCourse(id, courseData) {
    return apiClient.put(`/course/${id}`, courseData, {
      headers: {
        'Content-Type': 'application/json'
      }
    });
  },

  course(id) {
    return apiClient.get(`/course/${id}`);
  },

  postCourse(name, description, parent, messageId) {
    let formData = new FormData();
    formData.append('name', name);
    formData.append('description', description);
    formData.append('parentId', parent);
    formData.append('messageId', messageId);

    return apiClient.post(`/course`, formData);
  },

  // 创建新课程（直接创建，不是申请）
  createCourse(courseData) {
    return apiClient.post('/course', courseData, {
      headers: {
        'Content-Type': 'application/json'
      }
    });
  },

  readByPath(courseId, path) {
    console.log("read by path");
    return apiClient.get('/read', {
      params: { courseId: courseId, path: path },
    })
  },

  readByNode(nodeId) {
    console.log("read by node");
    return apiClient.get('/read', {
      params: { nodeId: nodeId },
    })
  },

  readByPost(postId) {
    console.log("read by post");
    return apiClient.get('/read', {
      params: { postId: postId },
    })
  },

  readByComment(commentId) {
    console.log("read by comment");
    return apiClient.get('/read', {
      params: { commentId: commentId },
    })
  },

  getUserContents(userId, lastId) {
    console.log("lastId: " + lastId);
    return apiClient.get('/user/contents', {
      params: { userId: userId, lastId: lastId },
    })
  },

  getUserArticle(userId, lastId) {
    return apiClient.get('/user/article', {
      params: { userId, userId, lastId: lastId },
    })
  },

  getPosting(params) {
    return apiClient.get(`/postings`, {
      params: params,
    });
  },

  addPosting(data) {
    return apiClient.post('/posting', data, {
      headers: {
        "Content-Type": "application/json",
      },
    })
  },

  putPosting(postingId, content) {
    const data = {
      "id": postingId,
      "content": content
    }
    console.log("params: " + JSON.stringify(data));
    return apiClient.put('/posting', data, {
      headers: {
        "Content-Type": "application/json",
      },
    })
  },

  deletePosting(postingId) {
    return apiClient.delete('/posting', {
      params: { id: postingId },
    })
  },

  postCensorList() {
    return apiClient.get('/post/censor')
  }, 

  approvePost(id, action) {
    console.log("id: " + id + ", action: " + action);
    const params = {
      id: id,
      action: action
    };
    console.log("params" + JSON.stringify(params));
    return apiClient.put('/post', null, {
      params: params
    });
  },

  postContents(data) {
    return apiClient.post('/contents', data);
  },

  postToc(courseId, indexArray) {
    let formData = new FormData();
    formData.append('courseId', courseId);
    formData.append('indexArray', indexArray);
    console.log("post /toc: " + new URLSearchParams(formData).toString());
    return apiClient.post('/toc', formData);
  },


  postContentsList(courseId, list) {
    let formData = new FormData();
    formData.append('courseId', courseId);
    formData.append('list', list);
    console.log("post /contents-list: " + new URLSearchParams(formData).toString());
    return apiClient.post('/contents-list', formData);
  },

  getComments(objectId, type, offsetId) {
    const params = {
      objectId: objectId,
      type: type,
      offsetId: offsetId,
    };
    console.log("params" + JSON.stringify(params));
    return apiClient.get('/comment', {
      params: params
    });
  },

  getCommentsByTopic(commentId, offsetId) {
    const params = {
      offsetId: offsetId,
    };
    console.log("params" + JSON.stringify(params));
    return apiClient.get('/comment/' + commentId + '/reply', {
      params: params
    });
  },

  postComment(content, objectId, type, replyTo, toUser) {
    const comment = {
      objectId: objectId,
      type: type,
      replyTo: replyTo,
      toUser: toUser,
      content: content
    };
    console.log("comment: " + JSON.stringify(comment));
    return apiClient.post('/comment', comment);
  },

  approveComment(id, action) {
    console.log("id: " + id + ", action: " + action);
    const params = {
      id: id,
      action: action
    };
    console.log("params" + JSON.stringify(params));
    return apiClient.put('/comment', null, {
      params: params
    });
  },


  upvote(objectId, objetcType, type) {
    let formData = new FormData();
    formData.append('objectId', objectId);
    formData.append('objectType', objetcType);
    formData.append('type', type);
    console.log("upvote: " + JSON.stringify(Object.fromEntries(formData.entries())));
    return apiClient.post('/upvote', formData);
  },

  postApplyCourse(title, summary, explanation, parentId) {
    let formData = new FormData();
    formData.append('title', title);
    formData.append('summary', summary);
    formData.append('explanation', explanation);
    formData.append('parentId', parentId);
    console.log("formData: " + JSON.stringify(Object.fromEntries(formData.entries())));
    return apiClient.post('/message/new-course', formData);
  },

  // 创建子课程
  createSubcourse(name, description, parentId) {
    let formData = new FormData();
    formData.append('name', name);
    formData.append('description', description);
    formData.append('parentId', parentId);
    console.log("creating subcourse: " + JSON.stringify(Object.fromEntries(formData.entries())));
    return apiClient.post('/subcourse', formData);
  },

  // 根据父课程ID获取子课程列表
  getApprovedSubcoursesByParent(parentId) {
    console.log("get subcourses by parent: " + parentId);
    return apiClient.get('/course/list/approved', {
      params: { parentId: parentId }
    });
  },

    // 根据父课程ID获取子课程列表
  getSubcoursesByParent(parentId) {
    console.log("get subcourses by parent: " + parentId);
    return apiClient.get('/course/list', {
      params: { parentId: parentId }
    });
  },

  getApplyCourseMessageByUser(userId, lastId) {
    console.log("begin get message");
    const params = {
      userId: userId,
      lastId: lastId
    };
    console.log("params: " + JSON.stringify(params));
    return apiClient.get('/message/course-apply', {
      params: params
    });
  },

  getSystemMessageByUser(type, userId, lastId) {
    console.log("begin get message");
    const params = {
      type: type,
      userId: userId,
      lastId: lastId
    };
    console.log("params: " + JSON.stringify(params));
    return apiClient.get('/message/system', {
      params: params
    });
  },


  getApplyCourseMessage(page, length) {
    console.log("begin get message");
    const params = {
      page: page,
      length: length,
    };
    console.log("params: " + JSON.stringify(params));
    return apiClient.get('/message/new-course', {
      params: params
    });
  },

  getMessage(userId, type, lastId, conversation) {
    console.log("begin get message");
    const params = {
      userId: userId,
      type: type,
      lastId: lastId,
      conversation: conversation
    };
    console.log("params: " + JSON.stringify(params));
    return apiClient.get('/message', {
      params: params
    });
  },

  postSystemMessage(type, toUserId, content) {
    let formData = new FormData();
    formData.append('type', type);
    formData.append('to', toUserId);
    formData.append('content', content);
    console.log("formData: " + JSON.stringify(Object.fromEntries(formData.entries())));
    return apiClient.post('/message/system', formData);
  },

  replyCourseApply(messageId, reply) {
    let formData = new FormData();
    formData.append('id', messageId);
    formData.append('reply', reply);
    console.log("formData: " + formData);
    return apiClient.put('/message/system', formData);
  },

  subscript(courseId) {
    let formData = new FormData();
    formData.append('courseId', courseId);
    console.log("formData: " + JSON.stringify(Object.fromEntries(formData.entries())));
    return apiClient.post('/user/subscription', formData);
  },

  unsubscript(courseId) {
    console.log("unsubscript");
    const params = {
      courseId: courseId
    };
    return apiClient.delete('/user/subscription', { params: params });
  },

  inviteUser(userId, nodeId) {
    let formData = new FormData();
    formData.append('userId', userId);
    formData.append('nodeId', nodeId);
    console.log("formData: " + JSON.stringify(Object.fromEntries(formData.entries())));
    return apiClient.post('/message/invite', formData)
  },


  commentCensorList() {
    return apiClient.get('/comment/censor')
  },

  getRoadmapsByProfession(professionId, lastId = 0) {
    // professionId: number or string, lastId: number (0 for first page)
    return apiClient.get(`/roadmap/list/${professionId}`, {
      params: { lastId: lastId }
    });
  },

  searchByName(name) {
    console.log("searchByName: " + name);
    return apiClient.get('/course/search', {
      params: { name: name }
    });
  },

  postRoadmap(professionId, content, description) {
    const formData = new FormData();
    formData.append('professionId', professionId);
    formData.append('content', content);
    formData.append('description', description);
    console.log("post /roadmap, professionId: " + professionId + ", content: " + content + ", description: " + description);
    return apiClient.post('/roadmap', formData);
  },

  // 课程点赞接口
  upvoteRoadmap(roadmapId) {
    console.log("put /roadmap/" + roadmapId + "/upvote");
    return apiClient.put(`/roadmap/${roadmapId}/upvote`);
  },

  // 课程置顶接口
  pinRoadmap(professionId, roadmapId) {
    console.log("post /roadmap/pin, professionId: " + professionId + ", roadmapId: " + roadmapId);
    return apiClient.post('/roadmap/pin', null, {
      params: {
        professionId: professionId,
        roadmapId: roadmapId
      }
    });
  },

  // 开始学习课程表接口
  startRoadmap(roadmapId) {
    console.log("post /user/roadmap, roadmapId: " + roadmapId);
    return apiClient.post('/user/roadmap', null, {
      params: {
        roadmapId: roadmapId
      }
    });
  },

  // 获取用户学习的课程表接口
  getUserRoadmaps() {
    console.log("get /user/roadmap/list");
    return apiClient.get('/user/roadmap/list');
  },

  // 开始学习课程接口
  startCourse(courseId) {
    console.log("post /user/course, courseId: " + courseId);
    return apiClient.post('/user/course', null, {
      params: {
        courseId: courseId
      }
    });
  },

  // 获取用户学习的课程列表接口
  getUserCourseList(lastId) {
    console.log("get /user/course/list, lastId: " + lastId);
    const params = {};
    if (lastId) {
      params.lastId = lastId;
    }
    return apiClient.get('/user/course/list', { params });
  },

  // 获取职业数据接口
  getProfessionList(params = {}) {
    console.log("get /profession/list/approved, params: ", params);
    return apiClient.get('/profession/list/approved', { params });
  },

  // 根据主分类获取职业列表
  getProfessionByMainCategory(mainCategoryId, lastId = null) {
    const params = { mainCategory: mainCategoryId };
    if (lastId) {
      params.lastId = lastId;
    }
    console.log("get /profession/list by main category, params: ", params);
    return apiClient.get('/profession/list', { params });
  },

  // 根据子分类获取职业列表
  getProfessionBySubCategory(mainCategoryId, subCategoryId, lastId = null) {
    const params = { 
      mainCategory: mainCategoryId,
      subCategory: subCategoryId 
    };
    if (lastId) {
      params.lastId = lastId;
    }
    console.log("get /profession/list by main and sub category, params: ", params);
    return apiClient.get('/profession/list', { params });
  },

  // 提交职业申请
  submitCareerApplication(professionData) {
    console.log("post /profession, data: ", professionData);
    return apiClient.post('/profession', professionData);
  },

  // 根据状态获取职业申请列表（管理员功能）
  getProfessionByState(state, lastId = 0) {
    const params = { 
      state: state,
      lastId: lastId 
    };
    console.log("get /profession/list by state, params: ", params);
    return apiClient.get('/profession/list', { params });
  },

  // 操作职业申请（通过/拒绝）
  operateProfession(id, action, rejectedReason = '') {
    const params = { 
      id: id,
      action: action
    };
    if (rejectedReason) {
      params.rejectedReason = rejectedReason;
    }
    console.log("post /profession/operate, params: ", params);
    return apiClient.post('/profession/operate', null, { params });
  },

  // 更新职业信息
  updateProfession(professionData) {
    console.log("put /profession, data: ", professionData);
    return apiClient.put('/profession', professionData);
  },

  // 删除职业申请
  deleteProfession(id) {
    const params = { id: id };
    console.log("delete /profession, params: ", params);
    return apiClient.delete('/profession', { params });
  },

  // 获取热门职业（按学习人数排行）
  getHotProfessions(limit = 10) {
    console.log("get hot professions, limit: ", limit);
    return apiClient.get('/profession/hot', {
      params: { limit: limit }
    });
  },

  // 获取热门课程（按收藏和正在学习人数排行）
  getHotCourses(limit = 10) {
    console.log("get hot courses, limit: ", limit);
    return apiClient.get('/course/hot', {
      params: { limit: limit }
    });
  },

  // 获取热门课程完整排行榜（前100名）
  getHotCoursesRanking() {
    console.log("get hot courses ranking");
    return apiClient.get('/course/ranking');
  },

  // 获取平台统计数据
  getPlatformStats() {
    console.log("get platform stats");
    return apiClient.get('/platform/stats');
  },

  // 获取用户今日统计
  getUserTodayStats(userId) {
    console.log("get user today stats, userId:", userId);
    return apiClient.get(`/api/stats/user/${userId}/today`);
  },

  // 获取用户昨日统计
  getUserYesterdayStats(userId) {
    console.log("get user yesterday stats, userId:", userId);
    return apiClient.get(`/api/stats/user/${userId}/yesterday`);
  },

  // 获取用户历史统计（支持7天、15天、30天等）
  getUserHistoryStats(userId, days = 7) {
    console.log("get user history stats, userId:", userId, "days:", days);
    return apiClient.get(`/api/stats/user/${userId}/history`, {
      params: { days: days }
    });
  },

  // 获取用户时间段统计（包含每日明细）
  getUserPeriodStats(userId, days = 7) {
    console.log("get user period stats, userId:", userId, "days:", days);
    return apiClient.get(`/api/stats/user/${userId}/period`, {
      params: { days: days }
    });
  },

  // 获取用户全部时间统计
  getUserAllTimeStats(userId) {
    console.log("get user all time stats, userId:", userId);
    return apiClient.get(`/api/stats/user/${userId}/all-time`);
  },

  // 记录文章访问
  recordView(articleId, userId = null, ipAddress = null) {
    const params = { articleId: articleId };
    if (userId) params.userId = userId;
    if (ipAddress) params.ipAddress = ipAddress;
    
    console.log("record view, params:", params);
    return apiClient.post('/api/stats/view', null, { params });
  },

  // 获取系统健康状态
  getStatsHealth() {
    console.log("get stats health");
    return apiClient.get('/api/stats/health');
  },

  // 手动触发Redis统计数据同步到数据库
  syncStatsManual() {
    console.log("manual sync stats");
    return apiClient.post('/api/stats/sync/manual');
  },

  // 手动触发指定日期的数据同步
  syncStatsSpecificDate(date = null) {
    console.log("sync specific date stats, date:", date);
    const params = {};
    if (date) {
      params.date = date;
    }
    return apiClient.post('/api/stats/sync/date', null, { params });
  },
};