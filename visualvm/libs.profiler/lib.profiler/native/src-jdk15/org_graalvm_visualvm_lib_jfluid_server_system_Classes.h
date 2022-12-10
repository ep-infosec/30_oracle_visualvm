/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_graalvm_visualvm_lib_jfluid_server_system_Classes */

#ifndef _Included_org_graalvm_visualvm_lib_jfluid_server_system_Classes
#define _Included_org_graalvm_visualvm_lib_jfluid_server_system_Classes
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_Classes
 * Method:    getAllLoadedClasses
 * Signature: ()[Ljava/lang/Class;
 */
JNIEXPORT jobjectArray JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_Classes_getAllLoadedClasses
  (JNIEnv *, jclass);

/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_Classes
 * Method:    cacheLoadedClasses
 * Signature: ([Ljava/lang/Class;I)V
 */
JNIEXPORT void JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_Classes_cacheLoadedClasses
  (JNIEnv *, jclass, jobjectArray, jint);

/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_Classes
 * Method:    getCachedClassFileBytes
 * Signature: (Ljava/lang/Class;)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_Classes_getCachedClassFileBytes
  (JNIEnv *, jclass, jclass);

/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_Classes
 * Method:    enableClassLoadHook
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_Classes_enableClassLoadHook
  (JNIEnv *, jclass);

/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_Classes
 * Method:    disableClassLoadHook
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_Classes_disableClassLoadHook
  (JNIEnv *, jclass);

/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_Classes
 * Method:    getObjectSize
 * Signature: (Ljava/lang/Object;)J
 */
JNIEXPORT jlong JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_Classes_getObjectSize
  (JNIEnv *, jclass, jobject);

/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_Classes
 * Method:    setWaitTrackingEnabled
 * Signature: (Z)Z
 */
JNIEXPORT jboolean JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_Classes_setWaitTrackingEnabled
  (JNIEnv *, jclass, jboolean);

/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_Classes
 * Method:    setParkTrackingEnabled
 * Signature: (Z)Z
 */
JNIEXPORT jboolean JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_Classes_setParkTrackingEnabled
  (JNIEnv *, jclass, jboolean);


/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_Classes
 * Method:    setSleepTrackingEnabled
 * Signature: (Z)Z
 */
JNIEXPORT jboolean JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_Classes_setSleepTrackingEnabled
  (JNIEnv *, jclass, jboolean);

/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_Classes
 * Method:    setLockContentionMonitoringEnabled
 * Signature: (Z)Z
 */
JNIEXPORT jboolean JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_Classes_setLockContentionMonitoringEnabled
  (JNIEnv *, jclass, jboolean);

/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_Classes
 * Method:    setVMObjectAllocEnabled
 * Signature: (Z)Z
 */
JNIEXPORT jboolean JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_Classes_setVMObjectAllocEnabled
  (JNIEnv *, jclass, jboolean);

/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_Classes
 * Method:    notifyAboutClassLoaderUnloading
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_Classes_notifyAboutClassLoaderUnloading
  (JNIEnv *, jclass);

/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_Classes
 * Method:    doRedefineClasses
 * Signature: ([Ljava/lang/Class;[[B)I
 */
JNIEXPORT jint JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_Classes_doRedefineClasses
  (JNIEnv *, jclass, jobjectArray, jobjectArray);

#ifdef __cplusplus
}
#endif
#endif