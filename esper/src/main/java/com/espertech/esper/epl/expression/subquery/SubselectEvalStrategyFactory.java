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
package com.espertech.esper.epl.expression.subquery;

import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.CoercionException;
import com.espertech.esper.type.RelationalOpEnum;

import java.util.Collection;
import java.util.Map;

/**
 * Factory for subselect evaluation strategies.
 */
public class SubselectEvalStrategyFactory
{
    /**
     * Create a strategy.
     * @param subselectExpression expression node
     * @param isNot true if negated
     * @param isAll true for ALL
     * @param isAny true for ANY
     * @param relationalOp relational op, if any
     * @return strategy
     * @throws com.espertech.esper.epl.expression.core.ExprValidationException if expression validation fails
     */
    public static SubselectEvalStrategy createStrategy(ExprSubselectNode subselectExpression,
                                                       boolean isNot,
                                                       boolean isAll,
                                                       boolean isAny,
                                                       RelationalOpEnum relationalOp) throws ExprValidationException
    {
        if (subselectExpression.getChildNodes().length != 1)
        {
            throw new ExprValidationException("The Subselect-IN requires 1 child expression");
        }
        ExprNode valueExpr = subselectExpression.getChildNodes()[0];

        // Must be the same boxed type returned by expressions under this
        Class typeOne = JavaClassHelper.getBoxedType(subselectExpression.getChildNodes()[0].getExprEvaluator().getType());

        // collections, array or map not supported
        if ((typeOne.isArray()) || (JavaClassHelper.isImplementsInterface(typeOne, Collection.class)) || (JavaClassHelper.isImplementsInterface(typeOne, Map.class)))
        {
            throw new ExprValidationException("Collection or array comparison is not allowed for the IN, ANY, SOME or ALL keywords");
        }

        Class typeTwo;
        if (subselectExpression.getSelectClause() != null)
        {
            typeTwo = subselectExpression.getSelectClause()[0].getExprEvaluator().getType();
        }
        else
        {
            typeTwo = subselectExpression.getRawEventType().getUnderlyingType();
        }

        if (relationalOp != null)
        {
            if ((typeOne != String.class) || (typeTwo != String.class))
            {
                if (!JavaClassHelper.isNumeric(typeOne))
                {
                    throw new ExprValidationException("Implicit conversion from datatype '" +
                            typeOne.getSimpleName() +
                            "' to numeric is not allowed");
                }
                if (!JavaClassHelper.isNumeric(typeTwo))
                {
                    throw new ExprValidationException("Implicit conversion from datatype '" +
                            typeTwo.getSimpleName() +
                            "' to numeric is not allowed");
                }
            }

            Class compareType = JavaClassHelper.getCompareToCoercionType(typeOne, typeTwo);
            RelationalOpEnum.Computer computer = relationalOp.getComputer(compareType, typeOne, typeTwo);

            ExprEvaluator selectClause = subselectExpression.getSelectClause() == null ? null : subselectExpression.getSelectClause()[0].getExprEvaluator();
            ExprEvaluator filterExpr = subselectExpression.getFilterExpr();
            if (isAny)
            {
                return new SubselectEvalStrategyRelOpAny(computer, valueExpr.getExprEvaluator(),selectClause ,filterExpr);
            }
            return new SubselectEvalStrategyRelOpAll(computer, valueExpr.getExprEvaluator(), selectClause, filterExpr);
        }

        // Get the common type such as Bool, String or Double and Long
        Class coercionType;
        boolean mustCoerce;
        try
        {
            coercionType = JavaClassHelper.getCompareToCoercionType(typeOne, typeTwo);
        }
        catch (CoercionException ex)
        {
            throw new ExprValidationException("Implicit conversion from datatype '" +
                    typeTwo.getSimpleName() +
                    "' to '" +
                    typeOne.getSimpleName() +
                    "' is not allowed");
        }

        // Check if we need to coerce
        mustCoerce = false;
        if ((coercionType != JavaClassHelper.getBoxedType(typeOne)) ||
            (coercionType != JavaClassHelper.getBoxedType(typeTwo)))
        {
            if (JavaClassHelper.isNumeric(coercionType))
            {
                mustCoerce = true;
            }
        }

        ExprEvaluator value = valueExpr.getExprEvaluator();
        ExprEvaluator select = subselectExpression.getSelectClause() == null ? null : subselectExpression.getSelectClause()[0].getExprEvaluator();
        ExprEvaluator filter = subselectExpression.getFilterExpr() == null ? null : subselectExpression.getFilterExpr();
        if (isAll)
        {
            return new SubselectEvalStrategyEqualsAll(isNot, mustCoerce, coercionType, value, select, filter);
        }
        else if (isAny)
        {
            return new SubselectEvalStrategyEqualsAny(isNot, mustCoerce, coercionType, value, select, filter);
        }
        else
        {
            return new SubselectEvalStrategyEqualsIn(isNot, mustCoerce, coercionType, value, select, filter);
        }
    }
}