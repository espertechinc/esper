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
package com.espertech.esper.compiler.internal.util;

import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClass;

import java.util.List;

public class CompilableItem {
    private final String providerClassName;
    private final List<CodegenClass> classes;
    private final CompilableItemPostCompileLatch postCompileLatch;

    public CompilableItem(String providerClassName, List<CodegenClass> classes, CompilableItemPostCompileLatch postCompileLatch) {
        this.providerClassName = providerClassName;
        this.classes = classes;
        this.postCompileLatch = postCompileLatch;
    }

    public String getProviderClassName() {
        return providerClassName;
    }

    public List<CodegenClass> getClasses() {
        return classes;
    }

    public CompilableItemPostCompileLatch getPostCompileLatch() {
        return postCompileLatch;
    }
}
