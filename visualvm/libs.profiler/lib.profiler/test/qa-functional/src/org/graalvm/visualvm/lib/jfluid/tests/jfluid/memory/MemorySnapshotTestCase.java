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
 * MemoryTestCase.java
 *
 * Created on July 19, 2005, 5:21 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */
package org.graalvm.visualvm.lib.jfluid.tests.jfluid.memory;

import org.graalvm.visualvm.lib.jfluid.ProfilerEngineSettings;
import org.graalvm.visualvm.lib.jfluid.TargetAppRunner;
import org.graalvm.visualvm.lib.jfluid.results.CCTNode;
import org.graalvm.visualvm.lib.jfluid.results.EventBufferResultsProvider;
import org.graalvm.visualvm.lib.jfluid.results.ProfilingResultsDispatcher;
import org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.memory.AllocMemoryResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.results.memory.LivenessMemoryResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.results.memory.MemoryCCTManager;
import org.graalvm.visualvm.lib.jfluid.results.memory.MemoryCCTProvider;
import org.graalvm.visualvm.lib.jfluid.results.memory.MemoryCallGraphBuilder;
import org.graalvm.visualvm.lib.jfluid.results.memory.MemoryResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.results.memory.PresoObjAllocCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.memory.RuntimeMemoryCCTNode;
import org.graalvm.visualvm.lib.jfluid.tests.jfluid.*;
import org.graalvm.visualvm.lib.jfluid.tests.jfluid.utils.*;
import org.graalvm.visualvm.lib.jfluid.utils.StringUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;


/**
 *
 * @author ehucka
 */
public abstract class MemorySnapshotTestCase extends CommonProfilerTestCase {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class MemoryResultListener implements MemoryCCTProvider.Listener {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final Object resultsLock = new Object();
        private boolean hasResults = false;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void cctEstablished(RuntimeCCTNode appRootNode) {
            System.out.println("Memory CCT Established");

            synchronized (resultsLock) {
                hasResults = true;
                resultsLock.notify();
            }
        }

        public void cctReset() {
            synchronized (resultsLock) {
                hasResults = false;
                resultsLock.notify();
            }
        }

        public boolean wait4results(long timeout) {
            synchronized (resultsLock) {
                if (!hasResults) {
                    try {
                        resultsLock.wait(timeout);
                    } catch (InterruptedException e) {
                    }
                }

                return hasResults;
            }
        }

