import json
import statistics
import subprocess
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
NPM = ROOT / "node-v24.14.1-win-x64" / "npm.cmd"
MVN = ROOT / "apache-maven-3.9.14" / "bin" / "mvn.cmd"
ADMIN_DIR = ROOT / "retail" / "retail-admin-pro"
SERVER_DIR = ROOT / "retail" / "retail-server"
LOG_DIR = ROOT / ".codex-runtime" / "real-test-logs"
LOG_DIR.mkdir(parents=True, exist_ok=True)


def start_process(args, cwd, stdout_name, stderr_name):
    stdout = (LOG_DIR / stdout_name).open("w", encoding="utf-8", errors="ignore")
    stderr = (LOG_DIR / stderr_name).open("w", encoding="utf-8", errors="ignore")
    flags = getattr(subprocess, "CREATE_NO_WINDOW", 0)
    return subprocess.Popen(args, cwd=str(cwd), stdout=stdout, stderr=stderr, creationflags=flags)


def wait_http(url, timeout=60):
    deadline = time.time() + timeout
    while time.time() < deadline:
        status, _, _ = request_once(url)
        if status is not None:
            return True
        time.sleep(0.5)
    return False


def request_once(url, method="GET", body=None):
    data = None
    headers = {}
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    start = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            content = response.read(512)
            return response.status, time.perf_counter() - start, content.decode("utf-8", errors="ignore")
    except urllib.error.HTTPError as exc:
        content = exc.read(512)
        return exc.code, time.perf_counter() - start, content.decode("utf-8", errors="ignore")
    except Exception as exc:
        return None, time.perf_counter() - start, str(exc)


def percentile(values, pct):
    if not values:
        return 0
    ordered = sorted(values)
    index = min(len(ordered) - 1, max(0, round((pct / 100) * (len(ordered) - 1))))
    return ordered[index]


def run_load(name, url, users, total, expected_status=200, method="GET", body=None):
    start = time.perf_counter()
    results = []
    with ThreadPoolExecutor(max_workers=users) as pool:
        futures = [pool.submit(request_once, url, method, body) for _ in range(total)]
        for future in as_completed(futures):
            results.append(future.result())
    elapsed = time.perf_counter() - start
    latencies = [item[1] * 1000 for item in results if item[1] is not None]
    ok = [item for item in results if item[0] == expected_status]
    statuses = {}
    for status, _, _ in results:
        statuses[str(status)] = statuses.get(str(status), 0) + 1
    return {
        "scene": name,
        "url": url,
        "users": users,
        "samples": total,
        "expected_status": expected_status,
        "success": len(ok),
        "error_rate": round((total - len(ok)) / total * 100, 2),
        "avg_ms": round(statistics.mean(latencies), 2) if latencies else 0,
        "p95_ms": round(percentile(latencies, 95), 2),
        "throughput_req_s": round(total / elapsed, 2) if elapsed else 0,
        "statuses": statuses,
    }


def print_result(item):
    print(
        "RESULT | {scene} | users={users} | samples={samples} | success={success} | "
        "error_rate={error_rate}% | avg={avg_ms}ms | p95={p95_ms}ms | throughput={throughput_req_s}/s | statuses={statuses}".format(
            **item
        )
    )


def main():
    print("MODULE=performance-smoke")
    print("TIME=" + time.strftime("%Y-%m-%d %H:%M:%S"))

    vite_port = 18849
    spring_port = 18090
    vite = start_process(
        [str(NPM), "run", "dev", "--", "--host", "127.0.0.1", "--port", str(vite_port), "--strictPort"],
        ADMIN_DIR,
        "perf-vite-out.log",
        "perf-vite-err.log",
    )
    spring = start_process(
        [str(MVN), "spring-boot:run", f"-Dspring-boot.run.arguments=--server.port={spring_port}"],
        SERVER_DIR,
        "perf-spring-out.log",
        "perf-spring-err.log",
    )

    try:
        frontend_url = f"http://127.0.0.1:{vite_port}/"
        backend_health = f"http://127.0.0.1:{spring_port}/actuator/health"
        print("WAIT | frontend | " + str(wait_http(frontend_url, timeout=45)))
        print("WAIT | backend | " + str(wait_http(backend_health, timeout=90)))

        scenarios = [
            ("frontend-home-baseline", frontend_url, 1, 20, 200),
            ("frontend-home-load", frontend_url, 5, 100, 200),
            ("frontend-home-pressure", frontend_url, 10, 200, 200),
            ("backend-health-diagnostic", backend_health, 5, 50, 200),
        ]
        for name, url, users, total, expected in scenarios:
            item = run_load(name, url, users, total, expected)
            print_result(item)
    finally:
        for process in (vite, spring):
            process.terminate()
        time.sleep(1)
        for process in (vite, spring):
            if process.poll() is None:
                process.kill()


if __name__ == "__main__":
    main()
