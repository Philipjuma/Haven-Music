cmake_minimum_required(VERSION 3.22.1)

project("pj_haven_dsp")

add_library(
        pj_haven_dsp
        SHARED
        pj_haven_jni.cpp
        PJHavenDSP.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        pj_haven_dsp
        ${log-lib}
)
