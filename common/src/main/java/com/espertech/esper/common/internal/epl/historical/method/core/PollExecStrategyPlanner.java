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
                conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyEventBeans.class);
            } else if (metadata.getOptionalMapType() != null) {
                if (targetMethod.getReturnType().isArray()) {
                    conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyArrayMap.class);
                } else if (metadata.isCollection()) {
                    conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyCollectionMap.class);
                } else if (metadata.isIterator()) {
                    conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyIteratorMap.class);
                } else {
                    conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyPlainMap.class);
                }
            } else if (metadata.getOptionalOaType() != null) {
                if (targetMethod.getReturnType() == Object[][].class) {
                    conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyArrayOA.class);
                } else if (metadata.isCollection()) {
                    conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyCollectionOA.class);
                } else if (metadata.isIterator()) {
                    conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyIteratorOA.class);
                } else {
                    conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyPlainOA.class);
                }
            } else {
                if (targetMethod.getReturnType().isArray()) {
                    conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyArrayPOJO.class);
                } else if (metadata.isCollection()) {
                    conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyCollectionPOJO.class);
                } else if (metadata.isIterator()) {
                    conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyIteratorPOJO.class);
                } else {
                    conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyPlainPOJO.class);
                }
            }
        } else {
            target = new MethodTargetStrategyScriptForge(metadata.getScriptExpression());
            conversion = new MethodConversionStrategyForge(eventType, MethodConversionStrategyScript.class);
        }
        return new Pair<>(target, conversion);
    }
}
