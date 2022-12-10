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
package org.graalvm.visualvm.lib.ui.locks;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreeNode;
import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.results.CCTNode;
import org.graalvm.visualvm.lib.jfluid.results.locks.LockCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.locks.LockRuntimeCCTNode;
import org.graalvm.visualvm.lib.jfluid.utils.StringUtils;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.components.FlatToolBar;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.ui.components.table.LabelBracketTableCellRenderer;
import org.graalvm.visualvm.lib.ui.components.table.LabelTableCellRenderer;
import org.graalvm.visualvm.lib.ui.results.DataView;
import org.graalvm.visualvm.lib.ui.swing.PopupButton;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTable;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTableContainer;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTreeTable;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTreeTableModel;
import org.graalvm.visualvm.lib.ui.swing.SearchUtils;
import org.graalvm.visualvm.lib.ui.swing.renderer.HideableBarRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.McsTimeRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.NumberPercentRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.NumberRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class LockContentionPanel extends DataView {
    
    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.locks.Bundle"); // NOI18N
    private static final String ENABLE_LOCKS_MONITORING = messages.getString("LockContentionPanel_EnableLocksMonitoring"); // NOI18N
    private static final String ENABLE_LOCKS_MONITORING_TOOLTIP = messages.getString("LockContentionPanel_EnableLocksMonitoringToolTip"); // NOI18N
    private static final String NO_PROFILING = messages.getString("LockContentionPanel_NoProfiling"); // NOI18N
    private static final String LOCKS_THREADS_COLUMN_NAME = messages.getString("LockContentionPanel_LocksThreadsColumnName"); // NOI18N
    private static final String LOCKS_THREADS_COLUMN_TOOLTIP = messages.getString("LockContentionPanel_LocksThreadsColumnToolTip"); // NOI18N
//    private static final String TIME_COLUMN_NAME = messages.getString("LockContentionPanel_TimeColumnName"); // NOI18N
//    private static final String TIME_COLUMN_TOOLTIP = messages.getString("LockContentionPanel_TimeColumnToolTip"); // NOI18N
    private static final String TIME_REL_COLUMN_NAME = messages.getString("LockContentionPanel_TimeRelColumnName"); // NOI18N
    private static final String TIME_REL_COLUMN_TOOLTIP = messages.getString("LockContentionPanel_TimeRelColumnToolTip"); // NOI18N
    private static final String WAITS_COLUMN_NAME = messages.getString("LockContentionPanel_WaitsColumnName"); // NOI18N
    private static final String WAITS_COLUMN_TOOLTIP = messages.getString("LockContentionPanel_WaitsColumnToolTip"); // NOI18N
    private static final String DISPLAY_MODE = messages.getString("LockContentionPanel_DisplayMode"); // NOI18N
    private static final String MODE_THREADS = messages.getString("LockContentionPanel_ModeThreads"); // NOI18N
    private static final String MODE_MONITORS = messages.getString("LockContentionPanel_ModeMonitors"); // NOI18N
    private static final String SEARCH_THREADS_SCOPE = messages.getString("LockContentionPanel_SearchThreadsScope"); // NOI18N
    private static final String SEARCH_MONITORS_SCOPE = messages.getString("LockContentionPanel_SearchMonitorsScope"); // NOI18N
    private static final String SEARCH_SCOPE_TOOLTIP = messages.getString("LockContentionPanel_SearchScopeTooltip"); // NOI18N
    // -----
    private boolean refreshIsRunning;
    
    public static enum Aggregation { BY_THREADS, BY_MONITORS }
    
    private final ProfilerToolbar toolbar;
    
    private final LocksTreeTableModel treeTableModel;
    private final ProfilerTreeTable treeTable;
    private final ProfilerTableContainer treeTablePanel;
    private final JComboBox modeCombo;
    
    private int columnCount;
    
    private String[] columnNames;
    private TableCellRenderer[] columnRenderers;
    private String[] columnToolTips;
    private int[] columnWidths;
    
    private Aggregation aggregation = Aggregation.BY_THREADS;
    
    private final JPanel contentPanel;
    private final JPanel notificationPanel;
    private final JButton enableLockContentionButton;
    private final JLabel enableLockContentionLabel1;
    private final JLabel enableLockContentionLabel2;
    
    private LockRuntimeCCTNode root;
    private long countsInMicrosec = 1;
    
    private final HideableBarRenderer hbrTime;
    private final HideableBarRenderer hbrWaits;

    private long lastupdate;
    
    private boolean searchThreads = true;
    private boolean searchMonitors = true;
    
    
    public LockContentionPanel() { 
    
        toolbar = ProfilerToolbar.create(true);
        
        JLabel modeLabel = new JLabel(DISPLAY_MODE);
        modeLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        toolbar.add(modeLabel);
        
        modeCombo = new JComboBox(new Object[] { MODE_THREADS, MODE_MONITORS }) {
            protected void fireActionEvent() {
                super.fireActionEvent();
                treeTable.clearSelection();
                prepareResults();
            }
            public Dimension getMaximumSize() {
                Dimension dim = getPreferredSize();
                dim.width += 20;
                return dim;
            }
        };
        modeCombo.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                          final boolean isSelected, final boolean cellHasFocus) {
                DefaultListCellRenderer dlcr =
                        (DefaultListCellRenderer)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (MODE_THREADS.equals(value.toString())) {
                    dlcr.setIcon(Icons.getIcon(ProfilerIcons.THREAD));
                } else if (MODE_MONITORS.equals(value.toString())) {
                    dlcr.setIcon(Icons.getIcon(ProfilerIcons.WINDOW_LOCKS));
                }

                return dlcr;
            }
        });
        modeLabel.setLabelFor(modeCombo);
        toolbar.add(modeCombo);
        
        initColumnsData();
        
        treeTableModel = new LocksTreeTableModel();
        
        treeTable = new ProfilerTreeTable(treeTableModel, true, true, new int[] { 0 }) {
//            protected Object getValueForPopup(int row) {
//                if (row == -1) return null;
//                if (row >= getModel().getRowCount()) return null; // #239936
//                return Integer.valueOf(convertRowIndexToModel(row));
//            }
            protected void populatePopup(JPopupMenu popup, Object value, Object userValue) {
                popup.add(createCopyMenuItem());
                popup.addSeparator();
                
                popup.add(new JMenuItem(SearchUtils.ACTION_FIND) {
                    protected void fireActionPerformed(ActionEvent e) { activateSearch(); }
                });
            }
        };
        treeTable.setRootVisible(false);
        treeTable.setShowsRootHandles(true);
        
        treeTable.providePopupMenu(true);
        
        treeTable.setMainColumn(0);
        treeTable.setFitWidthColumn(0);
        
        treeTable.setSortColumn(1);
        treeTable.setDefaultSortOrder(1, SortOrder.DESCENDING);
        
        LockContentionRenderer lcRenderer = new LockContentionRenderer();
        treeTable.setTreeCellRenderer(lcRenderer);
        
        Number refTime = new Long(123456);
        
        NumberPercentRenderer npr = new NumberPercentRenderer(new McsTimeRenderer());
