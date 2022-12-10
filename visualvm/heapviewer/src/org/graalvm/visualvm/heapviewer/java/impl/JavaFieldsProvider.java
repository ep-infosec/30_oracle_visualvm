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

package org.graalvm.visualvm.heapviewer.java.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.SortOrder;
import org.graalvm.visualvm.heapviewer.java.InstanceNode;
import org.graalvm.visualvm.heapviewer.java.InstanceReferenceNode;
import org.graalvm.visualvm.heapviewer.java.PrimitiveNode;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.ui.UIThresholds;
import org.graalvm.visualvm.heapviewer.utils.NodesComputer;
import org.graalvm.visualvm.heapviewer.utils.ProgressIterator;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "JavaFieldsProvider_MoreNodes=<another {0} fields left>",
    "JavaFieldsProvider_SamplesContainer=<sample {0} fields>",
    "JavaFieldsProvider_NodesContainer=<fields {0}-{1}>"
})
public abstract class JavaFieldsProvider extends HeapViewerNode.Provider {
    
    public HeapViewerNode[] getNodes(HeapViewerNode parent, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
        List<FieldValue> fields = getFields(parent, heap);
        return getNodes(fields, parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
    }
    
    static HeapViewerNode[] getNodes(final List<FieldValue> fields, final HeapViewerNode parent, final Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
        if (fields == null) return null;
        
        NodesComputer<Integer> computer = new NodesComputer<Integer>(fields.size(), UIThresholds.MAX_INSTANCE_FIELDS) {
            protected boolean sorts(DataType dataType) {
                return !DataType.COUNT.equals(dataType);
            }
            protected HeapViewerNode createNode(Integer index) {
                FieldValue field = fields.get(index);
                return field instanceof ObjectFieldValue ?
                        new InstanceReferenceNode.Field((ObjectFieldValue)field, false) :
                        new PrimitiveNode.Field(field);
            }
            protected ProgressIterator<Integer> objectsIterator(int index, Progress progress) {
                Iterator<Integer> iterator = integerIterator(index, fields.size());
                return new ProgressIterator(iterator, index, false, progress);
            }
            protected String getMoreNodesString(String moreNodesCount)  {
                return Bundle.JavaFieldsProvider_MoreNodes(moreNodesCount);
            }
            protected String getSamplesContainerString(String objectsCount)  {
                return Bundle.JavaFieldsProvider_SamplesContainer(objectsCount);
            }
            protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                return Bundle.JavaFieldsProvider_NodesContainer(firstNodeIdx, lastNodeIdx);
            }
        };

        return computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders, progress);
    }
    
    
    protected abstract List<FieldValue> getFields(HeapViewerNode parent, Heap heap);
    
    
    @ServiceProvider(service=HeapViewerNode.Provider.class, position = 200)
    @NbBundle.Messages({
        "InstanceFieldsProvider_Name=fields"
    })
    public static class InstanceFieldsProvider extends JavaFieldsProvider {
        
        // TODO: will be configurable, ideally by instance
        private boolean includeStaticFields = true;
        private boolean includeInstanceFields = true;
        
        public String getName() {
            return Bundle.InstanceFieldsProvider_Name();
        }

        public boolean supportsView(Heap heap, String viewID) {
            return viewID.startsWith("java_"); // NOI18N
        }

        public boolean supportsNode(HeapViewerNode parent, Heap heap, String viewID) {
            if (parent instanceof InstanceNode && !InstanceNode.Mode.INCOMING_REFERENCE.equals(((InstanceNode)parent).getMode())) {
                Instance instance = ((InstanceNode)parent).getInstance();
                if (instance == null) return false;
                JavaClass jcls = instance.getJavaClass();
                if (jcls.isArray()) return false;
                if (Class.class.getName().equals(jcls.getName())) {
                    JavaClass jclass = heap.getJavaClassByID(instance.getInstanceId());
                    return jclass == null;
                }
                return true;
            } else {
                return false;
            }
        }

        
        protected List<FieldValue> getFields(HeapViewerNode parent, Heap heap) {
            return getFields(parent, heap, includeInstanceFields, includeStaticFields);
        }
        
        static List<FieldValue> getFields(HeapViewerNode parent, Heap heap, boolean instanceFields, boolean staticFields) {
            Instance instance = HeapViewerNode.getValue(parent, DataType.INSTANCE, heap);
            if (instance == null) return null;
            
            if (staticFields == instanceFields) {
                List<FieldValue> fields = new ArrayList(instance.getFieldValues());
                fields.addAll(instance.getStaticFieldValues());
                return fields;
            } else if (instanceFields) {
                return instance.getFieldValues();
            } else {
                return instance.getStaticFieldValues();
            }
        }
        
    }
    
    @ServiceProvider(service=HeapViewerNode.Provider.class, position = 250)
    @NbBundle.Messages({
        "ClassFieldsProvider_Name=static fields"
    })
    public static class ClassFieldsProvider extends JavaFieldsProvider {
        
        public String getName() {
            return Bundle.ClassFieldsProvider_Name();
        }

        public boolean supportsView(Heap heap, String viewID) {
            return viewID.startsWith("java_"); // NOI18N
        }

        public boolean supportsNode(HeapViewerNode parent, Heap heap, String viewID) {
            if (parent instanceof InstanceNode && !InstanceNode.Mode.INCOMING_REFERENCE.equals(((InstanceNode)parent).getMode())) {
                Instance instance = ((InstanceNode)parent).getInstance();
                if (instance == null) return false;
                JavaClass jcls = instance.getJavaClass();
                if (jcls.isArray()) return false;
                if (Class.class.getName().equals(jcls.getName())) {
                    JavaClass jclass = heap.getJavaClassByID(instance.getInstanceId());
                    return jclass != null;
                }
            }
            return false;
        }
        
        protected List<FieldValue> getFields(HeapViewerNode parent, Heap heap) {
            Instance instance = HeapViewerNode.getValue(parent, DataType.INSTANCE, heap);
            if (instance == null) return null;
            JavaClass jclass = heap.getJavaClassByID(instance.getInstanceId());
            return jclass == null ? null : jclass.getStaticFieldValues();
        }
        
    }
    
}
