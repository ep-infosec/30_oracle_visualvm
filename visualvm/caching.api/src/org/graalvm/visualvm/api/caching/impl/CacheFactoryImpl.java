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

package org.graalvm.visualvm.api.caching.impl;


import org.graalvm.visualvm.api.caching.Cache;
import org.graalvm.visualvm.api.caching.CacheFactory;
import org.graalvm.visualvm.api.caching.EntryFactory;
import org.graalvm.visualvm.api.caching.Persistor;
import org.openide.util.lookup.ServiceProvider;

/**
 * The implementation of {@linkplain CacheFactory}
 * @author Jaroslav Bachorik
 */
@ServiceProvider(service=CacheFactoryImpl.class)
public class CacheFactoryImpl {
    public <K,V> Cache<K,V> weakMapCache() {
        return new CacheImpl<>(new WeakKeyFactory<>());
    }
    public <K,V> Cache<K,V> weakMapCache(EntryFactory<K,V> resolver, Persistor<K,V> persistor) {
        return new CacheImpl<>(resolver, new WeakKeyFactory<>(), persistor);
    }
    public <K,V> Cache<K,V> weakMapCache(Persistor<K,V> persistor) {
        return new CacheImpl<>(new WeakKeyFactory<>(), persistor);
    }
    public <K,V> Cache<K,V> weakMapCache(EntryFactory<K,V> resolver) {
        return new CacheImpl<>(resolver, new WeakKeyFactory<>());
    }

    public <K,V> Cache<K,V> softMapCache() {
        return new CacheImpl<>(new SoftKeyFactory<>());
    }
    public <K,V> Cache<K,V> softMapCache(EntryFactory<K,V> resolver, Persistor<K,V> persistor) {
        return new CacheImpl<>(resolver, new SoftKeyFactory<>(), persistor);
    }
    public <K,V> Cache<K,V> softMapCache(Persistor<K,V> persistor) {
        return new CacheImpl<>(new SoftKeyFactory<>(), persistor);
    }
    public <K,V> Cache<K,V> softMapCache(EntryFactory<K,V> resolver) {
        return new CacheImpl<>(resolver, new SoftKeyFactory<>());
    }
}
