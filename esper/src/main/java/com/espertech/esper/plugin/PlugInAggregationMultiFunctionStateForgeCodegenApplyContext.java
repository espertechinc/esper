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
import com.espertech.esper.epl.expression.accessagg.ExprPlugInAggMultiFunctionNodeFactory;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;

public class PlugInAggregationMultiFunctionStateForgeCodegenApplyContext {
    private final ExprPlugInAggMultiFunctionNodeFactory parent;
    private final int column;
    private final CodegenMethodNode method;
    private final ExprForgeCodegenSymbol symbols;
    private final CodegenClassScope classScope;
    private final CodegenNamedMethods namedMethods;

    public PlugInAggregationMultiFunctionStateForgeCodegenApplyContext(ExprPlugInAggMultiFunctionNodeFactory parent, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        this.parent = parent;
        this.column = column;
        this.method = method;
        this.symbols = symbols;
        this.classScope = classScope;
        this.namedMethods = namedMethods;
    }

    public ExprPlugInAggMultiFunctionNodeFactory getParent() {
        return parent;
    }

    public int getColumn() {
        return column;
    }

    public CodegenMethodNode getMethod() {
        return method;
    }

    public ExprForgeCodegenSymbol getSymbols() {
        return symbols;
    }

    public CodegenClassScope getClassScope() {
        return classScope;
    }

    public CodegenNamedMethods getNamedMethods() {
        return namedMethods;
    }
}
