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
package com.espertech.esper.epl.agg.access;

import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;

import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class AggregationAgentCodegenSymbols extends ExprForgeCodegenSymbol {
    public final static String NAME_AGENTSTATE = "state";
    public final static CodegenExpressionRef REF_AGENTSTATE = ref(NAME_AGENTSTATE);

    private CodegenExpressionRef optionalStateRef;

    public AggregationAgentCodegenSymbols(boolean allowUnderlyingReferences, Boolean newDataValue) {
        super(allowUnderlyingReferences, newDataValue);
    }

    public CodegenExpressionRef getAddState(CodegenMethodScope scope) {
        if (optionalStateRef == null) {
            optionalStateRef = REF_AGENTSTATE;
        }
        scope.addSymbol(optionalStateRef);
        return optionalStateRef;
    }

    @Override
    public void provide(Map<String, Class> symbols) {
        super.provide(symbols);
        if (optionalStateRef != null) {
            symbols.put(optionalStateRef.getRef(), AggregationState.class);
        }
    }
}
