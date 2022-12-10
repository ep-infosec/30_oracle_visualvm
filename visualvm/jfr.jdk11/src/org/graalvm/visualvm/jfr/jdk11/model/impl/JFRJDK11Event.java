/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.jdk11.model.impl;

import java.time.Duration;
import java.time.Instant;
import jdk.jfr.consumer.RecordedEvent;
import org.graalvm.visualvm.jfr.jdk9.model.impl.JFRJDK9Event;
import org.graalvm.visualvm.jfr.model.JFRPropertyNotAvailableException;

/**
 *
 * @author Jiri Sedlacek
 */
final class JFRJDK11Event extends JFRJDK9Event {    
    
    JFRJDK11Event(RecordedEvent event, long id) {
        super(event, id);
    }

    
    @Override
    public boolean getBoolean(String key) throws JFRPropertyNotAvailableException {
        try {
            return event.getBoolean(key);
        } catch (IllegalArgumentException e) {
            throw new JFRPropertyNotAvailableException(e);
        }
    }

    @Override
    public byte getByte(String key) throws JFRPropertyNotAvailableException {
        try {
            return event.getByte(key);
        } catch (IllegalArgumentException e) {
            throw new JFRPropertyNotAvailableException(e);
        }
    }

    @Override
    public char getChar(String key) throws JFRPropertyNotAvailableException {
        try {
            return event.getChar(key);
        } catch (IllegalArgumentException e) {
            throw new JFRPropertyNotAvailableException(e);
        }
    }

    @Override
    public double getDouble(String key) throws JFRPropertyNotAvailableException {
        try {
            return event.getDouble(key);
        } catch (IllegalArgumentException e) {
            throw new JFRPropertyNotAvailableException(e);
        }
    }

    @Override
    public float getFloat(String key) throws JFRPropertyNotAvailableException {
        try {
            return event.getFloat(key);
        } catch (IllegalArgumentException e) {
            throw new JFRPropertyNotAvailableException(e);
        }
    }

    @Override
    public int getInt(String key) throws JFRPropertyNotAvailableException {
        try {
            return event.getInt(key);
        } catch (IllegalArgumentException e) {
            throw new JFRPropertyNotAvailableException(e);
        }
    }

    @Override
    public long getLong(String key) throws JFRPropertyNotAvailableException {
        try {
            return event.getLong(key);
        } catch (IllegalArgumentException e) {
            throw new JFRPropertyNotAvailableException(e);
        }
    }

    @Override
    public short getShort(String key) throws JFRPropertyNotAvailableException {
        try {
            return event.getShort(key);
        } catch (IllegalArgumentException e) {
            throw new JFRPropertyNotAvailableException(e);
        }
    }
    
    @Override
    public String getString(String key) throws JFRPropertyNotAvailableException {
        try {
            return event.getString(key);
        } catch (IllegalArgumentException e) {
            throw new JFRPropertyNotAvailableException(e);
        }
    }
    
    
    @Override
    public Instant getInstant(String key) throws JFRPropertyNotAvailableException {
        switch (key) {
            case "eventTime": // NOI18N
            case "startTime": // NOI18N
                return event.getStartTime();
                
            case "endTime": // NOI18N
                return event.getEndTime();
        }
        
        try {
            return event.getInstant(key);
        } catch (IllegalArgumentException e) {
            throw new JFRPropertyNotAvailableException(e);
        }
    }
    
    @Override
    public Duration getDuration(String key) throws JFRPropertyNotAvailableException {
        if ("eventDuration".equals(key)) { // NOI18N
            return event.getDuration();
        } else try {
            return event.getDuration(key);
        } catch (IllegalArgumentException e) {
            throw new JFRPropertyNotAvailableException(e);
        }
    }
    
    
    @Override
    public boolean equals(Object o) {
        return o instanceof JFRJDK11Event ? event.equals(((JFRJDK11Event)o).event) : false;
    }
    
}
