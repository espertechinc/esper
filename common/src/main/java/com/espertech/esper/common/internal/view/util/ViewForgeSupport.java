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
package com.espertech.esper.common.internal.view.util;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeSummaryVisitor;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.view.core.ViewForgeEnv;
import com.espertech.esper.common.internal.view.core.ViewParameterException;

import java.util.ArrayList;
import java.util.List;

public class ViewForgeSupport {
    public static Object validateAndEvaluate(String viewName, ExprNode expression, ViewForgeEnv viewForgeEnv, int streamNumber)
            throws ViewParameterException {
        return validateAndEvaluateExpr(viewName, expression, new StreamTypeServiceImpl(false), viewForgeEnv, 0, streamNumber);
    }

    public static Object evaluateAssertNoProperties(String viewName, ExprNode expression, int index) throws ViewParameterException {
        validateNoProperties(viewName, expression, index);
        return expression.getForge().getExprEvaluator().evaluate(null, false, null);
    }

    /**
     * Assert and throws an exception if the expression passed returns a non-constant value.
     *
     * @param viewName   textual name of view
     * @param expression expression to check
     * @param index      number offset of expression in view parameters
     * @throws ViewParameterException if assertion fails
     */
    public static void assertReturnsNonConstant(String viewName, ExprNode expression, int index) throws ViewParameterException {
        if (expression.getForge().getForgeConstantType().isCompileTimeConstant()) {
            String message = "Invalid view parameter expression " + index + getViewDesc(viewName) + ", the expression returns a constant result value, are you sure?";
            throw new ViewParameterException(message);
        }
    }

    public static void validateNoProperties(String viewName, ExprNode expression, int index) throws ViewParameterException {
        ExprNodeSummaryVisitor visitor = new ExprNodeSummaryVisitor();
        expression.accept(visitor);
        if (!visitor.isPlain()) {
            String message = "Invalid view parameter expression " + index + getViewDesc(viewName) + ", " + visitor.getMessage() + " are not allowed within the expression";
            throw new ViewParameterException(message);
        }
    }

    public static Object validateAndEvaluateExpr(String viewName, ExprNode expression, StreamTypeService streamTypeService, ViewForgeEnv viewForgeEnv, int expressionNumber, int streamNumber)
            throws ViewParameterException {
        ExprNode validated = validateExpr(viewName, expression, streamTypeService, viewForgeEnv, expressionNumber, streamNumber);

        try {
            return validated.getForge().getExprEvaluator().evaluate(null, true, null);
        } catch (RuntimeException ex) {
            String message = "Failed to evaluate parameter expression " + expressionNumber + getViewDesc(viewName);
            if (ex.getMessage() != null) {
                message += ": " + ex.getMessage();
            }
            throw new ViewParameterException(message, ex);
        }
    }

    public static ExprForge validateSizeSingleParam(String viewName, List<ExprNode> expressionParameters, ViewForgeEnv viewForgeEnv, int streamNumber) throws ViewParameterException {
        ExprNode[] validated = validate(viewName, expressionParameters, viewForgeEnv, streamNumber);
        if (validated.length != 1) {
            throw new ViewParameterException(getViewParamMessage(viewName));
        }
        return validateSizeParam(viewName, validated[0], 0);
    }

    public static ExprForge validateSizeParam(String viewName, ExprNode sizeNode, int expressionNumber) throws ViewParameterException {
        ExprForge forge = sizeNode.getForge();
        Class returnType = JavaClassHelper.getBoxedType(sizeNode.getForge().getEvaluationType());
        if (!JavaClassHelper.isNumeric(returnType) || JavaClassHelper.isFloatingPointClass(returnType) || returnType == Long.class) {
            throw new ViewParameterException(getViewParamMessage(viewName));
        }
        if (sizeNode.getForge().getForgeConstantType().isCompileTimeConstant()) {
            Number size = (Number) evaluate(forge.getExprEvaluator(), expressionNumber, viewName);
            if (!validateSize(size)) {
                throw new ViewParameterException(getSizeValidationMsg(viewName, size));
            }
        }
        return forge;
    }

