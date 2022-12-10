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
package org.graalvm.visualvm.lib.profiler.heapwalk.details.basic;

import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectArrayInstance;
import org.graalvm.visualvm.lib.jfluid.heap.PrimitiveArrayInstance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ArrayDetailsProvider_OneItemString=1 item",                                // NOI18N
    "# {0} - number of items",                                                  // NOI18N
    "ArrayDetailsProvider_ItemsNumberString={0} items"                          // NOI18N
})
@ServiceProvider(service=DetailsProvider.class)
public final class ArrayDetailsProvider extends DetailsProvider {

    public String getDetailsString(String className, Instance instance) {
        if (instance instanceof PrimitiveArrayInstance) {
            if ("char[]".equals(instance.getJavaClass().getName())) {           // NOI18N
                return DetailsUtils.getPrimitiveArrayString(
                        instance, 0, -1, null, "...");                          // NOI18N
            } else {
                return getItemsString(((PrimitiveArrayInstance)instance).getLength());
            }
        } else if (instance instanceof ObjectArrayInstance) {
            return getItemsString(((ObjectArrayInstance)instance).getLength());
        }
        return null;
    }

    public View getDetailsView(String className, Instance instance) {
        if (instance instanceof PrimitiveArrayInstance)
            return new ArrayValueView(className, instance);
        else return null;
    }

    private static String getItemsString(int length) {
        return length == 1 ? Bundle.ArrayDetailsProvider_OneItemString() :
                             Bundle.ArrayDetailsProvider_ItemsNumberString(length);
    }
    
}
