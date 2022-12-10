/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.results.memory;

import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.jfluid.global.ProfilingSessionStatus;
import org.graalvm.visualvm.lib.jfluid.results.ResultsSnapshot;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.graalvm.visualvm.lib.jfluid.results.FilterSortSupport;


/**
 * Results snapshot for Memory Profiling.
 *
 * @author Ian Formanek
 */
public abstract class MemoryResultsSnapshot extends ResultsSnapshot {
    /***************************************************************************
    +------------------------------------------------------------------------------+
    | Profiler memory snapshot format description                                  |
    +------------------------------------------------------------------------------+
    int         version
    long        timestamp
    long        duration
    int         # profiled classes
    ===> for(# profiled classes)
    string      class name
    long        object size per class
    <===
    boolean     contains stacktraces
    int         # stacktraces
    ===> for(# stacktraces)
    :::> load node
    int         type (RuntimeMemoryCCTNode.TYPE_RuntimeMemoryCCTNode,
                      RuntimeMemoryCCTNode.TYPE_RuntimeObjAllocTermCCTNode,
                      RuntimeMemoryCCTNode.RuntimeObjLivenessTermCCTNode)
    int         methodId
    int         # children
    ======> for(# children)
    >load node<
    <=====
    <::: load node
    <===
    ***************************************************************************/

    private JMethodIdTable table;
    /** [0-nProfiledClasses] class names */
    String[] classNames;

    /** [0-nProfiledClasses] total size in bytes for tracked instances of this class */
    long[] objectsSizePerClass;

    /** [0-nProfiledClasses] class Id -> root of its allocation traces tree */
    private RuntimeMemoryCCTNode[] stacksForClasses;

    /** total number of profiled classes */
    int nProfiledClasses;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public MemoryResultsSnapshot() {
    } // No-arg constructor needed for above serialization methods to work

