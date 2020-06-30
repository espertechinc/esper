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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.groupby;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.enummethod.dot.EnumMethodEnum;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base.*;
import com.espertech.esper.common.internal.rettype.EPChainableType;
import com.espertech.esper.common.internal.rettype.EPChainableTypeClass;

import java.util.Collection;
import java.util.Map;

import static com.espertech.esper.common.internal.util.JavaClassHelper.getTypeClassOrObjectType;

public class ExprDotForgeGroupByOneParam extends ExprDotForgeLambdaThreeForm {

    protected EPChainableType initAndNoParamsReturnType(EventType inputEventType, EPTypeClass collectionComponentType) {
        throw new IllegalStateException();
    }

    protected ThreeFormNoParamFactory.ForgeFunction noParamsForge(EnumMethodEnum enumMethod, EPChainableType type, StatementCompileTimeServices services) {
        throw new IllegalStateException();
    }

    protected ThreeFormInitFunction initAndSingleParamReturnType(EventType inputEventType, EPTypeClass collectionComponentType) {
        return lambda -> {
            EPTypeClass key = getTypeClassOrObjectType(lambda.getBodyForge().getEvaluationType());
            EPTypeClass component;
            if (collectionComponentType != null) {
                component = collectionComponentType;
            } else {
                component = inputEventType.getUnderlyingEPType();
            }
            EPTypeClass value = new EPTypeClassParameterized(Collection.class, new EPTypeClass[] {component});
            EPTypeClass map = new EPTypeClassParameterized(Map.class, new EPTypeClass[] {key, value});
            return new EPChainableTypeClass(map);
        };
    }

    protected ThreeFormEventPlainFactory.ForgeFunction singleParamEventPlain(EnumMethodEnum enumMethod) {
        return (lambda, typeInfo, services) -> new EnumGroupByOneParamEvent(lambda);
    }

    protected ThreeFormEventPlusFactory.ForgeFunction singleParamEventPlus(EnumMethodEnum enumMethod) {
        return (lambda, indexEventType, numParameters, typeInfo, services) -> new EnumGroupByOneParamEventPlus(lambda, indexEventType, numParameters);
    }

    protected ThreeFormScalarFactory.ForgeFunction singleParamScalar(EnumMethodEnum enumMethod) {
        return (lambda, eventType, numParams, typeInfo, services) -> new EnumGroupByOneParamScalar(lambda, eventType, numParams);
    }
}
