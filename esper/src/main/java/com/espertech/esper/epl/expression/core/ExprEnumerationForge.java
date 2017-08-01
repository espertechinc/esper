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

import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.event.EventAdapterService;

/**
 * Interface for evaluating of an event tuple.
 */
public interface ExprEnumerationForge {

    Class getComponentTypeCollection() throws ExprValidationException;

    EventType getEventTypeCollection(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException;

    EventType getEventTypeSingle(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException;

    ExprEnumerationEval getExprEvaluatorEnumeration();

    CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenParamSetExprPremade params, CodegenContext context);

    CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenParamSetExprPremade params, CodegenContext context);

    CodegenExpression evaluateGetEventBeanCodegen(CodegenParamSetExprPremade params, CodegenContext context);

    ExprNodeRenderable getForgeRenderable();
}
