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
package org.graalvm.visualvm.sampler.truffle.cpu;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.VisualVM;
import org.graalvm.visualvm.sampler.truffle.TruffleDataProvider;
import org.openide.util.NbBundle;

/**
 *
 * @author Tomas Hurka
 */
public final class ThreadInfoProvider extends TruffleDataProvider {

    public ThreadInfoProvider(Application app, String mode, boolean trackFlags) {
        status = initialize(app, mode, trackFlags);
    }

    private String initialize(Application application, String mode, boolean trackFlags) {
        String st = initJMXConn(application);

        if (st != null) return st;
        try {
            if (!checkAndLoadJMX(application)) {
                return NbBundle.getMessage(ThreadInfoProvider.class, "MSG_unavailable_threads", VisualVM.getInstance().getLogfileHandle());
            }
            if (!tbean.isStackTracesEnabled()) {
                return NbBundle.getMessage(ThreadInfoProvider.class, "MSG_unavailable_stacktraces");
            }
            tbean.setTrackFlags(trackFlags);
            if (tbean.isModeAvailable()) tbean.setMode(mode);
            tbean.dumpAllThreads();
        } catch (SecurityException e) {
            LOGGER.log(Level.INFO, "threadBean.getThreadInfo(ids, maxDepth) throws SecurityException for " + application, e); // NOI18N
            return NbBundle.getMessage(ThreadInfoProvider.class, "MSG_unavailable_threads", VisualVM.getInstance().getLogfileHandle()); // NOI18N
        } catch (Throwable t) {
            LOGGER.log(Level.INFO, "threadBean.getThreadInfo(ids, maxDepth) throws Throwable for " + application, t); // NOI18N
            return NbBundle.getMessage(ThreadInfoProvider.class, "MSG_unavailable_threads", VisualVM.getInstance().getLogfileHandle()); // NOI18N
        }
        return null;
    }

    Map<String, Object>[] dumpAllThreads() throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        return tbean.dumpAllThreads();
    }

    void setOptions(String mode, boolean trackFlags) {
        try {
            if (tbean.isModeAvailable()) tbean.setMode(mode);
            tbean.setTrackFlags(trackFlags);
        } catch (Exception ex) {
            LOGGER.log(Level.INFO, "threadBean.setMode(), setOptions()", ex); // NOI18N
        }
    }

    public boolean isModeVailable() {
        try {
            return tbean.isModeAvailable();
        } catch (Exception ex) {
            LOGGER.log(Level.INFO, "threadBean.isModeVailable(), isModeVailable()", ex); // NOI18N
        }
        return false;
    }
}
