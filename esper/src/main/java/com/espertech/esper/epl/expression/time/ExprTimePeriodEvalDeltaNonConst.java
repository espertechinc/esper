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
package com.espertech.esper.epl.expression.time;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.schedule.TimeProvider;

public interface ExprTimePeriodEvalDeltaNonConst {
    public long deltaAdd(long currentTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context);
    public CodegenExpression deltaAddCodegen(CodegenExpression reference, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope);

    public long deltaSubtract(long currentTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context);

    public long deltaUseEngineTime(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, TimeProvider timeProvider);

    public ExprTimePeriodEvalDeltaResult deltaAddWReference(long current, long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context);

}
