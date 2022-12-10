/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.core.ui;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.ui.DataSourceView.Alert;
import org.graalvm.visualvm.uisupport.UISupport;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.openide.util.RequestProcessor;
import org.openide.util.WeakListeners;
import org.openide.windows.TopComponent;

/**
 *
 * @author Jiri Sedlacek
 */
class DataSourceWindow extends TopComponent implements PropertyChangeListener {

    private static final RequestProcessor PROCESSOR =
            new RequestProcessor("DataSourceWindow Processor", 5); // NOI18N
    
    private static final Logger LOGGER = Logger.getLogger(DataSourceWindow.class.getName());
    
    private int viewsCount = 0;
    private DataSource dataSource;
    private DataSourceDescriptor dataSourceDescriptor;
    private DataSourceWindowTabbedPane.ViewContainer singleViewContainer;
    private JPanel multiViewContainer;
    private AlertListener alertListener;

    DataSourceWindow(DataSource dataSource) {
        this.dataSource = dataSource;
        initAppearance();
        initComponents();
    }
    
    
    public DataSource getDataSource() {
        return dataSource;
    }

    public void addView(DataSourceView view) {
        if (viewsCount == 0) {
            singleViewContainer = new DataSourceWindowTabbedPane.ViewContainer(new DataSourceCaption(view.getDataSource()), view);
            add(singleViewContainer, BorderLayout.CENTER);
            doLayout();
            alertListener = new AlertListener();
        } else if (viewsCount == 1) {
            remove(singleViewContainer);

            add(multiViewContainer, BorderLayout.CENTER);
            tabbedContainer.addView(dataSource, singleViewContainer.getView());
            tabbedContainer.addView(dataSource, view);
            doLayout();
            singleViewContainer.getCaption().finish();
            singleViewContainer = null;
        } else {
            tabbedContainer.addView(dataSource, view);
        }
        viewsCount++;
        view.addPropertyChangeListener(WeakListeners.propertyChange(alertListener,view));
    }
    
    private void insertView(DataSourceView view, int index) {
        if (viewsCount == 0) {
            singleViewContainer = new DataSourceWindowTabbedPane.ViewContainer(new DataSourceCaption(view.getDataSource()), view);
            add(singleViewContainer, BorderLayout.CENTER);
            doLayout();
            alertListener = new AlertListener();
        } else if (viewsCount == 1) {
            remove(singleViewContainer);

            add(multiViewContainer, BorderLayout.CENTER);
            tabbedContainer.addView(dataSource, singleViewContainer.getView());
            tabbedContainer.insertView(dataSource, view, index);
            doLayout();
            singleViewContainer.getCaption().finish();
            singleViewContainer = null;
        } else {
            tabbedContainer.insertView(dataSource, view, index);
        }
        viewsCount++;
        view.addPropertyChangeListener(WeakListeners.propertyChange(alertListener,view));
    }
    
    public void selectView(DataSourceView view) {
        if (viewsCount > 1) {
            int viewIndex = indexOf(view);
            if (viewIndex == -1) throw new RuntimeException("View " + view + " not present in DataSourceWindow " + this);   // NOI18N
            else tabbedContainer.setViewIndex(viewIndex);
        }
    }
    
