@echo off
"C:\\Users\\FotosFrangoudes\\AppData\\Local\\Android\\Sdk\\cmake\\3.18.1\\bin\\cmake.exe" ^
  "-HC:\\Users\\FotosFrangoudes\\Downloads\\Reinherit\\openCvSdk460\\libcxx_helper" ^
  "-DCMAKE_SYSTEM_NAME=Android" ^
  "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON" ^
  "-DCMAKE_SYSTEM_VERSION=21" ^
  "-DANDROID_PLATFORM=android-21" ^
  "-DANDROID_ABI=x86" ^
  "-DCMAKE_ANDROID_ARCH_ABI=x86" ^
  "-DANDROID_NDK=C:\\Users\\FotosFrangoudes\\AppData\\Local\\Android\\Sdk\\ndk\\23.1.7779620" ^
  "-DCMAKE_ANDROID_NDK=C:\\Users\\FotosFrangoudes\\AppData\\Local\\Android\\Sdk\\ndk\\23.1.7779620" ^
  "-DCMAKE_TOOLCHAIN_FILE=C:\\Users\\FotosFrangoudes\\AppData\\Local\\Android\\Sdk\\ndk\\23.1.7779620\\build\\cmake\\android.toolchain.cmake" ^
  "-DCMAKE_MAKE_PROGRAM=C:\\Users\\FotosFrangoudes\\AppData\\Local\\Android\\Sdk\\cmake\\3.18.1\\bin\\ninja.exe" ^
  "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=C:\\Users\\FotosFrangoudes\\Downloads\\Reinherit\\openCvSdk460\\build\\intermediates\\cxx\\RelWithDebInfo\\5k2xg5m4\\obj\\x86" ^
  "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=C:\\Users\\FotosFrangoudes\\Downloads\\Reinherit\\openCvSdk460\\build\\intermediates\\cxx\\RelWithDebInfo\\5k2xg5m4\\obj\\x86" ^
  "-DCMAKE_BUILD_TYPE=RelWithDebInfo" ^
  "-BC:\\Users\\FotosFrangoudes\\Downloads\\Reinherit\\openCvSdk460\\.cxx\\RelWithDebInfo\\5k2xg5m4\\x86" ^
  -GNinja ^
  "-DANDROID_STL=c++_shared"
