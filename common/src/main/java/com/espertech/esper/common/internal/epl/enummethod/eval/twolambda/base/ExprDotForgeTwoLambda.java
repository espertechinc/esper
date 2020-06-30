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
package com.espertech.esper.common.internal.epl.enummethod.eval.twolambda.base;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class ExprDotForgeTwoLambda extends ExprDotForgeEnumMethodBase {

    protected abstract TwoLambdaThreeFormEventPlainFactory.ForgeFunction twoParamEventPlain();
    protected abstract TwoLambdaThreeFormEventPlusFactory.ForgeFunction twoParamEventPlus();
    protected abstract TwoLambdaThreeFormScalarFactory.ForgeFunction twoParamScalar();

    public EnumForgeDescFactory getForgeFactory(DotMethodFP footprint, List<ExprNode> parameters, EnumMethodEnum enumMethod, String enumMethodUsedName, EventType inputEventType, EPTypeClass collectionComponentType, ExprValidationContext validationContext)
        throws ExprValidationException {
        if (parameters.size() < 2) {
            throw new IllegalStateException();
        }

        ExprLambdaGoesNode lambdaFirst = (ExprLambdaGoesNode) parameters.get(0);
        ExprLambdaGoesNode lambdaSecond = (ExprLambdaGoesNode) parameters.get(1);
        if (lambdaFirst.getGoesToNames().size() != lambdaSecond.getGoesToNames().size()) {
            throw new ExprValidationException("Enumeration method '" + enumMethodUsedName + "' expected the same number of parameters for both the key and the value expression");
        }
        int numParameters = lambdaFirst.getGoesToNames().size();

        if (inputEventType != null) {
            String streamNameFirst = lambdaFirst.getGoesToNames().get(0);
            String streamNameSecond = lambdaSecond.getGoesToNames().get(0);
            if (numParameters == 1) {
                return new TwoLambdaThreeFormEventPlainFactory(inputEventType, streamNameFirst, streamNameSecond, twoParamEventPlain());
            }

            Map<String, Object> fieldsFirst = new LinkedHashMap<>();
            Map<String, Object> fieldsSecond = new LinkedHashMap<>();
            fieldsFirst.put(lambdaFirst.getGoesToNames().get(1), EPTypePremade.INTEGERBOXED.getEPType());
            fieldsSecond.put(lambdaSecond.getGoesToNames().get(1), EPTypePremade.INTEGERBOXED.getEPType());
            if (numParameters > 2) {
                fieldsFirst.put(lambdaFirst.getGoesToNames().get(2), EPTypePremade.INTEGERBOXED.getEPType());
                fieldsSecond.put(lambdaSecond.getGoesToNames().get(2), EPTypePremade.INTEGERBOXED.getEPType());
            }
            ObjectArrayEventType typeFirst = ExprDotNodeUtility.makeTransientOAType(enumMethodUsedName, fieldsFirst, validationContext.getStatementRawInfo(), validationContext.getStatementCompileTimeService());
            ObjectArrayEventType typeSecond = ExprDotNodeUtility.makeTransientOAType(enumMethodUsedName, fieldsSecond, validationContext.getStatementRawInfo(), validationContext.getStatementCompileTimeService());
            return new TwoLambdaThreeFormEventPlusFactory(inputEventType, streamNameFirst, streamNameSecond, typeFirst, typeSecond, lambdaFirst.getGoesToNames().size(), twoParamEventPlus());
        }

        Map<String, Object> fieldsFirst = new LinkedHashMap<>();
        Map<String, Object> fieldsSecond = new LinkedHashMap<>();
        fieldsFirst.put(lambdaFirst.getGoesToNames().get(0), collectionComponentType);
        fieldsSecond.put(lambdaSecond.getGoesToNames().get(0), collectionComponentType);
        if (numParameters > 1) {
            fieldsFirst.put(lambdaFirst.getGoesToNames().get(1), EPTypePremade.INTEGERBOXED.getEPType());
            fieldsSecond.put(lambdaSecond.getGoesToNames().get(1), EPTypePremade.INTEGERBOXED.getEPType());
        }
        if (numParameters > 2) {
            fieldsFirst.put(lambdaFirst.getGoesToNames().get(2), EPTypePremade.INTEGERBOXED.getEPType());
            fieldsSecond.put(lambdaSecond.getGoesToNames().get(2), EPTypePremade.INTEGERBOXED.getEPType());
        }
        ObjectArrayEventType typeFirst = ExprDotNodeUtility.makeTransientOAType(enumMethodUsedName, fieldsFirst, validationContext.getStatementRawInfo(), validationContext.getStatementCompileTimeService());
        ObjectArrayEventType typeSecond = ExprDotNodeUtility.makeTransientOAType(enumMethodUsedName, fieldsSecond, validationContext.getStatementRawInfo(), validationContext.getStatementCompileTimeService());
        return new TwoLambdaThreeFormScalarFactory(typeFirst, typeSecond, lambdaFirst.getGoesToNames().size(), twoParamScalar());
    }
}
