/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.core.datasource;

import org.graalvm.visualvm.core.datasupport.Stateful;

/**
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
public abstract class StatefulDataSource extends DataSource implements Stateful {

    private int state = STATE_UNKNOWN;
    private int modCount;
    private final Object stateLock = new Object();
    
    protected StatefulDataSource() {
        this(STATE_AVAILABLE);
    }
    
    protected StatefulDataSource(int state) {
        this.state = state;
    }

    public final int getState() {
        synchronized (stateLock) {
            return state;
        }
    }

    public final int getModCount() {
        synchronized (stateLock) {
            return modCount;
        }
    }

    protected final void setState(final int newState) {
        synchronized (stateLock) {
            final int oldState = state;
            state = newState;
            if (oldState != newState && newState == STATE_AVAILABLE) {
                modCount++;
            }
            if (DataSource.EVENT_QUEUE.isRequestProcessorThread()) {
                getChangeSupport().firePropertyChange(PROPERTY_STATE, oldState, newState);
            } else {
                DataSource.EVENT_QUEUE.post(new Runnable() {
                    public void run() {
                        getChangeSupport().firePropertyChange(PROPERTY_STATE, oldState, newState);
                    }
                });
            }
        }
    }
}
