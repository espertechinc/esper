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
import com.espertech.esper.epl.expression.dot.ExprDotNodeUtility;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.arr.ObjectArrayEventType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public class ExprDotEvalAverage extends ExprDotEvalEnumMethodBase {

    public EventType[] getAddStreamTypes(String enumMethodUsedName, List<String> goesToNames, EventType inputEventType, Class collectionComponentType, List<ExprDotEvalParam> bodiesAndParameters, EventAdapterService eventAdapterService) {
        return ExprDotNodeUtility.getSingleLambdaParamEventType(enumMethodUsedName, goesToNames, inputEventType, collectionComponentType, eventAdapterService);
    }

    public EnumEval getEnumEval(EngineImportService engineImportService, EventAdapterService eventAdapterService, StreamTypeService streamTypeService, int statementId, String enumMethodUsedName, List<ExprDotEvalParam> bodiesAndParameters, EventType inputEventType, Class collectionComponentType, int numStreamsIncoming, boolean disablePropertyExpressionEventCollCache) {

        if (bodiesAndParameters.isEmpty()) {
            if (collectionComponentType == BigDecimal.class || collectionComponentType == BigInteger.class) {
                super.setTypeInfo(EPTypeHelper.singleValue(BigDecimal.class));
                return new EnumEvalAverageBigDecimalScalar(numStreamsIncoming, engineImportService.getDefaultMathContext());
            }
            super.setTypeInfo(EPTypeHelper.singleValue(Double.class));
            return new EnumEvalAverageScalar(numStreamsIncoming);
        }

        ExprDotEvalParamLambda first = (ExprDotEvalParamLambda) bodiesAndParameters.get(0);
        Class returnType = first.getBodyEvaluator().getType();

        if (returnType == BigDecimal.class || returnType == BigInteger.class) {
            super.setTypeInfo(EPTypeHelper.singleValue(BigDecimal.class));
            if (inputEventType == null) {
                return new EnumEvalAverageBigDecimalScalarLambda(first.getBodyEvaluator(), first.getStreamCountIncoming(),
                        (ObjectArrayEventType) first.getGoesToTypes()[0], engineImportService.getDefaultMathContext());
            }
            return new EnumEvalAverageBigDecimalEvents(first.getBodyEvaluator(), first.getStreamCountIncoming(), engineImportService.getDefaultMathContext());
        }
        super.setTypeInfo(EPTypeHelper.singleValue(Double.class));
        if (inputEventType == null) {
            return new EnumEvalAverageScalarLambda(first.getBodyEvaluator(), first.getStreamCountIncoming(),
                    (ObjectArrayEventType) first.getGoesToTypes()[0]);
        }
        return new EnumEvalAverageEvents(first.getBodyEvaluator(), first.getStreamCountIncoming());
    }
}
