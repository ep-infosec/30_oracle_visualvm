/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.java.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.java.ClassNode;
import org.graalvm.visualvm.heapviewer.java.InstanceNode;
import org.graalvm.visualvm.heapviewer.java.LocalObjectNode;
import org.graalvm.visualvm.heapviewer.java.StackFrameNode;
import org.graalvm.visualvm.heapviewer.java.ThreadNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.RootNode;
import org.graalvm.visualvm.heapviewer.utils.HeapUtils;
import org.graalvm.visualvm.lib.jfluid.heap.GCRoot;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.JavaFrameGCRoot;
import org.graalvm.visualvm.lib.jfluid.heap.JniLocalGCRoot;
import org.graalvm.visualvm.lib.jfluid.heap.ThreadObjectGCRoot;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.DetailsSupport;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "JavaThreadsProvider_LocalVariable=local variable",
    "JavaThreadsProvider_UnknownLocalVariable=unknown local variable",
    "JavaThreadsProvider_JniLocal=JNI local",
    "JavaThreadsProvider_UnknownJniLocal=unknown JNI local",
    "JavaThreadsProvider_UnknownThread=unknown thread",
    "JavaThreadsProvider_CannotResolveClassMsg=Cannot resolve class",
    "JavaThreadsProvider_CannotResolveInstanceMsg=Cannot resolve instance"
})
class JavaThreadsProvider {
    
    private static final String OPEN_THREADS_URL = "file:/stackframe/";     // NOI18N
    
    
    static String getThreadName(Instance instance) {
        String threadName = getThreadInstanceName(instance);
        Long threadId = (Long)instance.getValueOfField("tid");    // NOI18N
        Boolean daemon = (Boolean)instance.getValueOfField("daemon"); // NOI18N
        Integer priority = (Integer)instance.getValueOfField("priority"); // NOI18N
        Integer threadStatus = (Integer)instance.getValueOfField("threadStatus"); // NOI18N

        if (daemon == null) {
            Instance holder = (Instance)instance.getValueOfField("holder");  // NOI18N
            if (holder != null) {
                daemon = (Boolean)holder.getValueOfField("daemon"); // NOI18N
                priority = (Integer)holder.getValueOfField("priority"); // NOI18N
                threadStatus = (Integer)holder.getValueOfField("threadStatus"); // NOI18N
            }
        }

        String tName = "\"" + threadName + "\"" + (daemon.booleanValue() ? " daemon" : "") + " prio=" + priority; // NOI18N
        if (threadId != null) tName += " tid=" + threadId; // NOI18N
        if (threadStatus != null) tName += " " + toThreadState(threadStatus.intValue()); // NOI18N
        
        return tName;
    }
    
    static ThreadObjectGCRoot getOOMEThread(Heap heap) {
        Collection<GCRoot> roots = heap.getGCRoots();

        for (GCRoot root : roots) {
            if(root.getKind().equals(GCRoot.THREAD_OBJECT)) {
                ThreadObjectGCRoot threadRoot = (ThreadObjectGCRoot)root;
                StackTraceElement[] stackTrace = threadRoot.getStackTrace();
                
                if (stackTrace!=null && stackTrace.length>=1) {
                    StackTraceElement ste = stackTrace[0];
                    
                    if (OutOfMemoryError.class.getName().equals(ste.getClassName()) && "<init>".equals(ste.getMethodName())) {  // NOI18N
                        return threadRoot;
                    }
                }
            }
        }
        return null;
    }
    
