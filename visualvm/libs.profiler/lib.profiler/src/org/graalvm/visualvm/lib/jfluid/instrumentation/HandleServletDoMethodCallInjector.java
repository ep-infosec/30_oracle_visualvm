/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.instrumentation;

import org.graalvm.visualvm.lib.jfluid.classfile.DynamicClassInfo;


/**
 * Specialized subclass of Injector, that provides injection of our standard handleServletDoMethod(javax.servlet.http.HttpServletRequest request)
 * call into javax.servlet.http.HttpServlet methods
 * void doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
 * void doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
 * void doPut(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
 * void doDelete(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
 *
 *  @author Tomas Hurka
 */
class HandleServletDoMethodCallInjector extends SpecialCallInjector {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static String[] methodNames = { "doGet", "doPost", "doPut", "doDelete" }; // NOI18N
    private static String SIGNATURE = "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V"; // NOI18N
    private static String[] methodSignatures = { SIGNATURE, SIGNATURE, SIGNATURE, SIGNATURE };

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    HandleServletDoMethodCallInjector(DynamicClassInfo clazz, int baseCPoolCount, int methodIdx) {
        super(clazz, baseCPoolCount, methodIdx);
        targetMethodIdx = CPExtensionsRepository.miContents_HandleServletDoMethodIdx + baseCPoolCount;
        initializeInjectedCode();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    static String getClassName() {
        return "javax/servlet/http/HttpServlet"; // NOI18N
    }

    static String[] getMethodNames() {
        return methodNames;
    }

    static String[] getMethodSignatures() {
        return methodSignatures;
    }

    private void initializeInjectedCode() {
        injectedCodeLen = 4;
        injectedCode = new byte[injectedCodeLen];
        injectedCode[0] = (byte) opc_aload_1;
        injectedCode[1] = (byte) opc_invokestatic;
        // Positions 2, 3 are occupied by method index
        injectedCodeMethodIdxPos = 2;
    }
}
