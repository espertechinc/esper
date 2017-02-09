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

import java.util.List;

public class ExprDotEvalAllOfAnyOf extends ExprDotEvalEnumMethodBase {

    public EventType[] getAddStreamTypes(String enumMethodUsedName, List<String> goesToNames, EventType inputEventType, Class collectionComponentType, List<ExprDotEvalParam> bodiesAndParameters, EventAdapterService eventAdapterService) {
        return ExprDotNodeUtility.getSingleLambdaParamEventType(enumMethodUsedName, goesToNames, inputEventType, collectionComponentType, eventAdapterService);
    }

    public EnumEval getEnumEval(EngineImportService engineImportService, EventAdapterService eventAdapterService, StreamTypeService streamTypeService, int statementId, String enumMethodUsedName, List<ExprDotEvalParam> bodiesAndParameters, EventType inputEventType, Class collectionComponentType, int numStreamsIncoming, boolean disablePropertyExpressionEventCollCache) {
        ExprDotEvalParamLambda first = (ExprDotEvalParamLambda) bodiesAndParameters.get(0);

        super.setTypeInfo(EPTypeHelper.singleValue(Boolean.class));
        if (inputEventType != null) {
            if (super.getEnumMethodEnum() == EnumMethodEnum.ALLOF) {
                return new EnumEvalAllOfEvents(first.getBodyEvaluator(), first.getStreamCountIncoming());
            }
            return new EnumEvalAnyOfEvents(first.getBodyEvaluator(), first.getStreamCountIncoming());
        }

        if (super.getEnumMethodEnum() == EnumMethodEnum.ALLOF) {
            return new EnumEvalAllOfScalar(first.getBodyEvaluator(), first.getStreamCountIncoming(), (ObjectArrayEventType) first.getGoesToTypes()[0]);
        }
        return new EnumEvalAnyOfScalar(first.getBodyEvaluator(), first.getStreamCountIncoming(), (ObjectArrayEventType) first.getGoesToTypes()[0]);
    }
}
