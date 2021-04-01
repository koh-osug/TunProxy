project(TunProxy CXX)
cmake_minimum_required(VERSION 3.10.2)

include_directories(.)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp)
set(EXECUTABLE_OUTPUT_PATH "${CMAKE_CURRENT_SOURCE_DIR}/src/main/assets/${ANDROID_ABI}")

add_library( # Sets the name of the library.
        tun2http

        # Sets the library as a shared library.
        SHARED

        src/main/cpp/dhcp.cpp
        src/main/cpp/dns.cpp
        src/main/cpp/icmp.cpp
        src/main/cpp/ip.cpp
        src/main/cpp/http.cpp
        src/main/cpp/tun2http.cpp
        src/main/cpp/session.cpp
        src/main/cpp/tcp.cpp
        src/main/cpp/tls.cpp
        src/main/cpp/udp.cpp
        src/main/cpp/util.cpp
        )

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

target_link_libraries( # Specifies the target library.
        tun2http
        ${log-lib}
        )

set(TEST_SRC_DIR ${CMAKE_CURRENT_SOURCE_DIR}/src/test/cpp)
include(${CMAKE_CURRENT_SOURCE_DIR}/src/test/cpp/CMakeLists.txt)
