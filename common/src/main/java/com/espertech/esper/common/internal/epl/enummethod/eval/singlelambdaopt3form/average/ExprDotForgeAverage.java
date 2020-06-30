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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.enummethod.dot.EnumMethodEnum;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base.*;
import com.espertech.esper.common.internal.rettype.EPChainableType;
import com.espertech.esper.common.internal.rettype.EPChainableTypeHelper;

import java.math.BigDecimal;
import java.math.BigInteger;

public class ExprDotForgeAverage extends ExprDotForgeLambdaThreeForm {

    protected EPChainableType initAndNoParamsReturnType(EventType inputEventType, EPTypeClass collectionComponentType) {
        if (collectionComponentType.getType() == BigDecimal.class || collectionComponentType.getType() == BigInteger.class) {
            return EPChainableTypeHelper.singleValue(EPTypePremade.BIGDECIMAL.getEPType());
        }
        return EPChainableTypeHelper.singleValue(EPTypePremade.DOUBLEBOXED.getEPType());
    }

    protected ThreeFormNoParamFactory.ForgeFunction noParamsForge(EnumMethodEnum enumMethod, EPChainableType type, StatementCompileTimeServices services) {
        if (EPChainableTypeHelper.getNormalizedEPType(type).equals(EPTypePremade.DOUBLEBOXED.getEPType())) {
            return streamCountIncoming -> new EnumAverageScalarNoParam(streamCountIncoming);
        }
        return streamCountIncoming -> new EnumAverageBigDecimalScalarNoParam(streamCountIncoming, services.getClasspathImportServiceCompileTime().getDefaultMathContext());
    }

    protected ThreeFormInitFunction initAndSingleParamReturnType(EventType inputEventType, EPTypeClass collectionComponentType) {
        return lambda -> {
            EPTypeClass returnType = (EPTypeClass) lambda.getBodyForge().getEvaluationType();
            if (returnType.getType() == BigDecimal.class || returnType.getType() == BigInteger.class) {
                return EPChainableTypeHelper.singleValue(EPTypePremade.BIGDECIMAL.getEPType());
            }
            return EPChainableTypeHelper.singleValue(EPTypePremade.DOUBLEBOXED.getEPType());
        };
    }

    protected ThreeFormEventPlainFactory.ForgeFunction singleParamEventPlain(EnumMethodEnum enumMethod) {
        return (lambda, typeInfo, services) -> {
            if (EPChainableTypeHelper.getNormalizedEPType(typeInfo).equals(EPTypePremade.DOUBLEBOXED.getEPType())) {
                return new EnumAverageEvent(lambda);
            }
            return new EnumAverageBigDecimalEvent(lambda, services.getClasspathImportServiceCompileTime().getDefaultMathContext());
        };
    }

    protected ThreeFormEventPlusFactory.ForgeFunction singleParamEventPlus(EnumMethodEnum enumMethod) {
        return (lambda, fieldType, numParameters, typeInfo, services) -> {
            if (EPChainableTypeHelper.getNormalizedEPType(typeInfo).equals(EPTypePremade.DOUBLEBOXED.getEPType())) {
                return new EnumAverageEventPlus(lambda, fieldType, numParameters);
            }
            return new EnumAverageBigDecimalEventPlus(lambda, fieldType, numParameters, services.getClasspathImportServiceCompileTime().getDefaultMathContext());
        };
    }

    protected ThreeFormScalarFactory.ForgeFunction singleParamScalar(EnumMethodEnum enumMethod) {
        return (lambda, fieldType, numParams, typeInfo, services) -> {
            if (EPChainableTypeHelper.getNormalizedEPType(typeInfo).equals(EPTypePremade.DOUBLEBOXED.getEPType())) {
                return new EnumAverageScalar(lambda, fieldType, numParams);
            }
            return new EnumAverageBigDecimalScalar(lambda, fieldType, numParams, services.getClasspathImportServiceCompileTime().getDefaultMathContext());
        };
    }
}
