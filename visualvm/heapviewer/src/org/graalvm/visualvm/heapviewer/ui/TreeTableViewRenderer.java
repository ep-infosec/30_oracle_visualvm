/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.ui;

import java.util.HashMap;
import java.util.Map;
import javax.accessibility.AccessibleContext;
import javax.swing.Icon;
import javax.swing.JComponent;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.ErrorNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.LoopNode;
import org.graalvm.visualvm.heapviewer.model.ProgressNode;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.NormalBoldGrayRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.ProfilerRenderer;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TreeTableViewRenderer_Loop=loop to {0}"
})
class TreeTableViewRenderer implements ProfilerRenderer {
    
    // Don't make this static, leaks the last rendered value -> model -> UI -> Universe
    private /*static*/ final HeapViewerRenderer FALLBACK = new FallbackRenderer();
    
    private final Map<Class<? extends HeapViewerNode>, HeapViewerRenderer> lookup = new HashMap();
    
    private ProfilerRenderer current = FALLBACK;
    
    
    {
        lookup.put(LoopNode.class, new LoopNodeRenderer());
        lookup.put(ProgressNode.class, new ProgressNodeRenderer());
        lookup.put(ErrorNode.class, new ErrorNodeRenderer());
    }
    
    
    void registerRenderers(Map<Class<? extends HeapViewerNode>, HeapViewerRenderer> renderers) {
        lookup.putAll(renderers);
    }
    
    
    public void setValue(Object value, int row) {
        current = resolve(value.getClass());
        current.setValue(value, row);
    }

    public int getHorizontalAlignment() {
        return current.getHorizontalAlignment();
    }

    public JComponent getComponent() {
        return current.getComponent();
    }

    public void move(int x, int y) {
        current.move(x, y);
    }

    public AccessibleContext getAccessibleContext() {
        return current.getAccessibleContext();
    }
    
    public String toString() {
        return current.toString();
    }
    
    
    HeapViewerRenderer resolve(Class cls) {
        HeapViewerRenderer renderer = lookup.get(cls);
        while (renderer == null && cls != null) {
            cls = cls.getSuperclass();
            renderer = lookup.get(cls);
        }
        return renderer != null ? renderer : FALLBACK;
    }
    
    
    private class LoopNodeRenderer implements HeapViewerRenderer {
        
        private HeapViewerRenderer impl;
    
        
        public void setValue(Object value, int row) {
            HeapViewerNode loop = HeapViewerNode.getValue((HeapViewerNode)value, DataType.LOOP, null);
            impl = resolve(loop.getClass());
            impl.setValue(value, row);
        }

        public int getHorizontalAlignment() {
            return impl.getHorizontalAlignment();
        }

        public JComponent getComponent() {
            return impl.getComponent();
        }

        public void move(int x, int y) {
            impl.move(x, y);
        }

        public AccessibleContext getAccessibleContext() {
            return impl.getAccessibleContext();
        }
        
        public Icon getIcon() {
            return impl.getIcon();
        }

        public String getShortName() {
            return Bundle.TreeTableViewRenderer_Loop(impl.getShortName());
        }
        
        public String toString() {
            return impl.toString();
        }

    }
    
    private static class ProgressNodeRenderer extends NormalBoldGrayRenderer implements HeapViewerRenderer {
    
        ProgressNodeRenderer() {
            setNormalValue("X"); // NOI18N
            setBoldValue("  "); // NOI18N
            setIcon(Icons.getIcon(HeapWalkerIcons.PROGRESS));
        }
        
        public void setValue(Object value, int row) {
            ProgressNode node = (ProgressNode)value;
            
            String text = node.getText();
            String progressText = node.getProgressText();
            
            setNormalValue(text);
            setGrayValue(progressText);
        }
        
        public String getShortName() {
            return getNormalValue();
        }

    }
    
    private static class ErrorNodeRenderer extends LabelRenderer implements HeapViewerRenderer {
    
        ErrorNodeRenderer() {
            setIcon(Icons.getIcon(GeneralIcons.ERROR));
        }
        
        public void setValue(Object value, int row) {
            setText(value == null ? "" : value.toString()); // NOI18N
        }
        
        public String getShortName() {
            return getText();
        }

    }
    
    private static class FallbackRenderer extends LabelRenderer implements HeapViewerRenderer {}
    
}
