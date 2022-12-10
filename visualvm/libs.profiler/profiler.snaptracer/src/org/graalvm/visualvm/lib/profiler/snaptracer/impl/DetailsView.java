/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.snaptracer.impl;

import javax.swing.JComponent;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.details.DetailsPanel;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.swing.VisibilityHandler;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.timeline.TimelineSupport;

/**
 *
 * @author Jiri Sedlacek
 */
final class DetailsView {

    private final TimelineSupport timelineSupport;
    private DetailsPanel panel;

    private boolean hasData;

    private VisibilityHandler viewHandler;

    // --- Constructor ---------------------------------------------------------

    DetailsView(TracerModel model) {
        timelineSupport = model.getTimelineSupport();
    }


    // --- Internal interface --------------------------------------------------

    void registerViewListener(VisibilityHandler viewHandler) {
        if (panel != null) {
            viewHandler.handle(panel);
        } else {
            this.viewHandler = viewHandler;
        }

    }

    boolean isShowing() {
        return panel != null && panel.isShowing();
    }

    boolean hasData() {
        return hasData;
    }


    // --- UI implementation ---------------------------------------------------

    JComponent getView() {
        panel = new DetailsPanel(timelineSupport);

        timelineSupport.addSelectionListener(new TimelineSupport.SelectionListener() {
            public void intervalsSelectionChanged() {}
            public void indexSelectionChanged() {}
            public void timeSelectionChanged(boolean timestampsSelected, boolean justHovering) {}
        });

        if (viewHandler != null) {
            viewHandler.handle(panel);
            viewHandler = null;
        }

        return panel;
    }

}
