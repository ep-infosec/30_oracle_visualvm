/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.graalvm.application.type;

import java.util.Properties;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.type.ApplicationType;
import org.graalvm.visualvm.application.type.MainClassApplicationTypeFactory;
import org.openide.util.NbBundle;

/**
 * Factory which recognizes GraalVM and some applications based on GraalVM
 *
 * @author Tomas Hurka
 */
public class GraalVMApplicationTypeFactory extends MainClassApplicationTypeFactory {

    private static final String MAIN_CLASS = "com.oracle.graalvm.Main"; // NOI18N
    private static final String LEGACY_MAIN_CLASS = "com.oracle.graalvm.launcher.LegacyLauncher"; // NOI18N
    private static final String JAVASCRIPT_MAIN_CLASS = "com.oracle.truffle.js.shell.JSLauncher";   // NOI18N
    private static final String R_LEGACY_MAIN_CLASS = "com.oracle.truffle.r.legacylauncher.LegacyLauncher"; // NOI18N
    private static final String R_MAIN_CLASS = "com.oracle.truffle.r.launcher.RMain"; // NOI18N
    private static final String RUBY_MAIN_CLASS = "org.truffleruby.launcher.RubyLauncher"; // NOI18N
    private static final String PYTHON_MAIN_CLASS = "com.oracle.graal.python.shell.GraalPythonMain"; // NOI18N
    private static final String PYTHON_EE_MAIN_CLASS = "com.oracle.graal.python.enterprise.shell.GraalPythonEnterpriseMain"; // NOI18N
    private static final String LLVM_MAIN_CLASS = "com.oracle.truffle.llvm.launcher.LLVMLauncher"; // NOI18N
    private static final String GRAAL_SYSPROP_ID = "graalvm.home"; // NOI18N
    private static final String GRAAL_SYSPROP1_ID = "org.graalvm.home"; // NOI18N
    private static final String LAUNCHER_SYSPROP_ID = "org.graalvm.launcher.class";  // NOI18N
    private static final String JVM_ARG_GRAAL_ID = "-D"+GRAAL_SYSPROP_ID+"="; // NOI18N
    private static final String JVM_ARG_GRAAL1_ID = "-Dgraal.CompilerConfiguration="; // NOI18N
    private static final String ARG_GRAAL_ID = "--"; // NOI18N
    private static final String JVM_ARG_NODEJS_ID = "-Dtruffle.js.DirectByteBuffer=true";  // NOI18N
    private static final String JVM_ARG_NODEJS1_ID = "-Dtruffle.js.DebugPropertyName=GraalJsDebug";  // NOI18N

    private static final String JAVASCRIPT_ID = "com.oracle.truffle.js.shell.Shell"; // NOI18N
    private static final String R_ID = "com.oracle.truffle.r.engine.shell.RCommand";    // NOI18N
    private static final String LEGACY_RSCRIPT_ID = "com.oracle.truffle.r.launcher.RscriptCommand"; // NOI18N
    private static final String LEGACY_R_ID = "com.oracle.truffle.r.launcher.RCommand"; // NOI18N
    private static final String RUBY_ID = "org.truffleruby.Main"; // NOI18N;
    private static final String NODEJS_ID = "node.js"; // NOI18N;
    private static final String PYTHON_ID = "GraalPythonMain"; // NOI18N
    private static final String LLVM_ID = "LLVM"; // NOI18N

