import { resolve } from "path";
import type { PluginOption } from "vite";
import vue from "@vitejs/plugin-vue";
import vueJsx from "@vitejs/plugin-vue-jsx";
import { createSvgIconsPlugin } from "vite-plugin-svg-icons";
import { createHtmlPlugin } from "vite-plugin-html";
import { visualizer } from "rollup-plugin-visualizer";
import { codeInspectorPlugin } from "code-inspector-plugin";
import VueSetupExtend from "unplugin-vue-setup-extend-plus/vite";
import viteCompression from "vite-plugin-compression";
import { VitePWA } from "vite-plugin-pwa";
import vueDevTools from "vite-plugin-vue-devtools";

/**
 * 根据环境变量按需装配 Vite 插件。
 * 开发态默认只启用前 5 个（vue / jsx / setup-extend / html 注入 / svg 雪碧图），
 * 其余（压缩 / PWA / 分析 / devtools / code-inspector）由 .env 开关控制。
 */
export function createVitePlugins(viteEnv: Record<string, any>): (PluginOption | PluginOption[])[] {
  const {
    VITE_GLOB_APP_TITLE,
    VITE_REPORT,
    VITE_PWA,
    VITE_DEVTOOLS,
    VITE_CODEINSPECTOR,
    VITE_BUILD_COMPRESS,
    VITE_BUILD_COMPRESS_DELETE_ORIGIN_FILE
  } = viteEnv;

  const plugins: (PluginOption | PluginOption[])[] = [
    vue(),
    vueJsx(),
    // <script setup name="xxx"> 语法支持
    VueSetupExtend(),
    // index.html 中 <%- title %> 注入
    createHtmlPlugin({
      minify: true,
      inject: { data: { title: VITE_GLOB_APP_TITLE } }
    }),
    // src/assets/icons 下的 svg -> <svg-icon name="icon-xxx" />
    createSvgIconsPlugin({
      iconDirs: [resolve(process.cwd(), "src/assets/icons")],
      symbolId: "icon-[dir]-[name]"
    })
  ];

  // 源码定位（点击页面元素跳转到编辑器对应代码）
  if (VITE_CODEINSPECTOR) plugins.push(codeInspectorPlugin({ bundler: "vite" }));

  // vue devtools
  if (VITE_DEVTOOLS) plugins.push(vueDevTools());

  // 打包压缩
  if (VITE_BUILD_COMPRESS && VITE_BUILD_COMPRESS !== "none") {
    const compressList = String(VITE_BUILD_COMPRESS).split(",");
    if (compressList.includes("gzip")) {
      plugins.push(viteCompression({ ext: ".gz", deleteOriginFile: VITE_BUILD_COMPRESS_DELETE_ORIGIN_FILE }));
    }
    if (compressList.includes("brotli")) {
      plugins.push(
        viteCompression({ ext: ".br", algorithm: "brotliCompress", deleteOriginFile: VITE_BUILD_COMPRESS_DELETE_ORIGIN_FILE })
      );
    }
  }

  // PWA
  if (VITE_PWA) {
    plugins.push(
      VitePWA({
        registerType: "autoUpdate",
        manifest: {
          name: VITE_GLOB_APP_TITLE,
          short_name: VITE_GLOB_APP_TITLE,
          theme_color: "#ffffff"
        }
      })
    );
  }

  // 打包体积分析
  if (VITE_REPORT) {
    plugins.push(visualizer({ filename: "stats.html", gzipSize: true, brotliSize: true }) as PluginOption);
  }

  return plugins;
}
