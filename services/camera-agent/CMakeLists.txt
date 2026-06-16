cmake_minimum_required(VERSION 3.20)

project(retail_camera_agent VERSION 0.1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_executable(retail-camera-agent
    src/main.cpp
)

if(MINGW)
    target_link_options(retail-camera-agent PRIVATE
        -static
        -static-libgcc
        -static-libstdc++
    )
endif()

target_link_libraries(retail-camera-agent PRIVATE
    ws2_32
    mf
    mfplat
    mfreadwrite
    mfuuid
    windowscodecs
    shlwapi
    ole32
    oleaut32
    uuid
)
