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
package com.espertech.esper.common.internal.epl.join.querygraph;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class QueryGraphValueEntryCustomOperationForge implements QueryGraphValueEntryForge {
    private final Map<Integer, ExprNode> positionalExpressions = new HashMap<>();

    public Map<Integer, ExprNode> getPositionalExpressions() {
        return positionalExpressions;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(QueryGraphValueEntryCustomOperation.class, this.getClass(), classScope);
        method.getBlock().declareVar(Map.class, "map", newInstance(HashMap.class, constant(CollectionUtil.capacityHashMap(positionalExpressions.size()))));
        for (Map.Entry<Integer, ExprNode> entry : positionalExpressions.entrySet()) {
            method.getBlock().exprDotMethod(ref("map"), "put", constant(entry.getKey()), ExprNodeUtilityCodegen.codegenEvaluator(entry.getValue().getForge(), method, this.getClass(), classScope));
        }
        method.getBlock()
                .declareVar(QueryGraphValueEntryCustomOperation.class, "op", newInstance(QueryGraphValueEntryCustomOperation.class))
                .exprDotMethod(ref("op"), "setPositionalExpressions", ref("map"))
                .methodReturn(ref("op"));
        return localMethod(method);
    }
}

