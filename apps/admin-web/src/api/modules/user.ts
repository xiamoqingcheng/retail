import { ResPage } from "@/api/interface/index";
import { AdminUserInfo } from "@/api/interface/index";
import http from "@/api";

export const getAdminUserInfo = () => {
  return http.get<AdminUserInfo>("/api/admin/user/info");
};

export const changePassword = (params: { oldPassword: string; newPassword: string }) => {
  return http.put("/api/admin/user/password", params);
};

export const getAdminUserPage = (params: { page: number; size: number }) => {
  return http.get<ResPage<AdminUserInfo>>("/api/admin/user/page", params);
};

export const createAdminUser = (params: { username: string; password: string }) => {
  return http.post<AdminUserInfo>("/api/admin/user", params);
};

export const updateAdminUserStatus = (params: { id: number; status: number }) => {
  return http.put(`/api/admin/user/${params.id}/status`, { status: params.status });
};

export const deleteAdminUser = (id: number) => {
  return http.delete(`/api/admin/user/${id}`);
};
