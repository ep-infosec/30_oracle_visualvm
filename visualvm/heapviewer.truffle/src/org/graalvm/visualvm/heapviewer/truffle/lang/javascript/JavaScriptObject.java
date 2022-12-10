/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.truffle.lang.javascript;

import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObject;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;

/**
 *
 * @author Jiri Sedlacek
 */
class JavaScriptObject extends DynamicObject {    
    
    JavaScriptObject(Instance instance) {
        super(instance);
    }
    
    JavaScriptObject(String type, Instance instance) {
        super(type, instance); // NOI18N
    }
    
    
    @Override
    protected String computeType() {
        Heap heap = getInstance().getJavaClass().getHeap();
        return JavaScriptLanguage.instance().fragmentFromHeap(heap).getObjectType(getInstance(), getShape());
    }
    
    String computeDisplayType() {
        String type = getType();
        if (type.startsWith("<")) { // NOI18N
            type = super.computeType();
            if (type.startsWith("JS")) type = type.substring(2); // NOI18N
        }
        return type;
    }
    
    
    boolean isJavaScriptObject() {
        return JavaScriptHeapFragment.JS_LANG_ID.equals(getLanguageId().getName());
    }
    
    static boolean isJavaScriptObject(Instance instance) {
        return DynamicObject.isDynamicObject(instance) &&
               JavaScriptHeapFragment.JS_LANG_ID.equals(DynamicObject.getLanguageId(instance).getName());
    }
    
}
