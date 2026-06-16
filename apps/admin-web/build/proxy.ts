import type { ProxyOptions } from "vite";

type ProxyItem = [string, string];
type ProxyList = ProxyItem[];
type ProxyTargetList = Record<string, ProxyOptions>;

/**
 * 根据 VITE_PROXY（[前缀, 目标] 列表）生成 Vite 开发服务器的 proxy 配置。
 *
 * 注意：本项目后端所有路由本身就挂在 /api 下（如 /api/auth/login），
 * 因此这里【不做 rewrite 去掉前缀】，让 /api/xxx 原样转发到目标。
 * 例：["/api", "http://localhost:8080"] => /api/auth/login -> http://localhost:8080/api/auth/login
 */
export function createProxy(list: ProxyList = []): ProxyTargetList {
  const ret: ProxyTargetList = {};

  for (const [prefix, target] of list) {
    const isHttps = /^https:\/\//.test(target);
    ret[prefix] = {
      target,
      changeOrigin: true,
      ws: true,
      // https 目标时忽略证书校验
      ...(isHttps ? { secure: false } : {})
    };
  }

  return ret;
}
