import http from "@/api";

export namespace Dashboard {
  export interface Stats {
    todaySales: number;
    todayOrderCount: number;
    todayVisitors: number;
    alertCount: number;
    yesterdaySales: number;
    yesterdayOrderCount: number;
    yesterdayVisitors: number;
    trendData: { date: string; sales: number; orders: number }[];
    hotGoods: { goodsId: number; name: string; quantity: number }[];
    urgentList: { id: number; name: string; stock: number; safeStock: number }[];
    alertList: { id: number; msg: string; level: string; time: string; resolved: boolean }[];
    categoryPieData: { name: string; value: number }[];
    hourlyTraffic: { hour: string; today: number; yesterday: number }[];
    categoryStockData: { name: string; stock: number; safeStock: number; max: number }[];
  }
}

export const getDashboardStats = (range: string = "week") => {
  return http.get<Dashboard.Stats>("/api/admin/dashboard/stats", { range }, { loading: false });
};
