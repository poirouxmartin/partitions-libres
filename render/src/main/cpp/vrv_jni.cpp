// JNI bridge: one vrv::Toolkit per handle, called from com.musique.render.Verovio.
#include <jni.h>

#include <string>

#include "toolkit.h"

namespace {

std::string ToStd(JNIEnv *env, jstring s)
{
    const char *chars = env->GetStringUTFChars(s, nullptr);
    std::string out(chars);
    env->ReleaseStringUTFChars(s, chars);
    return out;
}

vrv::Toolkit *Tk(jlong handle)
{
    return reinterpret_cast<vrv::Toolkit *>(handle);
}

} // namespace

extern "C" {

JNIEXPORT jlong JNICALL Java_com_musique_render_Verovio_nCreate(JNIEnv *env, jclass, jstring resourcePath)
{
    // initFont=false: the compile-time RESOURCE_DIR does not exist on the device.
    auto *tk = new vrv::Toolkit(false);
    if (!tk->SetResourcePath(ToStd(env, resourcePath))) {
        delete tk;
        return 0;
    }
    return reinterpret_cast<jlong>(tk);
}

JNIEXPORT void JNICALL Java_com_musique_render_Verovio_nDestroy(JNIEnv *, jclass, jlong handle)
{
    delete Tk(handle);
}

JNIEXPORT jstring JNICALL Java_com_musique_render_Verovio_nVersion(JNIEnv *env, jclass, jlong handle)
{
    return env->NewStringUTF(Tk(handle)->GetVersion().c_str());
}

JNIEXPORT jboolean JNICALL Java_com_musique_render_Verovio_nSetOptions(JNIEnv *env, jclass, jlong handle, jstring json)
{
    return Tk(handle)->SetOptions(ToStd(env, json));
}

JNIEXPORT jboolean JNICALL Java_com_musique_render_Verovio_nLoadData(JNIEnv *env, jclass, jlong handle, jstring data)
{
    return Tk(handle)->LoadData(ToStd(env, data));
}

JNIEXPORT jint JNICALL Java_com_musique_render_Verovio_nPageCount(JNIEnv *, jclass, jlong handle)
{
    return Tk(handle)->GetPageCount();
}

JNIEXPORT jstring JNICALL Java_com_musique_render_Verovio_nRenderToSvg(JNIEnv *env, jclass, jlong handle, jint page)
{
    return env->NewStringUTF(Tk(handle)->RenderToSVG(page, false).c_str());
}

} // extern "C"
