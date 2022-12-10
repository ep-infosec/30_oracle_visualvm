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
package org.graalvm.visualvm.heapviewer.console.r.engine;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;

/**
 *
 * @author Tomas Hurka
 */
public class REngine {
    final private static Logger LOGGER = Logger.getLogger(REngine.class.getName());
    private static Boolean supported;
    
    private AtomicBoolean cancelled;
    private final Heap heap;
    private Context rContext;
    private Value javaToDf;
    private ROut outStream;

    public static synchronized boolean isSupported() {
        if (supported == null) {
            try {
                Context c = Context.newBuilder("R").allowAllAccess(true).build();
                c.eval("R", "invisible(42)");
                supported = Boolean.TRUE;
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "REngine isSupported", e); // NOI18N
                supported = Boolean.FALSE;
            } catch (NoClassDefFoundError e) {
                LOGGER.log(Level.INFO, "REngine isSupported", e); // NOI18N
                supported = Boolean.FALSE;
            }
        }
        return supported.booleanValue();
    }

    public REngine(Heap h) {
        heap = h;
        initRContext();
    }

    private void initRContext() {
        cancelled = new AtomicBoolean(false);
        outStream = new ROut();
        rContext = Context.newBuilder("R").allowAllAccess(true).out(outStream).build();
        String javaToDfSrc =
                "function(c1, c2, c3, c4) { " +
                "   data.frame(ClassName=as.vector(c1, 'character')," +
                "              ClassId=as.vector(c2, 'character')," +
                "              Instances=as.vector(c3, 'integer')," +
                "              InstancesSize=as.vector(c4, 'integer')," +
                "stringsAsFactors = F)" +
                "}";
        javaToDf = rContext.eval("R", javaToDfSrc);
        List<JavaClass> classes = heap.getAllClasses();
        exportSymbol(rContext, "HeapClasses", javaToDf.execute(
                new NamesArray(classes),
                new ClassIDArray(classes),
                new InstancesArray(classes),
                new InstancesSizeArray(classes)));
        rContext.eval("R", "HeapClasses <- import('HeapClasses');");
        exportSymbol(rContext, "heap", heap);
        rContext.eval("R", "heap <- import('heap');");
        rContext.eval("R", "options(width=256)");
    }

    private void exportSymbol(Context ctx, String name, Object value) {
        ctx.getPolyglotBindings().putMember(name, value);
    }

    public synchronized void cancelQuery() {
        cancelled.set(true);
    }

    public synchronized boolean isCancelled() {
        return cancelled.get();
    }

    public void executeQuery(String rQuery, ObjectVisitor objectVisitor) {
        if (rContext == null) {
            initRContext();
        }
        cancelled.set(false);
        outStream.setVisitor(objectVisitor);
        rContext.eval("R", rQuery);
    }
    
    public Context getContext() {
        return rContext;
    }

    public static interface ObjectVisitor {

        // return true to terminate the result set callback earlier
        public boolean visit(Object o);

        public static final ObjectVisitor DEFAULT = new ObjectVisitor() {

            public boolean visit(Object o) {
                if (o != null && LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.finest(o.toString());
                }

                return true; // prevent calling "visit" for the rest of the result set
            }
        };
    }
    
    private static class ROut extends OutputStream {

        private ObjectVisitor visitor;
        private StringBuilder output;

        private ROut() {
            output = new StringBuilder();
        }
        
        
        @Override
        public void write(int b) throws IOException {
            if (b == '\n') {
                visitor.visit(output.toString());
                output = new StringBuilder();
            } else {
                output.append((char) b);
            }
        }

        private void setVisitor(ObjectVisitor objectVisitor) {
            visitor = objectVisitor;
        }
    }
}
