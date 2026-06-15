#define WIN32_LEAN_AND_MEAN

#include <windows.h>
#include <winsock2.h>
#include <ws2tcpip.h>
#include <mfapi.h>
#include <mfidl.h>
#include <mfreadwrite.h>
#include <wincodec.h>
#include <objidl.h>
#include <propidl.h>

#include <algorithm>
#include <atomic>
#include <chrono>
#include <cctype>
#include <cstdint>
#include <cstdlib>
#include <iomanip>
#include <iostream>
#include <map>
#include <memory>
#include <mutex>
#include <sstream>
#include <string>
#include <thread>
#include <vector>

namespace {

constexpr const char *kVersion = "0.1.0";
constexpr const char *kDiscoveryMessage = "RETAIL_CAMERA_DISCOVER";
constexpr int kDefaultPort = 8765;
constexpr int kDefaultDiscoveryPort = 8766;

std::atomic_bool g_running{true};
SOCKET g_listen_socket = INVALID_SOCKET;

template <typename T>
class ComPtr {
public:
    ComPtr() = default;
    explicit ComPtr(T *ptr) : ptr_(ptr) {}
    ~ComPtr() { reset(); }

    ComPtr(const ComPtr &) = delete;
    ComPtr &operator=(const ComPtr &) = delete;

    ComPtr(ComPtr &&other) noexcept : ptr_(other.ptr_) { other.ptr_ = nullptr; }
    ComPtr &operator=(ComPtr &&other) noexcept {
        if (this != &other) {
            reset();
            ptr_ = other.ptr_;
            other.ptr_ = nullptr;
        }
        return *this;
    }

    T *get() const { return ptr_; }
    T **put() {
        reset();
        return &ptr_;
    }
    T *operator->() const { return ptr_; }
    explicit operator bool() const { return ptr_ != nullptr; }