//        npr.setValue(refTime, -1);
        hbrTime = new HideableBarRenderer(npr);
        hbrTime.setMaxValue(refTime.longValue());
        treeTable.setColumnRenderer(1, hbrTime);
        treeTable.setDefaultColumnWidth(1, hbrTime.getOptimalWidth());
        
        hbrWaits = new HideableBarRenderer(new NumberRenderer());
        hbrWaits.setMaxValue(1234567);
//        treeTable.setDefaultColumnWidth(3, hbrWaits.getOptimalWidth());
        treeTable.setColumnRenderer(2, hbrWaits);
        treeTable.setDefaultColumnWidth(2, hbrWaits.getMaxNoBarWidth());
        
        treeTable.setColumnToolTips(new String[] { LOCKS_THREADS_COLUMN_TOOLTIP,
                                                   TIME_REL_COLUMN_TOOLTIP,
                                                   WAITS_COLUMN_TOOLTIP });
        
//        NumberRenderer numberRenderer = new NumberRenderer();
//        numberRenderer.setValue(refTime, -1);
//        treeTable.setDefaultColumnWidth(3, numberRenderer.getPreferredSize().width);
//        treeTable.setColumnRenderer(3, numberRenderer);

        // Disable traversing table cells using TAB and Shift+TAB
        Set keys = new HashSet(treeTable.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
        treeTable.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys);

        keys = new HashSet(treeTable.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK));
        treeTable.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, keys);
        
        treeTablePanel = new ProfilerTableContainer(treeTable, false, null);
