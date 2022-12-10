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

package org.graalvm.visualvm.lib.jfluid.results.cpu;

import org.graalvm.visualvm.lib.jfluid.results.CCTNode;
import org.graalvm.visualvm.lib.jfluid.results.ExportDataDumper;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Presentation-Time CPU Profiling Calling Context Tree (CCT) Node backed by the flattened tree data array in
 * CPUCCTContainer. These nodes are constructed on demand, i.e. only when the user opens some node in the CCT on screen.
 * They contain minimum amount of data in the node instance itself. As a result, a tree constructed of such nodes has
 * a very small overhead on top of the flattened data that already exists (and has relatively low space consumption and
 * construction time). The drawback is that it's difficult to add elements to a tree represented in the flattened form.
 *
 * @author Misha Dmitriev
 */
public class PrestimeCPUCCTNodeBacked extends PrestimeCPUCCTNode {

    private static NumberFormat percentFormat=null;
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected int selfCompactDataOfs;
    protected Set<Integer> compactDataOfs;
    protected int nChildren;

//    protected int methodID;
//
//    protected int nCalls;
//    protected long sleepTime0;
//    protected long totalTime0;
//    protected long totalTime1;
//    protected long waitTime0;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Constructor for creating normal nodes representing methods
     */
    public PrestimeCPUCCTNodeBacked(CPUCCTContainer container, PrestimeCPUCCTNode parent, int compactDataOfs) {
        super(container, parent, container.getMethodIdForNodeOfs(compactDataOfs));
        selfCompactDataOfs = compactDataOfs;
        this.compactDataOfs = new HashSet();
        this.compactDataOfs.add(selfCompactDataOfs);
        this.container = container;
        
        nChildren = container.getNChildrenForNodeOfs(compactDataOfs);
        
//        methodId = container.getMethodIdForNodeOfs(compactDataOfs);
        nCalls = container.getNCallsForNodeOfs(compactDataOfs);
        sleepTime0 = container.getSleepTime0ForNodeOfs(compactDataOfs);
        totalTime0 = container.getTotalTime0ForNodeOfs(compactDataOfs);
        if (container.collectingTwoTimeStamps)
            totalTime1 = container.getTotalTime1ForNodeOfs(compactDataOfs);
        waitTime0 = container.getWaitTime0ForNodeOfs(compactDataOfs);
    }

    /**
     * Constructor for creating a node that represent a whole thread
     */
    protected PrestimeCPUCCTNodeBacked(CPUCCTContainer container, PrestimeCPUCCTNode[] children) {
        super(container, null, -1);
        setThreadNode();
        this.children = children;
        nChildren = children == null ? 0 : children.length;
        for (int i = 0; i < nChildren; i++)
            if (children[i] != null) children[i].parent = this;
    }
    
    PrestimeCPUCCTNodeBacked() {}

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    PrestimeCPUCCTNode createCopy() {
        PrestimeCPUCCTNodeBacked copy = new PrestimeCPUCCTNodeBacked();
        setupCopy(copy);
        return copy;
    }
    
    void setupCopy(PrestimeCPUCCTNodeBacked node) {
        super.setupCopy(node);
        node.selfCompactDataOfs = selfCompactDataOfs;
        node.compactDataOfs = compactDataOfs;
//        node.compactDataOfs = new HashSet();
//        node.compactDataOfs.add(node.selfCompactDataOfs);
        node.nChildren = nChildren;
    }
    
    
    public CCTNode createFilteredNode() {
        PrestimeCPUCCTNodeBacked filtered = new PrestimeCPUCCTNodeBacked();
        setupFilteredNode(filtered);
        return filtered;
    }
    
    protected void setupFilteredNode(PrestimeCPUCCTNodeBacked filtered) {
        super.setupFilteredNode(filtered);
        filtered.nChildren = filtered.children.length;
    }
    
    public void merge(CCTNode node) {
        super.merge(node);
        nChildren = children.length;
    }
    
