/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.utils;

import java.awt.event.ActionEvent;
import java.text.Format;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.SortOrder;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.MoreNodesNode;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.model.RootNode;
import org.graalvm.visualvm.heapviewer.model.TextNode;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerNodeAction;
import org.graalvm.visualvm.heapviewer.ui.UIThresholds;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.ui.Formatters;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "MoreObjectsNode_SamplesContainer=<sample {0} objects>",
    "MoreObjectsNode_NodesContainer=<objects {0}-{1}>"
})
abstract class MoreObjectsNode<T> extends MoreNodesNode {
    
    private static final int AGGREGATION = 1000;
    private static final int MAX_BUFFER_SIZE = 1000000;
    
    private T[] previousObjects;
    private final T previousObject;
    private final int previousObjectOffset;
    private int lastKnownPreviousObjectIndex;
    
    private final int objectsCount;
    private final int iteratorObjectsCount;
    
    private final int nodesCount;
    private final int nodesOffset;
    
    
    MoreObjectsNode(String text, int objectsCount, int iteratorObjectsCount, T previousObject, int previousObjectOffset) {
        super(text);
        
        this.objectsCount = objectsCount;
        this.iteratorObjectsCount = iteratorObjectsCount;
        
        this.nodesCount = (int)Math.ceil((objectsCount - previousObjectOffset + 1) / (double)AGGREGATION);
        nodesOffset = (int)Math.floor((previousObjectOffset + 1) / (double)AGGREGATION);
        
        this.previousObject = previousObject;
        this.previousObjectOffset = previousObjectOffset;
        
        resetChildren();
    }
    
    
    protected abstract boolean sorts(DataType dataType);
    
    protected abstract HeapViewerNode createNode(T object);
    
    // Preferably a ProgressIterator or its wrapper updating the progress
    protected abstract Iterator<T> objectsIterator(int index, Progress progress);
    
    
    protected String getSamplesContainerString(String objectsCount)  {
        return Bundle.MoreObjectsNode_SamplesContainer(objectsCount);
    }
    
    protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
        return Bundle.MoreObjectsNode_NodesContainer(firstNodeIdx, lastNodeIdx);
    }
    
    
    protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
        lastKnownPreviousObjectIndex = -1;
        
        if (nodesCount == 1) {
            return computeChildren(-1, heap, viewID, null, dataTypes, sortOrders, progress);
        } else {
            previousObjects = (T[])new Object[nodesCount - 1];
            
            Format format = Formatters.numberFormat();
            
            int containersOffset = objectsCount >= UIThresholds.SAMPLE_OBJECTS_THRESHOLD ? 1 : 0;
            HeapViewerNode[] nodes = new HeapViewerNode[nodesCount + containersOffset];
            
            for (int i = 0; i < nodesCount; i++) {
                int firstItem = getFirstItemIndex(i) + 1;
                int lastItem = getLastItemIndex(i) + 1;
                nodes[i + containersOffset] = new ObjectsContainer(getNodesContainerString(format.format(firstItem), format.format(lastItem)), i);
            }
            
            if (containersOffset > 0) nodes[0] = new SampleContainer(getSamplesContainerString(format.format(UIThresholds.SAMPLE_OBJECTS_COUNT)), UIThresholds.SAMPLE_OBJECTS_COUNT);
            
            return nodes;
        }
    }
    
    
    private T getPreviousObject(int containerIndex, Heap heap, HeapViewerNodeFilter viewFilter, DataType dataType, SortOrder sortOrder, Progress progress) throws InterruptedException {
        if (containerIndex <= 0) return previousObject;
        
        T object = previousObjects[containerIndex - 1];
        if (object != null) return object;
        
        int steps = containerIndex - lastKnownPreviousObjectIndex - 1; /*System.err.println(">>> steps " + steps);*/
        int bufferSize = (steps + nodesOffset) * AGGREGATION;
        if (lastKnownPreviousObjectIndex == -1) bufferSize -= (previousObjectOffset + 1); /*System.err.println(">>> bufferSize " + bufferSize);*/
//        System.err.println(">>>   XXX bufferSize " + bufferSize);
        
        if (bufferSize > MAX_BUFFER_SIZE) {
            // -----------------------------------------------------------------
            // --- TODO:
            // ---  - improve to minimize bufferIterations (include the result for long[] getInstanceIDs)
            // ---  - improve to revert the sortOrder for second half of the results
            // -----------------------------------------------------------------
            
//            System.err.println(">>> bufferSize " + bufferSize + ", MAX_BUFFER_SIZE " + MAX_BUFFER_SIZE);
            int bufferIterations = bufferSize / MAX_BUFFER_SIZE;
            if (bufferIterations * MAX_BUFFER_SIZE < bufferSize) bufferIterations += 1;
//            System.err.println(">>> containerIndex " + containerIndex + ", lastInstanceIDIndex " + lastInstanceIDIndex + ", bufferSize " + bufferSize + ", bufferIterations " + bufferIterations);
            int bufferDelta = steps / bufferIterations;
//            System.err.println(">>> bufferDelta " + bufferDelta);

            Thread worker = Thread.currentThread();
            
            int lastInstanceIDIndexX = lastKnownPreviousObjectIndex;
            for (int i = 1; i <= bufferIterations; i++) {
                getPreviousObject(lastInstanceIDIndexX + bufferDelta * i, heap, viewFilter, dataType, sortOrder, progress);
                if (worker.isInterrupted()) throw new InterruptedException();
            }
            
            return getPreviousObject(containerIndex, heap, viewFilter, dataType, sortOrder, progress);
        } else {
            object = lastKnownPreviousObjectIndex == -1 ? previousObject : previousObjects[lastKnownPreviousObjectIndex]; /*System.err.println(">>> instance " + new InstanceNode(heap.getInstanceByID(instance)));*/

            SortedObjectsBuffer<T> buffer = new SortedObjectsBuffer<T>(bufferSize, object, dataType, sortOrder, viewFilter, heap, getParent()) {
                protected boolean sorts(DataType dataType) { return MoreObjectsNode.this.sorts(dataType); }
                protected HeapViewerNode createNode(T object) { return MoreObjectsNode.this.createNode(object); }
            };
            
//            progress.setupKnownSteps(iteratorObjectsCount);
            
            Iterator<T> objectsIt = objectsIterator(0, progress);
            while (objectsIt.hasNext()) buffer.add(objectsIt.next());
            
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            
            T[] objects = buffer.getObjects();
            
//            progress.finish();
            
            int offset = lastKnownPreviousObjectIndex == -1 ? -previousObjectOffset - 1 : 0; /*System.err.println(">>> offset in results " + offset);*/
            for (int i = 0; i < steps; i++) {
                int resultsIndex = (i + 1 + nodesOffset) * AGGREGATION + offset - 1; /*System.err.println(">>>    resultsIndex " + resultsIndex);*/
                int updatedIndex = lastKnownPreviousObjectIndex + 1 + i; /*System.err.println(">>>    updatedIndex " + updatedIndex);*/
                previousObjects[updatedIndex] = objects[resultsIndex];
            }

            lastKnownPreviousObjectIndex = containerIndex - 1; /*System.err.println(">>> lastInstanceIDIndex " + lastInstanceIDIndex);*/
            return previousObjects[containerIndex - 1];
        }
    }
    
    private T[] getObjects(int containerIndex, Heap heap, HeapViewerNodeFilter viewFilter, DataType dataType, SortOrder sortOrder, Progress progress) throws InterruptedException {
        int start = MoreObjectsNode.this.getFirstItemIndex(containerIndex);
        int end = MoreObjectsNode.this.getLastItemIndex(containerIndex);
        
        T object;
        
        try {
            progress.setupUnknownSteps();
            
            object = getPreviousObject(containerIndex, heap, viewFilter, dataType, sortOrder, progress);
        } finally {
            progress.finish();
        }
        
        SortedObjectsBuffer<T> buffer = new SortedObjectsBuffer<T>(end - start + 1, object, dataType, sortOrder, viewFilter, heap, getParent()) {
            protected boolean sorts(DataType dataType) { return MoreObjectsNode.this.sorts(dataType); }
            protected HeapViewerNode createNode(T object) { return MoreObjectsNode.this.createNode(object); }
        };
        
        T[] objects;
        
        try {
            progress.setupKnownSteps(iteratorObjectsCount);
            
            Iterator<T> objectsIt = objectsIterator(0, progress);
            while (objectsIt.hasNext()) buffer.add(objectsIt.next());
            
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            
            objects = buffer.getObjects();
        } finally {        
            progress.finish();
        }
        
        if (containerIndex >= 0 && containerIndex < nodesCount - 1) {
            if (previousObjects[containerIndex] == null) {
                previousObjects[containerIndex] = objects[objects.length - 1];
                lastKnownPreviousObjectIndex = containerIndex;
            }
        }
        
        return objects;
    }
    
    private int getFirstItemIndex(int containerIndex) {
        return containerIndex <= 0 ? previousObjectOffset + 1 : AGGREGATION * (containerIndex + nodesOffset);
    }
    
    private int getLastItemIndex(int containerIndex) {
        return containerIndex >= 0 && containerIndex < nodesCount - 1 ? AGGREGATION * (containerIndex + 1 + nodesOffset) - 1 : objectsCount - 1;
    }
    
    private HeapViewerNode[] loadChildren(int containerIndex, Progress progress) throws InterruptedException {
        int start = MoreObjectsNode.this.getFirstItemIndex(containerIndex);
        int end = MoreObjectsNode.this.getLastItemIndex(containerIndex);
        
        HeapViewerNode[] nodes;
        
        try {
            progress.setupKnownSteps(end);
            
            int i = 0;
            nodes = new HeapViewerNode[end - start + 1];
            Iterator<T> objectsIt = objectsIterator(start, progress);
            while (i < nodes.length && objectsIt.hasNext()) nodes[i++] = createNode(objectsIt.next());
            
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        } finally {        
            progress.finish();
        }

        return nodes;
    }
    
    private HeapViewerNode[] computeChildren(int containerIndex, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {            
//        long start = System.currentTimeMillis();
//        try {
        
        // No sorting - plain fetch
        SortOrder sortOrder = sortOrders == null || sortOrders.isEmpty() ? null : sortOrders.get(0);
        if (sortOrder == null || sortOrder.equals(SortOrder.UNSORTED)) return MoreObjectsNode.this.loadChildren(containerIndex, progress);

        // Sorting by count or own size - plain fetch
        DataType dataType = dataTypes == null || dataTypes.isEmpty() ? null : dataTypes.get(0);
        if (dataType == null || !sorts(dataType)) return MoreObjectsNode.this.loadChildren(containerIndex, progress);

        // Sorting - must resolve instanceIDs
        T[] objects = MoreObjectsNode.this.getObjects(containerIndex, heap, null, dataType, sortOrder, progress);
        
//        System.err.println(">>> Children: " + Arrays.toString(objects));
        Thread worker = Thread.currentThread();
        HeapViewerNode[] nodes = new HeapViewerNode[objects.length];
        for (int i = 0; i < nodes.length; i++) {
//            System.err.println(">>> Creating node at idx " + i + " from object " + objects[i]);
            nodes[i] = createNode(objects[i]);
            if (worker.isInterrupted()) throw new InterruptedException();
        }

        return nodes;
        
//        } finally {
//            System.err.println(">>> Container " + containerIndex + " computed in " + (System.currentTimeMillis() - start));
//        }
    }
    
    private HeapViewerNode[] computeSampleChildren(int type, int count, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
        // TODO: use random-access for indexable version
        int index = 0;
        int step = objectsCount / (count - 1);
        
        HeapViewerNode[] nodes;
        
        try {
            progress.setupKnownSteps(iteratorObjectsCount);
            
            int i = 0;
            Integer[] indexes = new Integer[count];
            if (type == 0) {
                int hitIndex = 0;
                for (int j = 0; j < count; j++) {
                    indexes[j] = hitIndex;
                    hitIndex = j == count - 2 ? objectsCount - 1 : hitIndex + step;
                }
            } else {
                Random r = new Random(type*7);
                SortedSet<Integer> idSet = new TreeSet();
                while (idSet.size() < count) {
                     idSet.add(r.nextInt(objectsCount));
                }
                idSet.toArray(indexes);
            }
            nodes = new HeapViewerNode[count];
            Iterator<T> objectsIt = objectsIterator(0, progress);
            while (index < count && i < objectsCount && objectsIt.hasNext()) {
                T object = objectsIt.next();

                if (i == indexes[index]) {
                    nodes[index++] = createNode(object);
                }
                
                i++;
            }

            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        } finally {
            progress.finish();
        }
        
        return nodes;
    }
    
    
    private class SampleContainer extends TextNode {
        
        private final int count;
        private int type;
        
        SampleContainer(String text, int count) {
            super(text);
            this.count = count;
            resetChildren();
        }
        
        protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {            
            return MoreObjectsNode.this.computeSampleChildren(type, count, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
        }
        
        void incrementType() {
            type++;
            resetChildren();
            RootNode root = RootNode.get(this);
            if (root != null) {
                // clear node cache
                root.retrieveChildren(this);
                root.updateChildren(root);
            }
        }
    }
    
    @ServiceProvider(service=HeapViewerNodeAction.Provider.class)
    public static class ShuffleActionProvider extends HeapViewerNodeAction.Provider {

        @Override
        public boolean supportsView(HeapContext context, String viewID) {
            return true;
        }

        @Override
        public HeapViewerNodeAction[] getActions(HeapViewerNode node, HeapContext context, HeapViewerActions actions) {
            if (node instanceof MoreObjectsNode.SampleContainer) {
                return new HeapViewerNodeAction[]{new ShuffleAction((MoreObjectsNode.SampleContainer)node)};
            }
            return null;
        }
    }

    @NbBundle.Messages("MoreObjectsNode_ShuffleAction=Different Sample")
    private static class ShuffleAction extends HeapViewerNodeAction {

        private final MoreObjectsNode.SampleContainer node;

        ShuffleAction(MoreObjectsNode.SampleContainer node) {
            super(org.graalvm.visualvm.heapviewer.utils.Bundle.MoreObjectsNode_ShuffleAction(), 110);
            this.node = node;
            setEnabled(!node.isLeaf());
        }

        public boolean isMiddleButtonDefault(ActionEvent e) {
            int modifiers = e.getModifiers();
            return (modifiers & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK &&
                   (modifiers & ActionEvent.SHIFT_MASK) != ActionEvent.SHIFT_MASK;
        }

        public void actionPerformed(ActionEvent e) {
            node.incrementType();
        }
    }

    private class ObjectsContainer extends TextNode {
        
        private final int containerIndex;
    
        ObjectsContainer(String text, int containerIndex) {
            super(text);
            this.containerIndex = containerIndex;
            resetChildren();
        }
        
        protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {            
            return MoreObjectsNode.this.computeChildren(containerIndex, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
        }
    
    }
    
}