//        treeTablePanel.clearBorders();
        
        notificationPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 15));
        notificationPanel.setBackground(treeTable.getBackground());
        UIUtils.decorateProfilerPanel(notificationPanel);

        Border myRolloverBorder = new CompoundBorder(new FlatToolBar.FlatRolloverButtonBorder(Color.GRAY, Color.LIGHT_GRAY),
                                                     new FlatToolBar.FlatMarginBorder());

        enableLockContentionLabel1 = new JLabel(ENABLE_LOCKS_MONITORING);
        enableLockContentionLabel1.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 3));
        enableLockContentionLabel1.setForeground(Color.DARK_GRAY);

        enableLockContentionButton = new JButton(Icons.getIcon(ProfilerIcons.VIEW_LOCKS_32));
        enableLockContentionButton.setToolTipText(ENABLE_LOCKS_MONITORING_TOOLTIP);
        enableLockContentionButton.setContentAreaFilled(false);
        enableLockContentionButton.setMargin(new Insets(3, 3, 3, 3));
        enableLockContentionButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        enableLockContentionButton.setHorizontalTextPosition(SwingConstants.CENTER);
        enableLockContentionButton.setRolloverEnabled(true);
        enableLockContentionButton.setBorder(myRolloverBorder);
        enableLockContentionButton.getAccessibleContext().setAccessibleName(ENABLE_LOCKS_MONITORING_TOOLTIP);

        enableLockContentionLabel2 = new JLabel(NO_PROFILING);
        enableLockContentionLabel2.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 0));
        enableLockContentionLabel2.setForeground(Color.DARK_GRAY);
        enableLockContentionLabel2.setVisible(false);

        notificationPanel.add(enableLockContentionLabel1);
        notificationPanel.add(enableLockContentionButton);
        notificationPanel.add(enableLockContentionLabel2);
        
        contentPanel = new JPanel(new CardLayout());
        contentPanel.add(notificationPanel, "DISABLED"); // NOI18N
        contentPanel.add(treeTablePanel, "ENABLED"); // NOI18N
        contentPanel.setOpaque(true);
        contentPanel.setBackground(UIUtils.getProfilerResultsBackground());
        
        add(contentPanel, BorderLayout.CENTER);
        
        prepareResults(); // Disables combo
        
        registerActions();
    }
    
    
    protected abstract ProfilerClient getProfilerClient();
    
    
    private void registerActions() {
        ActionMap map = getActionMap();
        
//        map.put(FilterUtils.FILTER_ACTION_KEY, new AbstractAction() {
//            public void actionPerformed(ActionEvent e) { activateFilter(); }
//        });
        
        map.put(SearchUtils.FIND_ACTION_KEY, new AbstractAction() {
            public void actionPerformed(ActionEvent e) { activateSearch(); }
        });
    }
    
    protected SearchUtils.TreeHelper getSearchHelper() {
        return new SearchUtils.TreeHelper() {
            public int getNodeType(TreeNode tnode) {
                LockCCTNode node = (LockCCTNode)tnode;
                CCTNode parent = node.getParent();
                if (parent == null) return SearchUtils.TreeHelper.NODE_SKIP_DOWN;
                
                if (node.isThreadLockNode()) {
                    return searchThreads  ? SearchUtils.TreeHelper.NODE_SEARCH_DOWN :
                                            SearchUtils.TreeHelper.NODE_SKIP_DOWN;
                } else if (node.isMonitorNode()) {
                    return searchMonitors ? SearchUtils.TreeHelper.NODE_SEARCH_DOWN :
                                            SearchUtils.TreeHelper.NODE_SKIP_DOWN;
                }
                
                return SearchUtils.TreeHelper.NODE_SKIP_DOWN;
            }
        };
    }
    
    protected Component[] getSearchOptions() {
        PopupButton pb = new PopupButton (Icons.getIcon(ProfilerIcons.TAB_CALL_TREE)) {
            protected void populatePopup(JPopupMenu popup) {
                popup.add(new JCheckBoxMenuItem(SEARCH_THREADS_SCOPE, searchThreads) {
                    {
                        if (!searchMonitors) setEnabled(false);
                    }
                    protected void fireActionPerformed(ActionEvent e) {
                        super.fireActionPerformed(e);
                        searchThreads = !searchThreads;
                    }
                });
                popup.add(new JCheckBoxMenuItem(SEARCH_MONITORS_SCOPE, searchMonitors) {
                    {
                        if (!searchThreads) setEnabled(false);
                    }
                    protected void fireActionPerformed(ActionEvent e) {
                        super.fireActionPerformed(e);
                        searchMonitors = !searchMonitors;
                    }
                });
            }
        };
        pb.setToolTipText(SEARCH_SCOPE_TOOLTIP);
        return new Component[] { Box.createHorizontalStrut(5), pb };
    }
    
    protected ProfilerTable getResultsComponent() {
        return treeTable;
    }
    
    protected boolean hasBottomFilterFindMargin() {
        return true;
    }
    
    
    public void setAggregation(Aggregation aggregation) {
        this.aggregation = aggregation;
        prepareResults();
    }
    
    public Aggregation getAggregation() {
        return aggregation;
    }
    
    public boolean isRefreshing() {
        return refreshIsRunning;
    }

    public long getLastUpdate() {
        return lastupdate;
    }

    public void setData(final LockRuntimeCCTNode appRootNode) {
        refreshIsRunning = true;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                root = appRootNode;
            }
        });
        prepareResults();
