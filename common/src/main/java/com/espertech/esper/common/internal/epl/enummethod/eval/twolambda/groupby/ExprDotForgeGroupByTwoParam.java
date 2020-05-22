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
package com.espertech.esper.common.internal.epl.enummethod.eval.twolambda.groupby;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.enummethod.eval.twolambda.base.ExprDotForgeTwoLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.twolambda.base.TwoLambdaThreeFormEventPlainFactory;
import com.espertech.esper.common.internal.epl.enummethod.eval.twolambda.base.TwoLambdaThreeFormEventPlusFactory;
import com.espertech.esper.common.internal.epl.enummethod.eval.twolambda.base.TwoLambdaThreeFormScalarFactory;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;

import java.util.Map;

public class ExprDotForgeGroupByTwoParam extends ExprDotForgeTwoLambda {

    protected EPType returnType(EventType inputEventType, Class collectionComponentType) {
        return EPTypeHelper.singleValue(Map.class);
    }

    protected TwoLambdaThreeFormEventPlainFactory.ForgeFunction twoParamEventPlain() {
        return (first, second, streamCountIncoming, typeInfo, services) -> new EnumGroupByTwoParamEventPlain(first.getBodyForge(), streamCountIncoming, second.getBodyForge());
    }

    protected TwoLambdaThreeFormEventPlusFactory.ForgeFunction twoParamEventPlus() {
        return (first, second, streamCountIncoming, firstType, secondType, numParameters, typeInfo, services) -> new EnumGroupByTwoParamEventPlus(first.getBodyForge(), streamCountIncoming, firstType,
            second.getBodyForge(), numParameters);
    }

    protected TwoLambdaThreeFormScalarFactory.ForgeFunction twoParamScalar() {
        return (first, second, eventTypeFirst, eventTypeSecond, streamCountIncoming, numParams, typeInfo) ->
            new EnumGroupByTwoParamScalar(first.getBodyForge(), streamCountIncoming, second.getBodyForge(), eventTypeFirst, numParams);
    }
}
