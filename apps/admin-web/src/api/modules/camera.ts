import http from "@/api";

const cameraPath = (cameraNo: string) => encodeURIComponent(cameraNo);

export interface CameraBindingPayload {
  id?: number | string | null;
  camera_no?: string;
  shelf_id?: string;
  shelfId?: string;
}

export interface CameraAgentCamera {
  id: string;
  index: number | string;
  name?: string;
}

export interface CameraAgentInfo {
  service?: string;
  version?: string;
  host: string;
  port: number;
  available_indexes?: Array<number | string>;
  cameras?: CameraAgentCamera[];
}

/**
 * @name 摄像头调度模块
 */
// 查询可用的物理摄像头索引
export const getAvailableHardwareCameras = () => {
  return http.get<Array<number | string>>("/api/admin/camera/available_hardware");
};

// 自动发现局域网内运行中的摄像头扩展端
export const discoverCameraAgents = (timeoutMillis = 1500) => {
  return http.get<CameraAgentInfo[]>(`/api/admin/camera/agents/discover?timeoutMillis=${timeoutMillis}`);
};

// 查询指定扩展端的可用摄像头
export const getCameraAgentAvailable = (host: string, port?: number | string) => {
  const params = new URLSearchParams({ host });
  if (port) params.set("port", String(port));
  return http.get<CameraAgentInfo>(`/api/admin/camera/agent/available?${params.toString()}`);
};

// 获取摄像头预览单帧（base64）
export const getCameraPreviewFrame = (cameraNo: string) => {
  return http.get<string>(`/api/admin/camera/preview/${cameraPath(cameraNo)}`);
};

// 新增摄像头绑定关系
export const createCameraBinding = (data: CameraBindingPayload) => {
  return http.post("/api/admin/camera", data);
};

// 查询摄像头列表
export const getCameraList = () => {
  return http.get<any[]>("/api/admin/camera/list");
};

// 更新摄像头绑定货架
export const updateCameraShelf = (data: CameraBindingPayload) => {
  return http.put("/api/admin/camera", data);
};

export const updateCameraStatus = (id: number | string, status: number) => {
  return http.put(`/api/admin/camera/${id}/status`, { status });
};

// 删除摄像头
export const deleteCamera = (id: number | string) => {
  return http.delete(`/api/admin/camera/${id}`);
};

// 更新 AI 定时巡检配置并重启任务
export const updateSchedulerConfig = (data: { intervalMinutes: number; batchSize: number }) => {
  return http.put<{ intervalMinutes: number; batchSize: number }>("/api/admin/camera/scheduler/config", data);
};

// 查询当前 AI 定时巡检配置
export const getSchedulerConfig = () => {
  return http.get<{ intervalMinutes: number; batchSize: number }>("/api/admin/camera/scheduler/config");
};

// 手动触发一次全量扫描
export const triggerSchedulerOnce = () => {
  return http.post("/api/admin/camera/scheduler/trigger");
};

// 主动释放摄像头资源
export const stopCameraStream = (cameraNo: string) => {
  return http.post(`/api/admin/camera/stream/stop?cameraNo=${cameraPath(cameraNo)}`);
};
