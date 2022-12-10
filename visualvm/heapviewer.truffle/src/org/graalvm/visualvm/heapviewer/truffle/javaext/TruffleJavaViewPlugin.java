/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.truffle.javaext;

import java.util.List;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.SortOrder;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.java.InstanceNode;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.model.RootNode;
import org.graalvm.visualvm.heapviewer.model.TextNode;
import org.graalvm.visualvm.heapviewer.truffle.TruffleLanguageHeapFragment;
import org.graalvm.visualvm.heapviewer.ui.HeapViewPlugin;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.TreeTableView;
import org.graalvm.visualvm.heapviewer.ui.TreeTableViewColumn;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TruffleJavaViewPlugin_Name=Java Object",
    "TruffleJavaViewPlugin_Description=Java Object",
    "TruffleJavaViewPlugin_NoSelection=<no object selected>"
})
public class TruffleJavaViewPlugin extends HeapViewPlugin {
    
    private final Heap heap;
    private Instance selected;
    
    private final TreeTableView objectsView;
    
    
    public TruffleJavaViewPlugin(HeapContext context, HeapViewerActions actions) {
        super(Bundle.TruffleJavaViewPlugin_Name(), Bundle.TruffleJavaViewPlugin_Description(), Icons.getIcon(GeneralIcons.JAVA_PROCESS));
        
        heap = context.getFragment().getHeap();
        
        objectsView = new TreeTableView("java_objects_truffleext", context, actions, TreeTableViewColumn.instancesPlain(heap)) { // NOI18N
            protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
                InstanceNode instanceNode = selected == null ? null : new InstanceNode(selected);
                HeapViewerNode result = instanceNode == null ? new TextNode(Bundle.TruffleJavaViewPlugin_NoSelection()) : instanceNode;
                return new HeapViewerNode[] { result };
            }
            protected void childrenChanged() {
                HeapViewerNode[] children = getRoot().getChildren();
                for (HeapViewerNode child : children) expandNode(child);
                
                if (children.length > 0) {
                    children = children[0].getChildren();
                    if (children.length > 0 && children[0] instanceof TextNode) expandNode(children[0]);
                }
            }
        };
    }

    
    protected JComponent createComponent() {
        return objectsView.getComponent();
    }
    
    
    @Override
    protected void closed() {
        synchronized (objectsView) { selected = TruffleViewPlugin.NO_INSTANCE; }
        objectsView.closed();
    }
    
    
    protected void nodeSelected(HeapViewerNode node, boolean adjusting) {
        Instance selectedInstance = node == null ? null : HeapViewerNode.getValue(node, DataType.INSTANCE, heap);
        if (Objects.equals(selected, selectedInstance)) return;

        selected = selectedInstance;
        
        objectsView.reloadView();
    }
    
    
    @ServiceProvider(service=HeapViewPlugin.Provider.class, position = 1000)
    public static class Provider extends HeapViewPlugin.Provider {

        public HeapViewPlugin createPlugin(HeapContext context, HeapViewerActions actions, String viewID) {
            if (TruffleLanguageHeapFragment.isTruffleHeap(context))
                return new TruffleJavaViewPlugin(context, actions);
            return null;
        }
        
    }
    
}
