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

import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.GroupByClauseExpressions;
import com.espertech.esper.type.RelationalOpEnum;
import com.espertech.esper.util.CoercionException;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.SimpleNumberCoercer;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import java.util.Collection;
import java.util.Map;

/**
 * Factory for subselect evaluation strategies.
 */
public class SubselectEvalStrategyNRFactory {

    public static SubselectEvalStrategyNR createStrategyExists(ExprSubselectExistsNode subselectExpression) {
        boolean aggregated = aggregated(subselectExpression.getSubselectAggregationType());
        boolean grouped = grouped(subselectExpression.getStatementSpecCompiled().getGroupByExpressions());
        if (grouped) {
            if (subselectExpression.havingExpr != null) {
                return new SubselectEvalStrategyNRExistsWGroupByWHaving(subselectExpression.havingExpr);
            }
            return SubselectEvalStrategyNRExistsWGroupBy.INSTANCE;
        }
        if (aggregated) {
            if (subselectExpression.havingExpr != null) {
                return new SubselectEvalStrategyNRExistsAggregated(subselectExpression.havingExpr);
            }
            return SubselectEvalStrategyNRExistsAlwaysTrue.INSTANCE;
        }
        return new SubselectEvalStrategyNRExistsDefault(subselectExpression.filterExpr, subselectExpression.havingExpr);
    }

    public static SubselectEvalStrategyNR createStrategyAnyAllIn(ExprSubselectNode subselectExpression,
                                                                 boolean isNot,
                                                                 boolean isAll,
                                                                 boolean isAny,
                                                                 RelationalOpEnum relationalOp,
                                                                 EngineImportService engineImportService,
                                                                 String statementName) throws ExprValidationException {
        if (subselectExpression.getChildNodes().length != 1) {
            throw new ExprValidationException("The Subselect-IN requires 1 child expression");
        }
        ExprNode valueExpr = subselectExpression.getChildNodes()[0];

        // Must be the same boxed type returned by expressions under this
        Class typeOne = JavaClassHelper.getBoxedType(subselectExpression.getChildNodes()[0].getForge().getEvaluationType());

        // collections, array or map not supported
        if ((typeOne.isArray()) || (JavaClassHelper.isImplementsInterface(typeOne, Collection.class)) || (JavaClassHelper.isImplementsInterface(typeOne, Map.class))) {
            throw new ExprValidationException("Collection or array comparison is not allowed for the IN, ANY, SOME or ALL keywords");
        }

        Class typeTwo;
        if (subselectExpression.getSelectClause() != null) {
            typeTwo = subselectExpression.getSelectClause()[0].getForge().getEvaluationType();
        } else {
            typeTwo = subselectExpression.getRawEventType().getUnderlyingType();
        }

        boolean aggregated = aggregated(subselectExpression.getSubselectAggregationType());
        boolean grouped = grouped(subselectExpression.getStatementSpecCompiled().getGroupByExpressions());
        ExprEvaluator selectEval = subselectExpression.getSelectClause() == null ? null : ExprNodeCompiler.allocateEvaluator(subselectExpression.getSelectClause()[0].getForge(), engineImportService, SubselectEvalStrategyNRFactory.class, false, statementName);
        ExprEvaluator valueEval = ExprNodeCompiler.allocateEvaluator(valueExpr.getForge(), engineImportService, SubselectEvalStrategyNRFactory.class, false, statementName);
        ExprEvaluator filterEval = subselectExpression.getFilterExpr();
        ExprEvaluator havingEval = subselectExpression.getHavingExpr();

        if (relationalOp != null) {
            if ((typeOne != String.class) || (typeTwo != String.class)) {
                if (!JavaClassHelper.isNumeric(typeOne)) {
                    throw new ExprValidationException("Implicit conversion from datatype '" +
                            typeOne.getSimpleName() +
                            "' to numeric is not allowed");
                }
                if (!JavaClassHelper.isNumeric(typeTwo)) {
                    throw new ExprValidationException("Implicit conversion from datatype '" +
                            typeTwo.getSimpleName() +
                            "' to numeric is not allowed");
                }
            }

            Class compareType = JavaClassHelper.getCompareToCoercionType(typeOne, typeTwo);
            RelationalOpEnum.Computer computer = relationalOp.getComputer(compareType, typeOne, typeTwo);

            if (isAny) {
                if (grouped) {
                    return new SubselectEvalStrategyNRRelOpAnyWGroupBy(valueEval, selectEval, false, computer, havingEval);
                }
                if (aggregated) {
                    return new SubselectEvalStrategyNRRelOpAllAnyAggregated(valueEval, selectEval, false, computer, havingEval);
                }
                return new SubselectEvalStrategyNRRelOpAnyDefault(valueEval, selectEval, false, computer, filterEval);
            }

            // handle ALL
            if (grouped) {
                return new SubselectEvalStrategyNRRelOpAllWGroupBy(valueEval, selectEval, true, computer, havingEval);
            }
            if (aggregated) {
                return new SubselectEvalStrategyNRRelOpAllAnyAggregated(valueEval, selectEval, true, computer, havingEval);
            }
            return new SubselectEvalStrategyNRRelOpAllDefault(valueEval, selectEval, true, computer, filterEval);
        }

        SimpleNumberCoercer coercer = getCoercer(typeOne, typeTwo);
        if (isAll) {
            if (grouped) {
                return new SubselectEvalStrategyNREqualsAllWGroupBy(valueEval, selectEval, true, isNot, coercer, havingEval);
            }
            if (aggregated) {
                return new SubselectEvalStrategyNREqualsAllAnyAggregated(valueEval, selectEval, true, isNot, coercer, havingEval);
            }
            return new SubselectEvalStrategyNREqualsAllDefault(valueEval, selectEval, true, isNot, coercer, filterEval);
        } else if (isAny) {
            if (grouped) {
                return new SubselectEvalStrategyNREqualsAnyWGroupBy(valueEval, selectEval, false, isNot, coercer, havingEval);
            }
            if (aggregated) {
                return new SubselectEvalStrategyNREqualsAllAnyAggregated(valueEval, selectEval, true, isNot, coercer, havingEval);
            }
            return new SubselectEvalStrategyNREqualsAnyDefault(valueEval, selectEval, false, isNot, coercer, filterEval);
        } else {
            if (grouped) {
                return new SubselectEvalStrategyNREqualsInWGroupBy(valueEval, selectEval, isNot, coercer, havingEval);
            }
            if (aggregated) {
                return new SubselectEvalStrategyNREqualsInAggregated(valueEval, selectEval, isNot, coercer, havingEval);
            }
            if (filterEval == null) {
                return new SubselectEvalStrategyNREqualsInUnfiltered(valueEval, selectEval, isNot, coercer);
            }
            return new SubselectEvalStrategyNREqualsInFiltered(valueEval, selectEval, isNot, coercer, filterEval);
        }
    }

