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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.firstoflastof;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.enummethod.dot.EnumMethodEnum;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base.*;
import com.espertech.esper.common.internal.rettype.EPChainableType;
import com.espertech.esper.common.internal.rettype.EPChainableTypeClass;
import com.espertech.esper.common.internal.rettype.EPChainableTypeHelper;

public class ExprDotForgeFirstLastOf extends ExprDotForgeLambdaThreeForm {

    protected EPChainableType initAndNoParamsReturnType(EventType inputEventType, EPTypeClass collectionComponentType) {
        if (inputEventType != null) {
            return EPChainableTypeHelper.singleEvent(inputEventType);
        }
        return new EPChainableTypeClass(collectionComponentType);
    }

    protected ThreeFormNoParamFactory.ForgeFunction noParamsForge(EnumMethodEnum enumMethod, EPChainableType type, StatementCompileTimeServices services) {
        if (enumMethod == EnumMethodEnum.FIRSTOF) {
            return streamCountIncoming -> new EnumFirstOf(streamCountIncoming, type);
        } else {
            return streamCountIncoming -> new EnumLastOf(streamCountIncoming, type);
        }
    }

    protected ThreeFormInitFunction initAndSingleParamReturnType(EventType inputEventType, EPTypeClass collectionComponentType) {
        return lambda -> initAndNoParamsReturnType(inputEventType, collectionComponentType);
    }

    protected ThreeFormEventPlainFactory.ForgeFunction singleParamEventPlain(EnumMethodEnum enumMethod) {
        return (lambda, typeInfo, services) -> {
            if (enumMethod == EnumMethodEnum.FIRSTOF) {
                return new EnumFirstOfEvent(lambda);
            } else {
                return new EnumLastOfEvent(lambda);
            }
        };
    }

    protected ThreeFormEventPlusFactory.ForgeFunction singleParamEventPlus(EnumMethodEnum enumMethod) {
        return (lambda, fieldType, numParameters, typeInfo, services) -> {
            if (enumMethod == EnumMethodEnum.FIRSTOF) {
                return new EnumFirstOfEventPlus(lambda, fieldType, numParameters);
            } else {
                return new EnumLastOfEventPlus(lambda, fieldType, numParameters);
            }
        };
    }

    protected ThreeFormScalarFactory.ForgeFunction singleParamScalar(EnumMethodEnum enumMethod) {
        return (lambda, eventType, numParams, typeInfo, services) -> {
            if (enumMethod == EnumMethodEnum.FIRSTOF) {
                return new EnumFirstOfScalar(lambda, eventType, numParams, typeInfo);
            } else {
                return new EnumLastOfScalar(lambda, eventType, numParams, typeInfo);
            }
        };
    }
}
