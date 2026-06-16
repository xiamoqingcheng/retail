import router from "@/routers/index";
import { LOGIN_URL } from "@/config";
import { RouteRecordRaw } from "vue-router";
import { ElNotification } from "element-plus";
import { useUserStore } from "@/stores/modules/user";
import { useAuthStore } from "@/stores/modules/auth";
import i18n from "@/languages/index";

// 引入 views 文件夹下所有 vue 文件
const modules = import.meta.glob("@/views/**/*.vue");

const resolveViewComponent = (componentPath: string) => {
  const normalizedPath = componentPath.startsWith("/") ? componentPath : `/${componentPath}`;
  return modules[`/src/views${normalizedPath}.vue`];
};

/**
 * @description 初始化动态路由
 */
export const initDynamicRouter = async () => {
  const userStore = useUserStore();
  const authStore = useAuthStore();

  try {
    // 1.获取菜单列表 && 按钮权限列表
    await authStore.getAuthMenuList();
    await authStore.getAuthButtonList();

    // 2.判断当前用户有没有菜单权限
    if (!authStore.authMenuListGet.length) {
      ElNotification({
        title: i18n.global.t("error.noPermission"),
        message: i18n.global.t("error.noMenu"),
        type: "warning",
        duration: 3000
      });
      userStore.setToken("");
      router.replace(LOGIN_URL);
      return Promise.reject("No permission");
    }

    // 3.添加动态路由
    authStore.flatMenuListGet.forEach(item => {
      item.children && delete item.children;
      if (item.component && typeof item.component == "string") {
        const viewComponent = resolveViewComponent(item.component);
        if (!viewComponent) return;
        item.component = viewComponent;
      }
      if (item.meta.isFull) {
        router.addRoute(item as unknown as RouteRecordRaw);
      } else {
        router.addRoute("layout", item as unknown as RouteRecordRaw);
      }
    });
  } catch (error) {
    // 当按钮 || 菜单请求出错时，重定向到登陆页
    userStore.setToken("");
    router.replace(LOGIN_URL);
    return Promise.reject(error);
  }
};
