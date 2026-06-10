// 请求响应参数（不包含data）
export interface Result {
  code: string;
  msg: string;
  message?: string; // 兼容后端 Result<T> 返回的 message 字段
}

// 请求响应参数（包含data）
export interface ResultData<T = any> extends Result {
  data: T;
}

// 分页响应参数
export interface ResPage<T> {
  list?: T[];
  records?: T[];
  pageNum?: number;
  pageSize?: number;
  current?: number;
  size?: number;
  total: number;
}

// 分页请求参数
export interface ReqPage {
  pageNum: number;
  pageSize: number;
}

// 文件上传模块
export namespace Upload {
  export interface ResFileUrl {
    fileUrl: string;
  }
}

// 登录模块
export namespace Login {
  export interface ReqLoginForm {
    username: string;
    password: string;
  }
  export interface ResLogin {
    token: string;
    tokenType: string;
  }
  export interface ResAuthButtons {
    [key: string]: string[];
  }
}

// 用户管理模块
export namespace User {
  export interface ReqUserParams extends ReqPage {
    username: string;
  }
  export interface ResUserList {
    id: number;
    username: string;
    role: string;
    status: number;
    createTime: string;
  }
  export interface ResStatus {
    userLabel: string;
    userValue: number;
  }
}

// Admin user
export interface AdminUserInfo {
  id: number;
  username: string;
  role: string;
  status: number;
  createTime: string;
}
