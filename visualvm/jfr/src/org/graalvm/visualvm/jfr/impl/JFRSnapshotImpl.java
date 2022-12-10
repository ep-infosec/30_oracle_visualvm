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

package org.graalvm.visualvm.jfr.impl;

import java.io.File;
import java.io.IOException;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.datasupport.Utils;
import org.graalvm.visualvm.jfr.JFRSnapshot;
import org.graalvm.visualvm.jfr.JFRSnapshotSupport;

/**
 *
 * @author Jiri Sedlacek
 */
class JFRSnapshotImpl extends JFRSnapshot {
    
    private final Storage storage;
    
    
    JFRSnapshotImpl(File file) throws IOException {
        this(file, new Storage());
    }
    
    JFRSnapshotImpl(File file, Storage storage) throws IOException {
        super(file);
        this.storage = storage;
    }
    
    public boolean supportsDelete() {
        return false;
    }
    
    
    protected Storage createStorage() {
        return storage;
    }

    public boolean supportsUserRemove() {
        return true;
    }
    
    
    protected void remove() {
        File file = getFile();
        if (JFRSnapshotSupport.getStorageDirectory().equals(file.getParentFile())) Utils.delete(file, true);
        setFile(null);
        getStorage().deleteCustomPropertiesStorage();
    }
    
}
