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
package com.espertech.esper.common.internal.epl.historical.method.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.epl.historical.method.poll.*;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;

import java.lang.reflect.Method;

public class PollExecStrategyPlanner {
    public static Pair<MethodTargetStrategyForge, MethodConversionStrategyForge> plan(MethodPollingViewableMeta metadata, Method targetMethod, EventType eventType) {
        MethodTargetStrategyForge target = null;
        MethodConversionStrategyForge conversion = null;

        // class-based evaluation
        if (metadata.getMethodProviderClass() != null) {
            // Construct polling strategy as a method invocation
            MethodPollingExecStrategyEnum strategy = metadata.getStrategy();
            VariableMetaData variable = metadata.getVariable();
            if (variable == null) {
                target = new MethodTargetStrategyStaticMethodForge(metadata.getMethodProviderClass(), targetMethod);
            } else {
                target = new MethodTargetStrategyVariableForge(variable, targetMethod);
            }

            if (metadata.getEventTypeEventBeanArray() != null) {
                conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyEventBeans.EPTYPE);
            } else if (metadata.getOptionalMapType() != null) {
                if (targetMethod.getReturnType().isArray()) {
                    conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyArrayMap.EPTYPE);
                } else if (metadata.isCollection()) {
                    conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyCollectionMap.EPTYPE);
                } else if (metadata.isIterator()) {
                    conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyIteratorMap.EPTYPE);
                } else {
                    conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyPlainMap.EPTYPE);
                }
            } else if (metadata.getOptionalOaType() != null) {
                if (targetMethod.getReturnType() == Object[][].class) {
                    conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyArrayOA.EPTYPE);
                } else if (metadata.isCollection()) {
                    conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyCollectionOA.EPTYPE);
                } else if (metadata.isIterator()) {
                    conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyIteratorOA.EPTYPE);
                } else {
                    conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyPlainOA.EPTYPE);
                }
            } else {
                if (targetMethod.getReturnType().isArray()) {
                    conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyArrayPOJO.EPTYPE);
                } else if (metadata.isCollection()) {
                    conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyCollectionPOJO.EPTYPE);
                } else if (metadata.isIterator()) {
                    conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyIteratorPOJO.EPTYPE);
                } else {
                    conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyPlainPOJO.EPTYPE);
                }
            }
        } else {
            target = new MethodTargetStrategyScriptForge(metadata.getScriptExpression());
            conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyScript.EPTYPE);
        }
        return new Pair<>(target, conversion);
    }
}
