/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler;

import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.visualvm.lib.common.ProfilingSettings;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.profiler.api.ProfilerIDESettings;
import org.graalvm.visualvm.lib.profiler.api.ProfilerStorage;
import org.graalvm.visualvm.lib.profiler.spi.SessionListener;
import org.graalvm.visualvm.lib.profiler.v2.SnapshotsWindow;
import org.openide.util.Lookup;
import org.openide.util.Lookup.Provider;
import org.openide.util.lookup.ServiceProvider;


/**
 *
 * @author Jaroslav Bachorik
 */
@NbBundle.Messages({
    "HeapDumpWatch_OOME_PROTECTION_OPEN_HEAPDUMP=Profiled application crashed and generated heap dump.\nDo you wish to open it in heapwalker?",
    "HeapDumpWatch_OOME_PROTECTION_REMOVE_HEAPDUMP=You chose not to open the generated heap dump.\nThe heap dump can take a significant amount of disk space.\nShould it be deleted?"
})
@ServiceProvider(service=SessionListener.class)
public class HeapDumpWatch extends SessionListener.Adapter {
    private static final Logger LOG = Logger.getLogger(HeapDumpWatch.class.getName());

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private  class HeapDumpFolderListener extends FileChangeAdapter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void fileDataCreated(FileEvent fileEvent) {
            captureHeapDump(fileEvent.getFile());
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private FileObject monitoredPath;
    private HeapDumpFolderListener listener;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of HeapDumpWatch */
    public HeapDumpWatch() {
        listener = new HeapDumpFolderListener();
    }

    public static String getHeapDumpPath(Lookup.Provider project) {
        ProfilerIDESettings gps = ProfilerIDESettings.getInstance();
        if (!gps.isOOMDetectionEnabled()) return null;
        
        int oomeDetectionMode = gps.getOOMDetectionMode();

        switch (oomeDetectionMode) {
            case ProfilerIDESettings.OOME_DETECTION_TEMPDIR:
                return System.getProperty("java.io.tmpdir"); // NOI18N
            case ProfilerIDESettings.OOME_DETECTION_PROJECTDIR:

                try {
                    return FileUtil.toFile(ProfilerStorage.getProjectFolder(project, true)).getAbsolutePath();
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Cannot resolve project settings directory:\n" + e.getMessage(), e);
                    
                    return null;
                }
            case ProfilerIDESettings.OOME_DETECTION_CUSTOMDIR:
                return gps.getCustomHeapdumpPath();
        }

        return null;
    }
    
    @Override
    public void onShutdown() {
        release();
    }

    @Override
    public void onStartup(ProfilingSettings ps, Provider p) {
        if (ProfilerIDESettings.getInstance().isOOMDetectionEnabled()) {
            String oomePath = getHeapDumpPath(p);
            if (oomePath != null) {
                monitor(oomePath);
            }
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    private void monitor(String path) throws IllegalArgumentException {
        if ((path == null) || path.isEmpty()) {
            throw new IllegalArgumentException("The path \"" + path + "\" can't be null."); // NOI18N
        }

        FileObject fo = FileUtil.toFileObject(FileUtil.normalizeFile(new File(path)));

        if (fo != null) {
            if (!fo.isFolder()) {
                throw new IllegalArgumentException("The given path \"" + path + "\" is invalid. It must be a folder"); // NOI18N
            }

            fo.getChildren();
            fo.addFileChangeListener(listener);
            monitoredPath = fo;
        }
    }
    
    private void captureHeapDump(FileObject heapDump) {
        if (!heapDump.getExt().equals(ResultsManager.HEAPDUMP_EXTENSION)) {
            return; // NOI18N
        }

        if (heapDump.getName().startsWith(ResultsManager.HEAPDUMP_PREFIX)) {
            return; // custom heapdump
        }

        SnapshotsWindow.instance().refreshFolder(heapDump.getParent(), true);
//        if (ProfilerControlPanel2.hasDefault())
//            ProfilerControlPanel2.getDefault().refreshSnapshotsList(); // refresh list of snapshots

        try {
            if (ProfilerDialogs.displayConfirmation(Bundle.HeapDumpWatch_OOME_PROTECTION_OPEN_HEAPDUMP())) {
                ResultsManager.getDefault().openSnapshot(heapDump);
            } else if (ProfilerDialogs.displayConfirmation(Bundle.HeapDumpWatch_OOME_PROTECTION_REMOVE_HEAPDUMP())) {
                heapDump.delete();
                SnapshotsWindow.instance().refreshFolder(heapDump.getParent(), true);
//                if (ProfilerControlPanel2.hasDefault())
//                    ProfilerControlPanel2.getDefault().refreshSnapshotsList();
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            release();
        }
    }

    private void release() {
        if (monitoredPath != null) {
            monitoredPath.removeFileChangeListener(listener);
            monitoredPath = null;
        }
    }
}