    private static SimpleNumberCoercer getCoercer(Class typeOne, Class typeTwo) throws ExprValidationException {
        // Get the common type such as Bool, String or Double and Long
        Class coercionType;
        boolean mustCoerce;
        try {
            coercionType = JavaClassHelper.getCompareToCoercionType(typeOne, typeTwo);
        } catch (CoercionException ex) {
            throw new ExprValidationException("Implicit conversion from datatype '" +
                    typeTwo.getSimpleName() +
                    "' to '" +
                    typeOne.getSimpleName() +
                    "' is not allowed");
        }

        // Check if we need to coerce
        mustCoerce = false;
        if ((coercionType != JavaClassHelper.getBoxedType(typeOne)) ||
                (coercionType != JavaClassHelper.getBoxedType(typeTwo))) {
            if (JavaClassHelper.isNumeric(coercionType)) {
                mustCoerce = true;
            }
        }
        return !mustCoerce ? null : SimpleNumberCoercerFactory.getCoercer(null, coercionType);
    }

    private static boolean grouped(GroupByClauseExpressions groupByExpressions) {
        return groupByExpressions != null && groupByExpressions.getGroupByNodes() != null && groupByExpressions.getGroupByNodes().length != 0;
    }

    private static boolean aggregated(ExprSubselectNode.SubqueryAggregationType subqueryAggregationType) {
        return subqueryAggregationType != null && subqueryAggregationType != ExprSubselectNode.SubqueryAggregationType.NONE;
    }
}
