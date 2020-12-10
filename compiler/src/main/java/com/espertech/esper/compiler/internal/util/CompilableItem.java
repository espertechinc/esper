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
import com.espertech.esper.common.internal.context.compile.ContextCompileTimeDescriptor;
import com.espertech.esper.common.internal.fabric.FabricCharge;

import java.util.List;
import java.util.Map;

public class CompilableItem {
    private final String providerClassName;
    private final List<CodegenClass> classes;
    private final CompilableItemPostCompileLatch postCompileLatch;
    private final Map<String, byte[]> classesProvided;
    private final ContextCompileTimeDescriptor contextDescriptor;
    private final FabricCharge fabricCharge;

    public CompilableItem(String providerClassName, List<CodegenClass> classes, CompilableItemPostCompileLatch postCompileLatch, Map<String, byte[]> classesProvided, ContextCompileTimeDescriptor contextDescriptor, FabricCharge fabricCharge) {
        this.providerClassName = providerClassName;
        this.classes = classes;
        this.postCompileLatch = postCompileLatch;
        this.classesProvided = classesProvided;
        this.contextDescriptor = contextDescriptor;
        this.fabricCharge = fabricCharge;
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

    public Map<String, byte[]> getClassesProvided() {
        return classesProvided;
    }

    public ContextCompileTimeDescriptor getContextDescriptor() {
        return contextDescriptor;
    }

    public FabricCharge getFabricCharge() {
        return fabricCharge;
    }
}
