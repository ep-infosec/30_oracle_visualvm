/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.views.sampler;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import org.graalvm.visualvm.core.ui.components.Spacer;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.views.components.MessageComponent;
import org.graalvm.visualvm.lib.ui.components.HTMLLabel;
import org.graalvm.visualvm.lib.ui.components.HTMLTextArea;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
final class SamplerViewSupport {
    
    static abstract class MasterViewSupport extends JPanel {
        
        MasterViewSupport(JFRModel model) {
            initComponents(model);
        }
        
        
        DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView(NbBundle.getMessage(SamplerViewSupport.class, "LBL_Sampler"), null, this); // NOI18N
        }
        
        
        abstract void showCPU();
        
        abstract void showMemory();
        
        
        void showProgress() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    statusValueLabel.setVisible(true);
                    cpuButton.setEnabled(false);
                    memoryButton.setEnabled(false);
                }
            });
        }
        
        void hideProgress() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    statusValueLabel.setVisible(false);
                    cpuButton.setEnabled(true);
                    memoryButton.setEnabled(true);
                }
            });
        }
        
        
        private void handleCPUData() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
//                    System.err.println(">>> handleCPUData");
                    memoryButton.setSelected(false);
                    showCPU();
//                    memoryButton.invalidate();
//                    memoryButton.setEnabled(false);
//                    memoryButton.setEnabled(true);
                }
            });
        }
        
        private void handleMemoryData() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
//                    System.err.println(">>> handleMemoryData");
                    cpuButton.setSelected(false);
                    showMemory();
//                    cpuButton.invalidate();
//                    cpuButton.setEnabled(false);
//                    cpuButton.setEnabled(true);
                }
            });
        }
        
        
        private void initComponents(JFRModel model) {
            setOpaque(false);
            
            if (model == null) {
                setLayout(new BorderLayout());
                add(MessageComponent.notAvailable(), BorderLayout.CENTER);
            } else {
                setLayout(new GridBagLayout());
                setBorder(BorderFactory.createEmptyBorder(11, 5, 20, 5));

                GridBagConstraints constraints;

                // modeLabel
                modeLabel = new JLabel(NbBundle.getMessage(SamplerViewSupport.class, "LBL_Profile")); // NOI18N
                modeLabel.setFont(modeLabel.getFont().deriveFont(Font.BOLD));
                Dimension d = modeLabel.getPreferredSize();
                modeLabel.setText(NbBundle.getMessage(SamplerViewSupport.class, "LBL_Sample")); // NOI18N
                d.width = Math.max(d.width, modeLabel.getPreferredSize().width);
                modeLabel.setPreferredSize(d);
                modeLabel.setOpaque(false);
                constraints = new GridBagConstraints();
                constraints.gridx = 0;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 8, 0, 0);
                add(modeLabel, constraints);

                // cpuButton
                cpuButton = new OneWayToggleButton(NbBundle.getMessage(SamplerViewSupport.class, "LBL_Cpu")); // NOI18N
                cpuButton.setIcon(new ImageIcon(ImageUtilities.loadImage("org/graalvm/visualvm/sampler/resources/cpu.png", true))); // NOI18N
                cpuButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) { handleCPUData(); }
                });
                constraints = new GridBagConstraints();
                constraints.gridx = 2;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 8, 0, 0);
                add(cpuButton, constraints);

                // memoryButton
                memoryButton = new OneWayToggleButton(NbBundle.getMessage(SamplerViewSupport.class, "LBL_Memory")); // NOI18N
                memoryButton.setIcon(new ImageIcon(ImageUtilities.loadImage("org/graalvm/visualvm/sampler/resources/memory.png", true))); // NOI18N
                memoryButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) { handleMemoryData(); }
                });
                constraints = new GridBagConstraints();
                constraints.gridx = 3;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 8, 0, 0);
                add(memoryButton, constraints);

                // statusValueLabel
                statusValueLabel = new HTMLLabel("<nobr><b>Progress:</b> reading data...</nobr>");
    //            stopButton.setIcon(new ImageIcon(ImageUtilities.loadImage("org/graalvm/visualvm/sampler/resources/stop.png", true))); // NOI18N
    //            stopButton.addActionListener(new ActionListener() {
    //                public void actionPerformed(ActionEvent e) { handleStopProfiling(); }
    //            });
                constraints = new GridBagConstraints();
                constraints.gridx = 4;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 20, 0, 0);
                add(statusValueLabel, constraints);
                statusValueLabel.setVisible(false);

                // filler1
                constraints = new GridBagConstraints();
                constraints.gridx = 5;
                constraints.gridy = 2;
                constraints.weightx = 1;
                constraints.weighty = 1;
                constraints.gridwidth = GridBagConstraints.REMAINDER;
                constraints.fill = GridBagConstraints.BOTH;
                constraints.anchor = GridBagConstraints.NORTHWEST;
                constraints.insets = new Insets(0, 0, 0, 0);
                add(Spacer.create(), constraints);

    //            // statusLabel
    //            statusLabel = new JLabel(NbBundle.getMessage(SamplerViewSupport.class, "LBL_Status")); // NOI18N
    //            statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
    //            statusLabel.setOpaque(false);
    //            constraints = new GridBagConstraints();
    //            constraints.gridx = 0;
    //            constraints.gridy = 3;
    //            constraints.gridwidth = 1;
    //            constraints.fill = GridBagConstraints.NONE;
    //            constraints.anchor = GridBagConstraints.WEST;
    //            constraints.insets = new Insets(6, 8, 4, 0);
    //            add(statusLabel, constraints);

    //            // statusValueLabel
    //            statusValueLabel = new HTMLLabel() {
    //                public void setText(String text) {super.setText("<nobr>" + text + "</nobr>"); } // NOI18N
    //                protected void showURL(URL url) {}
    //
    //                // NOTE: overriding dimensions prevents UI "jumping" when changing the link
    //                public Dimension getPreferredSize() {
    //                    Dimension dim = super.getPreferredSize();
    //                    dim.height = getRefLabelHeight();
    //                    return dim;
    //                }
    //                public Dimension getMinimumSize() { return getPreferredSize(); }
    //                public Dimension getMaximumSize() { return getPreferredSize(); }
    //            };
    //            statusValueLabel.setOpaque(false);
    //            statusValueLabel.setFocusable(false);
    //            constraints = new GridBagConstraints();
    //            constraints.gridx = 1;
    //            constraints.gridy = 3;
    //            constraints.gridwidth = GridBagConstraints.REMAINDER;
    //            constraints.fill = GridBagConstraints.NONE;
    //            constraints.anchor = GridBagConstraints.WEST;
    //            constraints.insets = new Insets(6, 8, 4, 8);
    //            add(statusValueLabel, constraints);

    //            // filler2
    //            constraints = new GridBagConstraints();
    //            constraints.gridx = 2;
    //            constraints.gridy = 3;
    //            constraints.weightx = 1;
    //            constraints.weighty = 1;
    //            constraints.gridwidth = GridBagConstraints.REMAINDER;
    //            constraints.fill = GridBagConstraints.BOTH;
    //            constraints.anchor = GridBagConstraints.NORTHWEST;
    //            constraints.insets = new Insets(0, 0, 0, 0);
    //            add(Spacer.create(), constraints);

                Dimension cpuD     = cpuButton.getPreferredSize();
                Dimension memoryD  = memoryButton.getPreferredSize();
    //            Dimension stopD    = stopButton.getPreferredSize();

                Dimension maxD = new Dimension(Math.max(cpuD.width, memoryD.width), Math.max(cpuD.height, memoryD.height));
    //            maxD = new Dimension(Math.max(maxD.width, stopD.width), Math.max(maxD.height, stopD.height));

                cpuButton.setPreferredSize(maxD);
                cpuButton.setMinimumSize(maxD);
                memoryButton.setPreferredSize(maxD);
                memoryButton.setMinimumSize(maxD);
    //            stopButton.setPreferredSize(maxD);
    //            stopButton.setMinimumSize(maxD);
            }
        }

        private JLabel modeLabel;
        private JToggleButton cpuButton;
        private JToggleButton memoryButton;