    void reset(T *ptr = nullptr) {
        if (ptr_ != nullptr) {
            ptr_->Release();
        }
        ptr_ = ptr;
    }

private:
    T *ptr_ = nullptr;
};

class ComInit {
public:
    ComInit() : hr_(CoInitializeEx(nullptr, COINIT_MULTITHREADED)) {}
    ~ComInit() {
        if (hr_ == S_OK || hr_ == S_FALSE) {
            CoUninitialize();
        }
    }
    bool ok() const { return SUCCEEDED(hr_) || hr_ == RPC_E_CHANGED_MODE; }

private:
    HRESULT hr_;
};

struct Config {
    int port = kDefaultPort;
    int discovery_port = kDefaultDiscoveryPort;
    int quality = 80;
    int fps = 8;
    int idle_seconds = 60;
    bool discovery_enabled = true;
    std::string advertise_host;
};

struct CameraInfo {
    int index = 0;
    std::string name;
};

std::string trim(std::string value) {
    auto is_space = [](unsigned char ch) { return std::isspace(ch) != 0; };
    value.erase(value.begin(), std::find_if(value.begin(), value.end(), [&](unsigned char ch) {
        return !is_space(ch);
    }));
    value.erase(std::find_if(value.rbegin(), value.rend(), [&](unsigned char ch) {
        return !is_space(ch);
    }).base(), value.end());
    return value;
}

std::string wide_to_utf8(const std::wstring &value) {
    if (value.empty()) {
        return {};
    }
    int size = WideCharToMultiByte(CP_UTF8, 0, value.data(), static_cast<int>(value.size()),
                                   nullptr, 0, nullptr, nullptr);
    if (size <= 0) {
        return {};
    }
    std::string output(size, '\0');
    WideCharToMultiByte(CP_UTF8, 0, value.data(), static_cast<int>(value.size()),
                        output.data(), size, nullptr, nullptr);
    return output;
}

std::string host_name() {
    char name[MAX_COMPUTERNAME_LENGTH + 1] = {};
    DWORD size = static_cast<DWORD>(sizeof(name));
    if (GetComputerNameA(name, &size)) {
        return std::string(name, size);
    }
    return "localhost";
}

std::vector<std::string> local_ipv4_addresses() {
    std::vector<std::string> addresses;
    char name[256] = {};
    if (gethostname(name, sizeof(name)) == SOCKET_ERROR) {
        return {"127.0.0.1"};
    }

    addrinfo hints = {};
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;

    addrinfo *results = nullptr;
    if (getaddrinfo(name, nullptr, &hints, &results) != 0 || results == nullptr) {
        return {"127.0.0.1"};
    }

    for (addrinfo *item = results; item != nullptr; item = item->ai_next) {
        auto *addr = reinterpret_cast<sockaddr_in *>(item->ai_addr);
        char ip[INET_ADDRSTRLEN] = {};
        if (inet_ntop(AF_INET, &addr->sin_addr, ip, sizeof(ip)) == nullptr) {
            continue;
        }
        std::string value = ip;
        if (value == "127.0.0.1" || value.rfind("169.254.", 0) == 0) {
            continue;
        }
        if (std::find(addresses.begin(), addresses.end(), value) == addresses.end()) {
            addresses.push_back(value);
        }
    }
    freeaddrinfo(results);

    if (addresses.empty()) {
        addresses.push_back("127.0.0.1");
    }
    return addresses;
}

std::string json_escape(const std::string &value) {
    std::ostringstream out;
    for (unsigned char ch : value) {
        switch (ch) {
        case '"': out << "\\\""; break;
        case '\\': out << "\\\\"; break;
        case '\b': out << "\\b"; break;
        case '\f': out << "\\f"; break;
        case '\n': out << "\\n"; break;
        case '\r': out << "\\r"; break;
        case '\t': out << "\\t"; break;
        default:
            if (ch < 0x20) {
                out << "\\u" << std::hex << std::setw(4) << std::setfill('0') << static_cast<int>(ch);
            } else {
                out << ch;
            }
        }
    }
    return out.str();
}

std::string hresult_message(HRESULT hr) {
    char *message = nullptr;
    DWORD size = FormatMessageA(
        FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
        nullptr,
        static_cast<DWORD>(hr),
        MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
        reinterpret_cast<LPSTR>(&message),
        0,
        nullptr);
    std::string result = size > 0 && message != nullptr ? trim(message) : "HRESULT 0x" + std::to_string(hr);
    if (message != nullptr) {
        LocalFree(message);
    }
    return result;
}

std::string base64_encode(const std::vector<std::uint8_t> &data) {
    static constexpr char table[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    std::string output;
    output.reserve(((data.size() + 2) / 3) * 4);

    for (size_t i = 0; i < data.size(); i += 3) {
        uint32_t block = static_cast<uint32_t>(data[i]) << 16;
        if (i + 1 < data.size()) {
            block |= static_cast<uint32_t>(data[i + 1]) << 8;
        }
        if (i + 2 < data.size()) {
            block |= static_cast<uint32_t>(data[i + 2]);
        }

        output.push_back(table[(block >> 18) & 0x3F]);
        output.push_back(table[(block >> 12) & 0x3F]);
        output.push_back(i + 1 < data.size() ? table[(block >> 6) & 0x3F] : '=');
        output.push_back(i + 2 < data.size() ? table[block & 0x3F] : '=');
    }
    return output;
}

class DeviceActivationList {
public:
    ~DeviceActivationList() {
        if (devices_ != nullptr) {
            for (UINT32 i = 0; i < count_; ++i) {
                if (devices_[i] != nullptr) {
                    devices_[i]->Release();
                }
            }
            CoTaskMemFree(devices_);
        }
    }

    HRESULT load() {
        ComPtr<IMFAttributes> attributes;
        HRESULT hr = MFCreateAttributes(attributes.put(), 1);
        if (FAILED(hr)) {
            return hr;
        }
        hr = attributes->SetGUID(MF_DEVSOURCE_ATTRIBUTE_SOURCE_TYPE,
                                 MF_DEVSOURCE_ATTRIBUTE_SOURCE_TYPE_VIDCAP_GUID);
        if (FAILED(hr)) {
            return hr;
        }
        return MFEnumDeviceSources(attributes.get(), &devices_, &count_);
    }

    UINT32 count() const { return count_; }
    IMFActivate *at(UINT32 index) const { return index < count_ ? devices_[index] : nullptr; }

private:
    IMFActivate **devices_ = nullptr;
    UINT32 count_ = 0;
};

std::vector<CameraInfo> enumerate_cameras() {
    DeviceActivationList activations;
    HRESULT hr = activations.load();
    if (FAILED(hr)) {
        return {};
    }

    std::vector<CameraInfo> cameras;
    cameras.reserve(activations.count());
    for (UINT32 i = 0; i < activations.count(); ++i) {
        std::string name = "Camera " + std::to_string(i);
        WCHAR *friendly_name = nullptr;
        UINT32 name_len = 0;
        if (SUCCEEDED(activations.at(i)->GetAllocatedString(
                MF_DEVSOURCE_ATTRIBUTE_FRIENDLY_NAME, &friendly_name, &name_len)) &&
            friendly_name != nullptr) {
            name = wide_to_utf8(std::wstring(friendly_name, name_len));
            CoTaskMemFree(friendly_name);
        }
        cameras.push_back({static_cast<int>(i), name});
    }
    return cameras;
}

class CameraDevice {
public:
    ~CameraDevice() {
        if (source_) {
            source_->Shutdown();
        }
    }

    bool open(int index, std::string &error) {
        DeviceActivationList activations;
        HRESULT hr = activations.load();
        if (FAILED(hr)) {
            error = "enumerate cameras failed: " + hresult_message(hr);
            return false;
        }
        if (index < 0 || static_cast<UINT32>(index) >= activations.count()) {
            error = "camera index is not available";
            return false;
        }

        IMFMediaSource *raw_source = nullptr;
        hr = activations.at(static_cast<UINT32>(index))->ActivateObject(IID_PPV_ARGS(&raw_source));
        if (FAILED(hr)) {
            error = "activate camera failed: " + hresult_message(hr);
            return false;
        }
        source_.reset(raw_source);

        ComPtr<IMFAttributes> reader_attributes;
        hr = MFCreateAttributes(reader_attributes.put(), 1);
        if (FAILED(hr)) {
            error = "create reader attributes failed: " + hresult_message(hr);
            return false;
        }
        reader_attributes->SetUINT32(MF_SOURCE_READER_ENABLE_VIDEO_PROCESSING, TRUE);

        IMFSourceReader *raw_reader = nullptr;
        hr = MFCreateSourceReaderFromMediaSource(source_.get(), reader_attributes.get(), &raw_reader);
        if (FAILED(hr)) {
            error = "create source reader failed: " + hresult_message(hr);
            return false;
        }
        reader_.reset(raw_reader);

        ComPtr<IMFMediaType> media_type;
        hr = MFCreateMediaType(media_type.put());
        if (FAILED(hr)) {
            error = "create media type failed: " + hresult_message(hr);
            return false;
        }
        media_type->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Video);
        media_type->SetGUID(MF_MT_SUBTYPE, MFVideoFormat_RGB32);

        hr = reader_->SetCurrentMediaType(MF_SOURCE_READER_FIRST_VIDEO_STREAM, nullptr, media_type.get());
        if (FAILED(hr)) {
            error = "select RGB32 output failed: " + hresult_message(hr);
            return false;
        }

        ComPtr<IMFMediaType> current_type;
        hr = reader_->GetCurrentMediaType(MF_SOURCE_READER_FIRST_VIDEO_STREAM, current_type.put());
        if (FAILED(hr)) {
            error = "read output media type failed: " + hresult_message(hr);
            return false;
        }

        UINT32 width = 0;
        UINT32 height = 0;
        hr = MFGetAttributeSize(current_type.get(), MF_MT_FRAME_SIZE, &width, &height);
        if (FAILED(hr) || width == 0 || height == 0) {
            error = "read frame size failed: " + hresult_message(hr);
            return false;
        }
        width_ = width;
        height_ = height;
        return true;
    }

    bool capture_jpeg(int quality, std::vector<std::uint8_t> &jpeg, std::string &error) {
        std::lock_guard<std::mutex> guard(mutex_);
        if (!reader_) {
            error = "camera is not open";
            return false;
        }

        ComPtr<IMFSample> sample;
        DWORD flags = 0;
        for (int attempt = 0; attempt < 20; ++attempt) {
            IMFSample *raw_sample = nullptr;
            DWORD stream_index = 0;
            LONGLONG timestamp = 0;
            HRESULT hr = reader_->ReadSample(MF_SOURCE_READER_FIRST_VIDEO_STREAM, 0,
                                             &stream_index, &flags, &timestamp, &raw_sample);
            if (FAILED(hr)) {
                error = "read frame failed: " + hresult_message(hr);
                return false;
            }
            sample.reset(raw_sample);
            if ((flags & MF_SOURCE_READERF_ENDOFSTREAM) != 0) {
                error = "camera stream ended";
                return false;
            }
            if (sample) {
                break;
            }
            Sleep(20);
        }

        if (!sample) {
            error = "camera did not return a frame";
            return false;
        }

        ComPtr<IMFMediaBuffer> buffer;
        HRESULT hr = sample->ConvertToContiguousBuffer(buffer.put());
        if (FAILED(hr)) {
            error = "copy frame buffer failed: " + hresult_message(hr);
            return false;
        }

        BYTE *data = nullptr;
        DWORD max_length = 0;
        DWORD current_length = 0;
        hr = buffer->Lock(&data, &max_length, &current_length);
        if (FAILED(hr)) {
            error = "lock frame buffer failed: " + hresult_message(hr);
            return false;
        }

        bool ok = encode_jpeg(data, current_length, quality, jpeg, error);
        buffer->Unlock();
        return ok;
    }

private:
    bool encode_jpeg(BYTE *pixels,
                     DWORD buffer_size,
                     int quality,
                     std::vector<std::uint8_t> &jpeg,
                     std::string &error) {
        if (pixels == nullptr || width_ == 0 || height_ == 0) {
            error = "empty frame";
            return false;
        }

        UINT source_stride = width_ * 4;
        UINT required_size = source_stride * height_;
        if (buffer_size < required_size) {
            error = "frame buffer is smaller than expected";
            return false;
        }

        UINT jpeg_stride = width_ * 3;
        std::vector<BYTE> bgr24(static_cast<size_t>(jpeg_stride) * height_);
        for (UINT32 y = 0; y < height_; ++y) {
            BYTE *dst_row = bgr24.data() + static_cast<size_t>(y) * jpeg_stride;
            BYTE *src_row = pixels + static_cast<size_t>(y) * source_stride;
            for (UINT32 x = 0; x < width_; ++x) {
                dst_row[x * 3 + 0] = src_row[x * 4 + 0];
                dst_row[x * 3 + 1] = src_row[x * 4 + 1];
                dst_row[x * 3 + 2] = src_row[x * 4 + 2];
            }
        }

        IWICImagingFactory *raw_factory = nullptr;
        HRESULT hr = CoCreateInstance(CLSID_WICImagingFactory, nullptr, CLSCTX_INPROC_SERVER,
                                      IID_PPV_ARGS(&raw_factory));
        if (FAILED(hr)) {
            error = "create WIC factory failed: " + hresult_message(hr);
            return false;
        }
        ComPtr<IWICImagingFactory> factory(raw_factory);

        IStream *raw_stream = nullptr;
        hr = CreateStreamOnHGlobal(nullptr, TRUE, &raw_stream);
        if (FAILED(hr)) {
            error = "create memory stream failed: " + hresult_message(hr);
            return false;
        }
        ComPtr<IStream> stream(raw_stream);

        IWICBitmapEncoder *raw_encoder = nullptr;
        hr = factory->CreateEncoder(GUID_ContainerFormatJpeg, nullptr, &raw_encoder);
        if (FAILED(hr)) {
            error = "create JPEG encoder failed: " + hresult_message(hr);
            return false;
        }
        ComPtr<IWICBitmapEncoder> encoder(raw_encoder);

        hr = encoder->Initialize(stream.get(), WICBitmapEncoderNoCache);
        if (FAILED(hr)) {
            error = "initialize JPEG encoder failed: " + hresult_message(hr);
            return false;
        }

        IWICBitmapFrameEncode *raw_frame = nullptr;
        IPropertyBag2 *raw_props = nullptr;
        hr = encoder->CreateNewFrame(&raw_frame, &raw_props);
        if (FAILED(hr)) {
            error = "create JPEG frame failed: " + hresult_message(hr);
            return false;
        }
        ComPtr<IWICBitmapFrameEncode> frame(raw_frame);
        ComPtr<IPropertyBag2> props(raw_props);

        PROPBAG2 option = {};
        option.dwType = PROPBAG2_TYPE_DATA;
        option.pstrName = const_cast<LPOLESTR>(L"ImageQuality");
        VARIANT value;
        VariantInit(&value);
        value.vt = VT_R4;
        value.fltVal = std::clamp(quality, 1, 100) / 100.0f;
        if (props) {
            props->Write(1, &option, &value);
        }
        VariantClear(&value);

        hr = frame->Initialize(props.get());
        if (FAILED(hr)) {
            error = "initialize JPEG frame failed: " + hresult_message(hr);
            return false;
        }
        frame->SetSize(width_, height_);

        WICPixelFormatGUID pixel_format = GUID_WICPixelFormat24bppBGR;
        hr = frame->SetPixelFormat(&pixel_format);
        if (FAILED(hr) || !IsEqualGUID(pixel_format, GUID_WICPixelFormat24bppBGR)) {
            error = "set JPEG pixel format failed";
            return false;
        }

        hr = frame->WritePixels(height_, jpeg_stride, static_cast<UINT>(bgr24.size()), bgr24.data());
        if (FAILED(hr)) {
            error = "write JPEG pixels failed: " + hresult_message(hr);
            return false;
        }
        frame->Commit();
        encoder->Commit();

        LARGE_INTEGER zero = {};
        ULARGE_INTEGER new_position = {};
        stream->Seek(zero, STREAM_SEEK_SET, &new_position);

        STATSTG stat = {};
        hr = stream->Stat(&stat, STATFLAG_NONAME);
        if (FAILED(hr) || stat.cbSize.QuadPart <= 0) {
            error = "read JPEG size failed: " + hresult_message(hr);
            return false;
        }

        jpeg.resize(static_cast<size_t>(stat.cbSize.QuadPart));
        ULONG bytes_read = 0;
        hr = stream->Read(jpeg.data(), static_cast<ULONG>(jpeg.size()), &bytes_read);
        if (FAILED(hr) || bytes_read != jpeg.size()) {
            error = "read JPEG memory stream failed: " + hresult_message(hr);
            return false;
        }
        return true;
    }

    std::mutex mutex_;
    ComPtr<IMFMediaSource> source_;
    ComPtr<IMFSourceReader> reader_;
    UINT32 width_ = 0;
    UINT32 height_ = 0;
};

class CameraManager {
public:
    bool capture_jpeg(int index, int quality, std::vector<std::uint8_t> &jpeg, std::string &error) {
        std::shared_ptr<Slot> slot;
        {
            std::lock_guard<std::mutex> guard(mutex_);
            auto found = slots_.find(index);
            if (found == slots_.end()) {
                auto device = std::make_shared<CameraDevice>();
                if (!device->open(index, error)) {
                    return false;
                }
                slot = std::make_shared<Slot>();
                slot->device = device;
                slots_[index] = slot;
            } else {
                slot = found->second;
            }
            slot->last_access = std::chrono::steady_clock::now();
        }

        bool ok = slot->device->capture_jpeg(quality, jpeg, error);
        slot->last_access = std::chrono::steady_clock::now();
        return ok;
    }

