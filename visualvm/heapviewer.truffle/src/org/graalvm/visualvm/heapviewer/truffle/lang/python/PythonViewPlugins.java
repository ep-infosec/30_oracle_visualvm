/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.truffle.lang.python;

import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.truffle.TruffleObjectPropertyPlugin;
import org.graalvm.visualvm.heapviewer.ui.HeapViewPlugin;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "PythonViewPlugins_PropertiesName=Attributes",
    "PythonViewPlugins_PropertiesDescription=Attributes",
    "PythonViewPlugins_ItemsName=Items",
    "PythonViewPlugins_ItemsDescription=Items",
    "PythonViewPlugins_ReferencesName=References",
    "PythonViewPlugins_ReferencesDescription=References"
})
final class PythonViewPlugins {
    
    // -------------------------------------------------------------------------
    // --- Attributes ----------------------------------------------------------
    // -------------------------------------------------------------------------
    
    @ServiceProvider(service=HeapViewPlugin.Provider.class, position = 200)
    public static class AttributesPluginProvider extends HeapViewPlugin.Provider {

        public HeapViewPlugin createPlugin(HeapContext context, HeapViewerActions actions, String viewID) {
            if (!PythonHeapFragment.isPythonHeap(context)) return null;
            
            PythonObjectProperties.AttributesProvider fieldsProvider = Lookup.getDefault().lookup(PythonObjectProperties.AttributesProvider.class);
            return new TruffleObjectPropertyPlugin(Bundle.PythonViewPlugins_PropertiesName(), Bundle.PythonViewPlugins_PropertiesDescription(), Icons.getIcon(ProfilerIcons.NODE_FORWARD), "python_objects_attributes", context, actions, fieldsProvider); // NOI18N
        }
        
    }
    
    
    // -------------------------------------------------------------------------
    // --- Items ---------------------------------------------------------------
    // -------------------------------------------------------------------------
    
    @ServiceProvider(service=HeapViewPlugin.Provider.class, position = 300)
    public static class ItemsPluginProvider extends HeapViewPlugin.Provider {

        public HeapViewPlugin createPlugin(HeapContext context, HeapViewerActions actions, String viewID) {
            if (!PythonHeapFragment.isPythonHeap(context)) return null;
            
            PythonObjectProperties.ItemsProvider fieldsProvider = Lookup.getDefault().lookup(PythonObjectProperties.ItemsProvider.class);
            return new TruffleObjectPropertyPlugin(Bundle.PythonViewPlugins_ItemsName(), Bundle.PythonViewPlugins_ItemsDescription(), Icons.getIcon(ProfilerIcons.NODE_FORWARD), "python_objects_items", context, actions, fieldsProvider); // NOI18N
        }
        
    }
    
    
    // -------------------------------------------------------------------------
    // --- References ----------------------------------------------------------
    // -------------------------------------------------------------------------
    
    @ServiceProvider(service=HeapViewPlugin.Provider.class, position = 400)
    public static class ReferencesPluginProvider extends HeapViewPlugin.Provider {

        public HeapViewPlugin createPlugin(HeapContext context, HeapViewerActions actions, String viewID) {
            if (!PythonHeapFragment.isPythonHeap(context)) return null;
            
            PythonObjectProperties.ReferencesProvider fieldsProvider = Lookup.getDefault().lookup(PythonObjectProperties.ReferencesProvider.class);
            return new TruffleObjectPropertyPlugin(Bundle.PythonViewPlugins_ReferencesName(), Bundle.PythonViewPlugins_ReferencesDescription(), Icons.getIcon(ProfilerIcons.NODE_REVERSE), "python_objects_references", context, actions, fieldsProvider); // NOI18N
        }
        
    }
    
}
