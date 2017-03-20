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
package com.espertech.esper.epl.methodbase;

import com.espertech.esper.epl.enummethod.dot.ExprLambdaGoesNode;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprStreamUnderlyingNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DotMethodUtil {

    public static DotMethodFPProvided getProvidedFootprint(List<ExprNode> parameters) {
        List<DotMethodFPProvidedParam> paramsList = new ArrayList<DotMethodFPProvidedParam>();
        for (ExprNode node : parameters) {
            if (!(node instanceof ExprLambdaGoesNode)) {
                paramsList.add(new DotMethodFPProvidedParam(0, node.getExprEvaluator().getType(), node));
                continue;
            }
            ExprLambdaGoesNode goesNode = (ExprLambdaGoesNode) node;
            paramsList.add(new DotMethodFPProvidedParam(goesNode.getGoesToNames().size(), null, goesNode));
        }
        return new DotMethodFPProvided(paramsList.toArray(new DotMethodFPProvidedParam[paramsList.size()]));
    }

    public static DotMethodFP validateParametersDetermineFootprint(DotMethodFP[] footprints, DotMethodTypeEnum methodType, String methodUsedName, DotMethodFPProvided providedFootprint, DotMethodInputTypeMatcher inputTypeMatcher)
            throws ExprValidationException {
        boolean isLambdaApplies = DotMethodTypeEnum.ENUM == methodType;

        // determine footprint candidates strictly based on parameters
        List<DotMethodFP> candidates = null;
        DotMethodFP bestMatch = null;
        for (DotMethodFP footprint : footprints) {

            DotMethodFPParam[] requiredParams = footprint.getParameters();
            if (requiredParams.length != providedFootprint.getParameters().length) {
                continue;
            }

            if (bestMatch == null) {    // take first if number of parameters matches
                bestMatch = footprint;
            }

            boolean paramMatch = true;
            int count = 0;
            for (DotMethodFPParam requiredParam : requiredParams) {

                DotMethodFPProvidedParam providedParam = providedFootprint.getParameters()[count++];
                if (requiredParam.getLambdaParamNum() != providedParam.getLambdaParamNum()) {
                    paramMatch = false;
                }
            }

            if (paramMatch) {
                if (candidates == null) {
                    candidates = new ArrayList<DotMethodFP>(2);
                }
                candidates.add(footprint);
            }
        }

        // if there are multiple candidates, eliminate by input (event bean collection or component collection)
        if (candidates != null && candidates.size() > 1) {
            Iterator<DotMethodFP> candidateIt = candidates.iterator();
            for (; candidateIt.hasNext(); ) {
                DotMethodFP fp = candidateIt.next();
                if (!inputTypeMatcher.matches(fp)) {
                    candidateIt.remove();
                }
            }
        }

        // handle single remaining candidate
        if (candidates != null && candidates.size() == 1) {
            DotMethodFP found = candidates.get(0);
            validateSpecificTypes(methodUsedName, methodType, found.getParameters(), providedFootprint.getParameters());
            return found;
        }

        // check all candidates in detail to see which one matches, take first one
        if (candidates != null && !candidates.isEmpty()) {
            bestMatch = candidates.get(0);
            Iterator<DotMethodFP> candidateIt = candidates.iterator();
            ExprValidationException firstException = null;
            for (; candidateIt.hasNext(); ) {
                DotMethodFP fp = candidateIt.next();
                try {
                    validateSpecificTypes(methodUsedName, methodType, fp.getParameters(), providedFootprint.getParameters());
                    return fp;
                } catch (ExprValidationException ex) {
                    if (firstException == null) {
                        firstException = ex;
                    }
                }
            }
            if (firstException != null) {
                throw firstException;
            }
        }

        String message = "Parameters mismatch for " + methodType.getTypeName() + " method '" + methodUsedName + "', the method ";
        if (bestMatch != null) {
            StringWriter buf = new StringWriter();
            buf.append(bestMatch.toStringFootprint(isLambdaApplies));
            buf.append(", but receives ");
            buf.append(DotMethodFP.toStringProvided(providedFootprint, isLambdaApplies));
            throw new ExprValidationException(message + "requires " + buf.toString());
        }

        if (footprints.length == 1) {
            throw new ExprValidationException(message + "requires " + footprints[0].toStringFootprint(isLambdaApplies));
        } else {
            StringWriter buf = new StringWriter();
            String delimiter = "";
            for (DotMethodFP footprint : footprints) {
                buf.append(delimiter);
                buf.append(footprint.toStringFootprint(isLambdaApplies));
                delimiter = ", or ";
            }
            throw new ExprValidationException(message + "has multiple footprints accepting " + buf +
                    ", but receives " + DotMethodFP.toStringProvided(providedFootprint, isLambdaApplies));
        }
    }

    private static void validateSpecificTypes(String methodUsedName, DotMethodTypeEnum type, DotMethodFPParam[] foundParams, DotMethodFPProvidedParam[] parameters)
            throws ExprValidationException {
        for (int i = 0; i < foundParams.length; i++) {
            DotMethodFPParam found = foundParams[i];
            DotMethodFPProvidedParam provided = parameters[i];

            // Lambda-type expressions not validated here
            if (found.getLambdaParamNum() > 0) {
                continue;
            }
            validateSpecificType(methodUsedName, type, found.getType(), found.getSpecificType(), provided.getReturnType(), i, provided.getExpression());
        }
    }

    public static void validateSpecificType(String methodUsedName, DotMethodTypeEnum type, DotMethodFPParamTypeEnum expectedTypeEnum, Class[] expectedTypeClasses, Class providedType, int parameterNum, ExprNode parameterExpression)
            throws ExprValidationException {
        String message = "Error validating " + type.getTypeName() + " method '" + methodUsedName + "', ";
        if (expectedTypeEnum == DotMethodFPParamTypeEnum.BOOLEAN && (!JavaClassHelper.isBoolean(providedType))) {
            throw new ExprValidationException(message + "expected a boolean-type result for expression parameter " + parameterNum + " but received " + JavaClassHelper.getClassNameFullyQualPretty(providedType));
        }
        if (expectedTypeEnum == DotMethodFPParamTypeEnum.NUMERIC && (!JavaClassHelper.isNumeric(providedType))) {
            throw new ExprValidationException(message + "expected a number-type result for expression parameter " + parameterNum + " but received " + JavaClassHelper.getClassNameFullyQualPretty(providedType));
        }
        if (expectedTypeEnum == DotMethodFPParamTypeEnum.SPECIFIC) {
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
                throw new ExprValidationException(message + "expected " + expected + "-type result for expression parameter " + parameterNum + " but received " + JavaClassHelper.getClassNameFullyQualPretty(providedType));
            }
        }
        if (expectedTypeEnum == DotMethodFPParamTypeEnum.TIME_PERIOD_OR_SEC) {
            if (parameterExpression instanceof ExprTimePeriod || parameterExpression instanceof ExprStreamUnderlyingNode) {
                return;
            }
            if (!(JavaClassHelper.isNumeric(providedType))) {
                throw new ExprValidationException(message + "expected a time-period expression or a numeric-type result for expression parameter " + parameterNum + " but received " + JavaClassHelper.getClassNameFullyQualPretty(providedType));
            }
        }
        if (expectedTypeEnum == DotMethodFPParamTypeEnum.DATETIME) {
            if (!(JavaClassHelper.isDatetimeClass(providedType))) {
                throw new ExprValidationException(message + "expected a long-typed, Date-typed or Calendar-typed result for expression parameter " + parameterNum + " but received " + JavaClassHelper.getClassNameFullyQualPretty(providedType));
            }
        }
    }
}
