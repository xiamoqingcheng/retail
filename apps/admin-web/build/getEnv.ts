// 将 .env / .env.[mode] 中的「字符串」环境变量转换为 vite.config.ts 需要的「强类型」运行时配置。
// 被 vite.config.ts 以 wrapperEnv(loadEnv(...)) 形式调用。
type Recordable<T = any> = Record<string, T>;

/**
 * 读取所有（VITE_ 开头）环境变量配置并转换类型
 * - "true"/"false" -> boolean
 * - VITE_PORT       -> number
 * - VITE_PROXY      -> [string, string][]（支持单引号写法）
 */
export function wrapperEnv(envConf: Recordable): Recordable {
  const ret: Recordable = {};

  for (const envName of Object.keys(envConf)) {
    let realName: any = envConf[envName].replace(/\\n/g, "\n");
    realName = realName === "true" ? true : realName === "false" ? false : realName;

    if (envName === "VITE_PORT") realName = Number(realName);
    if (envName === "VITE_PROXY" && realName) {
      try {
        realName = JSON.parse(realName.replace(/'/g, '"'));
      } catch (error) {
        realName = "";
      }
    }

    ret[envName] = realName;
  }

  return ret;
}
