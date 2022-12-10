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
/*
 * author Tomas Hurka
 *        Ian Formanek
 *        Misha Dmitriev
 */

#ifdef WIN32
#include <Windows.h>
#else
#include <sys/time.h>
#include <fcntl.h>
#include <time.h>
#endif

#ifdef SOLARIS
#define _STRUCTURED_PROC 1
#include <sys/procfs.h>
#endif

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <string.h>

#include "jni.h"
#include "jvmti.h"

#include "org_graalvm_visualvm_lib_jfluid_server_system_Stacks.h"

#include "common_functions.h"
#include "Threads.h"

#define NEEDS_CONVERSION (sizeof(jmethodID)!=sizeof(jint))
#define NO_OF_BASE_BITS 2
#define NO_OF_MASK_BITS (32-NO_OF_BASE_BITS)
#define NO_OF_BASE_ADDRESS (1<<NO_OF_BASE_BITS)
#define OFFSET_MASK ((1LL<<NO_OF_MASK_BITS)-1)
#define BASE_ADDRESS_MASK (~OFFSET_MASK)

#define MAX_FRAMES 16384

#define PACKEDARR_ITEMS 4

static jvmtiFrameInfo *_stack_frames_buffer = NULL;
static jint *_stack_id_buffer = NULL;
static jclass threadType = NULL;
static jclass intArrType = NULL;
static long long base_addresses[NO_OF_BASE_ADDRESS]={-1LL,-1LL,-1LL,-1LL};

static jint convert_jmethodID_to_jint(jmethodID jmethod) {
    if (NEEDS_CONVERSION) {
        long long base_address=(long long)jmethod&BASE_ADDRESS_MASK;
        unsigned int i;

        for (i=0;i<NO_OF_BASE_ADDRESS;i++) {
            if (base_addresses[i] == -1LL) {
                base_addresses[i] = base_address;
                //fprintf(stderr,"Profiler Agent: Registering new base %llx\n",base_address);
            }
            if (base_addresses[i]==base_address) {
                jint offset = (long long)jmethod&OFFSET_MASK;
                offset |= i<<NO_OF_MASK_BITS;
                //fprintf(stderr,"M %p -> %x\n",jmethod,offset);
                return offset;
            }
        }
        fprintf(stderr,"Profiler Agent Warning: Cannot convert %p\n",jmethod);
        return 0;
    } else {
        return (jint)jmethod;
    }
}

static jmethodID convert_jint_to_jmethodID(jint method) {
    if (NEEDS_CONVERSION) {
        int offset = method&OFFSET_MASK;
        int base_id = ((unsigned int)method)>>NO_OF_MASK_BITS;
        jmethodID jmethod = (jmethodID)(base_addresses[base_id]|offset);

        //fprintf(stderr,"X %x -> %p\n",method,jmethod);
        //fflush(stderr);
        return jmethod;
    } else {
        return (jmethodID)method;
    }
}

/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_Stacks
 * Method:    getCurrentJavaStackDepth
 * Signature: (Ljava/lang/Thread;)I
 */
JNIEXPORT jint JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_Stacks_getCurrentJavaStackDepth
    (JNIEnv *env, jclass clz, jobject jni_thread)
{
    jint count;

    (*_jvmti)->GetFrameCount(_jvmti, jni_thread, &count);
    return count;
}


/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_Stacks
 * Method:    createNativeStackFrameBuffer
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_Stacks_createNativeStackFrameBuffer
    (JNIEnv *env, jclass clz, jint sizeInFrames)
{
    if (_stack_frames_buffer != NULL) {
        Java_org_graalvm_visualvm_lib_jfluid_server_system_Stacks_clearNativeStackFrameBuffer(env, clz);
    }
    _stack_frames_buffer = calloc(sizeInFrames, sizeof(jvmtiFrameInfo));
    _stack_id_buffer = calloc(sizeInFrames, sizeof(jint));
}


/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_Stacks
 * Method:    clearNativeStackFrameBuffer
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_Stacks_clearNativeStackFrameBuffer
    (JNIEnv *env, jclass clz)
{
    if (_stack_frames_buffer != NULL) {
        free(_stack_frames_buffer);
    }
    if (_stack_id_buffer != NULL) {
        free(_stack_id_buffer);
    }
    _stack_frames_buffer = NULL;
    _stack_id_buffer = NULL;
}