    /**
     * Validate the view parameter expressions and return the validated expression for later execution.
     * <p>
     * Does not evaluate the expression.
     *
     * @param viewName            textual name of view
     * @param eventType           is the event type of the parent view or stream attached.
     * @param expressions         view expression parameter to validate
     * @param allowConstantResult true to indicate whether expressions that return a constant
     *                            result should be allowed; false to indicate that if an expression is known to return a constant result
     *                            the expression is considered invalid
     * @param streamNumber        stream number
     * @param viewForgeEnv        view forge env
     * @return object result value of parameter expressions
     * @throws ViewParameterException if the expressions fail to validate
     */
    public static ExprNode[] validate(String viewName, EventType eventType, List<ExprNode> expressions, boolean allowConstantResult, ViewForgeEnv viewForgeEnv, int streamNumber)
            throws ViewParameterException {
        List<ExprNode> results = new ArrayList<ExprNode>();
        int expressionNumber = 0;
        StreamTypeService streamTypeService = new StreamTypeServiceImpl(eventType, null, false);
        for (ExprNode expr : expressions) {
            ExprNode validated = validateExpr(viewName, expr, streamTypeService, viewForgeEnv, expressionNumber, streamNumber);
            results.add(validated);

            if ((!allowConstantResult) && (validated.getForge().getForgeConstantType().isCompileTimeConstant())) {
                String message = "Invalid view parameter expression " + expressionNumber + getViewDesc(viewName) + ", the expression returns a constant result value, are you sure?";
                throw new ViewParameterException(message);
            }

            expressionNumber++;
        }
        return results.toArray(new ExprNode[results.size()]);
    }

    public static ExprNode[] validate(String viewName, List<ExprNode> expressions, ViewForgeEnv viewForgeEnv, int streamNumber)
            throws ViewParameterException {
        ExprNode[] results = new ExprNode[expressions.size()];
        int expressionNumber = 0;
        StreamTypeService streamTypeService = new StreamTypeServiceImpl(false);
        for (ExprNode expr : expressions) {
            results[expressionNumber] = validateExpr(viewName, expr, streamTypeService, viewForgeEnv, expressionNumber, streamNumber);
            expressionNumber++;
        }
        return results;
    }

    public static ExprNode validateExpr(String viewName, ExprNode expression, StreamTypeService streamTypeService, ViewForgeEnv viewForgeEnv, int expressionNumber, int streamNumber)
            throws ViewParameterException {
        ExprNode validated;
        try {
            ExprValidationMemberNameQualifiedView names = new ExprValidationMemberNameQualifiedView(streamNumber);
            ExprValidationContext validationContext = new ExprValidationContextBuilder(streamTypeService, viewForgeEnv.getStatementRawInfo(), viewForgeEnv.getStatementCompileTimeServices())
                    .withMemberName(names).build();
            validated = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.VIEWPARAMETER, expression, validationContext);
        } catch (ExprValidationException ex) {
            String message = "Invalid parameter expression " + expressionNumber + getViewDesc(viewName);
            if (ex.getMessage() != null) {
                message += ": " + ex.getMessage();
            }
            throw new ViewParameterException(message, ex);
        }
        return validated;
    }

    public static Object evaluate(ExprEvaluator evaluator, int expressionNumber, String viewName)
            throws ViewParameterException {
        try {
            return evaluator.evaluate(null, true, null);
        } catch (RuntimeException ex) {
            String message = "Failed to evaluate parameter expression " + expressionNumber + getViewDesc(viewName);
            if (ex.getMessage() != null) {
                message += ": " + ex.getMessage();
            }
            throw new ViewParameterException(message, ex);
        }
    }

    public static void validateNoParameters(String viewName, List<ExprNode> expressionParameters) throws ViewParameterException {
        if (!expressionParameters.isEmpty()) {
            String errorMessage = viewName + " view requires an empty parameter list";
            throw new ViewParameterException(errorMessage);
        }
    }

    private static String getViewParamMessage(String viewName) {
        return viewName + " view requires a single integer-type parameter";
    }

    private static boolean validateSize(Number size) {
        return !(size == null || size.intValue() <= 0);
    }

    private static String getSizeValidationMsg(String viewName, Number size) {
        return viewName + " view requires a positive integer for size but received " + size;
    }

    private static String getViewDesc(String viewName) {
        return " for " + viewName + " view";
    }
}
