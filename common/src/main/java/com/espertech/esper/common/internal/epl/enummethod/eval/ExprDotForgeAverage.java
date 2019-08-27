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
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNodeUtility;
import com.espertech.esper.common.internal.epl.methodbase.DotMethodFP;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public class ExprDotForgeAverage extends ExprDotForgeEnumMethodBase {

    public EventType[] getAddStreamTypes(DotMethodFP footprint, int parameterNum, EnumMethodEnum enumMethod, String enumMethodUsedName, List<String> goesToNames, EventType inputEventType, Class collectionComponentType, List<ExprDotEvalParam> bodiesAndParameters, StreamTypeService streamTypeService, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) {
        return ExprDotNodeUtility.getSingleLambdaParamEventType(enumMethodUsedName, goesToNames, inputEventType, collectionComponentType, statementRawInfo, services);
    }

    public EnumForge getEnumForge(DotMethodFP footprint, EnumMethodDesc enumMethodEnum, StreamTypeService streamTypeService, String enumMethodUsedName, List<ExprDotEvalParam> bodiesAndParameters, EventType inputEventType, Class collectionComponentType, int numStreamsIncoming, boolean disablePropertyExpressionEventCollCache, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) {

        if (bodiesAndParameters.isEmpty()) {
            if (collectionComponentType == BigDecimal.class || collectionComponentType == BigInteger.class) {
                super.setTypeInfo(EPTypeHelper.singleValue(BigDecimal.class));
                return new EnumAverageBigDecimalScalarForge(numStreamsIncoming, services.getClasspathImportServiceCompileTime().getDefaultMathContext());
            }
            super.setTypeInfo(EPTypeHelper.singleValue(Double.class));
            return new EnumAverageScalarForge(numStreamsIncoming);
        }

        ExprDotEvalParamLambda first = (ExprDotEvalParamLambda) bodiesAndParameters.get(0);
        Class returnType = first.getBodyForge().getEvaluationType();

        if (returnType == BigDecimal.class || returnType == BigInteger.class) {
            super.setTypeInfo(EPTypeHelper.singleValue(BigDecimal.class));
            if (inputEventType == null) {
                return new EnumAverageBigDecimalScalarLambdaForge(first.getBodyForge(), first.getStreamCountIncoming(),
                        (ObjectArrayEventType) first.getGoesToTypes()[0], services.getClasspathImportServiceCompileTime().getDefaultMathContext());
            }
            return new EnumAverageBigDecimalEventsForge(first.getBodyForge(), first.getStreamCountIncoming(), services.getClasspathImportServiceCompileTime().getDefaultMathContext());
        }
        super.setTypeInfo(EPTypeHelper.singleValue(Double.class));
        if (inputEventType == null) {
            return new EnumAverageScalarLambdaForge(first.getBodyForge(), first.getStreamCountIncoming(),
                    (ObjectArrayEventType) first.getGoesToTypes()[0]);
        }
        return new EnumAverageEventsForge(first.getBodyForge(), first.getStreamCountIncoming());
    }
}
