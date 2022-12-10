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
package org.graalvm.visualvm.lib.ui.memory;

import javax.swing.Icon;
import javax.swing.UIManager;
import org.graalvm.visualvm.lib.jfluid.results.memory.PresoObjAllocCCTNode;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.LanguageIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.ui.swing.renderer.JavaNameRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public class MemoryJavaNameRenderer extends JavaNameRenderer {

    private static final Icon CLASS_ICON = Icons.getIcon(LanguageIcons.CLASS);
    private static final Icon REVERSE_ICON = Icons.getIcon(ProfilerIcons.NODE_REVERSE);
    private static final Icon REVERSE_ICON_DISABLED = UIManager.getLookAndFeel().getDisabledIcon(null, REVERSE_ICON);

    public void setValue(Object value, int row) {
        if (value instanceof PresoObjAllocCCTNode) {
            PresoObjAllocCCTNode node = (PresoObjAllocCCTNode)value;

            if (node.isFiltered()) {
                setNormalValue(""); // NOI18N
                setBoldValue("");
                setGrayValue(node.getNodeName()); // NOI18N
            } else {
                super.setValue(value, row);
            }

            if (node.isFiltered()) setIcon(REVERSE_ICON_DISABLED);
            else if (node.getMethodClassNameAndSig()[2] == null) setIcon(CLASS_ICON); // class name
            else setIcon(REVERSE_ICON); // method name
        } else {
            super.setValue(value, row);
        }

//        // TODO: <clinit> methods should be displayed with "()" similar to <init>
//        // PlainFormattableMethodName.getFullFormattedMethod()
//
//        if (getGrayValue().isEmpty()) System.err.println(">> value: " + ((PresoObjAllocCCTNode)value).getMethodClassNameAndSig()[2]);
//        // TODO: also "Objects allocated by reflection" should be excluded to display icon
//        if (getGrayValue().isEmpty()) setIcon(null); // class name
//        else setIcon(Icons.getIcon(ProfilerIcons.NODE_REVERSE)); // method name
    }
    
}
