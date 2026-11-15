package com.musique.render

/** Thin JNI wrapper around one vrv::Toolkit. Not thread-safe: call it from a single thread. */
class Verovio(resourcePath: String) : AutoCloseable {
    private var handle = nCreate(resourcePath)

    init {
        require(handle != 0L) { "Verovio: cannot load resources from $resourcePath" }
    }

    val version: String get() = nVersion(handle)

    fun setOptions(json: String): Boolean = nSetOptions(handle, json)

    fun loadData(data: String): Boolean = nLoadData(handle, data)

    fun pageCount(): Int = nPageCount(handle)

    fun renderToSvg(page: Int = 1): String = nRenderToSvg(handle, page)

    override fun close() {
        if (handle != 0L) {
            nDestroy(handle)
            handle = 0L
        }
    }

    companion object {
        init {
            System.loadLibrary("vrvjni")
        }

        @JvmStatic private external fun nCreate(resourcePath: String): Long
        @JvmStatic private external fun nDestroy(handle: Long)
        @JvmStatic private external fun nVersion(handle: Long): String
        @JvmStatic private external fun nSetOptions(handle: Long, json: String): Boolean
        @JvmStatic private external fun nLoadData(handle: Long, data: String): Boolean
        @JvmStatic private external fun nPageCount(handle: Long): Int
        @JvmStatic private external fun nRenderToSvg(handle: Long, page: Int): String
    }
}
