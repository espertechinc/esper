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
package com.espertech.esper.common.internal.epl.historical.indexingstrategy;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class PollResultIndexingStrategyMultiForge implements PollResultIndexingStrategyForge {
    private final int streamNum;
    private final PollResultIndexingStrategyForge[] indexingStrategies;

    public PollResultIndexingStrategyMultiForge(int streamNum, PollResultIndexingStrategyForge[] indexingStrategies) {
        this.streamNum = streamNum;
        this.indexingStrategies = indexingStrategies;
    }

    public String toQueryPlan() {
        StringWriter writer = new StringWriter();
        String delimiter = "";
        for (PollResultIndexingStrategyForge strategy : indexingStrategies) {
            writer.append(delimiter);
            writer.append(strategy.toQueryPlan());
            delimiter = ", ";
        }
        return this.getClass().getSimpleName() + " " + writer.toString();
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(PollResultIndexingStrategyMulti.class, this.getClass(), classScope);

        method.getBlock().declareVar(PollResultIndexingStrategy[].class, "strats", newArrayByLength(PollResultIndexingStrategy.class, constant(indexingStrategies.length)));
        for (int i = 0; i < indexingStrategies.length; i++) {
            method.getBlock().assignArrayElement(ref("strats"), constant(i), indexingStrategies[i].make(method, symbols, classScope));
        }

        method.getBlock()
                .declareVar(PollResultIndexingStrategyMulti.class, "strat", newInstance(PollResultIndexingStrategyMulti.class))
                .exprDotMethod(ref("strat"), "setIndexingStrategies", ref("strats"))
                .methodReturn(ref("strat"));
        return localMethod(method);
    }
}