/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_Stacks
 * Method:    getCurrentStackFrameIds
 * Signature: (Ljava/lang/Thread;I[I)I
 */
JNIEXPORT jint JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_Stacks_getCurrentStackFrameIds
    (JNIEnv *env, jclass clz, jthread jni_thread, jint depth, jintArray ret)
{
    jint i, count;
    if (_stack_frames_buffer == NULL) {
        /* Can happen if profiling stopped concurrently */
        return 0;
    }

    (*_jvmti)->GetStackTrace(_jvmti, jni_thread, 0, depth, _stack_frames_buffer, &count);

    for (i = 0; i < count; i++) {
        _stack_id_buffer[i] = convert_jmethodID_to_jint(_stack_frames_buffer[i].method);
    }
    (*env)->SetIntArrayRegion(env, ret, 0, count, _stack_id_buffer);

    return count;
}


static jbyte *byteData;
static jint *strOffsets;
static int byteDataLen, dataOfs, ofsIdx;

static void copy_into_data_array(char *s) {
    int len = strlen(s);
    if (dataOfs + len > byteDataLen) {
        jbyte *oldByteData = byteData;
        int newLen = byteDataLen * 2;

        if (newLen < dataOfs + len) {
          newLen = dataOfs+len;
        }
        byteData = malloc(newLen);
        memcpy(byteData, oldByteData, dataOfs);
        free(oldByteData);
        byteDataLen = newLen;
    }

    strncpy((char*)(byteData + dataOfs), s, len);
    strOffsets[ofsIdx++] = dataOfs;
    dataOfs += len;
}

static void copy_dummy_names_into_data_array() {
    copy_into_data_array("<unknown class>");
    copy_into_data_array("<unknown method>");
    copy_into_data_array("()V");
    copy_into_data_array("0");
}


/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_Stacks
 * Method:    getMethodNamesForJMethodIds
 * Signature: (I[I[I)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_Stacks_getMethodNamesForJMethodIds
  (JNIEnv *env, jclass clz, jint nMethods, jintArray jmethodIds, jintArray packedArrayOffsets)
{
    jvmtiError res;
    int i, len;
    jint *methodIds;
    jbyteArray ret;

    // fprintf (stderr, "1");
    methodIds = (jint*) malloc(sizeof(jint) * nMethods);
    (*env)->GetIntArrayRegion(env, jmethodIds, 0, nMethods, methodIds);
    strOffsets = (jint*) malloc(sizeof(jint) * nMethods * PACKEDARR_ITEMS);
    byteDataLen = nMethods * PACKEDARR_ITEMS * 10;  /* The initial size for the packed strings array */
    byteData = (jbyte*) malloc(byteDataLen);

    // fprintf (stderr, "2");
    dataOfs = ofsIdx = 0;

    for (i = 0; i < nMethods; i++) {
        jclass declaringClass;
        char *className, *genericSignature, *methodName, *methodSig, *genericMethodSig;
        jboolean native = JNI_FALSE;
        jmethodID methodID = convert_jint_to_jmethodID(methodIds[i]);

        //fprintf (stderr, "Going to call GetMethodDeclaringClass for methodId = %d\n", *(int*)methodID);

        res = (*_jvmti)->GetMethodDeclaringClass(_jvmti, methodID, &declaringClass);
        if (res != JVMTI_ERROR_NONE || declaringClass == NULL || *((int*)declaringClass) == 0) { /* Also a bug workaround */
            fprintf(stderr, "Profiler Agent Warning: Invalid declaringClass obtained from jmethodID\n");
            fprintf(stderr, "Profiler Agent Warning: mId = %p, *mId = %d\n", methodID, *(int*)methodID);
            fprintf(stderr, "Profiler Agent Warning: dCl = %p", declaringClass);
            if (declaringClass != NULL) {
                fprintf(stderr, ", *dCl = %d\n", *((int*)declaringClass));
            } else {
                fprintf(stderr, "\n");
            }
            // fprintf(stderr, "*** res = %d", res);
            copy_dummy_names_into_data_array();
            continue;
        }

        // fprintf (stderr, "Going to call GetClassSignature for methodId = %d, last res = %d, declaring class: %d\n", *(int*)methodID, res, *((int*)declaringClass));

        res = (*_jvmti)->GetClassSignature(_jvmti, declaringClass, &className, &genericSignature);
        if (res != JVMTI_ERROR_NONE) {
            fprintf(stderr, "Profiler Agent Warning: Couldn't obtain name of declaringClass = %p\n", declaringClass);
            copy_dummy_names_into_data_array();
            continue;
        }

        // fprintf (stderr, "Going to call GetMethodName for methodId = %d, last res = %d, signature: %s\n", *(int*)methodID, res, genericSignature);

        res = (*_jvmti)->GetMethodName(_jvmti, methodID, &methodName, &methodSig, &genericMethodSig);

        if (res != JVMTI_ERROR_NONE) {
            fprintf(stderr, "Profiler Agent Warning: Couldn't obtain name for methodID = %p\n", methodID);
            copy_dummy_names_into_data_array();
            continue;
        }

        // fprintf (stderr, "Going to call IsMethodNative for methodId = %d, last res = %d, signature: %s\n", *(int*)methodID, res, genericSignature);
        
        res = (*_jvmti)->IsMethodNative(_jvmti, methodID, &native);
        
        if (res != JVMTI_ERROR_NONE) {
            fprintf(stderr, "Profiler Agent Warning: Couldn't obtain native flag for methodID = %p\n", methodID);
        }

        // fprintf (stderr, "Going to copy results, last res = %d, method name: %s, sig: %s, genSig: %s, native %d\n", res, methodName, methodSig, genericMethodSig, native);

        len = strlen(className);
        if (className[0] == 'L' && className[len-1] == ';') {
            className[len-1] = 0;
            copy_into_data_array(className+1);
        } else {
            copy_into_data_array(className);
        }

        copy_into_data_array(methodName);
        copy_into_data_array(methodSig);
        copy_into_data_array(native?"1":"0");

        (*_jvmti)->Deallocate(_jvmti, (void*)className);

        if (genericSignature != NULL) {
            (*_jvmti)->Deallocate(_jvmti, (void*)genericSignature);
        }

        (*_jvmti)->Deallocate(_jvmti, (void*)methodName);
        (*_jvmti)->Deallocate(_jvmti, (void*)methodSig);
        if (genericMethodSig != NULL) {
            (*_jvmti)->Deallocate(_jvmti, (void*)genericMethodSig);
        }
    }

    // fprintf (stderr, "3");
    free(methodIds);

    ret = (*env)->NewByteArray(env, dataOfs);
    (*env)->SetByteArrayRegion(env, ret, 0, dataOfs, byteData);
    (*env)->SetIntArrayRegion(env, packedArrayOffsets, 0, nMethods*PACKEDARR_ITEMS, strOffsets);

    // fprintf (stderr, "4");
    free(strOffsets);
    free(byteData);

    return ret;
}


/*
 * Class:     org_graalvm_visualvm_lib_jfluid_server_system_Stacks
 * Method:    getAllStackTraces
 * Signature: ([[Ljava/lang/Thread;[[I[[[I)V
 */
JNIEXPORT void JNICALL Java_org_graalvm_visualvm_lib_jfluid_server_system_Stacks_getAllStackTraces
  (JNIEnv *env, jclass clz, jobjectArray threads, jobjectArray states, jobjectArray frames)
{
    jobjectArray jthreadArr;
    jobjectArray statesArr;
    jobjectArray methodIdArrArr;
    jvmtiStackInfo *stack_info;
    jint *state_buffer;
    jint thread_count;
    int ti;
    jvmtiError err;

    err = (*_jvmti)->GetAllStackTraces(_jvmti, MAX_FRAMES, &stack_info, &thread_count); 
    if (err != JVMTI_ERROR_NONE) {
       return;
    }
    if (threadType == NULL) {
        threadType = (*env)->FindClass(env, "java/lang/Thread");
        threadType = (*env)->NewGlobalRef(env, threadType);
    }
    if (intArrType == NULL) {
        intArrType = (*env)->FindClass(env, "[I");
        intArrType = (*env)->NewGlobalRef(env, intArrType);
    }
    jthreadArr = (*env)->NewObjectArray(env, thread_count, threadType, NULL);
    (*env)->SetObjectArrayElement(env, threads, 0, jthreadArr);
    statesArr = (*env)->NewIntArray(env, thread_count);
    (*env)->SetObjectArrayElement(env, states, 0, statesArr);
    methodIdArrArr = (*env)->NewObjectArray(env, thread_count, intArrType, NULL);
    (*env)->SetObjectArrayElement(env, frames, 0, methodIdArrArr);    
    state_buffer = calloc(thread_count, sizeof(jint));
    
    for (ti = 0; ti < thread_count; ti++) {
       jvmtiStackInfo *infop = &stack_info[ti];
       jthread thread = infop->thread;
       jint state = infop->state;
       jvmtiFrameInfo *frames = infop->frame_buffer;
       jobjectArray jmethodIdArr;
       jint *id_buffer;
       int fi;

       (*env)->SetObjectArrayElement(env, jthreadArr, ti, thread);
       state_buffer[ti] = convert_JVMTI_thread_status_to_jfluid_status(state);
       
       jmethodIdArr = (*env)->NewIntArray(env, infop->frame_count);
       (*env)->SetObjectArrayElement(env, methodIdArrArr, ti, jmethodIdArr);    
       id_buffer = calloc(infop->frame_count, sizeof(jint));
       for (fi = 0; fi < infop->frame_count; fi++) {
          id_buffer[fi] = convert_jmethodID_to_jint(frames[fi].method);
       }
       (*env)->SetIntArrayRegion(env, jmethodIdArr, 0, infop->frame_count, id_buffer);
       free(id_buffer);
    }
    (*env)->SetIntArrayRegion(env, statesArr, 0, thread_count, state_buffer);
    
    /* this one Deallocate call frees all data allocated by GetAllStackTraces */
    err = (*_jvmti)->Deallocate(_jvmti, (unsigned char*)stack_info);
    assert(err == JVMTI_ERROR_NONE);
    free(state_buffer);
}
