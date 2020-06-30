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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.minmaxby;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.enummethod.dot.EnumMethodEnum;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base.*;
import com.espertech.esper.common.internal.rettype.EPChainableType;
import com.espertech.esper.common.internal.rettype.EPChainableTypeClass;
import com.espertech.esper.common.internal.rettype.EPChainableTypeHelper;

public class ExprDotForgeMinByMaxBy extends ExprDotForgeLambdaThreeForm {

    protected EPChainableType initAndNoParamsReturnType(EventType inputEventType, EPTypeClass collectionComponentType) {
        throw new IllegalStateException();
    }

    protected ThreeFormNoParamFactory.ForgeFunction noParamsForge(EnumMethodEnum enumMethod, EPChainableType type, StatementCompileTimeServices services) {
        throw new IllegalStateException();
    }

    protected ThreeFormInitFunction initAndSingleParamReturnType(EventType inputEventType, EPTypeClass collectionComponentType) {
        return lambda -> {
            validateNonNull(lambda.getBodyForge().getEvaluationType());
            return inputEventType == null ? new EPChainableTypeClass(collectionComponentType) : EPChainableTypeHelper.singleEvent(inputEventType);
        };
    }

    protected ThreeFormEventPlainFactory.ForgeFunction singleParamEventPlain(EnumMethodEnum enumMethod) {
        return (lambda, typeInfo, services) -> new EnumMinMaxByEvents(lambda, enumMethod == EnumMethodEnum.MAXBY);
    }

    protected ThreeFormEventPlusFactory.ForgeFunction singleParamEventPlus(EnumMethodEnum enumMethod) {
        return (lambda, fieldType, numParameters, typeInfo, services) -> new EnumMinMaxByEventsPlus(lambda, fieldType, numParameters, enumMethod == EnumMethodEnum.MAXBY);
    }

    protected ThreeFormScalarFactory.ForgeFunction singleParamScalar(EnumMethodEnum enumMethod) {
        return (lambda, eventType, numParams, typeInfo, services) -> new EnumMinMaxByScalar(lambda, eventType, numParams, enumMethod == EnumMethodEnum.MAXBY, typeInfo);
    }
}
