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
package com.espertech.esper.common.internal.epl.enummethod.eval.twolambda.tomap;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForge;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeDesc;
import com.espertech.esper.common.internal.epl.enummethod.eval.twolambda.base.ExprDotForgeTwoLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.twolambda.base.TwoLambdaThreeFormEventPlainFactory;
import com.espertech.esper.common.internal.epl.enummethod.eval.twolambda.base.TwoLambdaThreeFormEventPlusFactory;
import com.espertech.esper.common.internal.epl.enummethod.eval.twolambda.base.TwoLambdaThreeFormScalarFactory;
import com.espertech.esper.common.internal.rettype.EPChainableType;
import com.espertech.esper.common.internal.rettype.EPChainableTypeClass;

import java.util.Map;

import static com.espertech.esper.common.internal.util.JavaClassHelper.getTypeClassOrObjectType;

public class ExprDotForgeToMap extends ExprDotForgeTwoLambda {

    protected TwoLambdaThreeFormEventPlainFactory.ForgeFunction twoParamEventPlain() {
        return (first, second, streamCountIncoming, services) -> buildDesc(first, second, new EnumToMapEvent(first.getBodyForge(), streamCountIncoming, second.getBodyForge()));
    }

    protected TwoLambdaThreeFormEventPlusFactory.ForgeFunction twoParamEventPlus() {
        return (first, second, streamCountIncoming, firstType, secondType, numParameters, services) -> buildDesc(first, second, new EnumToMapEventPlus(first.getBodyForge(), streamCountIncoming, firstType,
            second.getBodyForge(), numParameters));
    }

    protected TwoLambdaThreeFormScalarFactory.ForgeFunction twoParamScalar() {
        return (first, second, eventTypeFirst, eventTypeSecond, streamCountIncoming, numParams) -> buildDesc(first, second,
            new EnumToMapScalar(first.getBodyForge(), streamCountIncoming, second.getBodyForge(), eventTypeFirst, numParams));
    }

    private EnumForgeDesc buildDesc(ExprDotEvalParamLambda first, ExprDotEvalParamLambda second, EnumForge forge) {
        EPTypeClass key = getTypeClassOrObjectType(first.getBodyForge().getEvaluationType());
        EPTypeClass value = getTypeClassOrObjectType(second.getBodyForge().getEvaluationType());
        EPTypeClass map = new EPTypeClassParameterized(Map.class, new EPTypeClass[] {key, value});
        EPChainableType type = new EPChainableTypeClass(map);
        return new EnumForgeDesc(type, forge);
    }
}
