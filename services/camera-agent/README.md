# Retail Camera Agent

`retail-camera-agent` 是给局域网内客机使用的轻量摄像头扩展端。客机运行一个独立 exe 后，主机后端可以通过 HTTP 获取该电脑的摄像头列表、单帧 JPEG 和 MJPEG 实时流。

## 为什么用 C++

- 运行时不依赖 Python、OpenCV、Node、Go runtime 等外部环境。
- 摄像头采集使用 Windows Media Foundation。
- JPEG 编码使用 Windows Imaging Component。
- HTTP 与 UDP 发现使用 WinSock。

最终产物是 `retail-camera-agent.exe`，只依赖 Windows 自带系统 DLL。

## 构建

```powershell
cd services\camera-agent
.\build.ps1
```

生成文件：

```text
services\camera-agent\build\retail-camera-agent.exe
```

## 客机运行

普通使用时，直接双击 `retail-camera-agent.exe` 即可。窗口会保持打开，并自动输出：

- 本机可被主机访问的 IP / 服务地址
- 本机检测到的摄像头
- 可直接填入后台的摄像头编号，例如 `agent:192.168.1.23:8765:0`

保持该窗口打开，主机才能持续调用这台电脑的摄像头。

专业人员排障时仍可用命令行参数启动：

常用参数：

```text
--port 8765             HTTP 服务端口
--discovery-port 8766   UDP 局域网发现端口
--quality 80            JPEG 质量，1-100
--fps 8                 MJPEG 最大帧率
--idle-seconds 60       摄像头空闲释放时间
--no-discovery          关闭 UDP 发现
```

Windows 防火墙需要允许该 exe 的局域网入站访问，至少放行 TCP `8765` 和 UDP `8766`。

## 主机后端使用

后端支持两种摄像头编号：

```text
0                         主机本机摄像头，保持原逻辑
agent:192.168.1.23:8765:0 远端客机 192.168.1.23 上的 0 号摄像头
```

发现局域网扩展端：

```http
GET /api/admin/camera/agents/discover?timeoutMillis=1500
```

查询指定客机摄像头：

```http
GET /api/admin/camera/agent/available?host=192.168.1.23&port=8765
```

绑定摄像头时，把 `camera_no` 填成返回的 `agent:IP:端口:索引` 即可。已有预览、视频流、截图标注、定时巡检都会走同一套后端分流逻辑。

## 扩展端接口

```http
GET  /api/agent/health
GET  /api/agent/cameras/available
GET  /api/agent/cameras/frame/{index}
GET  /api/agent/cameras/stream/{index}
POST /api/agent/cameras/stream/{index}/stop
```

`/frame/{index}` 返回：

```json
{
  "camera_index": 0,
  "image_base64": "..."
}
```

`/stream/{index}` 返回 `multipart/x-mixed-replace; boundary=frame` MJPEG 流。