    static HeapViewerNode getNode(URL url, HeapContext context) {
        String urls = url.toString();
                
        if (HeapUtils.isInstance(urls)) {
            final Instance instance = HeapUtils.instanceFromHtml(urls, context.getFragment().getHeap());
            if (instance != null) return new InstanceNode(instance);
            else ProfilerDialogs.displayError(Bundle.JavaThreadsProvider_CannotResolveInstanceMsg());
        } else if (HeapUtils.isClass(urls)) {
            JavaClass javaClass = HeapUtils.classFromHtml(urls, context.getFragment().getHeap());
            if (javaClass != null) return new ClassNode(javaClass);
            else ProfilerDialogs.displayError(Bundle.JavaThreadsProvider_CannotResolveClassMsg());
        }

        return null;
    }
    
        
    static HeapViewerNode[] getThreadsNodes(RootNode rootNode, Heap heap) throws InterruptedException {
        List<HeapViewerNode> threadNodes = new ArrayList();
        
        Collection<GCRoot> roots = heap.getGCRoots();
        Map<ThreadObjectGCRoot,Map<Integer,List<GCRoot>>> javaFrameMap = computeJavaFrameMap(roots);
        ThreadObjectGCRoot oome = JavaThreadsProvider.getOOMEThread(heap);
        
        Thread worker = Thread.currentThread();
        
        for (GCRoot root : roots) {
            if (root.getKind().equals(GCRoot.THREAD_OBJECT)) {
                ThreadObjectGCRoot threadRoot = (ThreadObjectGCRoot)root;
                Instance threadInstance = threadRoot.getInstance();
                if (threadInstance != null) {
                    StackTraceElement stack[] = threadRoot.getStackTrace();
                    Map<Integer,List<GCRoot>> localsMap = javaFrameMap.get(threadRoot);

                    String tName = JavaThreadsProvider.getThreadName(threadInstance);

                    final List<HeapViewerNode> stackFrameNodes = new ArrayList();
                    ThreadNode threadNode = new ThreadNode(tName, threadRoot.equals(oome), threadInstance) {
                        protected HeapViewerNode[] computeChildren(RootNode root) {
                            return stackFrameNodes.toArray(HeapViewerNode.NO_NODES);
                        }
                    };

                    // -------------------------------------------------------------------
                    if(stack != null) {
                        for(int i = 0; i < stack.length; i++) {
                            final List<HeapViewerNode> localVariableNodes = new ArrayList();
                            if (localsMap != null) {
                                List<GCRoot> locals = localsMap.get(i);
                                if (locals != null) {
                                    for (GCRoot local : locals) {
                                        Instance localInstance = local.getInstance();
                                        if (localInstance != null) {
                                            String text = "";
                                            if (GCRoot.JAVA_FRAME.equals(local.getKind())) {
                                                text = Bundle.JavaThreadsProvider_LocalVariable();
                                            } else if (GCRoot.JNI_LOCAL.equals(local.getKind())) {
                                                text = Bundle.JavaThreadsProvider_JniLocal();
                                            }
                                            localVariableNodes.add(new LocalObjectNode(localInstance, text));
                                        } else {
                                            localVariableNodes.add(new LocalObjectNode.Unknown());                                              
                                        }
                                    }
                                }
                            }
                            
                            StackTraceElement stackElement = stack[i];
                            StackFrameNode stackFrameNode = new StackFrameNode(stackElement.toString(), localVariableNodes.toArray(HeapViewerNode.NO_NODES));
                            stackFrameNodes.add(stackFrameNode);
                            
                            if (worker.isInterrupted()) throw new InterruptedException();
                        }
                    }

                    threadNodes.add(threadNode);
                } else {
                    threadNodes.add(new ThreadNode.Unknown());
                }
            }
        }
        
        return threadNodes.toArray(HeapViewerNode.NO_NODES);
    }
    