        public void cctEstablished(RuntimeCCTNode appRootNode, boolean empty) {
            if (!empty) {
                cctEstablished(appRootNode);
            }
            //throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    MemoryCallGraphBuilder builder = new MemoryCallGraphBuilder();
    MemoryResultListener resultListener = null;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of MemoryTestCase
     */
    public MemorySnapshotTestCase(String name) {
        super(name);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    protected void checkClasses(MemoryResultsSnapshot snapshot, String[] prefixes) {
        ArrayList list = new ArrayList(128);

        if (snapshot instanceof AllocMemoryResultsSnapshot) {
            AllocMemoryResultsSnapshot alsnapshot = (AllocMemoryResultsSnapshot) snapshot;

            int[] objcnts = alsnapshot.getObjectsCounts();
            long[] objsizes = alsnapshot.getObjectsSizePerClass();
            String[] classnames = alsnapshot.getClassNames();

            for (int i = 0; i < snapshot.getNProfiledClasses(); i++) {
                boolean match = false;

                for (String prefixe : prefixes) {
                    if (classnames[i].startsWith(prefixe)) {
                        match = true;

                        break;
                    }
                }

                if (match) {
                    StringBuilder out = new StringBuilder();
                    out.append(complete(StringUtils.userFormClassName(classnames[i]), 32));
                    //out.append(complete(StringUtils.nBytesToString(objsizes[i]), 10));
                    out.append(complete(String.valueOf(objcnts[i]), 8));
                    list.add(out.toString());
                }
            }

            ref(complete("Name", 32) /*complete("Bytes", 10)+*/ + complete("Objects", 8));
        } else if (snapshot instanceof LivenessMemoryResultsSnapshot) {
            LivenessMemoryResultsSnapshot lsnapshot = (LivenessMemoryResultsSnapshot) snapshot;

            log("Max Value:        " + lsnapshot.getMaxValue());
            log("Number Alloc:     " + lsnapshot.getNAlloc());
            log("Instr Classes:    " + lsnapshot.getNInstrClasses());
            log("Total tracked:    " + lsnapshot.getNTotalTracked());
            log("Tracked bytes:    " + lsnapshot.getNTotalTrackedBytes());
            log("Tracked items:    " + lsnapshot.getNTrackedItems());

            float[] avgage = lsnapshot.getAvgObjectAge();
            int[] maxSurvGen = lsnapshot.getMaxSurvGen();
            long[] ntrackedallocobjects = lsnapshot.getNTrackedAllocObjects();
            int[] ntrackedliveobjects = lsnapshot.getNTrackedLiveObjects();
            int[] totalAllocObjects = lsnapshot.getnTotalAllocObjects();
            String[] classnames = lsnapshot.getClassNames();
            long[] trackedLiveObjectsSize = lsnapshot.getTrackedLiveObjectsSize();

            for (int i = 0; i < snapshot.getNProfiledClasses(); i++) {
                boolean match = false;

                for (String prefixe : prefixes) {
                    if (classnames[i].startsWith(prefixe)) {
                        match = true;

                        break;
                    }
                }

                if (match) {
                    StringBuilder out = new StringBuilder();
                    out.append(complete(StringUtils.userFormClassName(classnames[i]), 32));
                    //out.append(complete(StringUtils.nBytesToString(trackedLiveObjectsSize[i]), 10));
                    out.append(complete(String.valueOf(ntrackedliveobjects[i]), 10));
                    out.append(complete(String.valueOf(ntrackedallocobjects[i]), 8));
                    //out.append(complete(String.valueOf((int)avgage[i]), 8));
                    //out.append(complete(String.valueOf(maxSurvGen[i]), 8));
                    out.append(complete(String.valueOf(totalAllocObjects[i]), 8));
                    list.add(out.toString());
                }
            }

            ref(complete("Name", 32) /*complete("LiveBytes", 10)+*/ + complete("LiveObjs", 10)
                + complete("Allocs", 8) /*+complete("AvgAge", 8)+complete("MaxSurv", 8)*/ + complete("Total", 8));
        }

        //log results
        Collections.sort(list);

        for (int i = 0; i < list.size(); i++) {
            ref(list.get(i));
        }

        ref("");
    }

    protected void checkMemoryResults(TargetAppRunner targetAppRunner, String[] classPrefixes, String stacktraceClass)
                               throws Exception {
        targetAppRunner.getProfilerClient().forceObtainedResultsDump();

        boolean gotResults = resultListener.wait4results(10000);

        assertTrue("No memory results available after 10s", gotResults);
        log("results obtained: " + System.currentTimeMillis());

        MemoryResultsSnapshot snapshot = targetAppRunner.getProfilerClient().getMemoryProfilingResultsSnapshot(false);
        assertTrue(snapshot != null);
        log("snapshot taken: " + snapshot);

        ref((snapshot.containsStacks()) ? "Contains stacks." : "Does not contain stacks.");
        log("Begin time:       " + new Date(snapshot.getBeginTime()));
        log("Profiled classes: " + snapshot.getNProfiledClasses());
        log("Time Taken:       " + new Date(snapshot.getTimeTaken()));
        checkClasses(snapshot, classPrefixes);

        //stacktrace
        if (snapshot.containsStacks()) {
            int classid = -1;
            String[] classes = snapshot.getClassNames();

            for (int i = 0; i < snapshot.getNProfiledClasses(); i++) {
                if (classes[i].replace('/', '.').equals(stacktraceClass)) {
                    classid = i;
                }
            }

            assertTrue("Stack trace class wasn't find " + stacktraceClass, (classid > -1));

            MemoryCCTManager manager = new MemoryCCTManager(snapshot, classid, false);

            if (!manager.isEmpty()) {
                PresoObjAllocCCTNode root = manager.getRootNode();
                refNodes(root, "");
            }
        }

        testSerialization(snapshot);
    }

    protected boolean equals(String[] a1, String[] a2, int length) {
        for (int i = 0; i < length; i++) {
            if (!a1[i].equals(a2[i])) {
                return false;
            }
        }

        return true;
    }

    protected boolean equals(int[] a1, int[] a2, int length) {
        for (int i = 0; i < length; i++) {
            if (a1[i] != a2[i]) {
                return false;
            }
        }

        return true;
    }

    protected boolean equals(long[] a1, long[] a2, int length) {
        for (int i = 0; i < length; i++) {
            if (a1[i] != a2[i]) {
                return false;
            }
        }

        return true;
    }

    protected boolean equals(float[] a1, float[] a2, int length) {
        for (int i = 0; i < length; i++) {
            if (a1[i] != a2[i]) {
                return false;
            }
        }

        return true;
    }

    protected boolean equals(RuntimeMemoryCCTNode a1, RuntimeMemoryCCTNode a2)
                      throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        a1.writeToStream(dos);
        dos.close();

        byte[] bytes = baos.toByteArray();
        //write to bytes 2
        baos = new ByteArrayOutputStream();
        dos = new DataOutputStream(baos);
        a2.writeToStream(dos);
        dos.close();

        byte[] bytes2 = baos.toByteArray();

        if (bytes.length != bytes2.length) {
            return false;
        }

        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] != bytes2[i]) {
                return false;
            }
        }

