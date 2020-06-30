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
package com.espertech.esper.common.internal.epl.expression.subquery;

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.internal.compile.stage1.spec.GroupByClauseElement;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityValidate;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.type.RelationalOpEnum;
import com.espertech.esper.common.internal.util.CoercionException;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;

import java.util.List;

import static com.espertech.esper.common.internal.util.JavaClassHelper.isTypeString;

/**
 * Factory for subselect evaluation strategies.
 */
public class SubselectNRForgeFactory {

    public static SubselectForgeNR createStrategyExists(ExprSubselectExistsNode subselectExpression) {
        boolean aggregated = aggregated(subselectExpression.getSubselectAggregationType());
        boolean grouped = grouped(subselectExpression.getStatementSpecCompiled().getRaw().getGroupByExpressions());
        if (grouped) {
            if (subselectExpression.havingExpr != null) {
                return new SubselectForgeNRExistsWGroupByWHaving(subselectExpression, subselectExpression.havingExpr);
            }
            return new SubselectForgeNRExistsWGroupBy(subselectExpression);
        }
        if (aggregated) {
            if (subselectExpression.havingExpr != null) {
                return new SubselectForgeNRExistsAggregated(subselectExpression.havingExpr);
            }
            return SubselectForgeNRExistsAlwaysTrue.INSTANCE;
        }
        return new SubselectForgeNRExistsDefault(subselectExpression.filterExpr, subselectExpression.havingExpr);
    }

