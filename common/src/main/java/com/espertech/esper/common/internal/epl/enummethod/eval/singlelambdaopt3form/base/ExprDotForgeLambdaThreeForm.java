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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.enummethod.dot.EnumMethodEnum;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotForgeEnumMethodBase;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprLambdaGoesNode;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeDescFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNodeUtility;
import com.espertech.esper.common.internal.epl.methodbase.DotMethodFP;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.rettype.EPChainableType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class ExprDotForgeLambdaThreeForm extends ExprDotForgeEnumMethodBase {
    protected abstract EPChainableType initAndNoParamsReturnType(EventType inputEventType, EPTypeClass collectionComponentType);

    protected abstract ThreeFormNoParamFactory.ForgeFunction noParamsForge(EnumMethodEnum enumMethod, EPChainableType type, StatementCompileTimeServices services);

    protected abstract ThreeFormInitFunction initAndSingleParamReturnType(EventType inputEventType, EPTypeClass collectionComponentType);

    protected abstract ThreeFormEventPlainFactory.ForgeFunction singleParamEventPlain(EnumMethodEnum enumMethod);

    protected abstract ThreeFormEventPlusFactory.ForgeFunction singleParamEventPlus(EnumMethodEnum enumMethod);

    protected abstract ThreeFormScalarFactory.ForgeFunction singleParamScalar(EnumMethodEnum enumMethod);

    public EnumForgeDescFactory getForgeFactory(DotMethodFP footprint, List<ExprNode> parameters, EnumMethodEnum enumMethod, String enumMethodUsedName, EventType inputEventType, EPTypeClass collectionComponentType, ExprValidationContext validationContext) {
        if (parameters.isEmpty()) {
            EPChainableType type = initAndNoParamsReturnType(inputEventType, collectionComponentType);
            return new ThreeFormNoParamFactory(type, noParamsForge(enumMethod, type, validationContext.getStatementCompileTimeService()));
        }

        ExprLambdaGoesNode goesNode = (ExprLambdaGoesNode) parameters.get(0);
        List<String> goesToNames = goesNode.getGoesToNames();

        if (inputEventType != null) {
            String streamName = goesToNames.get(0);
            if (goesToNames.size() == 1) {
                return new ThreeFormEventPlainFactory(initAndSingleParamReturnType(inputEventType, collectionComponentType), inputEventType, streamName, singleParamEventPlain(enumMethod));
            }

            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put(goesToNames.get(1), EPTypePremade.INTEGERBOXED.getEPType());
            if (goesToNames.size() > 2) {
                fields.put(goesToNames.get(2), EPTypePremade.INTEGERBOXED.getEPType());
            }
            ObjectArrayEventType fieldType = ExprDotNodeUtility.makeTransientOAType(enumMethodUsedName, fields, validationContext.getStatementRawInfo(), validationContext.getStatementCompileTimeService());
            return new ThreeFormEventPlusFactory(initAndSingleParamReturnType(inputEventType, collectionComponentType), inputEventType, streamName, fieldType, goesToNames.size(), singleParamEventPlus(enumMethod));
        }

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put(goesToNames.get(0), collectionComponentType);
        if (goesToNames.size() > 1) {
            fields.put(goesToNames.get(1), EPTypePremade.INTEGERBOXED.getEPType());
        }
        if (goesToNames.size() > 2) {
            fields.put(goesToNames.get(2), EPTypePremade.INTEGERBOXED.getEPType());
        }
        ObjectArrayEventType type = ExprDotNodeUtility.makeTransientOAType(enumMethodUsedName, fields, validationContext.getStatementRawInfo(), validationContext.getStatementCompileTimeService());
        return new ThreeFormScalarFactory(initAndSingleParamReturnType(inputEventType, collectionComponentType), type, goesToNames.size(), singleParamScalar(enumMethod));
    }

    public EPTypeClass validateNonNull(EPType type) throws ExprValidationException {
        if (type == EPTypeNull.INSTANCE) {
            throw new ExprValidationException("Null-type is not allowed");
        }
        return (EPTypeClass) type;
    }
}