        return true;
    }

    protected ProfilerEngineSettings initMemorySnapshotTest(String projectName, String className) {
        ProfilerEngineSettings settings = initTest(projectName, className, null);
        //defaults
        settings.setThreadCPUTimerOn(false);
        settings.setAllocTrackEvery(1); //default is not strict - cannot be measured in test
        settings.setRunGCOnGetResultsInMemoryProfiling(true);

        return settings;
    }

    protected void refNodes(PresoObjAllocCCTNode root, String tab) {
        ref(tab + (PresoObjAllocCCTNode) root);

        if (root.getNChildren() > 0) {
            root.sortChildren(PresoObjAllocCCTNode.SORT_BY_NAME, false);

            CCTNode[] nodes = root.getChildren();

            for (CCTNode node : nodes) {
                refNodes((PresoObjAllocCCTNode) node, tab + " ");
            }
        }
    }

    protected void startMemorySnapshotTest(ProfilerEngineSettings settings, int instrMode, String[] classPrefixes,
                                           String stacktraceClass) {
        //create runner //instrMode CommonConstants.INSTR_OBJECT_ALLOCATIONS
        assertTrue(builder != null);

        TestProfilerAppHandler handler = new TestProfilerAppHandler(this);
        TargetAppRunner runner = new TargetAppRunner(settings, handler, new TestProfilingPointsProcessor());
        runner.addProfilingEventListener(Utils.createProfilingListener(this));

        builder.removeAllListeners();
        ProfilingResultsDispatcher.getDefault().removeAllListeners();

        resultListener = new MemoryResultListener();
        builder.addListener(resultListener);

        ProfilingResultsDispatcher.getDefault().addListener(builder);
        builder.startup(runner.getProfilerClient());

        try {
            assertTrue("not read calibration data", runner.readSavedCalibrationData());
            runner.getProfilerClient().initiateMemoryProfInstrumentation(instrMode);

            Process p = startTargetVM(runner);
            assertNotNull("Target JVM is not started", p);
            bindStreams(p);
            runner.attachToTargetVMOnStartup();
            
            waitForStatus(STATUS_RUNNING);
            assertTrue("runner is not running", runner.targetAppIsRunning());

            waitForStatus(STATUS_RESULTS_AVAILABLE | STATUS_APP_FINISHED);

            if (!isStatus(STATUS_APP_FINISHED)) {
                waitForStatus(STATUS_APP_FINISHED);
            }
            Thread.sleep(1000);
            checkMemoryResults(runner, classPrefixes, stacktraceClass);
            setStatus(STATUS_MEASURED);
        } catch (Exception ex) {
            log(ex);
            assertTrue("Exception thrown: " + ex.getMessage(), false);
        } finally {
            ProfilingResultsDispatcher.getDefault().pause(true);
            builder.shutdown();

            builder.removeListener(resultListener);
            ProfilingResultsDispatcher.getDefault().removeListener(builder);

            finalizeTest(runner);
        }
    }

    protected void testSerialization(MemoryResultsSnapshot snapshot) {
        try {
            //write to bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            snapshot.writeToStream(dos);
            dos.close();

            byte[] bytes = baos.toByteArray();
            MemoryResultsSnapshot snapshot2;

            if (snapshot instanceof LivenessMemoryResultsSnapshot) {
                snapshot2 = new LivenessMemoryResultsSnapshot();
            } else {
                snapshot2 = new AllocMemoryResultsSnapshot();
            }

            //read from bytes
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            DataInputStream dis = new DataInputStream(bis);
            snapshot2.readFromStream(dis);
            dis.close();

            //compare
            if (snapshot instanceof LivenessMemoryResultsSnapshot) {
                LivenessMemoryResultsSnapshot s1;
                LivenessMemoryResultsSnapshot s2;
                s1 = (LivenessMemoryResultsSnapshot) snapshot;
                s2 = (LivenessMemoryResultsSnapshot) snapshot2;
                assertEquals("Snapshot Serialization: diff containsStacks", s1.containsStacks(), s2.containsStacks());
                assertEquals("Snapshot Serialization: diff beginTime", s1.getBeginTime(), s2.getBeginTime());
                assertEquals("Snapshot Serialization: diff MaxValue", s1.getMaxValue(), s2.getMaxValue());
                assertEquals("Snapshot Serialization: diff NAlloc", s1.getNAlloc(), s2.getNAlloc());
                assertEquals("Snapshot Serialization: diff NInstrClasses", s1.getNInstrClasses(), s2.getNInstrClasses());
                assertEquals("Snapshot Serialization: diff NProfiledClasses", s1.getNProfiledClasses(), s2.getNProfiledClasses());
                assertEquals("Snapshot Serialization: diff NTotalTracked", s1.getNTotalTracked(), s2.getNTotalTracked());
                assertEquals("Snapshot Serialization: diff NTotalTrackedBytes", s1.getNTotalTrackedBytes(),
                             s2.getNTotalTrackedBytes());
                assertEquals("Snapshot Serialization: diff NTrackedItems", s1.getNTrackedItems(), s2.getNTrackedItems());
                assertEquals("Snapshot Serialization: diff TimeTaken", s1.getTimeTaken(), s2.getTimeTaken());

                assertTrue("Snapshot Serialization: diff AvgObjectAge",
                           equals(s1.getAvgObjectAge(), s2.getAvgObjectAge(), s1.getNProfiledClasses()));
                assertTrue("Snapshot Serialization: diff ClassNames",
                           equals(s1.getClassNames(), s2.getClassNames(), s1.getNProfiledClasses()));
                assertTrue("Snapshot Serialization: diff MaxSurvGen",
                           equals(s1.getMaxSurvGen(), s2.getMaxSurvGen(), s1.getNProfiledClasses()));
                assertTrue("Snapshot Serialization: diff NTrackedAllocObjects",
                           equals(s1.getNTrackedAllocObjects(), s2.getNTrackedAllocObjects(), s1.getNProfiledClasses()));
                assertTrue("Snapshot Serialization: diff NTrackedLiveObjects",
                           equals(s1.getNTrackedLiveObjects(), s2.getNTrackedLiveObjects(), s1.getNProfiledClasses()));
                assertTrue("Snapshot Serialization: diff ObjectsSizePerClass",
                           equals(s1.getObjectsSizePerClass(), s2.getObjectsSizePerClass(), s1.getNProfiledClasses()));
                assertTrue("Snapshot Serialization: diff TrackedLiveObjectsSize",
                           equals(s1.getTrackedLiveObjectsSize(), s2.getTrackedLiveObjectsSize(), s1.getNProfiledClasses()));
                assertTrue("Snapshot Serialization: diff nTotalAllocObjects",
                           equals(s1.getnTotalAllocObjects(), s2.getnTotalAllocObjects(), s1.getNProfiledClasses()));
            } else {
                AllocMemoryResultsSnapshot s1;
                AllocMemoryResultsSnapshot s2;
                s1 = (AllocMemoryResultsSnapshot) snapshot;
                s2 = (AllocMemoryResultsSnapshot) snapshot2;
                assertEquals("Snapshot Serialization: diff containsStacks", s1.containsStacks(), s2.containsStacks());
                assertEquals("Snapshot Serialization: diff beginTime", s1.getBeginTime(), s2.getBeginTime());
                assertEquals("Snapshot Serialization: diff NProfiledClasses", s1.getNProfiledClasses(), s2.getNProfiledClasses());
                assertEquals("Snapshot Serialization: diff TimeTaken", s1.getTimeTaken(), s2.getTimeTaken());

                assertTrue("Snapshot Serialization: diff ClassNames",
                           equals(s1.getClassNames(), s2.getClassNames(), s1.getNProfiledClasses()));
                assertTrue("Snapshot Serialization: diff ObjectsSizePerClass",
                           equals(s1.getObjectsSizePerClass(), s2.getObjectsSizePerClass(), s1.getNProfiledClasses()));
            }

            if (snapshot.containsStacks()) {
                Field field = snapshot.getClass().getSuperclass().getDeclaredField("stacksForClasses");
                field.setAccessible(true);

                RuntimeMemoryCCTNode[] stacksForClasses = (RuntimeMemoryCCTNode[]) field.get(snapshot);
                RuntimeMemoryCCTNode[] stacksForClasses2 = (RuntimeMemoryCCTNode[]) field.get(snapshot2);

                for (int i = 0; i < stacksForClasses.length; i++) {
                    if (stacksForClasses[i] != null) {
                        assertTrue("Snapshot Serialization: diff stacktraces " + snapshot.getClassName(i),
                                   equals(stacksForClasses[i], stacksForClasses2[i]));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            assertTrue("Snapshot Serialization: " + ex.getClass().getName() + ": " + ex.getMessage(), false);
        }
    }
}
