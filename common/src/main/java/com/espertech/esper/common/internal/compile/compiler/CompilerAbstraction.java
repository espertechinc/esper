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
package com.espertech.esper.common.internal.compile.compiler;

import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClass;

import java.util.List;

public interface CompilerAbstraction {
    CompilerAbstractionClassCollection newClassCollection();
    void compileClasses(List<CodegenClass> classes, CompilerAbstractionCompilationContext context, CompilerAbstractionClassCollection state);
    CompilerAbstractionCompileSourcesResult compileSources(List<String> sources, CompilerAbstractionCompilationContext context, CompilerAbstractionClassCollection state);
}
