#include <jni.h>

jbyte* jstring_to_jbytes(JNIEnv *env, jstring *string) {
    const jclass class = (*env)->GetObjectClass(env, string);

    const jmethodID getBytes = (*env)->GetMethodID(
		env,
		class,
		"getBytes",
		"(Ljava/lang/String;)[B"
    );

    const jbyteArray array = (jbyteArray) (*env)->CallObjectMethod(
		env,
		string,
		getBytes,
		(*env)->NewStringUTF(env, "UTF-8")
    );

    jbyte* bytes = (*env)->GetByteArrayElements(env, array, NULL);

    (*env)->ReleaseByteArrayElements(env, array, bytes, JNI_ABORT);
    (*env)->DeleteLocalRef(env, array);
    (*env)->DeleteLocalRef(env, class);

	return bytes;
}
