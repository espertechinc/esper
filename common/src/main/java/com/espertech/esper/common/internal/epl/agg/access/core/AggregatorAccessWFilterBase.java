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
package com.espertech.esper.common.internal.epl.agg.access.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedMethods;
import com.espertech.esper.common.internal.epl.agg.core.AggregatorAccess;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

public abstract class AggregatorAccessWFilterBase implements AggregatorAccess {
    protected final ExprNode optionalFilter;

    protected abstract void applyEnterFiltered(CodegenMethod method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope, CodegenNamedMethods namedMethods);

    protected abstract void applyLeaveFiltered(CodegenMethod method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope, CodegenNamedMethods namedMethods);

    public AggregatorAccessWFilterBase(ExprNode optionalFilter) {
        this.optionalFilter = optionalFilter;
    }

    public void applyEnterCodegen(CodegenMethod method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        if (optionalFilter != null) {
            AggregatorCodegenUtil.prefixWithFilterCheck(optionalFilter.getForge(), method, symbols, classScope);
        }
        applyEnterFiltered(method, symbols, classScope, namedMethods);
    }

    public void applyLeaveCodegen(CodegenMethod method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        if (optionalFilter != null) {
            AggregatorCodegenUtil.prefixWithFilterCheck(optionalFilter.getForge(), method, symbols, classScope);
        }
        applyLeaveFiltered(method, symbols, classScope, namedMethods);
    }
}
