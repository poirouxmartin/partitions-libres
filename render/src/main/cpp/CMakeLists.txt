cmake_minimum_required(VERSION 3.15)
project(vrvjni CXX)

# Verovio's own CMake project, built as an Android shared library (libverovio.so).
# Humdrum is left out: it is the largest importer and only MEI and MusicXML are needed.
set(BUILD_AS_ANDROID_LIBRARY ON CACHE BOOL "" FORCE)
set(NO_HUMDRUM_SUPPORT ON CACHE BOOL "" FORCE)
add_subdirectory(${VRV_DIR}/cmake verovio)

add_library(vrvjni SHARED vrv_jni.cpp)
target_include_directories(vrvjni PRIVATE
    ${VRV_DIR}/include
    ${VRV_DIR}/include/vrv
    ${VRV_DIR}/include/crc
    ${VRV_DIR}/include/midi
    ${VRV_DIR}/include/json
    ${VRV_DIR}/include/pugi
    ${VRV_DIR}/include/zip
    ${VRV_DIR}/include/tuning-library
    ${VRV_DIR}/libmei/dist
    ${VRV_DIR}/libmei/addons)
target_compile_definitions(vrvjni PRIVATE NO_HUMDRUM_SUPPORT NO_DARMS_SUPPORT NO_RUNTIME)
target_link_libraries(vrvjni verovio log)
target_link_options(vrvjni PRIVATE "-Wl,-z,max-page-size=0x4000")
