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
import com.espertech.esper.epl.enummethod.dot.EnumMethodEnum;
import com.espertech.esper.epl.enummethod.dot.ExprDotEvalEnumMethodBase;
import com.espertech.esper.epl.enummethod.dot.ExprDotEvalParam;
import com.espertech.esper.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.epl.expression.dot.ExprDotNodeUtility;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.util.JavaClassHelper;

import java.util.List;

public class ExprDotEvalMinMax extends ExprDotEvalEnumMethodBase {

    public EventType[] getAddStreamTypes(String enumMethodUsedName, List<String> goesToNames, EventType inputEventType, Class collectionComponentType, List<ExprDotEvalParam> bodiesAndParameters, EventAdapterService eventAdapterService) {
        return ExprDotNodeUtility.getSingleLambdaParamEventType(enumMethodUsedName, goesToNames, inputEventType, collectionComponentType, eventAdapterService);
    }

    public EnumEval getEnumEval(EngineImportService engineImportService, EventAdapterService eventAdapterService, StreamTypeService streamTypeService, int statementId, String enumMethodUsedName, List<ExprDotEvalParam> bodiesAndParameters, EventType inputEventType, Class collectionComponentType, int numStreamsIncoming, boolean disablePropertyExpressionEventCollCache) {
        boolean max = this.getEnumMethodEnum() == EnumMethodEnum.MAX;

        if (bodiesAndParameters.isEmpty()) {
            Class returnType = JavaClassHelper.getBoxedType(collectionComponentType);
            super.setTypeInfo(EPTypeHelper.singleValue(returnType));
            return new EnumEvalMinMaxScalar(numStreamsIncoming, max);
        }

        ExprDotEvalParamLambda first = (ExprDotEvalParamLambda) bodiesAndParameters.get(0);
        Class returnType = JavaClassHelper.getBoxedType(first.getBodyEvaluator().getType());
        super.setTypeInfo(EPTypeHelper.singleValue(returnType));

        if (inputEventType == null) {
            return new EnumEvalMinMaxScalarLambda(first.getBodyEvaluator(), first.getStreamCountIncoming(), max,
                    (ObjectArrayEventType) first.getGoesToTypes()[0]);
        }
        return new EnumEvalMinMaxEvents(first.getBodyEvaluator(), first.getStreamCountIncoming(), max);
    }
}