//        private JButton stopButton;
//        private JLabel statusLabel;
        private HTMLLabel statusValueLabel;

//        private static int refLabelHeight = -1;
//        private static int getRefLabelHeight() {
//            if (refLabelHeight == -1)
//                refLabelHeight = new HTMLLabel("X").getPreferredSize().height; // NOI18N
//            return refLabelHeight;
//        }


        private static final class OneWayToggleButton extends JToggleButton {

            OneWayToggleButton(String text) {
                super(text);
            }

            protected void processMouseEvent(MouseEvent e) {
                if (!isSelected() || MouseEvent.MOUSE_EXITED == e.getID()) super.processMouseEvent(e);
            }

            protected void processKeyEvent(KeyEvent e) {
                if (!isSelected()) super.processKeyEvent(e);
            }

        }
        
    }
    
    
    static final class SummaryViewSupport extends JPanel {
        
        SummaryViewSupport(JFRModel model) {
            super(new BorderLayout());
            
            HTMLTextArea summaryArea = new HTMLTextArea(getSummary(model));
            summaryArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            add(summaryArea, BorderLayout.CENTER);
        }
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(
                        SamplerViewSupport.class, "LBL_Summary"), null, 10, // NOI18N
                        new ScrollableContainer(this), null);
        }
        
        private String getSummary(JFRModel model) {
            StringBuilder builder = new StringBuilder();

            addCpuHeader(builder);
            builder.append("CPU samples ");
            builder.append("available."); // all events used, let's assume some of them contain stack traces
//            builder.append(model.containsEvent(JFRSnapshotSamplerViewProvider.CPUSampleChecker.class) ? "available." : "not recorded.");
            builder.append(" ");
            builder.append("Thread CPU load ");
            builder.append(model.containsEvent(JFRSnapshotSamplerViewProvider.ThreadCPUChecker.class) ? "available." : "not recorded.");

            addMemoryHeader(builder);
            builder.append("Heap histogram ");
            builder.append(model.containsEvent(JFRSnapshotSamplerViewProvider.ObjectCountChecker.class) ? "available." : "not recorded.");
            builder.append(" ");
            builder.append("Per thread allocations ");
            builder.append(model.containsEvent(JFRSnapshotSamplerViewProvider.ThreadAllocationsChecker.class) ? "available." : "not recorded.");
            
            return builder.toString();
        }

        private static void addCpuHeader(StringBuilder builder) {
            builder.append(NbBundle.getMessage(SamplerViewSupport.class, "LBL_Cpu_sampling")); // NOI18N
        }

        private static void addMemoryHeader(StringBuilder builder) {
            builder.append(NbBundle.getMessage(SamplerViewSupport.class, "LBL_Memory_sampling")); // NOI18N
        }
        
    }
    
}