    static String getThreadsHTML(HeapContext context) {        
//        boolean gotoSourceAvailable = context.getProject() != null && GoToSource.isAvailable();
        boolean gotoSourceAvailable = false;
        StringBuilder sb = new StringBuilder();
        Heap heap = context.getFragment().getHeap();
        Collection<GCRoot> roots = heap.getGCRoots();
        Map<ThreadObjectGCRoot,Map<Integer,List<GCRoot>>> javaFrameMap = computeJavaFrameMap(roots);
        ThreadObjectGCRoot oome = JavaThreadsProvider.getOOMEThread(heap);
        JavaClass javaClassClass = heap.getJavaClassByName(Class.class.getName());
        // Use this to enable VisualVM color scheme for threads dumps:
        // sw.append("<pre style='color: #cc3300;'>"); // NOI18N
        sb.append("<head><style>span.g {color: #666666;}</style></head>");
        sb.append("<pre>"); // NOI18N
        for (GCRoot root : roots) {
            if(root.getKind().equals(GCRoot.THREAD_OBJECT)) {
                ThreadObjectGCRoot threadRoot = (ThreadObjectGCRoot)root;
                Instance threadInstance = threadRoot.getInstance();
                if (threadInstance != null) {
                    String threadName = JavaThreadsProvider.getThreadName(threadInstance);
                    StackTraceElement stack[] = threadRoot.getStackTrace();
                    Map<Integer,List<GCRoot>> localsMap = javaFrameMap.get(threadRoot);
                    String style=""; // NOI18N

                    if (threadRoot.equals(oome)) {
                        style="style=\"color: #FF0000\""; // NOI18N
                    }                        
                    // --- Use this to enable VisualVM color scheme for threads dumps: ---
                    // sw.append("<span style=\"color: #0033CC\">"); // NOI18N
                    sb.append("<a name='").append(threadInstance.getInstanceId()).append("'><b ").append(style).append(">");   // NOI18N
                    // -------------------------------------------------------------------
                    sb.append(HeapUtils.htmlize(threadName));
                    // --- Use this to enable VisualVM color scheme for threads dumps: ---
                    // sw.append("</span><br>"); // NOI18N
                    sb.append("</b></a><br>");   // NOI18N
                    // -------------------------------------------------------------------
                    if(stack != null) {
                        for(int i = 0; i < stack.length; i++) {
                            String stackElHref;
                            StackTraceElement stackElement = stack[i];
                            String stackElementText = HeapUtils.htmlize(stackElement.toString());

                            if (gotoSourceAvailable) {
                                String className = stackElement.getClassName();
                                String method = stackElement.getMethodName();
                                int lineNo = stackElement.getLineNumber();
                                String stackUrl = OPEN_THREADS_URL+className+"|"+method+"|"+lineNo; // NOI18N

                                // --- Use this to enable VisualVM color scheme for threads dumps: ---
                                // stackElHref = "<a style=\"color: #CC3300;\" href=\""+stackUrl+"\">"+stackElement+"</a>"; // NOI18N
                                stackElHref = "<a href=\""+stackUrl+"\">"+stackElementText+"</a>";    // NOI18N
                                // -------------------------------------------------------------------
                            } else {
                                stackElHref = stackElementText;
                            }
                            sb.append("    at ").append(stackElHref).append("<br>");  // NOI18N
                            if (localsMap != null) {
                                List<GCRoot> locals = localsMap.get(Integer.valueOf(i));

                                if (locals != null) {
                                    for (GCRoot local : locals) {
                                        Instance localInstance = local.getInstance();

                                        if (localInstance != null) {
                                            String text = "";
                                            if (GCRoot.JAVA_FRAME.equals(local.getKind())) {
                                                text = Bundle.JavaThreadsProvider_LocalVariable();
                                            } else if (GCRoot.JNI_LOCAL.equals(local.getKind())) {
                                                text = Bundle.JavaThreadsProvider_JniLocal();
                                            }
                                            sb.append("       <span class=\"g\">" + text + ":</span> ").append(HeapUtils.instanceToHtml(localInstance, false, javaClassClass)).append("<br>"); // NOI18N
                                        } else {
                                            String text = "";
                                            if (GCRoot.JAVA_FRAME.equals(local.getKind())) {
                                                text = Bundle.JavaThreadsProvider_UnknownLocalVariable();
                                            } else if (GCRoot.JNI_LOCAL.equals(local.getKind())) {
                                                text = Bundle.JavaThreadsProvider_UnknownJniLocal();
                                            }
                                            sb.append("       <span class=\"g\">" + text + "</span><br>"); // NOI18N
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    sb.append(Bundle.JavaThreadsProvider_UnknownThread() + "<br>"); // NOI18N
                }
                sb.append("<br>");  // NOI18N
            }
        }
        sb.append("</pre>"); // NOI18N
        
        return sb.toString();
    }
    
    
    private static String getThreadInstanceName(Instance threadInstance) {
        Object threadName = threadInstance.getValueOfField("name");  // NOI18N
        if (threadName == null) return "*null*"; // NOI18N
        return DetailsSupport.getDetailsString((Instance)threadName);
    }
    
    private static Map<ThreadObjectGCRoot,Map<Integer,List<GCRoot>>> computeJavaFrameMap(Collection<GCRoot> roots) {
        Map<ThreadObjectGCRoot,Map<Integer,List<GCRoot>>> javaFrameMap = new HashMap();
        
        for (GCRoot root : roots) {
            ThreadObjectGCRoot threadObj;
            Integer frameNo;

            if (GCRoot.JAVA_FRAME.equals(root.getKind())) {
                JavaFrameGCRoot frameGCroot = (JavaFrameGCRoot) root;
                threadObj = frameGCroot.getThreadGCRoot();
                frameNo = frameGCroot.getFrameNumber();
            } else if (GCRoot.JNI_LOCAL.equals(root.getKind())) {
                JniLocalGCRoot jniGCroot = (JniLocalGCRoot) root;
                threadObj = jniGCroot.getThreadGCRoot();
                frameNo = jniGCroot.getFrameNumber();
            } else {
                continue;
            }

            Map<Integer,List<GCRoot>> stackMap = javaFrameMap.get(threadObj);
            List<GCRoot> locals;

            if (stackMap == null) {
                stackMap = new HashMap();
                javaFrameMap.put(threadObj,stackMap);
            }
            locals = stackMap.get(frameNo);
            if (locals == null) {
                locals = new ArrayList(2);
                stackMap.put(frameNo,locals);
            }
            locals.add(root);
        }
        return javaFrameMap;
    }
    
    /** taken from sun.misc.VM
     * 
     * Returns Thread.State for the given threadStatus
     */
    private static Thread.State toThreadState(int threadStatus) {
        if ((threadStatus & JVMTI_THREAD_STATE_RUNNABLE) != 0) {
            return Thread.State.RUNNABLE;
        } else if ((threadStatus & JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER) != 0) {
            return Thread.State.BLOCKED;
        } else if ((threadStatus & JVMTI_THREAD_STATE_WAITING_INDEFINITELY) != 0) {
            return Thread.State.WAITING;
        } else if ((threadStatus & JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT) != 0) {
            return Thread.State.TIMED_WAITING;
        } else if ((threadStatus & JVMTI_THREAD_STATE_TERMINATED) != 0) {
            return Thread.State.TERMINATED;
        } else if ((threadStatus & JVMTI_THREAD_STATE_ALIVE) == 0) {
            return Thread.State.NEW;
        } else {
            return Thread.State.RUNNABLE;
        }
    }
    
     /* The threadStatus field is set by the VM at state transition
     * in the hotspot implementation. Its value is set according to
     * the JVM TI specification GetThreadState function.
     */
    private final static int JVMTI_THREAD_STATE_ALIVE = 0x0001;
    private final static int JVMTI_THREAD_STATE_TERMINATED = 0x0002;
    private final static int JVMTI_THREAD_STATE_RUNNABLE = 0x0004;
    private final static int JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER = 0x0400;
    private final static int JVMTI_THREAD_STATE_WAITING_INDEFINITELY = 0x0010;
    private final static int JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT = 0x0020;
    
}
