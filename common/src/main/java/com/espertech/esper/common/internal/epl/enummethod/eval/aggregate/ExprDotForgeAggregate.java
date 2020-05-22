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
package com.espertech.esper.common.internal.epl.enummethod.eval.aggregate;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.enummethod.dot.*;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeDesc;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeDescFactory;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeLambdaDesc;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNodeUtility;
import com.espertech.esper.common.internal.epl.methodbase.DotMethodFP;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExprDotForgeAggregate extends ExprDotForgeEnumMethodBase {

    public EnumForgeDescFactory getForgeFactory(DotMethodFP footprint, List<ExprNode> parameters, EnumMethodEnum enumMethod, String enumMethodUsedName, EventType inputEventType, Class collectionComponentType, ExprValidationContext validationContext) {
        ExprLambdaGoesNode goesNode = (ExprLambdaGoesNode) parameters.get(1);
        int numParameters = goesNode.getGoesToNames().size();
        String firstName = goesNode.getGoesToNames().get(0);
        String secondName = goesNode.getGoesToNames().get(1);

        Map<String, Object> fields = new LinkedHashMap<>();
        Class initializationType = parameters.get(0).getForge().getEvaluationType();
        fields.put(firstName, initializationType);
        if (inputEventType == null) {
            fields.put(secondName, collectionComponentType);
        }
        if (numParameters > 2) {
            fields.put(goesNode.getGoesToNames().get(2), int.class);
            if (numParameters > 3) {
                fields.put(goesNode.getGoesToNames().get(3), int.class);
            }
        }

        ObjectArrayEventType evalEventType = ExprDotNodeUtility.makeTransientOAType(enumMethodUsedName, fields, validationContext.getStatementRawInfo(), validationContext.getStatementCompileTimeService());
        if (inputEventType == null) {
            return new EnumForgeDescFactoryAggregateScalar(evalEventType);
        }
        return new EnumForgeDescFactoryAggregateEvent(evalEventType, inputEventType, secondName, numParameters);
    }

    private static class EnumForgeDescFactoryAggregateScalar implements EnumForgeDescFactory {
        private final ObjectArrayEventType evalEventType;

        public EnumForgeDescFactoryAggregateScalar(ObjectArrayEventType evalEventType) {
            this.evalEventType = evalEventType;
        }

        public EnumForgeLambdaDesc getLambdaStreamTypesForParameter(int parameterNum) {
            return new EnumForgeLambdaDesc(new EventType[]{evalEventType}, new String[] {evalEventType.getName()});
        }

        public EnumForgeDesc makeEnumForgeDesc(List<ExprDotEvalParam> bodiesAndParameters, int streamCountIncoming, StatementCompileTimeServices services) {
            ExprForge init = bodiesAndParameters.get(0).getBodyForge();
            ExprDotEvalParamLambda compute = (ExprDotEvalParamLambda) bodiesAndParameters.get(1);
            EnumAggregateScalar forge = new EnumAggregateScalar(streamCountIncoming, init, compute.getBodyForge(), evalEventType, compute.getGoesToNames().size());
            EPType type = EPTypeHelper.singleValue(JavaClassHelper.getBoxedType(init.getEvaluationType()));
            return new EnumForgeDesc(type, forge);
        }
    }

    private static class EnumForgeDescFactoryAggregateEvent implements EnumForgeDescFactory {
        private final ObjectArrayEventType evalEventType;
        private final EventType inputEventType;
        private final String streamName;
        private final int numParameters;

        public EnumForgeDescFactoryAggregateEvent(ObjectArrayEventType evalEventType, EventType inputEventType, String streamName, int numParameters) {
            this.evalEventType = evalEventType;
            this.inputEventType = inputEventType;
            this.streamName = streamName;
            this.numParameters = numParameters;
        }

        public EnumForgeLambdaDesc getLambdaStreamTypesForParameter(int parameterNum) {
            return new EnumForgeLambdaDesc(new EventType[]{evalEventType, inputEventType}, new String[] {evalEventType.getName(), streamName});
        }

        public EnumForgeDesc makeEnumForgeDesc(List<ExprDotEvalParam> bodiesAndParameters, int streamCountIncoming, StatementCompileTimeServices services) {
            ExprForge init = bodiesAndParameters.get(0).getBodyForge();
            ExprDotEvalParamLambda compute = (ExprDotEvalParamLambda) bodiesAndParameters.get(1);
            EnumAggregateEvent forge = new EnumAggregateEvent(streamCountIncoming, init, compute.getBodyForge(), evalEventType, numParameters);
            EPType type = EPTypeHelper.singleValue(JavaClassHelper.getBoxedType(init.getEvaluationType()));
            return new EnumForgeDesc(type, forge);
        }
    }
}
