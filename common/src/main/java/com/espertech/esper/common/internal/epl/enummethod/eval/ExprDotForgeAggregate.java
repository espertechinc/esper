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
package com.espertech.esper.common.internal.epl.enummethod.eval;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.enummethod.dot.*;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNodeUtility;
import com.espertech.esper.common.internal.epl.methodbase.DotMethodFP;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.List;

public class ExprDotForgeAggregate extends ExprDotForgeEnumMethodBase {

    public EventType[] getAddStreamTypes(DotMethodFP footprint, int parameterNum, EnumMethodEnum enumMethod, String enumMethodUsedName, List<String> goesToNames, EventType inputEventType, Class collectionComponentType, List<ExprDotEvalParam> bodiesAndParameters, StreamTypeService streamTypeService, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) {
        EventType evalEventType;
        if (inputEventType == null) {
            evalEventType = ExprDotNodeUtility.makeTransientOAType(enumMethodUsedName, goesToNames.get(1), collectionComponentType, statementRawInfo, services);
        } else {
            evalEventType = inputEventType;
        }

        Class initializationType = bodiesAndParameters.get(0).getBodyForge().getEvaluationType();
        EventType typeResult = ExprDotNodeUtility.makeTransientOAType(enumMethodUsedName, goesToNames.get(0), initializationType, statementRawInfo, services);

        return new EventType[]{typeResult, evalEventType};
    }

    public EnumForge getEnumForge(DotMethodFP footprint, EnumMethodDesc enumMethodEnum, StreamTypeService streamTypeService, String enumMethodUsedName, List<ExprDotEvalParam> bodiesAndParameters, EventType inputEventType, Class collectionComponentType, int numStreamsIncoming, boolean disablePropertyExpressionEventCollCache, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) {
        ExprDotEvalParam initValueParam = bodiesAndParameters.get(0);
        ExprForge initValueEval = initValueParam.getBodyForge();
        super.setTypeInfo(EPTypeHelper.singleValue(JavaClassHelper.getBoxedType(initValueEval.getEvaluationType())));

        ExprDotEvalParamLambda resultAndAdd = (ExprDotEvalParamLambda) bodiesAndParameters.get(1);

        if (inputEventType != null) {
            return new EnumAggregateEventsForge(initValueEval,
                    resultAndAdd.getBodyForge(), resultAndAdd.getStreamCountIncoming(),
                    (ObjectArrayEventType) resultAndAdd.getGoesToTypes()[0]);
        } else {
            return new EnumAggregateScalarForge(initValueEval,
                    resultAndAdd.getBodyForge(), resultAndAdd.getStreamCountIncoming(),
                    (ObjectArrayEventType) resultAndAdd.getGoesToTypes()[0],
                    (ObjectArrayEventType) resultAndAdd.getGoesToTypes()[1]);
        }
    }
}
