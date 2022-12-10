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

package org.graalvm.visualvm.lib.charts.axis;

import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ResourceBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public class BytesAxisUtils {

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.charts.axis.Bundle"); // NOI18N
    public static final String UNITS_B = messages.getString("BytesAxisUtils_AbbrBytes"); // NOI18N
    public static final String UNITS_KB = messages.getString("BytesAxisUtils_AbbrKiloBytes"); // NOI18N
    public static final String UNITS_MB = messages.getString("BytesAxisUtils_AbbrMegaBytes"); // NOI18N
    public static final String UNITS_GB = messages.getString("BytesAxisUtils_AbbrGigaBytes"); // NOI18N
    public static final String UNITS_TB = messages.getString("BytesAxisUtils_AbbrTeraBytes"); // NOI18N
    public static final String UNITS_PB = messages.getString("BytesAxisUtils_AbbrPetaBytes"); // NOI18N
    public static final String UNITS_BPS = messages.getString("BytesAxisUtils_AbbrBitsPerSec"); // NOI18N
    public static final String UNITS_KBPS = messages.getString("BytesAxisUtils_AbbrKiloBitsPerSec"); // NOI18N
    public static final String UNITS_MBPS = messages.getString("BytesAxisUtils_AbbrMegaBitsPerSec"); // NOI18N
    public static final String UNITS_GBPS = messages.getString("BytesAxisUtils_AbbrGigaBitsPerSec"); // NOI18N
    public static final String UNITS_TBPS = messages.getString("BytesAxisUtils_AbbrTeraBitsPerSec"); // NOI18N
    public static final String UNITS_PBPS = messages.getString("BytesAxisUtils_AbbrPetaBitsPerSec"); // NOI18N
    private static final String SIZE_FORMAT = messages.getString("BytesAxisUtils_SizeFormat"); // NOI18N
    // -----

    public static final long[] bytesUnitsGrid = new long[] { 1, 2, 5, 10, 25, 50, 100, 250, 500 };
    public static final String[] radixUnits = new String[] { UNITS_B, UNITS_KB, UNITS_MB, UNITS_GB, UNITS_TB, UNITS_PB };
    public static final String[] radixUnitsBps = new String[] { UNITS_BPS, UNITS_KBPS, UNITS_MBPS, UNITS_GBPS, UNITS_TBPS, UNITS_PBPS };

    private static final NumberFormat FORMAT = NumberFormat.getInstance();

    public static long[] getBytesUnits(double scale, int minDistance) {
        if (Double.isNaN(scale) || scale == Double.POSITIVE_INFINITY || scale <= 0)
            return new long[] { -1, -1 };

        long bytesFactor = 1;
        long bytesRadix  = 0;

        while (true) {
            for (int i = 0; i < bytesUnitsGrid.length; i++)
                if ((bytesUnitsGrid[i] * scale * bytesFactor) >= minDistance)
                    return new long[] { bytesUnitsGrid[i] * bytesFactor, bytesRadix };

            bytesFactor *= 1024;
            bytesRadix  += 1;
        }
    }

    public static String getRadixUnits(BytesMark mark) {
        int radix = mark.getRadix();
        if (radix < 0 || radix >= radixUnits.length) return ""; // NOI18N
        return radixUnits[radix];
    }

    public static String getRadixUnitsBps(BytesMark mark) {
        int radix = mark.getRadix();
        if (radix < 0 || radix >= radixUnitsBps.length) return ""; // NOI18N
        return radixUnitsBps[radix];
    }

    public static String formatBytes(BytesMark mark) {
        int radix = mark.getRadix();
        long value = mark.getValue() / (long)Math.pow(1024, radix);
        String units = getRadixUnits(mark);

        return MessageFormat.format(SIZE_FORMAT, new Object[] { FORMAT.format(value), units });
    }

    public static String formatBitsPerSec(BytesMark mark) {
        int radix = mark.getRadix();
        long value = mark.getValue() / (long)Math.pow(1024, radix);
        String units = getRadixUnitsBps(mark);

        return MessageFormat.format(SIZE_FORMAT, new Object[] { FORMAT.format(value), units });
    }
}