    public PrestimeCPUCCTNodeBacked createRootCopy() {
        PrestimeCPUCCTNodeBacked copy = new PrestimeCPUCCTNodeBacked(container, parent, selfCompactDataOfs);
        
        copy.parent = null;
        
        copy.compactDataOfs.clear();
        copy.compactDataOfs.addAll(compactDataOfs);
        
        copy.children = null;
        copy.nChildren = nChildren;
        
        copy.methodId = methodId;
        copy.nCalls = nCalls;
        copy.sleepTime0 = sleepTime0;
        copy.totalTime0 = totalTime0;
        copy.totalTime1 = totalTime1;
        copy.waitTime0 = waitTime0;
        
        return copy;
    }

    public CCTNode getChild(int index) {
        getChildren();

        if (index < children.length) {
            return children[index];
        } else {
            return null;
        }
    }
    
    public CCTNode[] getChildren() {
        if (nChildren == 0) {
            return null;
        } else if (children != null) {
            return children;
        }
        
        List<PrestimeCPUCCTNodeBacked> childrenL = new ArrayList();
//        PrestimeCPUCCTNodeBacked filtered = null;
        
//        FilterSortSupport.Configuration config = container.getCPUResSnapshot().getFilterSortInfo(this);
        
        for (int ofs : compactDataOfs) {
            int chcount = container.getNChildrenForNodeOfs(ofs);
            for (int i = 0; i < chcount; i++) {
                PrestimeCPUCCTNodeBacked ch = new PrestimeCPUCCTNodeBacked(container,
                        this, container.getChildOfsForNodeOfs(ofs, i));
//                if (FilterSortSupport.passesFilter(config, ch.getNodeName())) {
                    int chindex = childrenL.indexOf(ch);
                    if (chindex != -1) childrenL.get(chindex).merge(ch);
                    else childrenL.add(ch);
//                } else {
//                    if (filtered == null) {
//                        filtered = ch;
//                        ch.setFilteredNode();
//                        childrenL.add(filtered);
//                    } else {
//                        filtered.merge(ch);
//                    }
//                }
            }
        }

        if (hasSelfTimeChild()) {
            PrestimeCPUCCTNodeBacked selfTimeChild =
                    new PrestimeCPUCCTNodeBacked(container, parent, selfCompactDataOfs);
            selfTimeChild.setSelfTimeNode();
            childrenL.add(selfTimeChild);
        }
        
//        if (isFilteredNode() && filtered != null && childrenL.size() == 1) {
//            // "naive" approach, collapse simple chain of filtered out nodes
//            children = (PrestimeCPUCCTNode[])filtered.getChildren();
//            nChildren = children == null ? 0 : children.length;
//            compactDataOfs = filtered.compactDataOfs;
//        } else {
            nChildren = childrenL.size();
            children = childrenL.toArray(new PrestimeCPUCCTNode[0]);
//        }
        
//        // Now that children are created, sort them in the order previously used
//        sortChildren(config.getSortBy(), config.getSortOrder());
        
        return children;
    }
    
    public boolean isLeaf() {
        if (nChildren == 0) return true;
        else if (children == null) return false;
        else return children.length == 0;
    }
    
    private boolean hasSelfTimeChild() {
        return !isThreadNode() && !isFiltered() && compactDataOfs.size() == 1;
    }
    
    protected void merge(PrestimeCPUCCTNodeBacked node) {
//        children = null;
//        nChildren += node.nChildren;
//        
//        nCalls += node.nCalls;
//        sleepTime0 += node.sleepTime0;
//        totalTime0 += node.totalTime0;
//        totalTime1 += node.totalTime1;
//        waitTime0 += node.waitTime0;
    }
    
