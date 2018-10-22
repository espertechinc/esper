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
package com.espertech.esper.common.internal.epl.expression.subquery;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public class SubselectForgeNRSymbol extends ExprSubselectEvalMatchSymbol {

    public final static String NAME_LEFTRESULT = "leftResult";

    public final static CodegenExpressionRef REF_LEFTRESULT = ref(NAME_LEFTRESULT);

    private final Class leftResultType;
    private CodegenExpressionRef optionalLeftResult;

    public SubselectForgeNRSymbol(Class leftResultType) {
        super();
        this.leftResultType = leftResultType;
    }

    public Class getLeftResultType() {
        return leftResultType;
    }

    public CodegenExpressionRef getAddLeftResult(CodegenMethodScope scope) {
        if (optionalLeftResult == null) {
            optionalLeftResult = REF_LEFTRESULT;
        }
        scope.addSymbol(optionalLeftResult);
        return optionalLeftResult;
    }

    @Override
    public void provide(Map<String, Class> symbols) {
        if (optionalLeftResult != null) {
            symbols.put(optionalLeftResult.getRef(), leftResultType);
        }
        super.provide(symbols);
    }
}
