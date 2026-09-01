# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.22.1)

project(TrebleCheck)

# Android 15 introduced devices with a 16 KB page size, and Android 16 enforces
# 16 KB alignment for every app that targets SDK 36 or newer. A 4 KB aligned
# .so simply fails to dlopen there, which silently turns Treble detection into
# "unknown". NDK r28+ passes this by default; set it explicitly so the result
# does not depend on which NDK happens to be in use.
add_link_options(-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384)

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add_library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.
add_library( # Specifies the name of the library.
        binderdetector

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        BinderDetector.cpp
)
# Disable build ID for reproducible builds
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
target_include_directories(
        trebledetector

        PRIVATE

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
# libvintf/include/vintf holds bare headers, one of which is Regex.h. On a
# case-insensitive filesystem (Windows, and macOS by default) having that
# directory on the normal -I path makes the `#include <regex.h>` inside Regex.h
# resolve to Regex.h itself, so regex_t/regcomp/REG_EXTENDED are never
# declared and the whole library fails to compile. -iquote is searched only for
# "quoted" includes, which is the form every libvintf source actually uses, so
# the bare headers stay reachable while <regex.h> falls through to the sysroot.
# The joined -iquote<dir> form is used so paths containing spaces survive.
target_compile_options(trebledetector PRIVATE "-iquote${CMAKE_CURRENT_LIST_DIR}/libvintf/include/vintf")

target_compile_options(trebledetector PRIVATE -Wno-c++11-narrowing -Wno-inconsistent-missing-override -Wno-c99-designator -ffile-prefix-map=${CMAKE_CURRENT_LIST_DIR}=src -ffile-prefix-map=${CMAKE_ANDROID_NDK}=ndk -ffile-prefix-map=${CMAKE_BINARY_DIR}=build)
target_link_libraries(trebledetector PRIVATE -Wl,--build-id=none -ffile-prefix-map=${CMAKE_CURRENT_LIST_DIR}=src -ffile-prefix-map=${CMAKE_ANDROID_NDK}=ndk -ffile-prefix-map=${CMAKE_BINARY_DIR}=build ${log-lib})
