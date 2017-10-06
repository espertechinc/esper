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
package com.espertech.esper.plugin;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenNamedMethods;

public class PlugInAggregationMultiFunctionStateForgeCodegenClearContext {
    private final int column;
    private final CodegenMethodNode method;
    private final CodegenClassScope classScope;
    private final CodegenNamedMethods namedMethods;

    public PlugInAggregationMultiFunctionStateForgeCodegenClearContext(int column, CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        this.column = column;
        this.method = method;
        this.classScope = classScope;
        this.namedMethods = namedMethods;
    }

    public int getColumn() {
        return column;
    }

    public CodegenMethodNode getMethod() {
        return method;
    }

    public CodegenClassScope getClassScope() {
        return classScope;
    }

    public CodegenNamedMethods getNamedMethods() {
        return namedMethods;
    }
}
