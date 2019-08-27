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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.hook.expr.EPLMethodInvocationContext;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.enummethod.dot.EnumMethodResolver;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprLambdaGoesNode;
import com.espertech.esper.common.internal.epl.expression.etc.*;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

public class ExprNodeUtilityResolve {
    public static ExprNodeUtilMethodDesc resolveMethodAllowWildcardAndStream(String className,
                                                                             Class optionalClass,
                                                                             String methodName,
                                                                             List<ExprNode> parameters,
                                                                             boolean allowWildcard,
                                                                             final EventType wildcardType,
                                                                             ExprNodeUtilResolveExceptionHandler exceptionHandler,
                                                                             String functionName,
                                                                             StatementRawInfo statementRawInfo,
                                                                             StatementCompileTimeServices services) throws ExprValidationException {
        Class[] paramTypes = new Class[parameters.size()];
        ExprForge[] childForges = new ExprForge[parameters.size()];
        int count = 0;
        boolean[] allowEventBeanType = new boolean[parameters.size()];
        boolean[] allowEventBeanCollType = new boolean[parameters.size()];
        ExprForge[] childEvalsEventBeanReturnTypesForges = new ExprForge[parameters.size()];
        boolean allConstants = true;
        for (ExprNode childNode : parameters) {
            if (!EnumMethodResolver.isEnumerationMethod(methodName, services.getClasspathImportServiceCompileTime()) && childNode instanceof ExprLambdaGoesNode) {
                throw new ExprValidationException("Unexpected lambda-expression encountered as parameter to UDF or static method '" + methodName + "'");
            }
            if (childNode instanceof ExprWildcard) {
                if (wildcardType == null || !allowWildcard) {
                    throw new ExprValidationException("Failed to resolve wildcard parameter to a given event type");
                }
                childForges[count] = new ExprEvalStreamNumUnd(0, wildcardType.getUnderlyingType());
                childEvalsEventBeanReturnTypesForges[count] = new ExprEvalStreamNumEvent(0);
                paramTypes[count] = wildcardType.getUnderlyingType();
                allowEventBeanType[count] = true;
                allConstants = false;
                count++;
                continue;
            }
            if (childNode instanceof ExprStreamUnderlyingNode) {
                ExprStreamUnderlyingNode und = (ExprStreamUnderlyingNode) childNode;
                TableMetaData tableMetadata = services.getTableCompileTimeResolver().resolveTableFromEventType(und.getEventType());
                if (tableMetadata == null) {
                    childForges[count] = childNode.getForge();
                    childEvalsEventBeanReturnTypesForges[count] = new ExprEvalStreamNumEvent(und.getStreamId());
                } else {
                    childForges[count] = new ExprEvalStreamTable(und.getStreamId(), und.getEventType().getUnderlyingType(), tableMetadata);
                    childEvalsEventBeanReturnTypesForges[count] = new ExprEvalStreamNumEventTable(und.getStreamId(), tableMetadata);
                }
                paramTypes[count] = childForges[count].getEvaluationType();
                allowEventBeanType[count] = true;
                allConstants = false;
                count++;
                continue;
            }
            if (childNode.getForge() instanceof ExprEnumerationForge) {
                ExprEnumerationForge enumeration = (ExprEnumerationForge) childNode.getForge();
                EventType eventType = enumeration.getEventTypeSingle(statementRawInfo, services);
                childForges[count] = childNode.getForge();
                paramTypes[count] = childForges[count].getEvaluationType();
                allConstants = false;
                if (eventType != null) {
                    childEvalsEventBeanReturnTypesForges[count] = new ExprEvalStreamNumEnumSingleForge(enumeration);
                    allowEventBeanType[count] = true;
                    count++;
                    continue;
                }
                EventType eventTypeColl = enumeration.getEventTypeCollection(statementRawInfo, services);
                if (eventTypeColl != null) {
                    childEvalsEventBeanReturnTypesForges[count] = new ExprEvalStreamNumEnumCollForge(enumeration);
                    allowEventBeanCollType[count] = true;
                    count++;
                    continue;
                }
            }

            paramTypes[count] = childNode.getForge().getEvaluationType();
            childForges[count] = childNode.getForge();
            count++;
            if (!(childNode.getForge().getForgeConstantType().isCompileTimeConstant())) {
                allConstants = false;
            }
        }

        // Try to resolve the method
        Method method;
        try {
            if (optionalClass != null) {
                method = services.getClasspathImportServiceCompileTime().resolveMethod(optionalClass, methodName, paramTypes, allowEventBeanType);
            } else {
                method = services.getClasspathImportServiceCompileTime().resolveMethodOverloadChecked(className, methodName, paramTypes, allowEventBeanType, allowEventBeanCollType);
            }
        } catch (Exception e) {
            throw exceptionHandler.handle(e);
        }

        // rewrite those evaluator that should return the event itself
        if (CollectionUtil.isAnySet(allowEventBeanType)) {
            for (int i = 0; i < parameters.size(); i++) {
                if (allowEventBeanType[i] && method.getParameterTypes()[i] == EventBean.class) {
                    childForges[i] = childEvalsEventBeanReturnTypesForges[i];
                }
            }
        }

        // rewrite those evaluators that should return the event collection
        if (CollectionUtil.isAnySet(allowEventBeanCollType)) {
            for (int i = 0; i < parameters.size(); i++) {
                if (allowEventBeanCollType[i] && method.getParameterTypes()[i] == Collection.class) {
                    childForges[i] = childEvalsEventBeanReturnTypesForges[i];
                }
            }
        }

        // add an evaluator if the method expects a context object
        if (!method.isVarArgs() && method.getParameterTypes().length > 0 &&
            method.getParameterTypes()[method.getParameterTypes().length - 1] == EPLMethodInvocationContext.class) {
            ExprEvalMethodContext node = new ExprEvalMethodContext(functionName);
            childForges = (ExprForge[]) CollectionUtil.arrayExpandAddSingle(childForges, node);
        }

        // handle varargs
        if (method.isVarArgs()) {
            // handle context parameter
            int numMethodParams = method.getParameterTypes().length;
            if (numMethodParams > 1 && method.getParameterTypes()[numMethodParams - 2] == EPLMethodInvocationContext.class) {
                ExprForge[] rewrittenForges = new ExprForge[childForges.length + 1];
                System.arraycopy(childForges, 0, rewrittenForges, 0, numMethodParams - 2);
                ExprEvalMethodContext node = new ExprEvalMethodContext(functionName);
                rewrittenForges[numMethodParams - 2] = node;
                System.arraycopy(childForges, numMethodParams - 2, rewrittenForges, numMethodParams - 1, childForges.length - (numMethodParams - 2));
                childForges = rewrittenForges;
            }

            childForges = ExprNodeUtilityMake.makeVarargArrayForges(method, childForges);
        }

        return new ExprNodeUtilMethodDesc(allConstants, childForges, method);
    }
}
