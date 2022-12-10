/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.gotosource.viewer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.graalvm.visualvm.core.datasource.Storage;

/**
 * @author Jiri Sedlacek
 */
class ExternalViewerLauncher implements Runnable {
    
    private static final String COMMAND_STRINGS_REGEX = "\'[^\']*\'|\"[^\"]*\"|\\S+"; // NOI18N
    
    
    private final List<String> command;
    
    
    ExternalViewerLauncher(List<String> command) {
        this.command = command;
    }
    
    
    public final void run() {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command(command);
        builder.directory(Storage.getTemporaryStorageDirectory());
        
        try { builder.start(); }
        catch (IOException e) { failed(e); }
    }
    
        
    protected void failed(IOException e) {}
    
    
    public static List<String> getCommandStrings(String commandString) {
        List<String> command = new ArrayList<>();
        
        Pattern pattern = Pattern.compile(COMMAND_STRINGS_REGEX);
        Matcher matcher = pattern.matcher(commandString);
        while (matcher.find()) command.add(matcher.group());
        
        return command;
    }
    
}
