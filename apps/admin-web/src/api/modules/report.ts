import http from "@/api";

export interface ReportSchedule {
  id?: number;
  enabled: number;
  intervalDays: number;
  intervalHours: number;
  intervalMinutes: number;
  lastRunTime?: string | null;
  nextRunTime?: string | null;
  updateTime?: string | null;
}

export interface ReportRecord {
  id: number;
  title: string;
  reportType: string;
  periodStart: string;
  periodEnd: string;
  createTime: string;
}

/**
 * @name 定时报表模块
 */
// 查询定时报表配置
export const getReportSchedule = () => {
  return http.get<ReportSchedule>("/api/admin/report/schedule");
};

// 更新定时报表配置
export const updateReportSchedule = (data: Partial<ReportSchedule>) => {
  return http.put<ReportSchedule>("/api/admin/report/schedule", data);
};

// 手动生成报表
export const generateReport = (data: { start: string; end: string }) => {
  return http.post<{ id: number; content: any }>("/api/admin/report/generate", data);
};

// 分页查询历史报表
export const getReportList = (page: number, size: number) => {
  return http.get<any>("/api/admin/report/list", { page, size });
};

// 查询单份报表完整内容
export const getReportDetail = (id: number) => {
  return http.get<any>(`/api/admin/report/${id}`);
};

// 删除报表
export const deleteReport = (id: number) => {
  return http.delete(`/api/admin/report/${id}`);
};

// 导出报表文件（xlsx / pdf），返回二进制 Blob
export const exportReportFile = (id: number, format: "xlsx" | "pdf") => {
  return http.get<Blob>(`/api/admin/report/${id}/export`, { format }, { responseType: "blob" });
};
