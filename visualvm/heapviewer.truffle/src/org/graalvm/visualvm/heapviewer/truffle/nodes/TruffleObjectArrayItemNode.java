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
package org.graalvm.visualvm.heapviewer.truffle.nodes;

import javax.swing.Icon;
import org.graalvm.visualvm.heapviewer.java.InstanceNode;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.truffle.TruffleObject;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerRenderer;
import org.graalvm.visualvm.lib.jfluid.heap.ArrayItemValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.MultiRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.NormalBoldGrayRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.ProfilerRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public interface TruffleObjectArrayItemNode<O extends TruffleObject> {
    
    public ArrayItemValue getItem();
    
    public String getItemName();
    
    
    public static abstract class InstanceBased<O extends TruffleObject.InstanceBased> extends TruffleObjectNode.InstanceBased<O> implements TruffleObjectArrayItemNode<O> {
        
        private ArrayItemValue item;
        
        
        public InstanceBased(O object, String type, ArrayItemValue item) {
            super(object, type);
            this.item = item;
        }
        
        
        @Override
        public String getName() {
            return getItemName() + " = " + getObjectName(); // NOI18N
        }
        
        
        public String getItemName() {
            return "[" + item.getIndex() + "]"; // NOI18N
        }

        public ArrayItemValue getItem() {
            return item;
        }


        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof TruffleObjectArrayItemNode.InstanceBased)) return false;
            return item.equals(((TruffleObjectArrayItemNode.InstanceBased)o).item);
        }

        public int hashCode() {
            return item.hashCode();
        }


        public InstanceNode createCopy() {
            return null;
        }
        
        protected void setupCopy(TruffleObjectArrayItemNode.InstanceBased copy) {
            super.setupCopy(copy);
            copy.item = item;
        }
        
    }
    
    
    public static class Renderer extends MultiRenderer implements HeapViewerRenderer {
        
        private final NormalBoldGrayRenderer fieldRenderer;
        private final LabelRenderer equalsRenderer;
        private final TruffleObjectNode.Renderer dobjectRenderer;
        private final ProfilerRenderer[] renderers;
        
        private final Heap heap;
        
        public Renderer(Heap heap, Icon icon) {
            this(heap, icon, "="); // NOI18N
        }
        
        public Renderer(Heap heap, Icon icon, String divider) {
            this.heap = heap;
            
            fieldRenderer = new NormalBoldGrayRenderer() {
                public void setValue(Object value, int row) {
                    TruffleObjectArrayItemNode node = (TruffleObjectArrayItemNode)value;
                    String name = node.getItemName();
                    if (name.startsWith("static ")) { // NOI18N
                        setNormalValue("static "); // NOI18N
                        setBoldValue(name.substring("static ".length())); // NOI18N
                    } else {
                        setNormalValue(""); // NOI18N
                        setBoldValue(name);
                    }
                    setIcon(Icons.getIcon(ProfilerIcons.NODE_FORWARD));
                }
            };
            
            equalsRenderer = new LabelRenderer() {
                public String toString() {
                    return " " + getText() + " "; // NOI18N
                }
            };
            equalsRenderer.setText(divider);
            equalsRenderer.setMargin(3, 0, 3, 0);
            
            dobjectRenderer = new TruffleObjectNode.Renderer(heap, icon);
            
            renderers = new ProfilerRenderer[] { fieldRenderer, equalsRenderer, dobjectRenderer };
        }

        protected ProfilerRenderer[] valueRenderers() {
            return renderers;
        }
        
        public void setValue(Object value, int row) {
            HeapViewerNode node = (HeapViewerNode)value;
            HeapViewerNode loop = HeapViewerNode.getValue(node, DataType.LOOP, heap);
            if (loop != null) node = loop;
            
            fieldRenderer.setValue(node, row);
            dobjectRenderer.setValue(value, row);
        }
        
        
        public Icon getIcon() {
            return fieldRenderer.getIcon();
        }
        
        public String getShortName() {
            return fieldRenderer.toString();
        }
        
    }
    
}
