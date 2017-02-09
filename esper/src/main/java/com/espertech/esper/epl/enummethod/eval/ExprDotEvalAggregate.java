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
package com.espertech.esper.epl.enummethod.eval;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.core.StreamTypeService;
import com.espertech.esper.epl.enummethod.dot.ExprDotEvalEnumMethodBase;
import com.espertech.esper.epl.enummethod.dot.ExprDotEvalParam;
import com.espertech.esper.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.dot.ExprDotNodeUtility;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.util.JavaClassHelper;

import java.util.List;

public class ExprDotEvalAggregate extends ExprDotEvalEnumMethodBase {

    public EventType[] getAddStreamTypes(String enumMethodUsedName, List<String> goesToNames, EventType inputEventType, Class collectionComponentType, List<ExprDotEvalParam> bodiesAndParameters, EventAdapterService eventAdapterService) {
        EventType evalEventType;
        if (inputEventType == null) {
            evalEventType = ExprDotNodeUtility.makeTransientOAType(enumMethodUsedName, goesToNames.get(1), collectionComponentType, eventAdapterService);
        } else {
            evalEventType = inputEventType;
        }

        Class initializationType = bodiesAndParameters.get(0).getBodyEvaluator().getType();
        EventType typeResult = ExprDotNodeUtility.makeTransientOAType(enumMethodUsedName, goesToNames.get(0), initializationType, eventAdapterService);

        return new EventType[]{typeResult, evalEventType};
    }

    public EnumEval getEnumEval(EngineImportService engineImportService, EventAdapterService eventAdapterService, StreamTypeService streamTypeService, int statementId, String enumMethodUsedName, List<ExprDotEvalParam> bodiesAndParameters, EventType inputEventType, Class collectionComponentType, int numStreamsIncoming, boolean disablePropertyExpressionEventCollCache) {
        ExprDotEvalParam initValueParam = bodiesAndParameters.get(0);
        ExprEvaluator initValueEval = initValueParam.getBodyEvaluator();
        super.setTypeInfo(EPTypeHelper.singleValue(JavaClassHelper.getBoxedType(initValueEval.getType())));

        ExprDotEvalParamLambda resultAndAdd = (ExprDotEvalParamLambda) bodiesAndParameters.get(1);

        if (inputEventType != null) {
            return new EnumEvalAggregateEvents(initValueEval,
                    resultAndAdd.getBodyEvaluator(), resultAndAdd.getStreamCountIncoming(),
                    (ObjectArrayEventType) resultAndAdd.getGoesToTypes()[0]);
        } else {
            return new EnumEvalAggregateScalar(initValueEval,
                    resultAndAdd.getBodyEvaluator(), resultAndAdd.getStreamCountIncoming(),
                    (ObjectArrayEventType) resultAndAdd.getGoesToTypes()[0],
                    (ObjectArrayEventType) resultAndAdd.getGoesToTypes()[1]);
        }
    }
}
