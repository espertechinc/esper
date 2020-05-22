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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.average;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.enummethod.dot.EnumMethodEnum;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base.*;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Function;

public class ExprDotForgeAverage extends ExprDotForgeLambdaThreeForm {

    protected EPType initAndNoParamsReturnType(EventType inputEventType, Class collectionComponentType) {
        if (collectionComponentType == BigDecimal.class || collectionComponentType == BigInteger.class) {
            return EPTypeHelper.singleValue(BigDecimal.class);
        }
        return EPTypeHelper.singleValue(Double.class);
    }

    protected ThreeFormNoParamFactory.ForgeFunction noParamsForge(EnumMethodEnum enumMethod, EPType type, StatementCompileTimeServices services) {
        if (EPTypeHelper.getNormalizedClass(type) == Double.class) {
            return streamCountIncoming -> new EnumAverageScalarNoParam(streamCountIncoming);
        }
        return streamCountIncoming -> new EnumAverageBigDecimalScalarNoParam(streamCountIncoming, services.getClasspathImportServiceCompileTime().getDefaultMathContext());
    }

    protected Function<ExprDotEvalParamLambda, EPType> initAndSingleParamReturnType(EventType inputEventType, Class collectionComponentType) {
        return lambda -> {
            Class returnType = lambda.getBodyForge().getEvaluationType();
            if (returnType == BigDecimal.class || returnType == BigInteger.class) {
                return EPTypeHelper.singleValue(BigDecimal.class);
            }
            return EPTypeHelper.singleValue(Double.class);
        };

    }

    protected ThreeFormEventPlainFactory.ForgeFunction singleParamEventPlain(EnumMethodEnum enumMethod) {
        return (lambda, typeInfo, services) -> {
            if (EPTypeHelper.getNormalizedClass(typeInfo) == Double.class) {
                return new EnumAverageEvent(lambda);
            }
            return new EnumAverageBigDecimalEvent(lambda, services.getClasspathImportServiceCompileTime().getDefaultMathContext());
        };
    }

    protected ThreeFormEventPlusFactory.ForgeFunction singleParamEventPlus(EnumMethodEnum enumMethod) {
        return (lambda, fieldType, numParameters, typeInfo, services) -> {
            if (EPTypeHelper.getNormalizedClass(typeInfo) == Double.class) {
                return new EnumAverageEventPlus(lambda, fieldType, numParameters);
            }
            return new EnumAverageBigDecimalEventPlus(lambda, fieldType, numParameters, services.getClasspathImportServiceCompileTime().getDefaultMathContext());
        };
    }

    protected ThreeFormScalarFactory.ForgeFunction singleParamScalar(EnumMethodEnum enumMethod) {
        return (lambda, fieldType, numParams, typeInfo, services) -> {
            if (EPTypeHelper.getNormalizedClass(typeInfo) == Double.class) {
                return new EnumAverageScalar(lambda, fieldType, numParams);
            }
            return new EnumAverageBigDecimalScalar(lambda, fieldType, numParams, services.getClasspathImportServiceCompileTime().getDefaultMathContext());
        };
    }
}