//        forceRefresh = false;
    }
    
    public void resetData() {
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                root = null;
                treeTableModel.setRoot(LockCCTNode.EMPTY);
            }
        });
    }
    
    public void addSaveViewAction(AbstractAction saveViewAction) {
        Component actionButton = toolbar.add(saveViewAction);
        toolbar.remove(actionButton);
        toolbar.add(actionButton, 0);
        toolbar.add(new JToolBar.Separator(), 1);
    }
    
    public void addExportAction(AbstractAction exportAction) {
        Component actionButton = toolbar.add(exportAction);
        toolbar.remove(actionButton);
        toolbar.add(actionButton, 0);
    }
    
    
    public void prepareResults() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (root == null) {refreshIsRunning = false; return; }
                
                LockCCTNode newRoot = null;
                switch (aggregation) {
                    case BY_THREADS:
                        newRoot = root.getThreads();
                        break;
                    case BY_MONITORS:
                        newRoot = root.getMonitors();
                        break;
                }
                
//                newRoot.sortChildren(getSortBy(sortingColumn), sortingOrder);
                hbrTime.setMaxValue(getTimeInMicroSec(newRoot));
                hbrWaits.setMaxValue(newRoot.getWaits());
                treeTableModel.setRoot(newRoot);
                lastupdate = System.currentTimeMillis();
                refreshIsRunning = false;
            }
        });
    }
    
    public void lockContentionDisabled() {
        ((CardLayout)(contentPanel.getLayout())).show(contentPanel, "DISABLED"); // NOI18N
//        updateZoomButtonsEnabledState();
//        threadsSelectionCombo.setEnabled(false);
    }

    public void lockContentionEnabled() {
        ((CardLayout)(contentPanel.getLayout())).show(contentPanel, "ENABLED"); // NOI18N
//        updateZoomButtonsEnabledState();
//        threadsSelectionCombo.setEnabled(true);
    }
    
    public void addLockContentionListener(ActionListener listener) {
        enableLockContentionButton.addActionListener(listener);
    }
    
    public void removeLockContentionListener(ActionListener listener) {
        enableLockContentionButton.removeActionListener(listener);
    }
    
    private long getTimeInMicroSec(LockCCTNode node) {
        return node.getTime() / countsInMicrosec;
    }

    private String getTimeInMillis(LockCCTNode node) {
        long microSec = getTimeInMicroSec(node);
        return StringUtils.mcsTimeToString(microSec);
    }
    
    private void initColumnsData() {
        columnCount = 3;
        
        columnWidths = new int[columnCount - 1]; // Width of the first column fits to width
        columnNames = new String[columnCount];
        columnToolTips = new String[columnCount];
        columnRenderers = new TableCellRenderer[columnCount];

        columnNames[0] = LOCKS_THREADS_COLUMN_NAME;
        columnToolTips[0] = LOCKS_THREADS_COLUMN_TOOLTIP;

//        columnNames[1] = TIME_COLUMN_NAME;
//        columnToolTips[1] = TIME_COLUMN_TOOLTIP;
        
        columnNames[1] = TIME_REL_COLUMN_NAME;
        columnToolTips[1] = TIME_REL_COLUMN_TOOLTIP;
        
        columnNames[2] = WAITS_COLUMN_NAME;
        columnToolTips[2] = WAITS_COLUMN_TOOLTIP;

        int maxWidth = getFontMetrics(getFont()).charWidth('W') * 12; // NOI18N // initial width of data columns

        columnRenderers[0] = null;

//        columnWidths[1 - 1] = maxWidth;
//        columnRenderers[1] = new CustomBarCellRenderer(0, 100);
        
        columnWidths[1 - 1] = maxWidth;
        columnRenderers[1] = new LabelBracketTableCellRenderer(JLabel.TRAILING);

        columnWidths[2 - 1] = maxWidth;
        columnRenderers[2] = new LabelTableCellRenderer(JLabel.TRAILING);
    }
    
    
    public Component getToolbar() {
        return toolbar.getComponent();
    }
    
    
    private class LocksTreeTableModel extends ProfilerTreeTableModel.Abstract {
        
        private LocksTreeTableModel() {
            super(LockCCTNode.EMPTY);
        }

        public boolean isCellEditable(TreeNode node, int columnIndex) {
            return false;
        }

        public Class getColumnClass(int column) {
            if (column == 0) {
                return JTree.class;
            } else if (column == 1) {
                return Long.class;
            } else if (column == 2) {
                return Long.class;
            }
            return null;
        }

        public int getColumnCount() {
            return columnCount;
        }

        public String getColumnName(int columnIndex) {
            return columnNames[columnIndex];
        }

        public Object getValueAt(TreeNode node, int columnIndex) {
            LockCCTNode lnode = (LockCCTNode)node;

            switch (columnIndex) {
                case 0:
                    return lnode;
                case 1:
                    return getTimeInMicroSec(lnode);
//                    return lnode;
//                    return getTimeInMillis(lnode) + " ms (" // NOI18N
//                    + percentFormat.format(lnode.getTimeInPerCent() / 100) + ")"; // NOI18N
                case 2:
                    return lnode.getWaits();
                    
                default:
                    return null;
            }
        }
        
        public void setValueAt(Object aValue, TreeNode node, int column) {}
        
    }
    
}
