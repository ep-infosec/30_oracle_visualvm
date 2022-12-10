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
package org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectArrayInstance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.BaseBuilders.ColorBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.BaseBuilders.IconBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.BorderBuilders.BorderBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.ComponentBuilders.ComponentBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.ComponentBuilders.ContainerBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.ComponentBuilders.JComponentBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.Utils.InstanceBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;

/**
 *
 * @author Jiri Sedlacek
 */
final class PaneBuilders {

    // Make sure subclasses are listed before base class if using isSubclassOf
    static ComponentBuilder<? extends Container> getBuilder(Instance instance) {
        if (DetailsUtils.isSubclassOf(instance, JViewport.class.getName())) {
            return new JViewportBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JScrollPane.class.getName())) {
            return new JScrollPaneBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JSplitPane.class.getName())) {
            return new JSplitPaneBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, BasicSplitPaneDivider.class.getName())) {
            return new BasicSplitPaneDividerBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JTabbedPane.class.getName())) {
            return new JTabbedPaneBuilder(instance);
        }
        return null;
    }
    
    
    private static class JViewportBuilder extends JComponentBuilder<JViewport> {
        
        JViewportBuilder(Instance instance) {
            super(instance);
        }
        
        protected JViewport createInstanceImpl() {
            return new JViewport();
        }
        
    }
    
    private static class JScrollPaneBuilder extends JComponentBuilder<JScrollPane> {
        
        private final BorderBuilder viewportBorder;
        
        JScrollPaneBuilder(Instance instance) {
            super(instance);
            
            viewportBorder = BorderBuilders.fromField(instance, "viewportBorder", false);

        }
        
        protected void setupInstance(JScrollPane instance) {
            super.setupInstance(instance);
            
            if (viewportBorder != null) {
                Border b = viewportBorder.createInstance();
                if (b != null) instance.setViewportBorder(b);
            }
        }
        
        protected JScrollPane createInstanceImpl() {
            return new JScrollPane();
        }
        
    }
    
    private static class JSplitPaneBuilder extends JComponentBuilder<JSplitPane> {
        
        private final int orientation;
        
        JSplitPaneBuilder(Instance instance) {
            super(instance);
            
            orientation = DetailsUtils.getIntFieldValue(instance, "orientation", JSplitPane.HORIZONTAL_SPLIT);

        }
        
        protected JSplitPane createInstanceImpl() {
            return new JSplitPane(orientation);
        }
        
    }
    
    private static class BasicSplitPaneDividerBuilder extends ContainerBuilder<BasicSplitPaneDivider> {
        
        private final int orientation;
//        private final int dividerSize;
        private final BorderBuilder border;
        
        BasicSplitPaneDividerBuilder(Instance instance) {
            super(instance, false);
            
            orientation = DetailsUtils.getIntFieldValue(instance, "orientation", JSplitPane.HORIZONTAL_SPLIT);
//            dividerSize = DetailsUtils.getIntFieldValue(instance, "dividerSize", 0);
            border = BorderBuilders.fromField(instance, "border", false);
        }
        
        protected void setupInstance(BasicSplitPaneDivider instance) {
            super.setupInstance(instance);
            
//            instance.setDividerSize(dividerSize);
            if (border != null) {
                Border b = border.createInstance();
                if (b != null) instance.setBorder(b);
            }
        }
        
        protected BasicSplitPaneDivider createInstanceImpl() {
            final JSplitPane split = new JSplitPane(orientation);
            BasicSplitPaneUI ui = split.getUI() instanceof BasicSplitPaneUI ?
                    (BasicSplitPaneUI)split.getUI() : new BasicSplitPaneUI() {
                        { installUI(split); }
                    };
            return new BasicSplitPaneDivider(ui);
        }
        
    }
    
    
    private static class PageImpl {
        final String title;
        final ColorBuilder background;
        final ColorBuilder foreground;
        final IconBuilder icon;
        final long component;
        final boolean enabled;
        
        PageImpl(String title, ColorBuilder background, ColorBuilder foreground,
                 IconBuilder icon, long component, boolean enabled) {
            this.title = title;
            this.background = background;
            this.foreground = foreground;
            this.icon = icon;
            this.component = component;
            this.enabled = enabled;
        }
    }
    
    private static class PageImplBuilder extends InstanceBuilder<List<PageImpl>> {
        
        private final List<PageImpl> pages;
        
        PageImplBuilder(Instance instance) {
            super(instance);
            
            pages = new ArrayList<>(1);
            
            Object _elementData = instance.getValueOfField("elementData");
            if (_elementData instanceof ObjectArrayInstance) {
                int size = DetailsUtils.getIntFieldValue(instance, "size", Integer.MIN_VALUE); // ArrayList, JDK 7+
                if (size == Integer.MIN_VALUE) size = DetailsUtils.getIntFieldValue(instance, "elementCount", 0); // Vector, JDK 6-
                
                if (size > 0) { // TODO: should read up to 'size' elements
                    ObjectArrayInstance elementData = (ObjectArrayInstance)_elementData;
                    for (Instance page : elementData.getValues()) {
                        if (page != null) {
                            Object comp = page.getValueOfField("component");
                            pages.add(new PageImpl(
                                            Utils.getFieldString(page, "title"),
                                            ColorBuilder.fromField(page, "background"),
                                            ColorBuilder.fromField(page, "foreground"),
                                            IconBuilder.fromField(page, "icon"),
                                            comp instanceof Instance ? ((Instance)comp).getInstanceId() : -1,
                                            DetailsUtils.getBooleanFieldValue(page, "enabled", true)
                                      ));
                        }
                    }
                }
            }
        }
        
        static PageImplBuilder fromField(Instance instance, String field) {
            Object pages = instance.getValueOfField(field);
            if (!(pages instanceof Instance)) return null;
            return new PageImplBuilder((Instance)pages);
        }
        
        protected List<PageImpl> createInstanceImpl() {
            return pages;
        }
        
    }
    
    private static class JTabbedPaneBuilder extends JComponentBuilder<JTabbedPane> {
        
        private final int tabPlacement;
        private final int tabLayoutPolicy;
        private final PageImplBuilder pages;
        private final long visCompId;
        private final InstanceBuilder<Component> visComp;
        private int selComp = -1;
        
        JTabbedPaneBuilder(Instance instance) {
            super(instance, false);
            
            tabPlacement = DetailsUtils.getIntFieldValue(instance, "tabPlacement", JTabbedPane.TOP);
            tabLayoutPolicy = DetailsUtils.getIntFieldValue(instance, "tabLayoutPolicy", JTabbedPane.WRAP_TAB_LAYOUT);
            pages = PageImplBuilder.fromField(instance, "pages");
            
            Object _visComp = instance.getValueOfField("visComp");
            if (_visComp instanceof Instance) {
                Instance visCompI = (Instance)_visComp;
                visCompId = visCompI.getInstanceId();
                visComp = ComponentBuilders.getBuilder(visCompI);
            } else {
                visCompId = Long.MIN_VALUE;
                visComp = null;
            }
        }
        
        protected void setupInstance(JTabbedPane instance) {
            super.setupInstance(instance);
            
            if (pages != null) {
                List<PageImpl> pageImpls = pages.createInstance();
                for (PageImpl page : pageImpls) {
                    int index = instance.getTabCount();
                    Component comp = null;
                    if (selComp == -1 && visComp != null && visCompId == page.component) {
//                        comp = new JPanel(null) { public boolean isOpaque() { return false; } };
                        comp = visComp.createInstance();
                        selComp = index;
                    }
                    instance.addTab(page.title, page.icon == null ? null : page.icon.createInstance(), comp);
                    if (page.background != null) {
                        Color background = page.background.createInstance();
                        if (background != null) instance.setBackgroundAt(index, background);
                    }
                    if (page.foreground != null) {
                        Color foreground = page.foreground.createInstance();
                        if (foreground != null) instance.setForegroundAt(index, foreground);
                    }
                    instance.setEnabledAt(index, page.enabled);
                }
            }
            
            if (selComp != -1) {
                instance.setSelectedIndex(selComp);
//                instance.add(visComp.createInstance());
                selComp = -1; // Cleanup for eventual Builder reuse
            }
        }
        
        protected JTabbedPane createInstanceImpl() {
            return new JTabbedPane(tabPlacement, tabLayoutPolicy) {
//                public Component add(Component component) {
//                    addImpl(component, null, getComponentCount());
//                    return component;
//                }
//                protected void processContainerEvent(ContainerEvent e) {}
            };
        }
        
    }
    
}