    void release(int index) {
        std::lock_guard<std::mutex> guard(mutex_);
        slots_.erase(index);
        ++release_versions_[index];
    }

    std::uint64_t release_token(int index) {
        std::lock_guard<std::mutex> guard(mutex_);
        return release_versions_[index];
    }

    bool released_since(int index, std::uint64_t token) {
        std::lock_guard<std::mutex> guard(mutex_);
        auto found = release_versions_.find(index);
        std::uint64_t current = found == release_versions_.end() ? 0 : found->second;
        return current != token;
    }

    void release_idle(int idle_seconds) {
        if (idle_seconds <= 0) {
            return;
        }
        std::lock_guard<std::mutex> guard(mutex_);
        auto now = std::chrono::steady_clock::now();
        for (auto it = slots_.begin(); it != slots_.end();) {
            auto idle_for = std::chrono::duration_cast<std::chrono::seconds>(now - it->second->last_access).count();
            if (idle_for > idle_seconds) {
                it = slots_.erase(it);
            } else {
                ++it;
            }
        }
    }

private:
    struct Slot {
        std::shared_ptr<CameraDevice> device;
        std::chrono::steady_clock::time_point last_access = std::chrono::steady_clock::now();
    };

    std::mutex mutex_;
    std::map<int, std::shared_ptr<Slot>> slots_;
    std::map<int, std::uint64_t> release_versions_;
};

CameraManager g_camera_manager;

std::string status_text(int status) {
    switch (status) {
    case 200: return "OK";
    case 204: return "No Content";
    case 400: return "Bad Request";
    case 404: return "Not Found";
    case 405: return "Method Not Allowed";
    case 500: return "Internal Server Error";
    default: return "OK";
    }
}

bool send_all(SOCKET socket, const char *data, size_t size) {
    size_t sent = 0;
    while (sent < size) {
        int chunk = static_cast<int>(std::min<size_t>(size - sent, 64 * 1024));
        int result = send(socket, data + sent, chunk, 0);
        if (result <= 0) {
            return false;
        }
        sent += static_cast<size_t>(result);
    }
    return true;
}

bool send_all(SOCKET socket, const std::string &data) {
    return send_all(socket, data.data(), data.size());
}

void send_response(SOCKET socket, int status, const std::string &content_type, const std::string &body) {
    std::ostringstream header;
    header << "HTTP/1.1 " << status << ' ' << status_text(status) << "\r\n"
           << "Content-Type: " << content_type << "\r\n"
           << "Content-Length: " << body.size() << "\r\n"
           << "Connection: close\r\n"
           << "Access-Control-Allow-Origin: *\r\n"
           << "Access-Control-Allow-Methods: GET,POST,OPTIONS\r\n"
           << "Access-Control-Allow-Headers: Content-Type\r\n"
           << "\r\n";
    send_all(socket, header.str());
    if (!body.empty()) {
        send_all(socket, body);
    }
}

void send_json(SOCKET socket, int status, const std::string &body) {
    send_response(socket, status, "application/json; charset=utf-8", body);
}

void send_error(SOCKET socket, int status, const std::string &message) {
    send_json(socket, status, "{\"detail\":\"" + json_escape(message) + "\"}");
}

struct HttpRequest {
    std::string method;
    std::string path;
};

bool read_request(SOCKET socket, HttpRequest &request) {
    std::string data;
    char buffer[2048];
    while (data.find("\r\n\r\n") == std::string::npos && data.size() < 16384) {
        int received = recv(socket, buffer, sizeof(buffer), 0);
        if (received <= 0) {
            return false;
        }
        data.append(buffer, received);
    }

    std::istringstream stream(data);
    std::string target;
    std::string version;
    stream >> request.method >> target >> version;
    if (request.method.empty() || target.empty()) {
        return false;
    }
    size_t query = target.find('?');
    request.path = query == std::string::npos ? target : target.substr(0, query);
    return true;
}

bool parse_int(const std::string &text, int &value) {
    if (text.empty() || !std::all_of(text.begin(), text.end(), [](unsigned char ch) { return std::isdigit(ch) != 0; })) {
        return false;
    }
    try {
        value = std::stoi(text);
        return value >= 0;
    } catch (...) {
        return false;
    }
}

bool starts_with(const std::string &value, const std::string &prefix) {
    return value.size() >= prefix.size() && value.compare(0, prefix.size(), prefix) == 0;
}

bool parse_index_route(const std::string &path, const std::string &prefix, int &index) {
    if (!starts_with(path, prefix)) {
        return false;
    }
    std::string rest = path.substr(prefix.size());
    if (rest.empty() || rest.find('/') != std::string::npos) {
        return false;
    }
    return parse_int(rest, index);
}

std::string agent_info_json(const Config &config) {
    std::string host = config.advertise_host.empty() ? host_name() : config.advertise_host;
    auto cameras = enumerate_cameras();
    std::ostringstream out;
    out << "{\"service\":\"retail-camera-agent\","
        << "\"version\":\"" << kVersion << "\","
        << "\"host\":\"" << json_escape(host) << "\","
        << "\"port\":" << config.port << ","
        << "\"available_indexes\":[";
    for (size_t i = 0; i < cameras.size(); ++i) {
        if (i > 0) {
            out << ',';
        }
        out << cameras[i].index;
    }
    out << "],\"cameras\":[";
    for (size_t i = 0; i < cameras.size(); ++i) {
        if (i > 0) {
            out << ',';
        }
        out << "{\"id\":\"agent:" << json_escape(host) << ':' << config.port << ':' << cameras[i].index << "\","
            << "\"index\":" << cameras[i].index << ","
            << "\"name\":\"" << json_escape(cameras[i].name) << "\"}";
    }
    out << "]}";
    return out.str();
}

void handle_frame(SOCKET socket, int index, const Config &config) {
    std::vector<std::uint8_t> jpeg;
    std::string error;
    if (!g_camera_manager.capture_jpeg(index, config.quality, jpeg, error)) {
        send_error(socket, error.find("not available") != std::string::npos ? 404 : 500, error);
        return;
    }

    std::ostringstream body;
    body << "{\"camera_index\":" << index << ",\"image_base64\":\"" << base64_encode(jpeg) << "\"}";
    send_json(socket, 200, body.str());
}

void handle_stream(SOCKET socket, int index, const Config &config) {
    std::ostringstream header;
    header << "HTTP/1.1 200 OK\r\n"
           << "Content-Type: multipart/x-mixed-replace; boundary=frame\r\n"
           << "Cache-Control: no-cache, no-store, must-revalidate\r\n"
           << "Connection: close\r\n"
           << "Access-Control-Allow-Origin: *\r\n"
           << "\r\n";
    if (!send_all(socket, header.str())) {
        return;
    }

    std::uint64_t release_token = g_camera_manager.release_token(index);
    int delay_ms = config.fps > 0 ? std::max(1, 1000 / config.fps) : 125;
    while (g_running.load() && !g_camera_manager.released_since(index, release_token)) {
        std::vector<std::uint8_t> jpeg;
        std::string error;
        if (!g_camera_manager.capture_jpeg(index, config.quality, jpeg, error)) {
            break;
        }

        std::ostringstream part;
        part << "--frame\r\n"
             << "Content-Type: image/jpeg\r\n"
             << "Content-Length: " << jpeg.size() << "\r\n\r\n";
        if (!send_all(socket, part.str()) ||
            !send_all(socket, reinterpret_cast<const char *>(jpeg.data()), jpeg.size()) ||
            !send_all(socket, "\r\n", 2)) {
            break;
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(delay_ms));
    }
}

void handle_client(SOCKET client, Config config) {
    ComInit com;
    if (!com.ok()) {
        send_error(client, 500, "COM initialization failed");
        closesocket(client);
        return;
    }

    HttpRequest request;
    if (!read_request(client, request)) {
        closesocket(client);
        return;
    }

    if (request.method == "OPTIONS") {
        send_response(client, 204, "text/plain", "");
        closesocket(client);
        return;
    }

    if (request.method == "GET" && request.path == "/api/agent/health") {
        send_json(client, 200, "{\"service\":\"retail-camera-agent\",\"version\":\"" + std::string(kVersion) + "\",\"status\":\"ok\"}");
    } else if (request.method == "GET" && request.path == "/api/agent/cameras/available") {
        send_json(client, 200, agent_info_json(config));
    } else if (request.method == "GET") {
        int index = -1;
        if (parse_index_route(request.path, "/api/agent/cameras/frame/", index)) {
            handle_frame(client, index, config);
        } else if (parse_index_route(request.path, "/api/agent/cameras/stream/", index)) {
            handle_stream(client, index, config);
        } else {
            send_error(client, 404, "not found");
        }
    } else if (request.method == "POST") {
        const std::string stream_prefix = "/api/agent/cameras/stream/";
        const std::string stop_suffix = "/stop";
        if (starts_with(request.path, stream_prefix) &&
            request.path.size() > stream_prefix.size() + stop_suffix.size() &&
            request.path.rfind(stop_suffix) == request.path.size() - stop_suffix.size()) {
            std::string index_text = request.path.substr(
                stream_prefix.size(),
                request.path.size() - stream_prefix.size() - stop_suffix.size());
            int index = -1;
            if (parse_int(index_text, index)) {
                g_camera_manager.release(index);
                send_json(client, 200, "{\"detail\":\"released\"}");
            } else {
                send_error(client, 400, "invalid camera index");
            }
        } else {
            send_error(client, 404, "not found");
        }
    } else {
        send_error(client, 405, "method not allowed");
    }

    closesocket(client);
}

void discovery_loop(Config config) {
    ComInit com;
    SOCKET socket_fd = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
    if (socket_fd == INVALID_SOCKET) {
        std::cerr << "UDP discovery socket failed\n";
        return;
    }

    BOOL reuse = TRUE;
    setsockopt(socket_fd, SOL_SOCKET, SO_REUSEADDR, reinterpret_cast<const char *>(&reuse), sizeof(reuse));

    sockaddr_in address = {};
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = htonl(INADDR_ANY);
    address.sin_port = htons(static_cast<u_short>(config.discovery_port));
    if (bind(socket_fd, reinterpret_cast<sockaddr *>(&address), sizeof(address)) == SOCKET_ERROR) {
        std::cerr << "UDP discovery bind failed on port " << config.discovery_port << "\n";
        closesocket(socket_fd);
        return;
    }

    char buffer[1024];
    while (g_running.load()) {
        sockaddr_in sender = {};
        int sender_len = sizeof(sender);
        int received = recvfrom(socket_fd, buffer, sizeof(buffer) - 1, 0,
                                reinterpret_cast<sockaddr *>(&sender), &sender_len);
        if (received <= 0) {
            continue;
        }
        buffer[received] = '\0';
        if (std::string(buffer, received).find(kDiscoveryMessage) == std::string::npos) {
            continue;
        }

        std::string body = agent_info_json(config);
        sendto(socket_fd, body.data(), static_cast<int>(body.size()), 0,
               reinterpret_cast<sockaddr *>(&sender), sender_len);
    }
    closesocket(socket_fd);
}

void reaper_loop(Config config) {
    while (g_running.load()) {
        std::this_thread::sleep_for(std::chrono::seconds(5));
        g_camera_manager.release_idle(config.idle_seconds);
    }
}

BOOL WINAPI console_handler(DWORD control_type) {
    if (control_type == CTRL_C_EVENT || control_type == CTRL_CLOSE_EVENT ||
        control_type == CTRL_BREAK_EVENT || control_type == CTRL_SHUTDOWN_EVENT) {
        g_running.store(false);
        if (g_listen_socket != INVALID_SOCKET) {
            closesocket(g_listen_socket);
        }
        return TRUE;
    }
    return FALSE;
}

void print_help() {
    std::cout << "retail-camera-agent " << kVersion << "\n"
              << "Usage: retail-camera-agent.exe [options]\n\n"
              << "Options:\n"
              << "  --port <port>             HTTP port, default 8765\n"
              << "  --discovery-port <port>   UDP discovery port, default 8766\n"
              << "  --quality <1-100>         JPEG quality, default 80\n"
              << "  --fps <fps>               MJPEG max FPS, default 8\n"
              << "  --idle-seconds <seconds>  Release idle camera, default 60\n"
              << "  --advertise-host <host>   Host shown in /available response\n"
              << "  --no-discovery            Disable UDP discovery\n"
              << "  --help                    Show this help\n";
}

void print_startup_summary(const Config &config) {
    auto ips = local_ipv4_addresses();
    auto cameras = enumerate_cameras();

    std::cout << "\n============================================================\n";
    std::cout << " Retail Camera Agent " << kVersion << " is running\n";
    std::cout << " Keep this window open while the main server uses cameras.\n";
    std::cout << "============================================================\n\n";

    std::cout << "HTTP port: " << config.port << "\n";
    std::cout << "UDP discovery: "
              << (config.discovery_enabled ? std::to_string(config.discovery_port) : "disabled")
              << "\n\n";

    std::cout << "This computer IP / service URLs:\n";
    for (const auto &ip : ips) {
        std::cout << "  http://" << ip << ":" << config.port << "\n";
    }

    std::cout << "\nCamera IDs to fill in the main admin page:\n";
    if (cameras.empty()) {
        std::cout << "  No camera was found on this computer.\n";
    } else {
        for (const auto &camera : cameras) {
            std::cout << "  [" << camera.name << "]\n";
            for (const auto &ip : ips) {
                std::cout << "    agent:" << ip << ":" << config.port << ":" << camera.index << "\n";
            }
        }
    }

    std::cout << "\nMain admin page can also use: Add Camera -> Agent Scan.\n";
    std::cout << "Press Ctrl+C or close this window to stop the agent.\n";
    std::cout << "============================================================\n\n";
    std::cout << std::flush;
}

bool read_int_arg(int argc, char **argv, int &i, int &target) {
    if (i + 1 >= argc) {
        return false;
    }
    try {
        target = std::stoi(argv[++i]);
        return true;
    } catch (...) {
        return false;
    }
}

bool parse_args(int argc, char **argv, Config &config) {
    for (int i = 1; i < argc; ++i) {
        std::string arg = argv[i];
        if (arg == "--help" || arg == "-h") {
            print_help();
            return false;
        } else if (arg == "--port") {
            if (!read_int_arg(argc, argv, i, config.port)) return false;
        } else if (arg == "--discovery-port") {
            if (!read_int_arg(argc, argv, i, config.discovery_port)) return false;
        } else if (arg == "--quality") {
            if (!read_int_arg(argc, argv, i, config.quality)) return false;
            config.quality = std::clamp(config.quality, 1, 100);
        } else if (arg == "--fps") {
            if (!read_int_arg(argc, argv, i, config.fps)) return false;
            config.fps = std::clamp(config.fps, 1, 60);
        } else if (arg == "--idle-seconds") {
            if (!read_int_arg(argc, argv, i, config.idle_seconds)) return false;
        } else if (arg == "--advertise-host") {
            if (i + 1 >= argc) return false;
            config.advertise_host = argv[++i];
        } else if (arg == "--no-discovery") {
            config.discovery_enabled = false;
        } else {
            std::cerr << "Unknown option: " << arg << "\n";
            return false;
        }
    }
    return config.port > 0 && config.port <= 65535 &&
           config.discovery_port > 0 && config.discovery_port <= 65535;
}

} // namespace

