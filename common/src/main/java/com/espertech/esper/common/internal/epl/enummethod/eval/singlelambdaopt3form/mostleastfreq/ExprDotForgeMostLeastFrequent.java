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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.mostleastfreq;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.enummethod.dot.EnumMethodEnum;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base.*;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.function.Function;

public class ExprDotForgeMostLeastFrequent extends ExprDotForgeLambdaThreeForm {

    protected EPType initAndNoParamsReturnType(EventType inputEventType, Class collectionComponentType) {
        Class returnType = JavaClassHelper.getBoxedType(collectionComponentType);
        return EPTypeHelper.singleValue(returnType);
    }

    protected ThreeFormNoParamFactory.ForgeFunction noParamsForge(EnumMethodEnum enumMethod, EPType type, StatementCompileTimeServices services) {
        return streamCountIncoming -> new EnumMostLeastFrequentScalarNoParam(streamCountIncoming, enumMethod == EnumMethodEnum.MOSTFREQUENT, EPTypeHelper.getNormalizedClass(type));
    }

    protected Function<ExprDotEvalParamLambda, EPType> initAndSingleParamReturnType(EventType inputEventType, Class collectionComponentType) {
        return lambda -> {
            Class returnType = JavaClassHelper.getBoxedType(lambda.getBodyForge().getEvaluationType());
            return  EPTypeHelper.singleValue(returnType);
        };
    }

    protected ThreeFormEventPlainFactory.ForgeFunction singleParamEventPlain(EnumMethodEnum enumMethod) {
        return (lambda, typeInfo1, services) -> new EnumMostLeastFrequentEvent(lambda, enumMethod == EnumMethodEnum.MOSTFREQUENT);
    }

    protected ThreeFormEventPlusFactory.ForgeFunction singleParamEventPlus(EnumMethodEnum enumMethod) {
        return (lambda, fieldType, numParameters, typeInfo1, services) -> new EnumMostLeastFrequentEventPlus(lambda, fieldType, numParameters, enumMethod == EnumMethodEnum.MOSTFREQUENT);
    }

    protected ThreeFormScalarFactory.ForgeFunction singleParamScalar(EnumMethodEnum enumMethod) {
        return (lambda, eventType, numParams, typeInfo, services) ->
            new EnumMostLeastFrequentScalar(lambda, eventType, numParams, enumMethod == EnumMethodEnum.MOSTFREQUENT);
    }
}
