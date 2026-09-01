cmake_minimum_required(VERSION 3.22.1)

project(TrebleCheck)

# Required for apps targeting SDK 36+: a 4 KB aligned .so fails to load on
# 16 KB page devices. NDK r28+ does this by default; be explicit anyway.
add_link_options(-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384)

add_library(binderdetector SHARED BinderDetector.cpp)

# No build ID, for reproducible builds.
target_link_libraries(binderdetector -Wl,--build-id=none)

find_library(log-lib log)


add_library(
        trebledetector

        SHARED

        libvintf/parse_string.cpp
        libvintf/parse_xml.cpp
        libvintf/CompatibilityMatrix.cpp
        libvintf/FileSystem.cpp
        libvintf/FQName.cpp
        libvintf/FqInstance.cpp
        libvintf/HalManifest.cpp
        libvintf/HalInterface.cpp
        libvintf/KernelConfigTypedValue.cpp
        libvintf/KernelConfigParser.cpp
        libvintf/KernelInfo.cpp
        libvintf/RuntimeInfo.cpp
        libvintf/ManifestHal.cpp
        libvintf/ManifestInstance.cpp
        libvintf/MatrixHal.cpp
        libvintf/MatrixInstance.cpp
        libvintf/MatrixKernel.cpp
        libvintf/PropertyFetcher.cpp
        libvintf/Regex.cpp
        libvintf/SystemSdk.cpp
        libvintf/TransportArch.cpp
        libvintf/VintfObject.cpp
        libvintf/XmlFile.cpp
        libvintf/utils.cpp

        libvintf/HostFileSystem.cpp
        libvintf/RuntimeInfo-host.cpp

        libvintf-shims/Apex.cpp
        libvintf-shims/android/fdsan.cpp
        libvintf-shims/android/log.cpp
        libvintf-shims/libbase/properties.cpp

        libbase/strings.cpp
        libbase/stringprintf.cpp
        libbase/logging.cpp
        libbase/threads.cpp
        libbase/errors_unix.cpp
        libbase/file.cpp
        libbase/posix_strerror_r.cpp
        libbase/parsebool.cpp

        Gki/libkver/kernel_release.cpp

        tinyxml2/tinyxml2.cpp

        hardware_interfaces/compatibility_matrices/exclude/fcm_exclude.cpp

        system_core/libutils/Errors.cpp

        TrebleDetector.cpp
)

target_compile_features(trebledetector PRIVATE cxx_std_17)
# SYSTEM: everything below is vendored AOSP or third-party code whose warnings
# are not actionable here.
target_include_directories(
        trebledetector

        SYSTEM PRIVATE

        libvintf/include
        libvintf/include-host
        libvintf-shims/include
        libbase/include
        Gki/libkver/include
        tinyxml2
        fmtlib/include
        hardware_interfaces/compatibility_matrices/exclude/include
        system_core/libutils/include
)
# libvintf/include/vintf must not go on the -I path: it holds bare headers, and
# on a case-insensitive filesystem the `#include <regex.h>` inside Regex.h would
# resolve to Regex.h itself. -iquote is searched only for quoted includes, which
# is the form libvintf actually uses. Joined form so paths with spaces survive.
target_compile_options(trebledetector PRIVATE "-iquote${CMAKE_CURRENT_LIST_DIR}/libvintf/include/vintf")

# std::iterator, deprecated in C++17, is still used by libvintf's own headers,
# which -iquote leaves outside the SYSTEM set above.
target_compile_options(trebledetector PRIVATE -Wno-deprecated-declarations)

target_compile_options(trebledetector PRIVATE -Wno-c++11-narrowing -Wno-inconsistent-missing-override -Wno-c99-designator -ffile-prefix-map=${CMAKE_CURRENT_LIST_DIR}=src -ffile-prefix-map=${CMAKE_ANDROID_NDK}=ndk -ffile-prefix-map=${CMAKE_BINARY_DIR}=build)
target_link_libraries(trebledetector PRIVATE -Wl,--build-id=none -ffile-prefix-map=${CMAKE_CURRENT_LIST_DIR}=src -ffile-prefix-map=${CMAKE_ANDROID_NDK}=ndk -ffile-prefix-map=${CMAKE_BINARY_DIR}=build ${log-lib})
