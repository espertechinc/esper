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
package com.espertech.esper.epl.expression.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.event.EventPropertyGetterSPI;

public interface ExprIdentNodeEvaluator extends ExprEvaluator {
    public boolean evaluatePropertyExists(EventBean[] eventsPerStream, boolean isNewData);

    public int getStreamNum();

    public EventPropertyGetterSPI getGetter();

    public boolean isContextEvaluated();

    public Class getEvaluationType();

    CodegenExpression codegen(CodegenParamSetExprPremade params, CodegenContext context);
}
