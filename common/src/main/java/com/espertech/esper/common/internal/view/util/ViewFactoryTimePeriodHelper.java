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
package com.espertech.esper.common.internal.view.util;

import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodComputeConstGivenDeltaForge;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodComputeForge;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodComputeNCGivenExprForge;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriod;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriodUtil;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.view.core.ViewForgeEnv;
import com.espertech.esper.common.internal.view.core.ViewParameterException;

public class ViewFactoryTimePeriodHelper {

    public static TimePeriodComputeForge validateAndEvaluateTimeDeltaFactory(String viewName,
                                                                             ExprNode expression,
                                                                             String expectedMessage,
                                                                             int expressionNumber,
                                                                             ViewForgeEnv viewForgeEnv,
                                                                             int streamNumber)
            throws ViewParameterException {
        StreamTypeService streamTypeService = new StreamTypeServiceImpl(false);
        TimePeriodComputeForge forge;
        if (expression instanceof ExprTimePeriod) {
            ExprTimePeriod validated = (ExprTimePeriod) ViewForgeSupport.validateExpr(viewName, expression, streamTypeService, viewForgeEnv, expressionNumber, streamNumber);
            forge = validated.getTimePeriodComputeForge();
        } else {
            ExprNode validated = ViewForgeSupport.validateExpr(viewName, expression, streamTypeService, viewForgeEnv, expressionNumber, streamNumber);
            Class returnType = JavaClassHelper.getBoxedType(validated.getForge().getEvaluationType());
            if (!JavaClassHelper.isNumeric(returnType)) {
                throw new ViewParameterException(expectedMessage);
            }
            if (validated.getForge().getForgeConstantType().isCompileTimeConstant()) {
                TimeAbacus timeAbacus = viewForgeEnv.getClasspathImportServiceCompileTime().getTimeAbacus();
                ExprEvaluator secondsEvaluator = validated.getForge().getExprEvaluator();
                Number time = (Number) ViewForgeSupport.evaluate(secondsEvaluator, 0, viewName);
                if (ExprTimePeriodUtil.validateTime(time, timeAbacus)) {
                } else {
                    throw new ViewParameterException(ExprTimePeriodUtil.getTimeInvalidMsg(viewName, "view", time));
                }
                long msec = timeAbacus.deltaForSecondsNumber(time);
                forge = new TimePeriodComputeConstGivenDeltaForge(msec);
            } else {
                forge = new TimePeriodComputeNCGivenExprForge(validated.getForge(), viewForgeEnv.getClasspathImportServiceCompileTime().getTimeAbacus());
            }
        }
        return forge;
    }
}
