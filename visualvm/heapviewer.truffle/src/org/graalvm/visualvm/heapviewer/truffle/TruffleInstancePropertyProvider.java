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
package org.graalvm.visualvm.heapviewer.truffle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.swing.SortOrder;
import org.graalvm.visualvm.heapviewer.java.InstanceNode;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObject;
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectArrayItemNode;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TerminalJavaNodes;
import org.graalvm.visualvm.heapviewer.ui.UIThresholds;
import org.graalvm.visualvm.heapviewer.utils.HeapOperations;
import org.graalvm.visualvm.heapviewer.utils.NodesComputer;
import org.graalvm.visualvm.heapviewer.utils.ProgressIterator;
import org.graalvm.visualvm.lib.jfluid.heap.ArrayItemValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.HeapProgress;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectArrayInstance;
import org.graalvm.visualvm.lib.jfluid.heap.PrimitiveArrayInstance;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TruffleInstancePropertyProvider_ComputingNodes=<computing {0}...>", // <computing items...>
    "TruffleInstancePropertyProvider_MoreNodes=<another {0} {1} left>", // <another 1234 items left>
    "TruffleInstancePropertyProvider_SamplesContainer=<sample {0} {1}>", // <sample 1234 items>
    "TruffleInstancePropertyProvider_NodesContainer=<{2} {0}-{1}>" // <items 1001 - 2000>
})
public abstract class TruffleInstancePropertyProvider<O extends TruffleObject, T extends TruffleType<O>, F extends TruffleLanguageHeapFragment<O, T>, L extends TruffleLanguage<O, T, F>, I> extends HeapViewerNode.Provider {
    
    private final L language;
    
    private final String propertyName;
    private final int maxPropertyItems;
    
    private final boolean displaysProgress;
    private final boolean filtersProperties;
    
    
    protected TruffleInstancePropertyProvider(String propertyName, L language, boolean displaysProgress, boolean filtersProperties, int maxPropertyItems) {
        this.language = language;
        this.propertyName = propertyName;
        this.maxPropertyItems = maxPropertyItems;
        this.displaysProgress = displaysProgress;
        this.filtersProperties = filtersProperties;
    }
    

    @Override
    public String getName() {
        return propertyName;
    }
    
    
    protected final L getLanguage() {
        return language;
    }

    
    @Override
    public abstract boolean supportsView(Heap heap, String viewID);

    @Override
    public abstract boolean supportsNode(HeapViewerNode node, Heap heap, String viewID);
    
    
    protected abstract Collection<I> getPropertyItems(Instance instance);
    
    protected boolean includeItem(I item) { return true; }
    
    protected abstract HeapViewerNode createNode(I item, Heap heap);

    
    @Override
    public final HeapViewerNode[] getNodes(HeapViewerNode parent, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
        Instance instance = HeapViewerNode.getValue(parent, DataType.INSTANCE, heap);
        return instance == null ? null : getNodes(instance, parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
    }
    
    final HeapViewerNode[] getNodes(Instance instance, HeapViewerNode parent, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
        Collection<I> itemsC = null;
        
        if (!displaysProgress) {
            itemsC = getPropertyItems(instance);
        } else {
            HeapOperations.OpProgressHandle pHandle = new HeapOperations.OpProgressHandle(Bundle.TruffleInstancePropertyProvider_ComputingNodes(propertyName));
            pHandle.setInitialDelay(1000);
            pHandle.start(HeapProgress.PROGRESS_MAX);
            pHandle.setProgress(0);

            try { itemsC = getPropertyItems(instance); }
            finally { pHandle.finish(); }
        }
        
        if (itemsC == null) return null;
        
        final List<I> items = new ArrayList<>(itemsC);
        
        if (filtersProperties) {
            Iterator<I> itemsIt = items.iterator();
            while (itemsIt.hasNext()) if (!includeItem(itemsIt.next())) itemsIt.remove();
        }
        
        NodesComputer<Integer> computer = new NodesComputer<Integer>(items.size(), maxPropertyItems) {
            protected boolean sorts(DataType dataType) {
                return !DataType.COUNT.equals(dataType);
            }
            protected HeapViewerNode createNode(Integer index) {
                return TruffleInstancePropertyProvider.this.createNode(items.get(index), heap);
            }
            protected ProgressIterator<Integer> objectsIterator(int index, Progress progress) {
                Iterator<Integer> iterator = integerIterator(index, items.size());
                return new ProgressIterator<>(iterator, index, false, progress);
            }
            protected String getMoreNodesString(String moreNodesCount)  {
                return Bundle.TruffleInstancePropertyProvider_MoreNodes(moreNodesCount, propertyName);
            }
            protected String getSamplesContainerString(String objectsCount)  {
                return Bundle.TruffleInstancePropertyProvider_SamplesContainer(objectsCount, propertyName);
            }
            protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                return Bundle.TruffleInstancePropertyProvider_NodesContainer(firstNodeIdx, lastNodeIdx, propertyName);
            }
        };

        return computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders, progress);
    }
    
    
    public static abstract class ArrayItems<O extends TruffleObject, T extends TruffleType<O>, F extends TruffleLanguageHeapFragment<O, T>, L extends TruffleLanguage<O, T, F>> extends TruffleInstancePropertyProvider<O, T, F, L, ArrayItemValue> {
        
        public ArrayItems(String propertyName, L language, boolean filtersProperties) {
            super(propertyName, language, false, filtersProperties, UIThresholds.MAX_ARRAY_ITEMS);
        }
        
        
        @Override
        public boolean supportsNode(HeapViewerNode parent, Heap heap, String viewID) {
            if (parent instanceof InstanceNode && !InstanceNode.Mode.INCOMING_REFERENCE.equals(((InstanceNode)parent).getMode())) {
                Instance instance = ((InstanceNode)parent).getInstance();
                return instance instanceof ObjectArrayInstance;
            } else {
                return false;
            }
        }
        
        @Override
        protected Collection<ArrayItemValue> getPropertyItems(Instance instance) {
            return ((ObjectArrayInstance)instance).getItems();
        }
        
        
        protected abstract HeapViewerNode createObjectArrayItemNode(O object, String type, ArrayItemValue item);
        
        
        @Override
        protected boolean includeItem(ArrayItemValue item) {
            Instance instance = item.getInstance();

            // display null fields
            if (instance == null) return true;
            
            // display primitive arrays
            if (instance instanceof PrimitiveArrayInstance) return true;
            
            // display language objects
            if (getLanguage().isLanguageObject(instance)) return true;

            // display DynamicObject fields
            if (DynamicObject.isDynamicObject(instance)) return true;

            // display selected Java fields
            return includeInstance(instance);
        }
        
        protected boolean includeInstance(Instance instance) { return true; }
        
        @Override
        protected final HeapViewerNode createNode(ArrayItemValue item, Heap heap) {
            Instance instance = item.getInstance();
            if (getLanguage().isLanguageObject(instance)) {
                O object = getLanguage().createObject(instance);
                return createObjectArrayItemNode(object, object.getType(), item);
            } else {
                return createForeignArrayItemNode(instance, item, heap);
            }
        }
        
        protected HeapViewerNode createForeignArrayItemNode(Instance instance, ArrayItemValue item, Heap heap) {
            if (DynamicObject.isDynamicObject(instance)) {
                DynamicObject dobj = new DynamicObject(instance);
                return new DynamicObjectArrayItemNode<>(dobj, dobj.getType(), item);
            } else {
                return new TerminalJavaNodes.ArrayItem(item, false);
            }
        }
        
    }
    
}
