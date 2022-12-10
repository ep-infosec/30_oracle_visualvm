/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.visualvm.jfr.model.JFRMethod;
import org.graalvm.visualvm.jfr.model.JFRStackFrame;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;

/**
 *
 * @author Jiri Sedlacek
 */
final class JFRGenericStackFrame extends JFRStackFrame {
    
    private final IMCFrame stackFrame;
    
    
    JFRGenericStackFrame(IMCFrame stackFrame) {
        this.stackFrame = stackFrame;
    }
    

    @Override
    public JFRMethod getMethod() {
        IMCMethod method = stackFrame.getMethod();
        return method == null ? null : new JFRGenericMethod(method);
    }

    @Override
    public int getLine() {
        return stackFrame.getFrameLineNumber();
    }
    
    @Override
    public int getBCI() {
        return stackFrame.getBCI();
    }

    @Override
    public String getType() {
        IMCFrame.Type type = stackFrame.getType();
        if (type.isUnknown()) {
            return "Native";        // NOI18N  // ??
        }
        return type.getName();
    }
    
    
    @Override
    public int hashCode() {
        return stackFrame.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        return o instanceof JFRGenericStackFrame ? stackFrame.equals(((JFRGenericStackFrame)o).stackFrame) : false;
    }
    
}
