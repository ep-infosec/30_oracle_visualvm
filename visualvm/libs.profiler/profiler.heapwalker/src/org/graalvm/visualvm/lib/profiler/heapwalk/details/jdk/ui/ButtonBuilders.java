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

import java.awt.Component;
import javax.swing.AbstractButton;
import javax.swing.DefaultButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.BaseBuilders.IconBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.BaseBuilders.InsetsBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.ComponentBuilders.ComponentBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.ComponentBuilders.JComponentBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.Utils.InstanceBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.Utils.JPopupMenuImpl;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;

/**
 *
 * @author Jiri Sedlacek
 */
final class ButtonBuilders {

    // Make sure subclasses are listed before base class if using isSubclassOf
    static ComponentBuilder<? extends JComponent> getBuilder(Instance instance) {
        if (DetailsUtils.isSubclassOf(instance, JButton.class.getName())) {
            return new JButtonBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JCheckBox.class.getName())) {
            return new JCheckBoxBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JRadioButton.class.getName())) {
            return new JRadioButtonBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JToggleButton.class.getName())) {
            return new JToggleButtonBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JCheckBoxMenuItem.class.getName())) {
            return new JCheckBoxMenuItemBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JRadioButtonMenuItem.class.getName())) {
            return new JRadioButtonMenuItemBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JMenu.class.getName())) {
            return new JMenuBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JMenuBar.class.getName())) {
            return new JMenuBarBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JMenuItem.class.getName())) {
            return new JMenuItemBuilder(instance);
        }
        return null;
    }
    
    
    private static final class DefaultButtonModelBuilder extends InstanceBuilder<DefaultButtonModel> {
        
        private final int stateMask;
        
        DefaultButtonModelBuilder(Instance instance) {
            super(instance);
            
            stateMask = DetailsUtils.getIntFieldValue(instance, "stateMask", 0);
        }
        
        static DefaultButtonModelBuilder fromField(Instance instance, String field) {
            Object model = instance.getValueOfField(field);
            if (!(model instanceof Instance)) return null;
            if (!DetailsUtils.isSubclassOf((Instance)model, DefaultButtonModel.class.getName())) return null;
            return new DefaultButtonModelBuilder((Instance)model);
        }
        
        protected void setupInstance(DefaultButtonModel instance) {
            super.setupInstance(instance);
            
            instance.setArmed((stateMask & DefaultButtonModel.ARMED) != 0);
            instance.setSelected((stateMask & DefaultButtonModel.SELECTED) != 0);
            instance.setEnabled((stateMask & DefaultButtonModel.ENABLED) != 0);
            instance.setPressed((stateMask & DefaultButtonModel.PRESSED) != 0);
            instance.setRollover((stateMask & DefaultButtonModel.ROLLOVER) != 0);
        }
        
        protected DefaultButtonModel createInstanceImpl() {
            return new DefaultButtonModel();
        }
        
    }
    
    private static abstract class AbstractButtonBuilder<T extends AbstractButton> extends JComponentBuilder<T> {
        
        private final DefaultButtonModelBuilder model;
        private final String text;
        private final InsetsBuilder margin;
        private final IconBuilder defaultIcon;
        private final int verticalAlignment;
        private final boolean borderPaintedSet;
        private final boolean paintBorder;
        private final boolean contentAreaFilledSet;
        private final boolean contentAreaFilled;
        private final int horizontalAlignment;
        private final int verticalTextPosition;
        private final int horizontalTextPosition;
        private final int iconTextGap;
        
        AbstractButtonBuilder(Instance instance) {
            this(instance, false);
        }
        
        AbstractButtonBuilder(Instance instance, boolean trackChildren) {
            super(instance, false);
            
            model = DefaultButtonModelBuilder.fromField(instance, "model");
            
            text = Utils.getFieldString(instance, "text");
            
            margin = InsetsBuilder.fromField(instance, "margin");
            
            defaultIcon = IconBuilder.fromField(instance, "defaultIcon");
            
            borderPaintedSet = DetailsUtils.getBooleanFieldValue(instance, "borderPaintedSet", false);
            paintBorder = DetailsUtils.getBooleanFieldValue(instance, "paintBorder", true);
            contentAreaFilledSet = DetailsUtils.getBooleanFieldValue(instance, "contentAreaFilledSet", false);
            contentAreaFilled = DetailsUtils.getBooleanFieldValue(instance, "contentAreaFilled", true);
            
            verticalAlignment = DetailsUtils.getIntFieldValue(instance, "verticalAlignment", JLabel.CENTER);
            horizontalAlignment = DetailsUtils.getIntFieldValue(instance, "horizontalAlignment", JLabel.LEADING);
            verticalTextPosition = DetailsUtils.getIntFieldValue(instance, "verticalTextPosition", JLabel.CENTER);
            horizontalTextPosition = DetailsUtils.getIntFieldValue(instance, "horizontalTextPosition", JLabel.TRAILING);
            iconTextGap = DetailsUtils.getIntFieldValue(instance, "iconTextGap", 4);
        }
        
        protected void setupInstance(T instance) {
            super.setupInstance(instance);
            
            if (model != null) instance.setModel(model.createInstance());
            
            instance.setText(text);
            
            if (margin != null) instance.setMargin(margin.createInstance());
            
            if (defaultIcon != null) instance.setIcon(defaultIcon.createInstance());
            
            if (borderPaintedSet) instance.setBorderPainted(paintBorder);
            if (contentAreaFilledSet) instance.setContentAreaFilled(contentAreaFilled);
            
            instance.setVerticalAlignment(verticalAlignment);
            instance.setHorizontalAlignment(horizontalAlignment);
            instance.setVerticalTextPosition(verticalTextPosition);
            instance.setHorizontalTextPosition(horizontalTextPosition);
            instance.setIconTextGap(iconTextGap);
        }
        
    }
    
    private static final class JButtonBuilder extends AbstractButtonBuilder<JButton> {
        
        JButtonBuilder(Instance instance) {
            super(instance);
        }
        
        protected JButton createInstanceImpl() {
            return new JButton();
        }
        
    }
    
    private static class JToggleButtonBuilder extends AbstractButtonBuilder<JToggleButton> {
        
        JToggleButtonBuilder(Instance instance) {
            super(instance);
        }
        
        protected JToggleButton createInstanceImpl() {
            return new JToggleButton();
        }
        
    }
    
    private static class JCheckBoxBuilder extends JToggleButtonBuilder {
        
        private final boolean flat;
        
        JCheckBoxBuilder(Instance instance) {
            super(instance);
            
            flat = DetailsUtils.getBooleanFieldValue(instance, "flat", false);
        }
        
        protected JToggleButton createInstanceImpl() {
            JCheckBox checkBox = new JCheckBox();
            checkBox.setBorderPaintedFlat(flat);
            return checkBox;
        }
        
    }
    
    private static class JRadioButtonBuilder extends JToggleButtonBuilder {
        
        JRadioButtonBuilder(Instance instance) {
            super(instance);
        }
        
        protected JToggleButton createInstanceImpl() {
            return new JRadioButton();
        }
        
    }
    
    private static class JMenuItemBuilder extends AbstractButtonBuilder<JMenuItem> {
        
        JMenuItemBuilder(Instance instance) {
            super(instance, true);
        }
        
        protected JMenuItem createInstanceImpl() {
            return new JMenuItem();
        }
        
        protected Component createPresenterImpl(JMenuItem instance) {
            JPopupMenu popupMenu = new JPopupMenuImpl();
            popupMenu.add(instance);
            return popupMenu;
        }
        
    }
    
    private static class JCheckBoxMenuItemBuilder extends JMenuItemBuilder {
        
        JCheckBoxMenuItemBuilder(Instance instance) {
            super(instance);
        }
        
        protected JMenuItem createInstanceImpl() {
            return new JCheckBoxMenuItem();
        }
        
    }
    
    private static class JRadioButtonMenuItemBuilder extends JMenuItemBuilder {
        
        JRadioButtonMenuItemBuilder(Instance instance) {
            super(instance);
        }
        
        protected JMenuItem createInstanceImpl() {
            return new JRadioButtonMenuItem();
        }
        
    }
    
    private static class JMenuBuilder extends JMenuItemBuilder {
        
        JMenuBuilder(Instance instance) {
            super(instance);
        }
        
        protected JMenuItem createInstanceImpl() {
            return new JMenu();
        }
        
        protected Component createPresenterImpl(JMenuItem instance) {
            JMenuBar menuBar = new JMenuBar();
            menuBar.add(instance);
            return menuBar;
        }
        
    }
    
    private static class JMenuBarBuilder extends JComponentBuilder<JMenuBar> {
        
        private final boolean paintBorder;
        private final InsetsBuilder margin;
        
        JMenuBarBuilder(Instance instance) {
            super(instance);
            
            paintBorder = DetailsUtils.getBooleanFieldValue(instance, "paintBorder", true);
            margin = InsetsBuilder.fromField(instance, "margin");
        }
        
        protected void setupInstance(JMenuBar instance) {
            super.setupInstance(instance);
            
            instance.setBorderPainted(paintBorder);
            if (margin != null) instance.setMargin(margin.createInstance());
        }
        
        protected JMenuBar createInstanceImpl() {
            return new JMenuBar();
        }
        
    }
    
}
