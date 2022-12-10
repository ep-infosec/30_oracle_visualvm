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

package org.graalvm.visualvm.lib.ui.components;

import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.components.table.*;


/**
 * This class implements JTable with extended CellTip support.
 * Added support for handling Home &amp; End keys.
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
public class JExtendedTable extends JTable implements CellTipAware, MouseListener, MouseMotionListener, MouseWheelListener {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected JToolTip cellTip;
    protected Rectangle rendererRect;
    protected int lastColumn = -1;
    protected int lastRow = -1;
    private String internalFindString;

    //------------------------------------
    // Find functionality stuff
    private String userFindString;
    private int userFindColumn;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public JExtendedTable(TableModel model) {
        super(model);

        setBackground(UIUtils.getProfilerResultsBackground());

        cellTip = createCellTip();
        cellTip.setBorder(BorderFactory.createLineBorder(getGridColor()));
        cellTip.setLayout(new BorderLayout());

        initListeners();

        CellTipManager.sharedInstance().registerComponent(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public JToolTip getCellTip() {
        return cellTip;
    }

    public Point getCellTipLocation() {
        if (rendererRect == null) {
            return null;
        }

        return new Point(rendererRect.getLocation().x - 1, rendererRect.getLocation().y - 1);
    }

    public int getFindColumn() {
        return userFindColumn;
    }

    public boolean isFindColumnValid() {
        return ((userFindColumn >= 0) && (userFindColumn < getColumnCount()));
    }

    public void setFindParameters(String findString, int findColumn) {
        userFindString = findString;
        userFindColumn = findColumn;
        internalFindString = getInternalFindString(userFindString);
    }

    public String getFindString() {
        return userFindString;
    }

    public boolean isFindStringDefined() {
        return ((userFindString != null) && (userFindString.trim().length() > 0));
    }

    public void setGridColor(Color gridColor) {
        super.setGridColor(gridColor);

        if ((gridColor == null) || (cellTip == null)) {
            return;
        }

        cellTip.setBorder(BorderFactory.createLineBorder(gridColor));
    }

    public boolean canFindBePerformed() {
        return (getRowCount() > 0) && isFindColumnValid() && isFindStringDefined();
    }

    public void ensureRowVisible(int row) {
        scrollRectToVisible(getCellRect(row, 0, true));
    }

    public boolean findFirst() {
        if (!canFindBePerformed()) {
            return false;
        }

        if (matchesFindCriterion(0)) {
            return selectFoundNode(0);
        } else {
            return doFindNext(0);
        }
    }

    public boolean findNext() {
        if (!canFindBePerformed()) {
            return false;
        }

        return doFindNext(getSearchRoot());
    }

    public boolean findPrevious() {
        if (!canFindBePerformed()) {
            return false;
        }

        return doFindPrevious(getSearchRoot());
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent event) {
    }

    public void mouseEntered(MouseEvent e) {
        CellTipManager.sharedInstance().setEnabled(false);
    }

    public void mouseExited(MouseEvent e) {
        // Return if mouseExit occured because of showing heavyweight celltip
        if (contains(e.getPoint()) && cellTip.isShowing()) {
            return;
        }

        CellTipManager.sharedInstance().setEnabled(false);
        lastRow = -1;
        lastColumn = -1;
    }

    public void mouseMoved(MouseEvent event) {
        // Identify table row and column at cursor
        int row = rowAtPoint(event.getPoint());
        int column = columnAtPoint(event.getPoint());

        // Return if table cell is the same as in previous event
        if ((row == lastRow) && (column == lastColumn)) {
            return;
        }

        lastRow = row;
        lastColumn = column;

        if ((row < 0) || (column < 0)) {
            CellTipManager.sharedInstance().setEnabled(false);

            return;
        }

        TableCellRenderer tableCellRenderer = getCellRenderer(row, column);

        if (!(tableCellRenderer instanceof TableCellRendererPersistent)) {
            return;
        }

        Component cellRenderer = ((TableCellRendererPersistent) tableCellRenderer).getTableCellRendererComponentPersistent(this,
                                                                                                                           getValueAt(row,
                                                                                                                                      column),
                                                                                                                           false,
                                                                                                                           false,
                                                                                                                           row,
                                                                                                                           column);
        Rectangle cellRect = getCellRect(row, column, false);

        // Return if celltip is not supported for the cell
        if (cellRenderer == null) {
            CellTipManager.sharedInstance().setEnabled(false);

            return;
        }

        int horizontalAlignment = ((EnhancedTableCellRenderer) cellRenderer).getHorizontalAlignment();

        if ((horizontalAlignment == SwingConstants.TRAILING) || (horizontalAlignment == SwingConstants.RIGHT)) {
            rendererRect = new Rectangle((cellRect.x + cellRect.width) - cellRenderer.getPreferredSize().width, cellRect.y,
                                         cellRenderer.getPreferredSize().width, cellRenderer.getPreferredSize().height);
        } else {
            rendererRect = new Rectangle(cellRect.x, cellRect.y, cellRenderer.getPreferredSize().width,
                                         cellRenderer.getPreferredSize().height);
        }

        // Return if cell contents is fully visible
        if ((rendererRect.x >= cellRect.x) && ((rendererRect.x + rendererRect.width) <= (cellRect.x + cellRect.width))) {
            CellTipManager.sharedInstance().setEnabled(false);

            return;
        }

        while (cellTip.getComponentCount() > 0) {
            cellTip.remove(0);
        }

        cellTip.add(cellRenderer, BorderLayout.CENTER);
        cellTip.setPreferredSize(new Dimension(rendererRect.width + 2, getRowHeight(row) + 2));

        CellTipManager.sharedInstance().setEnabled(true);
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        mouseMoved(e);
        CellTipManager.sharedInstance().setEnabled(false);
    }
    
    public void processMouseEvent(final MouseEvent e) {
        if (e instanceof MouseWheelEvent) {
            Component target = JExtendedTable.this.getParent();
            if (target == null || !(target instanceof JViewport))
                target = JExtendedTable.this;
            MouseEvent mwe = SwingUtilities.convertMouseEvent(
                    JExtendedTable.this, (MouseWheelEvent)e, target);
            target.dispatchEvent((MouseWheelEvent)mwe);
        } else {
            super.processMouseEvent((MouseEvent)e);
        }
    }

    public void selectRowByContents(String rowString, int columnIndex, boolean setVisible) {
        for (int i = 0; i < getRowCount(); i++) {
            if (getValueAt(i, columnIndex).toString().equals(rowString)) {
                getSelectionModel().setSelectionInterval(i, i);

                if (setVisible) {
                    ensureRowVisible(i);
                }

                return;
            }
        }

        getSelectionModel().clearSelection();
    }

    public void selectRowByInstance(Object instance, int columnIndex, boolean setVisible) {
        for (int i = 0; i < getRowCount(); i++) {
            if (getValueAt(i, columnIndex) == instance) {
                getSelectionModel().setSelectionInterval(i, i);

                if (setVisible) {
                    ensureRowVisible(i);
                }

                return;
            }
        }

        getSelectionModel().clearSelection();
    }

    public void selectRowsByInstances(Object[] instances, int columnIndex, boolean setVisible) {
        List instancesList = Arrays.asList(instances);

        getSelectionModel().clearSelection();

        for (int i = 0; i < getRowCount(); i++) {
            if (instancesList.contains(getValueAt(i, columnIndex))) {
                getSelectionModel().addSelectionInterval(i, i);
            }
        }

        if (setVisible && (getSelectedRow() != -1)) {
            ensureRowVisible(getSelectedRow());
        }
    }

    protected JToolTip createCellTip() {
        return new JToolTip();
    }

    private boolean isAnyRowSelected() {
        return getSelectedRow() != -1;
    }

    private String getInternalFindString(String findString) {
        if (findString == null) {
            return null;
        }

        return findString.toLowerCase();
    }

    private int getSearchRoot() {
        if (!isAnyRowSelected()) {
            return 0;
        } else {
            return getSelectedRow();
        }
    }

    private boolean doFindNext(int lastFoundRow) {
        for (int row = lastFoundRow + 1; row < getRowCount(); row++) {
            if (matchesFindCriterion(row)) {
                return selectFoundNode(row);
            }
        }

        return false;
    }

    private boolean doFindPrevious(int lastFoundRow) {
        for (int row = lastFoundRow - 1; row >= 0; row--) {
            if (matchesFindCriterion(row)) {
                return selectFoundNode(row);
            }
        }

        return false;
    }

    private void initListeners() {
        addMouseListener(this);
        addMouseMotionListener(this);

        // Required for correct updating of focused/unfocused selection
        addFocusListener(new FocusListener() {
                public void focusGained(FocusEvent e) {
                    if (getSelectedRows().length > 0) {
                        repaint();
                    }
                }

                public void focusLost(FocusEvent e) {
                    if (getSelectedRows().length > 0) {
                        repaint();
                    }
                }
            });

        addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    int rowCount = getRowCount();

                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_HOME:

                            if (rowCount > 0) {
                                setRowSelectionInterval(0, 0);
                            }

                            break;
                        case KeyEvent.VK_END:

                            if (rowCount > 0) {
                                setRowSelectionInterval(rowCount - 1, rowCount - 1);
                            }

                            break;
                    }
                }
            });
    }

    private boolean matchesFindCriterion(int row) {
        return getValueAt(row, userFindColumn).toString().toLowerCase().contains(internalFindString);
    }

    private boolean selectFoundNode(int row) {
        getSelectionModel().setSelectionInterval(row, row);
        requestFocusInWindow();

        Rectangle rect = getCellRect(row, userFindColumn, true);

        if (rect != null) {
            scrollRectToVisible(rect);

            return true;
        } else {
            return false;
        }
    }

    //------------------------------------
}
