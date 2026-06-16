import http from "@/api";

export interface AdminFeedback {
  id: number;
  userId?: number | null;
  feedbackType: string;
  content: string;
  contact?: string | null;
  apiBaseUrl?: string | null;
  systemInfo?: string | null;
  diagnosticInfo?: string | null;
  status: string;
  reply?: string | null;
  createTime: string;
  updateTime?: string | null;
  resolvedTime?: string | null;
}

export interface FeedbackPage {
  records: AdminFeedback[];
  total: number;
  current: number;
  size: number;
}

export const getFeedbackPage = (params: { page: number; size: number; status?: string; feedbackType?: string }) => {
  return http.get<FeedbackPage>("/api/admin/feedback/page", params, { loading: false });
};

export const updateFeedbackStatus = (id: number, params: { status: string; reply?: string }) => {
  return http.put<AdminFeedback>(`/api/admin/feedback/${id}/status`, params);
};