    private boolean isGraalVM(Jvm jvm, String mainClass) {
        if (MAIN_CLASS.equals(mainClass) || LEGACY_MAIN_CLASS.equals(mainClass)) {
            return true;
        }
        if (JAVASCRIPT_MAIN_CLASS.equals(mainClass)) {
            return true;
        }
        if (R_MAIN_CLASS.equals(mainClass)) {
            return true;
        }
        if (R_LEGACY_MAIN_CLASS.equals(mainClass)) {
            return true;
        }
        if (RUBY_MAIN_CLASS.equals(mainClass)) {
            return true;
        }
        if (PYTHON_MAIN_CLASS.equals(mainClass)) {
            return true;
        }
        if (PYTHON_EE_MAIN_CLASS.equals(mainClass)) {
            return true;
        }
        if (LLVM_MAIN_CLASS.equals(mainClass)) {
            return true;
        }
        if (mainClass == null || mainClass.isEmpty()) {    // there is no main class - detect native GraalVM launcher
            String args = jvm.getJvmArgs();
            if (args != null) {
                if (args.contains(JVM_ARG_GRAAL_ID) || args.contains(JVM_ARG_GRAAL1_ID) || args.contains(JVM_ARG_NODEJS_ID)) {
                    return true;
                }
            }
            if (jvm.isGetSystemPropertiesSupported()) {
                Properties sysProp = jvm.getSystemProperties();

                if (sysProp != null) {
                    if (sysProp.getProperty(GRAAL_SYSPROP_ID) != null
                            || sysProp.getProperty(GRAAL_SYSPROP1_ID) != null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String getLangID(Jvm jvm) {
        String args = jvm.getMainArgs();
        String mainClass = jvm.getMainClass();

        if ((mainClass == null || mainClass.isEmpty()) && jvm.isGetSystemPropertiesSupported()) {
            Properties sysProp = jvm.getSystemProperties();

            if (sysProp != null) {
                mainClass = sysProp.getProperty(LAUNCHER_SYSPROP_ID);
            }
        }
        if (LEGACY_MAIN_CLASS.equals(mainClass)) {
            if (args != null) {
                String[] argArr = args.split(" +");
                if (argArr.length > 1) {
                    return argArr[1];
                }
            }
        }
        if (JAVASCRIPT_MAIN_CLASS.equals(mainClass)) {
            return JAVASCRIPT_ID;
        }
        if (R_MAIN_CLASS.equals(mainClass)) {
            return R_ID;
        }
        if (R_LEGACY_MAIN_CLASS.equals(mainClass)) {
            return R_ID;
        }
        if (RUBY_MAIN_CLASS.equals(mainClass)) {
            return RUBY_ID;
        }
        if (PYTHON_MAIN_CLASS.equals(mainClass)) {
            return PYTHON_ID;
        }
        if (PYTHON_EE_MAIN_CLASS.equals(mainClass)) {
            return PYTHON_ID;
        }
        if (LLVM_MAIN_CLASS.equals(mainClass)) {
            return LLVM_ID;
        }
        if (args != null) {
            String[] argArr = args.split(" +");
            if (argArr.length > 2) {
                if (ARG_GRAAL_ID.equals(argArr[1])) {
                    return argArr[2];
                }
            }
        }
        return null;
    }

    private String getName(String lang) {
        if (lang == null) {
            return getMessage("LBL_GraalVM");
        }
        switch (lang) {
            case JAVASCRIPT_ID:
                return getMessage("LBL_Graalvm_Javascript");    // NOI18N
            case R_ID:
            case LEGACY_R_ID:
                return getMessage("LBL_Graalvm_R");     // NOI18N
            case LEGACY_RSCRIPT_ID:
                return getMessage("LBL_Graalvm_Rscript");     // NOI18N
            case RUBY_ID:
                return getMessage("LBL_Graalvm_Ruby");  // NOI18N
            case NODEJS_ID:
                return getMessage("LBL_Graalvm_Nodejs");  // NOI18N                
            case PYTHON_ID:
                return getMessage("LBL_Graalvm_Python");  // NOI18N
            case LLVM_ID:
                return getMessage("LBL_Graalvm_LLVM");  // NOI18N
            default:
                return lang;
        }
    }

    String getMessage(String string) {
        return NbBundle.getMessage(GraalVMApplicationTypeFactory.class, string);
    }

    @Override
    public ApplicationType createApplicationTypeFor(Application app, Jvm jvm, String mainClass) {
        if (isGraalVM(jvm, mainClass)) {
            String langId = getLangID(jvm);

            if (langId == null && (mainClass == null || mainClass.isEmpty())) {  // nodejs ???
                String jvmArgs = jvm.getJvmArgs();
                if (jvmArgs.contains(JVM_ARG_NODEJS_ID) || jvmArgs.contains(JVM_ARG_NODEJS1_ID)) {
                    langId = NODEJS_ID;
                }
            }
            String name = getName(langId);
            return new GraalVMApplicationType(app, jvm, name);
        }
        return null;
    }

}
