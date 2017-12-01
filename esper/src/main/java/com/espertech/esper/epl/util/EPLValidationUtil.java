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
package com.espertech.esper.epl.util;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.epl.join.hint.ExcludePlanHint;
import com.espertech.esper.epl.join.plan.FilterExprAnalyzer;
import com.espertech.esper.epl.join.plan.QueryGraph;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EPLValidationUtil {

    private static final Logger log = LoggerFactory.getLogger(ExprNodeUtilityCore.class);

    public static QueryGraph validateFilterGetQueryGraphSafe(ExprNode filterExpression, StatementContext statementContext, StreamTypeServiceImpl typeService) {
        ExcludePlanHint excludePlanHint = null;
        try {
            excludePlanHint = ExcludePlanHint.getHint(typeService.getStreamNames(), statementContext);
        } catch (ExprValidationException ex) {
            log.warn("Failed to consider exclude-plan hint: " + ex.getMessage(), ex);
        }

        QueryGraph queryGraph = new QueryGraph(1, excludePlanHint, false);
        validateFilterWQueryGraphSafe(queryGraph, filterExpression, statementContext, typeService);
        return queryGraph;
    }

    public static void validateFilterWQueryGraphSafe(QueryGraph queryGraph, ExprNode filterExpression, StatementContext statementContext, StreamTypeServiceImpl typeService) {
        try {
            ExprEvaluatorContextStatement evaluatorContextStmt = new ExprEvaluatorContextStatement(statementContext, false);
            ExprValidationContext validationContext = new ExprValidationContext(typeService, statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), null, statementContext.getTimeProvider(), statementContext.getVariableService(), statementContext.getTableService(), evaluatorContextStmt, statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, true, false, null, true);
            ExprNode validated = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.FILTER, filterExpression, validationContext);
            FilterExprAnalyzer.analyze(validated, queryGraph, false);
        } catch (Exception ex) {
            log.warn("Unexpected exception analyzing filterable expression '" + ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(filterExpression) + "': " + ex.getMessage(), ex);
        }
    }

    public static ExprNode validateSimpleGetSubtree(ExprNodeOrigin origin, ExprNode expression, StatementContext statementContext, EventType optionalEventType, boolean allowBindingConsumption)
            throws ExprValidationException {

        ExprNodeUtilityRich.validatePlainExpression(origin, expression);

        StreamTypeServiceImpl streamTypes;
        if (optionalEventType != null) {
            streamTypes = new StreamTypeServiceImpl(optionalEventType, null, true, statementContext.getEngineURI());
        } else {
            streamTypes = new StreamTypeServiceImpl(statementContext.getEngineURI(), false);
        }

        ExprValidationContext validationContext = new ExprValidationContext(streamTypes, statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), null, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), new ExprEvaluatorContextStatement(statementContext, false), statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, allowBindingConsumption, false, null, false);
        return ExprNodeUtilityRich.getValidatedSubtree(origin, expression, validationContext);
    }

    public static ExprValidationContext getExprValidationContextStatementOnly(StatementContext statementContext) {
        return new ExprValidationContext(new StreamTypeServiceImpl(statementContext.getEngineURI(), false), statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), null, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), new ExprEvaluatorContextStatement(statementContext, false), statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, false, false, null, false);
    }

    public static void validateParameterNumber(String invocableName, String invocableCategory, boolean isFunction, int expectedEnum, int receivedNum) throws ExprValidationException {
        if (expectedEnum != receivedNum) {
            throw new ExprValidationException(getInvokablePrefix(invocableName, invocableCategory, isFunction) + "expected " + expectedEnum + " parameters but received " + receivedNum + " parameters");
        }
    }

    public static void validateParameterType(String invocableName, String invocableCategory, boolean isFunction, EPLExpressionParamType expectedTypeEnum, Class[] expectedTypeClasses, Class providedType, int parameterNum, ExprNode parameterExpression)
            throws ExprValidationException {
        if (expectedTypeEnum == EPLExpressionParamType.BOOLEAN && (!JavaClassHelper.isBoolean(providedType))) {
            throw new ExprValidationException(getInvokablePrefix(invocableName, invocableCategory, isFunction) + "expected a boolean-type result for expression parameter " + parameterNum + " but received " + JavaClassHelper.getClassNameFullyQualPretty(providedType));
        }
        if (expectedTypeEnum == EPLExpressionParamType.NUMERIC && (!JavaClassHelper.isNumeric(providedType))) {
            throw new ExprValidationException(getInvokablePrefix(invocableName, invocableCategory, isFunction) + "expected a number-type result for expression parameter " + parameterNum + " but received " + JavaClassHelper.getClassNameFullyQualPretty(providedType));
        }
        if (expectedTypeEnum == EPLExpressionParamType.SPECIFIC) {
            Class boxedProvidedType = JavaClassHelper.getBoxedType(providedType);
            boolean found = false;
            for (Class expectedTypeClass : expectedTypeClasses) {
                Class boxedExpectedType = JavaClassHelper.getBoxedType(expectedTypeClass);
                if (boxedProvidedType != null && JavaClassHelper.isSubclassOrImplementsInterface(boxedProvidedType, boxedExpectedType)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                String expected;
                if (expectedTypeClasses.length == 1) {
                    expected = "a " + JavaClassHelper.getParameterAsString(expectedTypeClasses);
                } else {
                    expected = "any of [" + JavaClassHelper.getParameterAsString(expectedTypeClasses) + "]";
                }
                throw new ExprValidationException(getInvokablePrefix(invocableName, invocableCategory, isFunction) + "expected " + expected + "-type result for expression parameter " + parameterNum + " but received " + JavaClassHelper.getClassNameFullyQualPretty(providedType));
            }
        }
        if (expectedTypeEnum == EPLExpressionParamType.TIME_PERIOD_OR_SEC) {
            if (parameterExpression instanceof ExprTimePeriod || parameterExpression instanceof ExprStreamUnderlyingNode) {
                return;
            }
            if (!(JavaClassHelper.isNumeric(providedType))) {
                throw new ExprValidationException(getInvokablePrefix(invocableName, invocableCategory, isFunction) + "expected a time-period expression or a numeric-type result for expression parameter " + parameterNum + " but received " + JavaClassHelper.getClassNameFullyQualPretty(providedType));
            }
        }
        if (expectedTypeEnum == EPLExpressionParamType.DATETIME) {
            if (!(JavaClassHelper.isDatetimeClass(providedType))) {
                throw new ExprValidationException(getInvokablePrefix(invocableName, invocableCategory, isFunction) + "expected a long-typed, Date-typed or Calendar-typed result for expression parameter " + parameterNum + " but received " + JavaClassHelper.getClassNameFullyQualPretty(providedType));
            }
        }
    }

    public static void validateTableExists(TableService tableService, String name) throws ExprValidationException {
        if (tableService.getTableMetadata(name) != null) {
            throw new ExprValidationException("A table by name '" + name + "' already exists");
        }
    }

    public static void validateContextName(boolean table, String tableOrNamedWindowName, String tableOrNamedWindowContextName, String optionalContextName, boolean mustMatchContext)
            throws ExprValidationException {
        if (tableOrNamedWindowContextName != null) {
            if (optionalContextName == null || !optionalContextName.equals(tableOrNamedWindowContextName)) {
                throw getCtxMessage(table, tableOrNamedWindowName, tableOrNamedWindowContextName);
            }
        } else {
            if (mustMatchContext && optionalContextName != null) {
                throw getCtxMessage(table, tableOrNamedWindowName, tableOrNamedWindowContextName);
            }
        }
    }

    private static ExprValidationException getCtxMessage(boolean table, String tableOrNamedWindowName, String tableOrNamedWindowContextName) {
        String prefix = table ? "Table" : "Named window";
        return new ExprValidationException(prefix + " by name '" + tableOrNamedWindowName + "' has been declared for context '" + tableOrNamedWindowContextName + "' and can only be used within the same context");
    }

    public static String getInvokablePrefix(String invocableName, String invocableType, boolean isFunction) {
        return "Error validating " + invocableType + " " + (isFunction ? "function '" : "method '") + invocableName + "', ";
    }

    public static void validateParametersTypePredefined(ExprNode[] expressions, String invocableName, String invocableCategory, EPLExpressionParamType type) throws ExprValidationException {
        for (int i = 0; i < expressions.length; i++) {
            EPLValidationUtil.validateParameterType(invocableName, invocableCategory, true, type, null, expressions[i].getForge().getEvaluationType(), i, expressions[i]);
        }
    }
}
