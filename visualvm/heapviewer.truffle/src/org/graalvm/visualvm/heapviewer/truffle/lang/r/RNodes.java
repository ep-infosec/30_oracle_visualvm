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
package org.graalvm.visualvm.heapviewer.truffle.lang.r;

import java.util.Map;
import javax.swing.Icon;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TruffleLocalObjectNode;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TruffleObjectFieldNode;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TruffleObjectNode;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TruffleObjectReferenceNode;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TruffleOpenNodeActionProvider;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TruffleTypeNode;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerNodeAction;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerRenderer;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.LanguageIcons;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.DetailsSupport;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=HeapViewerNodeAction.Provider.class)
public class RNodes extends TruffleOpenNodeActionProvider<RObject, RType, RHeapFragment, RLanguage> {
    
    @Override
    public boolean supportsView(HeapContext context, String viewID) {
        return RHeapFragment.isRHeap(context);
    }
    
    @Override
    protected boolean supportsNode(HeapViewerNode node) {
        return node instanceof RNodes.RNode;
    }

    @Override
    protected RLanguage getLanguage() {
        return RLanguage.instance();
    }
    
    
    static String getLogicalValue(RObject object, String type) {
        return DetailsSupport.getDetailsString(object.getInstance());
    }
    
    
    private static String computeObjectName(TruffleObjectNode.InstanceBased<RObject> node) {
        if ("com.oracle.truffle.r.runtime.data.RLogical".equals(node.getInstance().getJavaClass().getName())) { // NOI18N
            String valueString = node.getLogicalValue();
            return "logical#" + valueString.substring(1, valueString.length() - 1); // NOI18N
        } else {
            String typeString = node.getTypeName();
            return typeString.substring(typeString.lastIndexOf('.') + 1) + "#" + node.getInstance().getInstanceNumber(); // NOI18N
        }
    }
    
    private static RObjectNode createCopy(TruffleObjectNode.InstanceBased<RObject> node) {
        return new RObjectNode(node.getTruffleObject(), node.getTypeName());
    }
    
    
    static interface RNode {}
    
    
    static class RObjectNode extends TruffleObjectNode.InstanceBased<RObject> implements RNode {
        
        RObjectNode(RObject robject) {
            this(robject, robject.getType());
        }

        RObjectNode(RObject robject, String type) {
            super(robject, type);
        }
        
        
        @Override
        protected String computeObjectName() {
            return RNodes.computeObjectName(this);
        }
        
        protected String computeLogicalValue(RObject object, String type) {
            String logicalValue = RNodes.getLogicalValue(object, type);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type);
        }
        
        
        public RObjectNode createCopy() {
            RObjectNode copy = RNodes.createCopy(this);
            setupCopy(copy);
            return copy;
        }

        protected void setupCopy(RObjectNode copy) {
            super.setupCopy(copy);
        }
        
    }
    
    static class RLocalObjectNode extends TruffleLocalObjectNode.InstanceBased<RObject> implements RNode {
        
        RLocalObjectNode(RObject object, String type) {
            super(object, type);
        }
        
        
        @Override
        protected String computeObjectName() {
            return RNodes.computeObjectName(this);
        }
        
        protected String computeLogicalValue(RObject object, String type) {
            String logicalValue = RNodes.getLogicalValue(object, type);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type);
        }
        
        
        public RObjectNode createCopy() {
            return RNodes.createCopy(this);
        }
        
    }
    
    static class RTypeNode extends TruffleTypeNode<RObject, RType> implements RNode {
        
        RTypeNode(RType type) {
            super(type);
        }

        @Override
        public HeapViewerNode createNode(RObject object) {
            String type = getType().getName();
            return new RNodes.RObjectNode(object, type);
        }

        @Override
        public TruffleTypeNode createCopy() {
            RTypeNode copy = new RTypeNode(getType());
            setupCopy(copy);
            return copy;
        }
        
        protected void setupCopy(RTypeNode copy) {
            super.setupCopy(copy);
        }
        
    }
    
    
    static class RObjectFieldNode extends TruffleObjectFieldNode.InstanceBased<RObject> implements RNode {
        
        RObjectFieldNode(RObject object, String type, FieldValue field) {
            super(object, type, field);
        }
        
        @Override
        protected String computeObjectName() {
            return RNodes.computeObjectName(this); // NOI18N
        }
        
        protected String computeLogicalValue(RObject object, String type) {
            String logicalValue = RNodes.getLogicalValue(object, type);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type);
        }
        
        
        public RObjectNode createCopy() {
            return RNodes.createCopy(this);
        }
        
    }
    
    static class RObjectReferenceNode extends TruffleObjectReferenceNode.InstanceBased<RObject> implements RNode {
        
        RObjectReferenceNode(RObject object, String type, FieldValue value) {
            super(object, type, value);
        }
        
        @Override
        protected String computeObjectName() {
            return RNodes.computeObjectName(this);
        }
        
        protected String computeLogicalValue(RObject object, String type) {
            String logicalValue = RNodes.getLogicalValue(object, type);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type);
        }
        
        
        public RObjectNode createCopy() {
            return RNodes.createCopy(this);
        }
        
    }
    
    static class RObjectAttributeReferenceNode extends TruffleObjectReferenceNode.InstanceBased<RObject> implements RNode {
        
        RObjectAttributeReferenceNode(RObject object, String type, FieldValue value) {
            super(object, type, value);
        }
        
        @Override
        protected String computeObjectName() {
            return RNodes.computeObjectName(this);
        }
        
        protected String computeLogicalValue(RObject object, String type) {
            String logicalValue = RNodes.getLogicalValue(object, type);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type);
        }
        
        
        public RObjectNode createCopy() {
            return RNodes.createCopy(this);
        }
        
    }
    
    
    @ServiceProvider(service=HeapViewerRenderer.Provider.class)
    public static class RNodesRendererProvider extends HeapViewerRenderer.Provider {

        public boolean supportsView(HeapContext context, String viewID) {
            return true;
        }

        public void registerRenderers(Map<Class<? extends HeapViewerNode>, HeapViewerRenderer> renderers, HeapContext context) {
            RLanguage language = RLanguage.instance();
            Icon instanceIcon = language.createLanguageIcon(Icons.getIcon(LanguageIcons.INSTANCE));
            Icon packageIcon = language.createLanguageIcon(Icons.getIcon(LanguageIcons.PACKAGE));

            Heap heap = context.getFragment().getHeap();

            renderers.put(RNodes.RObjectNode.class, new TruffleObjectNode.Renderer(heap, instanceIcon));

            renderers.put(RNodes.RTypeNode.class, new TruffleTypeNode.Renderer(packageIcon));

            renderers.put(RNodes.RObjectFieldNode.class, new TruffleObjectFieldNode.Renderer(heap, instanceIcon));

            renderers.put(RNodes.RObjectReferenceNode.class, new TruffleObjectReferenceNode.Renderer(heap, instanceIcon));

            renderers.put(RNodes.RObjectAttributeReferenceNode.class, new TruffleObjectReferenceNode.Renderer(heap, instanceIcon, "attribute in")); // NOI18N
        }

    }
    
}