    public void removeView(final DataSourceView view) {
        if (viewsCount == 1) {
            if (view != singleViewContainer.getView()) throw new RuntimeException("View " + view + " not present in DataSourceWindow " + this); // NOI18N
            view.viewWillBeRemoved();
            remove(singleViewContainer);
            singleViewContainer.getCaption().finish();
            singleViewContainer = null;
        } else {
            int viewIndex = indexOf(view);
            if (viewIndex == -1) throw new RuntimeException("View " + view + " not present in DataSourceWindow " + this);   // NOI18N
            view.viewWillBeRemoved();
            tabbedContainer.removeView(viewIndex);
            
            if (viewsCount == 2) {
                DataSourceView remaining = tabbedContainer.getViews().get(0);
                singleViewContainer = new DataSourceWindowTabbedPane.ViewContainer(new DataSourceCaption(remaining.getDataSource()), remaining);
                remove(multiViewContainer);
                tabbedContainer.removeView(0);
                add(singleViewContainer, BorderLayout.CENTER);
                doLayout();
            }
        }
        
        PROCESSOR.post(new Runnable() {
            public void run() {
                try { view.viewRemoved(); }
                catch (Exception e) { LOGGER.log(Level.WARNING, "Failed notifying removed view " + view, e); } // NOI18N
            }
        });
        
        DataSourceWindowManager.sharedInstance().unregisterClosedView(view);
        viewsCount--;
        if (viewsCount == 0 && isOpened()) close();
    }
    
    void clearView(final DataSourceView view, RequestProcessor notificationProcessor) {
        if (viewsCount == 1 && Objects.equals(singleViewContainer.getName(), view.getName())) {
            view.viewWillBeRemoved();
            singleViewContainer.removeAll();
            if (singleViewContainer.getCaption() != null) singleViewContainer.getCaption().finish();
            singleViewContainer.setReloading();
            singleViewContainer.doLayout();
            singleViewContainer.repaint();
        } else {
            int viewIndex = indexOf(view);
            if (viewIndex == -1) return;

            view.viewWillBeRemoved();
            tabbedContainer.clearView(viewIndex);
        }
        
        notificationProcessor.post(new Runnable() {
            public void run() {
                try { view.viewRemoved(); }
                catch (Exception e) { LOGGER.log(Level.WARNING, "Failed notifying removed view " + view, e); } // NOI18N
            }
        });
    }
    
    void updateView(final DataSourceView view, int index) {
        if (viewsCount == 1 && Objects.equals(singleViewContainer.getName(), view.getName())) {
            singleViewContainer.removeAll();
            singleViewContainer.setCaption(new DataSourceCaption(view.getDataSource()));
            singleViewContainer.setView(view);
            singleViewContainer.doLayout();
            singleViewContainer.repaint();
        } else {
            DataSourceWindowTabbedPane.ViewContainer container = tabbedContainer.getContainer(view);
            if (container != null) {
                container.removeAll();
                container.setCaption(new DataSourceCaption(view.getDataSource()));
                container.setView(view);
                container.doLayout();
                container.repaint();
            } else {
                insertView(view, index);
            }
        }
        
        PROCESSOR.post(new Runnable() {
            public void run() {
                try { view.viewAdded(); }
                catch (Exception e) { LOGGER.log(Level.WARNING, "Failed post-initialize view " + view, e); } // NOI18N
            }
        });
    }
    
    void closeUnregisteredView(final DataSourceView view) {
        if (viewsCount == 1) {
            if (view != singleViewContainer.getView()) throw new RuntimeException("View " + view + " not present in DataSourceWindow " + this); // NOI18N
            remove(singleViewContainer);
            singleViewContainer.getCaption().finish();
            singleViewContainer = null;
        } else {
            int viewIndex = indexOf(view);
            if (viewIndex == -1) throw new RuntimeException("View " + view + " not present in DataSourceWindow " + this);   // NOI18N
            else tabbedContainer.removeView(viewIndex);
            
            if (viewsCount == 2) {
                DataSourceView remaining = tabbedContainer.getViews().get(0);
                singleViewContainer = new DataSourceWindowTabbedPane.ViewContainer(new DataSourceCaption(remaining.getDataSource()), remaining);
                remove(multiViewContainer);
                tabbedContainer.removeView(0);
                add(singleViewContainer, BorderLayout.CENTER);
                doLayout();
            }
        }
        
        viewsCount--;
        if (viewsCount == 0 && isOpened()) close();
    }
    
    public void removeAllViews() {
        List<DataSourceView> views = getViews();
        for (DataSourceView view : views) removeView(view);
    }
    