    protected void resetChildren() {
//        if (compactDataOfs != null) {
//            compactDataOfs.clear();
//            compactDataOfs.add(selfCompactDataOfs);
//            nChildren = container.getNChildrenForNodeOfs(selfCompactDataOfs);
//        }
//        
//        if (children == null) return;
//        
//        if (!isThreadNode() || parent != null) { // thread nodes
//            children = null;
//        } else {
//            super.resetChildren();
//        }
    }
    
    public void setSelfTimeNode() {
        super.setSelfTimeNode();
        nChildren = 0;
        children = null;
        int ofs = selfCompactDataOfs;
        totalTime0 = container.getSelfTime0ForNodeOfs(ofs);
        if (container.collectingTwoTimeStamps)
            totalTime1 = container.getSelfTime1ForNodeOfs(ofs);
    }

//    public int getMethodId() {
//        return methodID;
//    }

//    public int getNCalls() {
//        return nCalls;
//    }

    public int getNChildren() {
        if (getChildren() == null) return 0;
        return nChildren;
    }

//    public long getSleepTime0() {
//        return sleepTime0;
//
//        // TODO: [wait] self time node?
//    }

//    public int getThreadId() {
//        return container.getThreadId();
//    }

//    public long getTotalTime0() {
//        return totalTime0;
//    }

    public float getTotalTime0InPerCent() {
        float result = (float) ((container.getWholeGraphNetTime0() > 0)
                                ? ((double) totalTime0 / (double) container.getWholeGraphNetTime0() * 100.0) : 0);

        return (result < 100) ? result : 100;
    }

//    public long getTotalTime1() {
//        return totalTime1;
//    }

    public float getTotalTime1InPerCent() {
        return (float) ((container.getWholeGraphNetTime1() > 0)
                        ? ((double) totalTime1 / (double) container.getWholeGraphNetTime1() * 100.0) : 0);
    }

//    public long getWaitTime0() {
//        return waitTime0;
//
//        // TODO: [wait] self time node?
//    }
    
    public void exportXMLData(ExportDataDumper eDD,String indent) {
        String newline = System.getProperty("line.separator"); // NOI18N
        StringBuffer result = new StringBuffer(indent+"<node>"+newline); //NOI18N
        result.append(indent).append(" <Name>").append(replaceHTMLCharacters(getNodeName())).append("</Name>").append(newline); //NOI18N
        CCTNode p = getParent();
        result.append(indent).append(" <Parent>").append(replaceHTMLCharacters((p==null)?("none"):(((PrestimeCPUCCTNodeBacked)getParent()).getNodeName()))).append("</Parent>").append(newline); //NOI18N
        result.append(indent).append(" <Time_Relative>").append(percentFormat.format(p!=null?(((double) getTotalTime0InPerCent())/100):100)).append("</Time_Relative>").append(newline); //NOI18N
        result.append(indent).append(" <Time>").append( p!=null ? getTotalTime0() : "N/A" ).append("</Time>").append(newline); //NOI18N
        if (container.collectingTwoTimeStamps) {
            result.append(indent).append(" <Time-CPU>").append(getTotalTime1()).append("</Time-CPU>").append(newline); //NOI18N
        }
        result.append(indent).append(" <Invocations>").append( p!=null ? getNCalls() : 1).append("</Invocations>").append(newline); //NOI18N
        eDD.dumpData(result); //dumps the current row
        // children nodes
        if (children!=null) {
            for (int i = 0; i < nChildren; i++) {
                ((PrestimeCPUCCTNodeBacked)children[i]).exportXMLData(eDD, indent+"  "); //NOI18N
            }
        } else {
            if (nChildren>0) {
                int tempNChildren=nChildren;
                PrestimeCPUCCTNode[] tempChildren=(PrestimeCPUCCTNode[]) getChildren();
                children=null;
                for (int i = 0; i < nChildren; i++) {
                    ((PrestimeCPUCCTNodeBacked)tempChildren[i]).exportXMLData(eDD, indent+"  "); //NOI18N
                }
                nChildren=tempNChildren;
            }
        }
        result=new StringBuffer(indent+"</node>"); //NOI18N
        eDD.dumpData(result);
    }

