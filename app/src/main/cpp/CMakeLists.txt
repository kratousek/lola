# example cmakelists:
# https://github.com/android/ndk-samples/blob/main/endless-tunnel/app/src/main/cpp/CMakeLists.txt


# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.4.1)

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add_library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.

add_library( # Specifies the name of the library.
        lolly-backend-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        TMSReader.c)



# Specifies a path to native header files.
include_directories(include/)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        lolly-backend-lib )
