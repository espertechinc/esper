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
package com.espertech.esper.common.internal.epl.agg.groupby;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class AggSvcGroupByReclaimAgedEvalFuncFactoryConstForge implements AggSvcGroupByReclaimAgedEvalFuncFactoryForge {
    private final double valueDouble;

    public AggSvcGroupByReclaimAgedEvalFuncFactoryConstForge(double valueDouble) {
        this.valueDouble = valueDouble;
    }

    public CodegenExpressionField make(CodegenClassScope classScope) {
        return classScope.addFieldUnshared(true, AggSvcGroupByReclaimAgedEvalFuncFactoryConst.class,
                newInstance(AggSvcGroupByReclaimAgedEvalFuncFactoryConst.class, constant(valueDouble)));
    }
}