    public static SubselectForgeNR createStrategyAnyAllIn(ExprSubselectNode subselectExpression,
                                                          boolean isNot,
                                                          boolean isAll,
                                                          boolean isAny,
                                                          RelationalOpEnum relationalOp,
                                                          ClasspathImportServiceCompileTime classpathImportService) throws ExprValidationException {
        if (subselectExpression.getChildNodes().length != 1) {
            throw new ExprValidationException("The Subselect-IN requires 1 child expression");
        }
        ExprNode valueExpr = subselectExpression.getChildNodes()[0];

        EPType typeOne = JavaClassHelper.getBoxedType(subselectExpression.getChildNodes()[0].getForge().getEvaluationType());
        EPTypeClass typeOneClass = ExprNodeUtilityValidate.validateLHSTypeAnyAllSomeIn(typeOne);

        EPTypeClass typeTwoClass;
        if (subselectExpression.getSelectClause() != null) {
            EPType selectType = subselectExpression.getSelectClause()[0].getForge().getEvaluationType();
            if (selectType == null || selectType == EPTypeNull.INSTANCE) {
                throw new ExprValidationException("Null-type value not allowed for the IN, ANY, SOME or ALL keywords");
            }
            typeTwoClass = (EPTypeClass) selectType;
        } else {
            typeTwoClass = subselectExpression.getRawEventType().getUnderlyingEPType();
        }

        boolean aggregated = aggregated(subselectExpression.getSubselectAggregationType());
        boolean grouped = grouped(subselectExpression.getStatementSpecCompiled().getRaw().getGroupByExpressions());
        ExprForge selectEval = subselectExpression.getSelectClause() == null ? null : subselectExpression.getSelectClause()[0].getForge();
        ExprForge valueEval = valueExpr.getForge();
        ExprForge filterEval = subselectExpression.filterExpr;
        ExprForge havingEval = subselectExpression.havingExpr;

        if (relationalOp != null) {
            if (!isTypeString(typeOne) || !isTypeString(typeTwoClass)) {
                if (!JavaClassHelper.isNumeric(typeOne)) {
                    throw new ExprValidationException("Implicit conversion from datatype '" +
                            typeOne.getTypeName() +
                            "' to numeric is not allowed");
                }
                if (!JavaClassHelper.isNumeric(typeTwoClass)) {
                    throw new ExprValidationException("Implicit conversion from datatype '" +
                            typeTwoClass.getTypeName() +
                            "' to numeric is not allowed");
                }
            }

            EPTypeClass compareType = JavaClassHelper.getCompareToCoercionType(typeOneClass, typeTwoClass);
            RelationalOpEnum.Computer computer = relationalOp.getComputer(compareType, typeOneClass, typeTwoClass);

            if (isAny) {
                if (grouped) {
                    return new SubselectForgeNRRelOpAnyWGroupBy(subselectExpression, valueEval, selectEval, false, computer, havingEval);
                }
                if (aggregated) {
                    return new SubselectForgeNRRelOpAllAnyAggregated(subselectExpression, valueEval, selectEval, false, computer, havingEval);
                }
                return new SubselectForgeStrategyNRRelOpAnyDefault(subselectExpression, valueEval, selectEval, false, computer, filterEval);
            }

            // handle ALL
            if (grouped) {
                return new SubselectForgeNRRelOpAllWGroupBy(subselectExpression, valueEval, selectEval, true, computer, havingEval);
            }
            if (aggregated) {
                return new SubselectForgeNRRelOpAllAnyAggregated(subselectExpression, valueEval, selectEval, true, computer, havingEval);
            }
            return new SubselectForgeNRRelOpAllDefault(subselectExpression, valueEval, selectEval, true, computer, filterEval);
        }

        SimpleNumberCoercer coercer = getCoercer(typeOneClass, typeTwoClass);
        if (isAll) {
            if (grouped) {
                return new SubselectForgeNREqualsAllAnyWGroupBy(subselectExpression, valueEval, selectEval, true, isNot, coercer, havingEval, true);
            }
            if (aggregated) {
                return new SubselectForgeNREqualsAllAnyAggregated(subselectExpression, valueEval, selectEval, true, isNot, coercer, havingEval);
            }
            return new SubselectForgeNREqualsDefault(subselectExpression, valueEval, selectEval, true, isNot, coercer, filterEval, true);
        } else if (isAny) {
            if (grouped) {
                return new SubselectForgeNREqualsAllAnyWGroupBy(subselectExpression, valueEval, selectEval, false, isNot, coercer, havingEval, false);
            }
            if (aggregated) {
                return new SubselectForgeNREqualsAllAnyAggregated(subselectExpression, valueEval, selectEval, true, isNot, coercer, havingEval);
            }
            return new SubselectForgeNREqualsDefault(subselectExpression, valueEval, selectEval, false, isNot, coercer, filterEval, false);
        } else {
            if (grouped) {
                return new SubselectForgeNREqualsInWGroupBy(subselectExpression, valueEval, selectEval, isNot, isNot, coercer, havingEval);
            }
            if (aggregated) {
                return new SubselectForgeNREqualsInAggregated(subselectExpression, valueEval, selectEval, isNot, isNot, coercer, havingEval);
            }
            return new SubselectForgeNREqualsIn(subselectExpression, valueEval, selectEval, isNot, isNot, coercer, filterEval);
        }
    }

    private static SimpleNumberCoercer getCoercer(EPTypeClass typeOne, EPTypeClass typeTwo) throws ExprValidationException {
        // Get the common type such as Bool, String or Double and Long
        EPTypeClass coercionType;
        boolean mustCoerce;
        try {
            coercionType = JavaClassHelper.getCompareToCoercionType(typeOne, typeTwo);
        } catch (CoercionException ex) {
            throw new ExprValidationException("Implicit conversion from datatype '" +
                    typeTwo +
                    "' to '" +
                    typeOne +
                    "' is not allowed");
        }

        // Check if we need to coerce
        mustCoerce = false;
        if (!coercionType.equals(JavaClassHelper.getBoxedType(typeOne)) ||
                (!(coercionType.equals(JavaClassHelper.getBoxedType(typeTwo))))) {
            if (JavaClassHelper.isNumeric(coercionType)) {
                mustCoerce = true;
            }
        }
        return !mustCoerce ? null : SimpleNumberCoercerFactory.getCoercer(null, coercionType);
    }

    private static boolean grouped(List<GroupByClauseElement> groupByExpressions) {
        return groupByExpressions != null && !groupByExpressions.isEmpty();
    }

    private static boolean aggregated(ExprSubselectNode.SubqueryAggregationType subqueryAggregationType) {
        return subqueryAggregationType != null && subqueryAggregationType != ExprSubselectNode.SubqueryAggregationType.NONE;
    }
}
