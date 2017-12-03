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
package com.espertech.esper.filterspec;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.lang.annotation.Annotation;

public interface FilterBooleanExpressionFactory {

    ExprNodeAdapterBase make(FilterSpecParamExprNode filterSpecParamExprNode, EventBean[] events, ExprEvaluatorContext exprEvaluatorContext, int agentInstanceId, EngineImportService engineImportService, Annotation[] annotations);
}
