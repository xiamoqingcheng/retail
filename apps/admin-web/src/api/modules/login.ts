import { Login } from "@/api/interface/index";
import http from "@/api";

export const loginApi = (params: Login.ReqLoginForm) => {
  return http.post<Login.ResLogin>(`/api/auth/login`, params, { loading: false });
};

export const getAuthMenuListApi = () => {
  return http.get<Menu.MenuOptions[]>("/api/admin/menu/list", {}, { loading: false });
};

export const getAuthButtonListApi = () => {
  return http.get<Login.ResAuthButtons>("/api/admin/menu/buttons", {}, { loading: false });
};

export const logoutApi = () => {
  return http.post(`/api/auth/logout`);
};
