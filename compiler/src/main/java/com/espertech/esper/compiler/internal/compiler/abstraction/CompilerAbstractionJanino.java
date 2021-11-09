/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.compiler.internal.compiler.abstraction;

import com.espertech.esper.common.internal.compile.compiler.*;
import com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationIDGenerator;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClass;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.compiler.internal.compiler.janino.JaninoCompiler;
import org.codehaus.commons.compiler.CompileException;

import java.util.*;

public class CompilerAbstractionJanino implements CompilerAbstraction {
    public final static CompilerAbstractionJanino INSTANCE = new CompilerAbstractionJanino();

    private CompilerAbstractionJanino() {
    }

    public CompilerAbstractionClassCollection newClassCollection() {
        return new CompilerAbstractionClassCollectionImpl();
    }

    public void compileClasses(List<CodegenClass> classes, CompilerAbstractionCompilationContext context, CompilerAbstractionClassCollection state) {
        CompilerAbstractionClassCollectionImpl janino = (CompilerAbstractionClassCollectionImpl) state;
        for (CodegenClass clazz : classes) {
            JaninoCompiler.compile(clazz, janino.getClasses(), janino.getClasses(), context);
        }
    }

    public CompilerAbstractionCompileSourcesResult compileSources(List<String> sources, CompilerAbstractionCompilationContext context, CompilerAbstractionClassCollection state) {
        int index = -1;
        Set<String> names = new LinkedHashSet<>(CollectionUtil.capacityHashMap(sources.size()));
        for (String classText : sources) {
            index++;
            String filename = "provided_" + index + "_" + CodeGenerationIDGenerator.generateClassNameUUID();
            Map<String, byte[]> output = new HashMap<>();

            try {
                JaninoCompiler.compile(classText, filename, state.getClasses(), output, context.getCompileResultConsumer(), context.getServices());
            } catch (RuntimeException ex) {
                String message = ex.getMessage().replace(CompileException.class.getName() + ": ", "");
                throw new RuntimeException(message + " for class [\"\"\"" + classText + "\"\"\"]", ex);
            }

            for (Map.Entry<String, byte[]> entry : output.entrySet()) {
                if (state.getClasses().containsKey(entry.getKey())) {
                    throw new RuntimeException("Duplicate class by name '" + entry.getKey() + "'");
                }
                names.add(entry.getKey());
            }

            state.add(output);
        }
        return new CompilerAbstractionCompileSourcesResult(names);
    }
}
