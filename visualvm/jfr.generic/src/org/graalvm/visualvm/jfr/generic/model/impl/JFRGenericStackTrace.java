/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.generic.model.impl;

import java.util.ArrayList;
import java.util.List;
import org.graalvm.visualvm.jfr.model.JFRStackFrame;
import org.graalvm.visualvm.jfr.model.JFRStackTrace;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCStackTrace;

/**
 *
 * @author Jiri Sedlacek
 */
final class JFRGenericStackTrace extends JFRStackTrace {
    
    private final IMCStackTrace stackTrace;
    
    
    JFRGenericStackTrace(IMCStackTrace stackTrace) {
        this.stackTrace = stackTrace;
    }

    
    @Override
    public List<JFRStackFrame> getFrames() {
        List<? extends IMCFrame> imcFrames = stackTrace.getFrames();
        List<JFRStackFrame> frames = new ArrayList<>(imcFrames.size());
        
        for (IMCFrame imcFrame : imcFrames)
            frames.add(new JFRGenericStackFrame(imcFrame));
        
        return frames;
    }
    
    @Override
    public boolean isTruncated() {
        return stackTrace.getTruncationState().isTruncated();
    }
    
    
    @Override
    public int hashCode() {
        return stackTrace.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        return o instanceof JFRGenericStackTrace ? stackTrace.equals(((JFRGenericStackTrace)o).stackTrace) : false;
    }
    
}