    public MemoryResultsSnapshot(long beginTime, long timeTaken, MemoryCCTProvider provider, ProfilerClient client)
                          throws ClientUtils.TargetAppOrVMTerminated {
        super(beginTime, timeTaken);

        // TODO [performance]: profile for performance - specifically which of the actions below is most time consuming
        ProfilingSessionStatus status = client.getStatus();
        status.beginTrans(false);

        try {
            performInit(client, provider);           
        } finally {
            status.endTrans();

            if (LOGGER.isLoggable(Level.FINEST)) {
                debugValues();
            }
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public String getClassName(int classId) {
        return classNames[classId];
    }

    public String[] getClassNames() {
        return classNames;
    }

    public JMethodIdTable getJMethodIdTable() {
        return table;
    }

    public int getNProfiledClasses() {
        return nProfiledClasses;
    }

    public long[] getObjectsSizePerClass() {
        return objectsSizePerClass;
    }

    public boolean containsStacks() {
        return stacksForClasses != null;
    }
    
    
    public abstract MemoryResultsSnapshot createDiff(MemoryResultsSnapshot snapshot);
    
    public void filterReverse(String filter, int filterType, int sortBy, boolean sortOrder, PresoObjAllocCCTNode root, int classId, boolean dontShowZeroLiveObjAllocPaths) {
        PresoObjAllocCCTNode rev =
                (PresoObjAllocCCTNode)createPresentationCCT(classId, dontShowZeroLiveObjAllocPaths);
        filter(filter, filterType, rev);
        root.children = rev.children;
        if (root.children != null) {
            for (PresoObjAllocCCTNode ch : root.children)
                ch.parent = root;
            root.sortChildren(sortBy, sortOrder);
        }
        if (!FilterSortSupport.passesFilter(filter, filterType, root.getNodeName())) {
//            root.setFilteredNode();
//            root.methodId = -1;
        } else {
//            root.resetFilteredNode();
        }
    }
    
    private void filter(String filter, int filterType, PresoObjAllocCCTNode node) {
        if (node.children != null) {
            PresoObjAllocCCTNode filtered = null;
            List<PresoObjAllocCCTNode> ch = new ArrayList();
            for (PresoObjAllocCCTNode n : node.children) {
                PresoObjAllocCCTNode nn = (PresoObjAllocCCTNode)n;
                if (FilterSortSupport.passesFilter(filter, filterType, nn.getNodeName())) {
                    int i = ch.indexOf(nn);
                    if (i == -1) ch.add(nn);
                    else ch.get(i).merge(nn);
                } else {
                    if (filtered == null) {
//                        nn.setFilteredNode();
//                        nn.methodId = -1;
                        filtered = nn;
                        ch.add(nn);
                    } else {
                        filtered.merge(nn);
                    }
                }
            }
            
            if (ch.isEmpty()) {
                node.children = null;
            } else {
                if (node.isFiltered() && filtered != null && ch.size() == 1) {
                    // "naive" approach, collapse simple chain of filtered out nodes
                    PresoObjAllocCCTNode n = ch.get(0);
                    filter(filter, filterType, n);
                    node.children = n.children;
                } else {
                    node.children = ch.toArray(new PresoObjAllocCCTNode[0]);
                }
            }
            
            if (node.children != null)
                for (PresoObjAllocCCTNode n : node.children)
                    filter(filter, filterType, (PresoObjAllocCCTNode)n);
        }
    }
    

    /**
     * Creates a presentation-time allocation stack traces CCT for given classId.
     *
     * @param classId                       Class ID of the class whose allocation stack traces we request
     * @param dontShowZeroLiveObjAllocPaths If true, allocation paths with zero live objects will not be included in CCT
     * @return presentation-time CCT with allocation stack traces or null if none are available
     */
    public PresoObjAllocCCTNode createPresentationCCT(int classId, boolean dontShowZeroLiveObjAllocPaths) {
        if (stacksForClasses == null) {
            return null;
        }

        RuntimeMemoryCCTNode rootNode = stacksForClasses[classId];

        if (rootNode == null) {
            return null;
        }

        return createPresentationCCT(rootNode, classId, dontShowZeroLiveObjAllocPaths);
    }

    public void readFromStream(DataInputStream in) throws IOException {
        super.readFromStream(in);

        nProfiledClasses = in.readInt();
        classNames = new String[nProfiledClasses];
        objectsSizePerClass = new long[nProfiledClasses];

        for (int i = 0; i < nProfiledClasses; i++) {
            classNames[i] = in.readUTF();
            objectsSizePerClass[i] = in.readLong();
        }

        if (in.readBoolean()) {
            int len = in.readInt();
            //System.err.println("Read len: " +len);
            stacksForClasses = new RuntimeMemoryCCTNode[len];

            for (int i = 0; i < len; i++) {
                int type = in.readInt();

                //System.err.println("  [" + i + "] = " + type);
                if (type != 0) {
                    stacksForClasses[i] = RuntimeMemoryCCTNode.create(type);
                    stacksForClasses[i].readFromStream(in);
                }
            }

            if (in.readBoolean()) {
                table = new JMethodIdTable();
                table.readFromStream(in);
            }
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            debugValues();
        }
    }

    //---- Serialization support
    public void writeToStream(DataOutputStream out) throws IOException {
        super.writeToStream(out);

        out.writeInt(nProfiledClasses);

        for (int i = 0; i < nProfiledClasses; i++) {
            out.writeUTF(classNames[i]);
            out.writeLong(objectsSizePerClass[i]);
        }

        out.writeBoolean(stacksForClasses != null);

        if (stacksForClasses != null) {
            out.writeInt(stacksForClasses.length);

            //.err.println("Stored len: " +stacksForClasses.length);
            for (RuntimeMemoryCCTNode stacksForClasse : stacksForClasses) {
                if (stacksForClasse == null) {
                    //System.err.println("  [" + i + "] = 0");
                    out.writeInt(0);
                } else {
                    out.writeInt(stacksForClasse.getType());
                    //System.err.println("  [" + i + "] = " + stacksForClasses[i].getType());
                    stacksForClasse.writeToStream(out);
                }
            }

            out.writeBoolean(table != null);

            if (table != null) {
                table.writeToStream(out);
            }
        }
    }

    /**
     * Will create presentation CCT for call stacks for given root node.
     *
     * @param rootNode                      The root node that contains allocation stack traces data
     * @param classId                       Id of class whose allocations we are requesting
     * @param dontShowZeroLiveObjAllocPaths if true, allocation paths with zero live objects will not be included
     * @return a non-null instance of the root of presentation-time allocations CCT
     */
    protected abstract PresoObjAllocCCTNode createPresentationCCT(RuntimeMemoryCCTNode rootNode, int classId,
                                                                  boolean dontShowZeroLiveObjAllocPaths);

    protected void performInit(ProfilerClient client, MemoryCCTProvider provider)
                                 throws ClientUtils.TargetAppOrVMTerminated {
        nProfiledClasses = provider.getNProfiledClasses();

        int len = 0;

        if (provider.getObjectsSizePerClass() != null) {
            //System.err.println("mcgb.objectsSizePerClass len is: "+mcgb.objectsSizePerClass.length);
            len = provider.getObjectsSizePerClass().length;
            objectsSizePerClass = new long[len];
            System.arraycopy(provider.getObjectsSizePerClass(), 0, objectsSizePerClass, 0, len);
        } /*else {
           System.err.println("mcgb.objectsSizePerClass is NULL");
           }   */
        String[] s_classNames = client.getStatus().getClassNames();
        //      len = s_classNames.length;
        len = nProfiledClasses;
        //System.err.println("status.classNames.length is: "+len );
        classNames = new String[len];
        System.arraycopy(s_classNames, 0, classNames, 0, len);

        //      System.out.println("Created snapshot [" + timeTaken + "] with " + classNames.length + " classes; nProfiledClasses = " + nProfiledClasses);
        RuntimeMemoryCCTNode[] stacks = provider.getStacksForClasses();
        if ((stacks != null) && checkContainsStacks(stacks)) {
            stacksForClasses = new RuntimeMemoryCCTNode[stacks.length];

            for (int i = 0; i < stacksForClasses.length; i++) {
                if (stacks[i] != null) {
                    stacksForClasses[i] = (RuntimeMemoryCCTNode) stacks[i].clone();
                }
            }

            table = new JMethodIdTable(client.getJMethodIdTable());
        }        
    }

    private boolean checkContainsStacks(RuntimeMemoryCCTNode[] stacksForClasses) {
        for (RuntimeMemoryCCTNode stacksForClass : stacksForClasses) {
            if (stacksForClass == null) {
                continue;
            }

            if (stacksForClass instanceof RuntimeObjAllocTermCCTNode) {
                continue;
            }

            if (stacksForClass instanceof RuntimeObjLivenessTermCCTNode) {
                continue;
            }

            return true;
        }

        return false; // no data but term nodes or nulls
    }

    void debugValues() {
        LOGGER.finest("nProfiledClasses: " + nProfiledClasses); // NOI18N
        LOGGER.finest("stacksForClasses.length: " + debugLength(stacksForClasses)); // NOI18N
        LOGGER.finest("objectsSizePerClass.length: " + debugLength(objectsSizePerClass));
        LOGGER.finest("classNames.length: " + debugLength(classNames)); // NOI18N
        LOGGER.finest("table: " + ((table == null) ? "null" : table.debug())); // NOI18N
    }
}
