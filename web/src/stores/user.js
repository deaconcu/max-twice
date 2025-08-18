import { defineStore } from "pinia";
import { ref, computed } from "vue";

export const useUserStore = defineStore("user", () => {
  // 用户 ID
  const userId = ref(null);
  const subscription = ref(null);

  // Getter
  const isLoggedIn = computed(() => userId.value !== null)

  // 设置用户 ID
  const setUserId = (id) => {
    userId.value = id;
    console.log("user id: " + userId.value);
  };

  const setSubscription = (list) => {
    subscription.value = list;
    console.log("subscription: " + subscription.value);
  };

  // 清除用户 ID（退出登录）
  const logout = () => {
    userId.value = null;
    subscription.value = null;
  };

  return { userId, subscription, setUserId, setSubscription, logout };
}, 
{
  persist: true // 启用持久化插件
});