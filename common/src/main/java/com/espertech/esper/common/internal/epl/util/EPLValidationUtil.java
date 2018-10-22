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
package com.espertech.esper.common.internal.epl.util;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriod;
import com.espertech.esper.common.internal.epl.join.analyze.FilterExprAnalyzer;
import com.espertech.esper.common.internal.epl.join.hint.ExcludePlanHint;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphForge;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeResolver;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableCompileTimeResolver;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.event.core.EventTypeCompileTimeResolver;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EPLValidationUtil {
    private static final Logger log = LoggerFactory.getLogger(EPLValidationUtil.class);

    public static void validateParametersTypePredefined(ExprNode[] expressions, String invocableName, String invocableCategory, EPLExpressionParamType type) throws ExprValidationException {
        for (int i = 0; i < expressions.length; i++) {
            EPLValidationUtil.validateParameterType(invocableName, invocableCategory, true, type, null, expressions[i].getForge().getEvaluationType(), i, expressions[i]);
        }
    }

    public static void validateTableExists(TableCompileTimeResolver tableCompileTimeResolver, String name) throws ExprValidationException {
        if (tableCompileTimeResolver.resolve(name) != null) {
            throw new ExprValidationException("A table by name '" + name + "' already exists");
        }
    }

    public static ExprNode validateSimpleGetSubtree(ExprNodeOrigin origin, ExprNode expression, EventType optionalEventType, boolean allowBindingConsumption, StatementRawInfo statementRawInfo, StatementCompileTimeServices services)
            throws ExprValidationException {

        ExprNodeUtilityValidate.validatePlainExpression(origin, expression);

        StreamTypeServiceImpl streamTypes;
        if (optionalEventType != null) {
            streamTypes = new StreamTypeServiceImpl(optionalEventType, null, true);
        } else {
            streamTypes = new StreamTypeServiceImpl(false);
        }

        ExprValidationContext validationContext = new ExprValidationContextBuilder(streamTypes, statementRawInfo, services).withAllowBindingConsumption(allowBindingConsumption).build();
        return ExprNodeUtilityValidate.getValidatedSubtree(origin, expression, validationContext);
    }

    public static QueryGraphForge validateFilterGetQueryGraphSafe(ExprNode filterExpression, StreamTypeServiceImpl typeService, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) {
        ExcludePlanHint excludePlanHint = null;
        try {
            excludePlanHint = ExcludePlanHint.getHint(typeService.getStreamNames(), statementRawInfo, services);
        } catch (ExprValidationException ex) {
            log.warn("Failed to consider exclude-plan hint: " + ex.getMessage(), ex);
        }

        QueryGraphForge queryGraph = new QueryGraphForge(1, excludePlanHint, false);
        if (filterExpression != null) {
            validateFilterWQueryGraphSafe(queryGraph, filterExpression, typeService, statementRawInfo, services);
        }
        return queryGraph;
    }

    public static void validateFilterWQueryGraphSafe(QueryGraphForge queryGraph, ExprNode filterExpression, StreamTypeServiceImpl typeService, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) {
        try {
            ExprValidationContext validationContext = new ExprValidationContextBuilder(typeService, statementRawInfo, services)
                    .withAllowBindingConsumption(true).withIsFilterExpression(true).build();
            ExprNode validated = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.FILTER, filterExpression, validationContext);
            FilterExprAnalyzer.analyze(validated, queryGraph, false);
        } catch (Exception ex) {
            log.warn("Unexpected exception analyzing filterable expression '" + ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(filterExpression) + "': " + ex.getMessage(), ex);
        }
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

    public static String getInvokablePrefix(String invocableName, String invocableType, boolean isFunction) {
        return "Error validating " + invocableType + " " + (isFunction ? "function '" : "method '") + invocableName + "', ";
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

    public static void validateAlreadyExistsTableOrVariable(String name, VariableCompileTimeResolver variableCompileTimeResolver, TableCompileTimeResolver tableCompileTimeResolver, EventTypeCompileTimeResolver eventTypeCompileTimeResolver) throws ExprValidationException {
        TableMetaData existingTable = tableCompileTimeResolver.resolve(name);
        if (existingTable != null) {
            throw new ExprValidationException("A table by name '" + name + "' has already been declared");
        }
        VariableMetaData existingVariable = variableCompileTimeResolver.resolve(name);
        if (existingVariable != null) {
            throw new ExprValidationException("A variable by name '" + name + "' has already been declared");
        }
        EventType existingEventType = eventTypeCompileTimeResolver.getTypeByName(name);
        if (existingEventType != null) {
            throw new ExprValidationException("An event type by name '" + name + "' has already been declared");
        }
    }
}
