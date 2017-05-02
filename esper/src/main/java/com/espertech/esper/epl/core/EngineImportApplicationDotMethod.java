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
package com.espertech.esper.epl.core;

import com.espertech.esper.epl.join.plan.FilterExprAnalyzerAffector;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.filter.FilterSpecCompilerAdvIndexDesc;

public interface EngineImportApplicationDotMethod {

    ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException;

    FilterExprAnalyzerAffector getFilterExprAnalyzerAffector();
    FilterSpecCompilerAdvIndexDesc getFilterSpecCompilerAdvIndexDesc();

    ExprEvaluator getExprEvaluator();

    String getLhsName();
    ExprNode[] getLhs();
    String getDotMethodName();
    String getRhsName();
    ExprNode[] getRhs();
}
