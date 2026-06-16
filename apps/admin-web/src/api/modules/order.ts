import http from "@/api";

export interface AdminOrderItem {
  goodsId: number;
  goodsName: string;
  goodsPrice: number;
  quantity: number;
  goodsImage?: string;
}

export interface AdminOrder {
  id: number;
  status: string;
  totalAmount: number;
  createTime: string;
  goods: AdminOrderItem[];
}

export interface AdminOrderPage {
  records: AdminOrder[];
  total: number;
  page: number;
  size: number;
}

export const getAdminOrderPage = (params: { page: number; size: number; status?: string }) => {
  return http.get<AdminOrderPage>("/api/admin/order/page", params, { loading: false });
};

export const getAdminOrderDetail = (id: number) => {
  return http.get<AdminOrder>("/api/admin/order/" + id, {}, { loading: false });
};