    public List<DataSourceView> getViews() {
        if (viewsCount == 1) {
            return Collections.singletonList(singleViewContainer.getView());
        } else {
            return tabbedContainer.getViews();
        }
    }
    
    public boolean containsView(DataSourceView view) {
        return indexOf(view) != -1;
    }
    
    
    private int indexOf(DataSourceView view) {
        if (viewsCount == 1) {
            return view == singleViewContainer.getView() ? 0 : -1;
        } else {
            return tabbedContainer.indexOfView(view);
        }
    }


    protected final void componentActivated() {
        super.componentActivated();
        if (singleViewContainer != null) singleViewContainer.requestFocusInWindow();
        else if (getComponentCount() > 0) getComponent(0).requestFocusInWindow();
    }
    
    public final boolean canClose() {
        for (DataSourceView view : getViews()) view.viewWillBeRemoved();
        
        return true;
    }
    
    protected final void componentClosed() {
        dataSourceDescriptor.removePropertyChangeListener(this);
        removeAllViews();
        DataSourceWindowManager.sharedInstance().unregisterClosedWindow(this);
        super.componentClosed();
    }
    
    
    public void propertyChange(final PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        if (DataSourceDescriptor.PROPERTY_NAME.equals(propertyName)) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { setName((String)evt.getNewValue()); }
            });
        } else if (DataSourceDescriptor.PROPERTY_ICON.equals(propertyName)) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { setIcon((Image)evt.getNewValue()); }
            });
        }
    }
    
    
    private void initAppearance() {
        dataSourceDescriptor = DataSourceDescriptorFactory.getDescriptor(dataSource);
        
        dataSourceDescriptor.addPropertyChangeListener(this);
        
        setName(dataSourceDescriptor.getName());
        setIcon(dataSourceDescriptor.getIcon());
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());

        // tabbedContainer
        tabbedContainer = new DataSourceWindowTabbedPane() {
            @Override
            protected void closeView(DataSourceWindowTabbedPane.ViewContainer view) {
                DataSourceWindow.this.removeView(tabbedContainer.getView(view));
            }
            
        };

        // multiViewContainer
        multiViewContainer = new JPanel(new BorderLayout());
        if (UISupport.isAquaLookAndFeel()) {
            multiViewContainer.setOpaque(true);
            multiViewContainer.setBackground(UISupport.getDefaultBackground());
        }
        multiViewContainer.add(tabbedContainer, BorderLayout.CENTER);

        add(multiViewContainer, BorderLayout.CENTER);
    }
    
    private void setAlert(DataSourceView view, Alert alert) {
        int viewIndex = tabbedContainer.indexOfView(view);
        
        tabbedContainer.setViewBackground(viewIndex,getAlertColor(alert));
        if (alert != Alert.OK) {
            requestAttention(false);
        } else if (getApplicationAlert(alert) == Alert.OK) {
            cancelRequestAttention();
        }
    }

    private Color getAlertColor(final Alert alert) {
        Color color = null;
        
        switch (alert) {
            case ERROR: 
                color = Color.RED;
                break;
            case WARNING:
                color = Color.YELLOW;
                break;
            case OK:
                color = null;
                break;
        }
        return color;
    }

    private Alert getApplicationAlert(Alert alert) {
        if (alert == Alert.ERROR) {
            return alert;
        }
        for (DataSourceView view : getViews()) {
            Alert a = view.getAlert();
            if (a == Alert.ERROR) {
                return a;
            }
            if (a == Alert.WARNING) {
                alert = a;
            }
        }
        return alert;
    }
    
    private DataSourceWindowTabbedPane tabbedContainer;
    
    
    public int getPersistenceType() { return TopComponent.PERSISTENCE_NEVER; }
    protected String preferredID() { return getClass().getName(); }

    private class AlertListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            if (DataSourceView.ALERT_PROP.equals(evt.getPropertyName())) {
                setAlert((DataSourceView) evt.getSource(), (Alert) evt.getNewValue());
            }
        }
    }
}
