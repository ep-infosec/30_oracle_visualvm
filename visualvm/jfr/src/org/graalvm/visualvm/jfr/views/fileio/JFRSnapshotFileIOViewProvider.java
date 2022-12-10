/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.views.fileio;

import org.graalvm.visualvm.jfr.JFRSnapshot;
import org.graalvm.visualvm.jfr.model.JFREventChecker;
import org.graalvm.visualvm.jfr.view.JFRViewTab;
import org.graalvm.visualvm.jfr.view.JFRViewTabProvider;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=JFRViewTabProvider.class)
public final class JFRSnapshotFileIOViewProvider extends JFRViewTabProvider {
    
    static final String EVENT_FILE_READ = "jdk.FileRead"; // NOI18N
    static final String EVENT_FILE_WRITE = "jdk.FileWrite"; // NOI18N
    
    
    protected JFRViewTab createView(JFRSnapshot jfrSnapshot) {
        return new JFRSnapshotFileIOView(jfrSnapshot);
    }
    
    
    @ServiceProvider(service=JFREventChecker.class)
    public static final class EventChecker extends JFREventChecker {
        
        public EventChecker() {
            super(checkedTypes());
        }
        
        static String[] checkedTypes() {
            return new String[] {
                EVENT_FILE_READ, EVENT_FILE_WRITE
            };
        }
        
    }
    
}