int main(int argc, char **argv) {
    SetConsoleOutputCP(CP_UTF8);
    SetConsoleCP(CP_UTF8);

    Config config;
    if (!parse_args(argc, argv, config)) {
        return 1;
    }

    SetConsoleCtrlHandler(console_handler, TRUE);

    ComInit com;
    if (!com.ok()) {
        std::cerr << "COM initialization failed\n";
        return 1;
    }

    HRESULT hr = MFStartup(MF_VERSION);
    if (FAILED(hr)) {
        std::cerr << "Media Foundation startup failed: " << hresult_message(hr) << "\n";
        return 1;
    }

    WSADATA wsa = {};
    if (WSAStartup(MAKEWORD(2, 2), &wsa) != 0) {
        std::cerr << "WSAStartup failed\n";
        MFShutdown();
        return 1;
    }

    g_listen_socket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (g_listen_socket == INVALID_SOCKET) {
        std::cerr << "HTTP socket failed\n";
        WSACleanup();
        MFShutdown();
        return 1;
    }

    BOOL reuse = TRUE;
    setsockopt(g_listen_socket, SOL_SOCKET, SO_REUSEADDR, reinterpret_cast<const char *>(&reuse), sizeof(reuse));

    sockaddr_in address = {};
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = htonl(INADDR_ANY);
    address.sin_port = htons(static_cast<u_short>(config.port));
    if (bind(g_listen_socket, reinterpret_cast<sockaddr *>(&address), sizeof(address)) == SOCKET_ERROR) {
        std::cerr << "HTTP bind failed on port " << config.port << "\n";
        closesocket(g_listen_socket);
        WSACleanup();
        MFShutdown();
        return 1;
    }

    if (listen(g_listen_socket, SOMAXCONN) == SOCKET_ERROR) {
        std::cerr << "HTTP listen failed\n";
        closesocket(g_listen_socket);
        WSACleanup();
        MFShutdown();
        return 1;
    }

    std::thread reaper(reaper_loop, config);
    reaper.detach();

    if (config.discovery_enabled) {
        std::thread(discovery_loop, config).detach();
    }

    print_startup_summary(config);

    while (g_running.load()) {
        sockaddr_in client_address = {};
        int client_len = sizeof(client_address);
        SOCKET client = accept(g_listen_socket, reinterpret_cast<sockaddr *>(&client_address), &client_len);
        if (client == INVALID_SOCKET) {
            if (!g_running.load()) {
                break;
            }
            continue;
        }
        std::thread(handle_client, client, config).detach();
    }

    g_running.store(false);
    if (g_listen_socket != INVALID_SOCKET) {
        closesocket(g_listen_socket);
    }
    WSACleanup();
    MFShutdown();
    return 0;
}
