/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.graalvm.visualvm.jfr.model.JFRModel;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ValuesConverter {
    
    private ValuesConverter() {}
    
    
    public static long instantToNanos(Instant instant) {
        return TimeUnit.SECONDS.toNanos(instant.getEpochSecond()) + instant.getNano();
    }
    
    public static long instantToRelativeNanos(Instant instant, JFRModel model) {
        return durationToNanos(model.toRelativeTime(instant));
    }
    
    public static long instantToMillis(Instant instant) {
        return instant.toEpochMilli();
    }
    
    public static long instantToRelativeMillis(Instant instant, JFRModel model) {
        return durationToMillis(model.toRelativeTime(instant));
    }

    public static long durationToNanos(Duration duration) {
        return duration.toNanos();
    }
    
    public static long durationToMicros(Duration duration) {
        long micros = duration.getSeconds() * 1000000;
        micros += duration.getNano() / 1000;
        return micros;
    }
    
    public static long durationToMillis(Duration duration) {
        return duration.toMillis();
    }

    public static long nanosToMillis(long nanos) {
        return nanos / 1000000;
    }
    
}