    public void exportHTMLData(ExportDataDumper eDD, int depth) {
        StringBuffer result = new StringBuffer("<tr><td class=\"method\"><pre class=\"method\">."); //NOI18N
        for (int i=0; i<depth; i++) {
            result.append("."); //NOI18N
        }
        result.append(replaceHTMLCharacters(getNodeName())).append("</pre></td><td class=\"right\">").append(percentFormat.format(((double) getTotalTime0InPerCent())/100)).append("</td><td class=\"right\">").append(getTotalTime0()); //NOI18N
        if (container.collectingTwoTimeStamps) {
            result.append("</td><td class=\"right\">").append(getTotalTime1()); //NOI18N
        }
        result.append("</td><td class=\"right\">").append(getNCalls()).append("</td></tr>"); //NOI18N
        eDD.dumpData(result); //dumps the current row
        // children nodes
        if (children!=null) {
            for (int i = 0; i < nChildren; i++) {
                ((PrestimeCPUCCTNodeBacked)children[i]).exportHTMLData(eDD, depth+1);
            }
        } else {
            if (nChildren>0) {
                int tempNChildren=nChildren;
                PrestimeCPUCCTNode[] tempChildren=(PrestimeCPUCCTNode[]) getChildren();
                children=null;
                for (int i = 0; i < nChildren; i++) {
                    ((PrestimeCPUCCTNodeBacked)tempChildren[i]).exportHTMLData(eDD, depth+1);
                }
                nChildren=tempNChildren;
            }
        }
    }

    private String replaceHTMLCharacters(String s) {
        StringBuilder sb = new StringBuilder();
        int len = s.length();
        for (int i = 0; i < len; i++) {
          char c = s.charAt(i);
          switch (c) {
              case '<': sb.append("&lt;"); break; // NOI18N
              case '>': sb.append("&gt;"); break; // NOI18N
              case '&': sb.append("&amp;"); break; // NOI18N
              case '"': sb.append("&quot;"); break; // NOI18N
              default: sb.append(c); break;
          }
        }
        return sb.toString();
    }

    public void exportCSVData(String separator, int depth, ExportDataDumper eDD) {
        StringBuffer result = new StringBuffer();
        String newLine = "\r\n"; // NOI18N
        String quote = "\""; // NOI18N
        String indent = " "; // NOI18N

        // this node
        result.append(quote);
        for (int i=0; i<depth; i++) {
            result.append(indent); // to simulate the tree structure in CSV
        }
        result.append(getNodeName()).append(quote).append(separator);
        result.append(quote).append(getTotalTime0InPerCent()).append(quote).append(separator);
        result.append(quote).append(getTotalTime0()).append(quote).append(separator);
        if (container.collectingTwoTimeStamps) {
            result.append(quote).append(getTotalTime1()).append(quote).append(separator);
        }
        result.append(quote).append(getNCalls()).append(quote).append(newLine);
        eDD.dumpData(result); //dumps the current row
        // children nodes
        if (children!=null) {
            for (int i = 0; i < nChildren; i++) {
                ((PrestimeCPUCCTNodeBacked)children[i]).exportCSVData(separator, depth+1, eDD);
            }
        } else {
            if (nChildren>0) {
                int tempNChildren=nChildren;
                PrestimeCPUCCTNode[] tempChildren=(PrestimeCPUCCTNode[]) getChildren();
                children=null;
                for (int i = 0; i < nChildren; i++) {
                    ((PrestimeCPUCCTNodeBacked)tempChildren[i]).exportCSVData(separator, depth+1, eDD);
                }
                nChildren=tempNChildren;
            }
        }
    }

    public static void setPercentFormat(NumberFormat percentFormat) {
        PrestimeCPUCCTNodeBacked.percentFormat = percentFormat;
    }
}
