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
package com.espertech.esper.common.client.dataflow.util;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowOperatorParameterProviderContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpInitializeContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.settings.ClasspathImportException;
import com.espertech.esper.common.internal.util.ClassInstantiationException;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility for data flow parameter resolution that considers parameter providers that are passed in.
 */
public class DataFlowParameterResolution {
    /**
     * Resolve a number value by first looking at the parameter value provider and by using the evaluator if one was provided,
     * returning the default value if no value was found and no evaluator was provided.
     *
     * @param name              parameter name
     * @param optionalEvaluator evaluator
     * @param defaultValue      default
     * @param context           initialization context
     * @return value
     */
    public static Number resolveNumber(String name, ExprEvaluator optionalEvaluator, Number defaultValue, DataFlowOpInitializeContext context) {
        Number resolvedFromProvider = tryParameterProvider(name, context, Number.class);
        if (resolvedFromProvider != null) {
            return resolvedFromProvider;
        }
        if (optionalEvaluator == null) {
            return defaultValue;
        }
        Number value = (Number) optionalEvaluator.evaluate(null, true, context.getAgentInstanceContext());
        if (value == null) {
            throw new EPException("Parameter '" + name + "' is null and is expected to have a value");
        }
        return value;
    }

    /**
     * Resolve a string value by first looking at the parameter value provider and by using the evaluator if one was provided,
     * throwing an exception if no value was provided.
     *
     * @param name              parameter name
     * @param optionalEvaluator evaluator
     * @param context           initialization context
     * @return value
     * @throws EPException if no value was found
     */
    public static String resolveStringRequired(String name, ExprEvaluator optionalEvaluator, DataFlowOpInitializeContext context) {
        String resolvedFromProvider = tryParameterProvider(name, context, String.class);
        if (resolvedFromProvider != null) {
            return resolvedFromProvider;
        }
        if (optionalEvaluator == null) {
            throw new EPException("Parameter by name '" + name + "' has no value");
        }
        String value = (String) optionalEvaluator.evaluate(null, true, context.getAgentInstanceContext());
        if (value == null) {
            throw new EPException("Parameter by name '" + name + "' has a null value");
        }
        return value;
    }

    /**
     * Resolve a string value by first looking at the parameter value provider and by using the evaluator if one was provided
     * or returning null if no value was found.
     *
     * @param name              parameter name
     * @param optionalEvaluator evaluator
     * @param context           initialization context
     * @return value
     * @throws EPException if no value was found
     */
    public static String resolveStringOptional(String name, ExprEvaluator optionalEvaluator, DataFlowOpInitializeContext context) {
        String resolvedFromProvider = tryParameterProvider(name, context, String.class);
        if (resolvedFromProvider != null) {
            return resolvedFromProvider;
        }
        if (optionalEvaluator == null) {
            return null;
        }
        return (String) optionalEvaluator.evaluate(null, true, context.getAgentInstanceContext());
    }

    /**
     * Resolve a typed value by first looking at the parameter value provider and by using the evaluator if one was provided
     * or returning the provided default value if no value was found.
     *
     * @param name              parameter name
     * @param optionalEvaluator evaluator
     * @param context           initialization context
     * @param clazz             type of value
     * @param defaultValue      default value
     * @param <T>               the type of value
     * @return value
     */
    public static <T> T resolveWithDefault(String name, ExprEvaluator optionalEvaluator, T defaultValue, Class<T> clazz, DataFlowOpInitializeContext context) {
        T resolvedFromProvider = tryParameterProvider(name, context, clazz);
        if (resolvedFromProvider != null) {
            return resolvedFromProvider;
        }
        if (optionalEvaluator == null) {
            return defaultValue;
        }
        T result = (T) optionalEvaluator.evaluate(null, true, context.getAgentInstanceContext());
        if (result == null) {
            return defaultValue;
        }
        if (JavaClassHelper.getBoxedType(clazz) == JavaClassHelper.getBoxedType(result.getClass())) {
            return result;
        }
        if (JavaClassHelper.isSubclassOrImplementsInterface(result.getClass(), clazz)) {
            return result;
        }
        if (JavaClassHelper.isSubclassOrImplementsInterface(JavaClassHelper.getBoxedType(result.getClass()), Number.class)) {
            return (T) SimpleNumberCoercerFactory.getCoercer(result.getClass(), JavaClassHelper.getBoxedType(clazz)).coerceBoxed((Number) result);
        }
        return (T) result;
    }

    /**
     * Resolve an instance from a class-name map.
     *
     * @param name          parameter name
     * @param configuration map with key 'class' for the class name
     * @param clazz         expected interface or supertype
     * @param context       initialization context
     * @param <T>           type of value returned
     * @return instance
     */
    public static <T> T resolveOptionalInstance(String name, Map<String, Object> configuration, Class<T> clazz, DataFlowOpInitializeContext context) {

        T resolvedFromProvider = tryParameterProvider(name, context, clazz);
        if (resolvedFromProvider != null) {
            return resolvedFromProvider;
        }

        if (configuration == null) {
            return null;
        }

        String className = (String) configuration.get("class");
        if (className == null) {
            throw new EPException("Failed to find 'class' parameter for parameter '" + name + "'");
        }

        Class theClass;
        try {
            theClass = context.getAgentInstanceContext().getClasspathImportServiceRuntime().resolveClass(className, false);
        } catch (ClasspathImportException e) {
            throw new EPException("Failed to find class for parameter '" + name + "': " + e.getMessage(), e);
        }

        try {
            return (T) JavaClassHelper.instantiate(clazz, theClass);
        } catch (ClassInstantiationException ex) {
            throw new EPException("Failed to instantiate class for parameter '" + name + "': " + ex.getMessage(), ex);
        }
    }

    /**
     * Resolve all entries in the map by first looking at the parameter value provider and by using the evaluator if one was provided
     * or returning the provided value if no evaluator was found.
     *
     * @param name    parameter name
     * @param evals   map of properties with either evaluator or constant type
     * @param context initialization context
     * @return value
     */
    public static Map<String, Object> resolveMap(String name, Map<String, Object> evals, DataFlowOpInitializeContext context) {
        if (evals == null) {
            return null;
        }
        if (evals.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : evals.entrySet()) {
            if (entry.getValue() instanceof ExprEvaluator) {
                try {
                    map.put(name, ((ExprEvaluator) entry.getValue()).evaluate(null, true, context.getAgentInstanceContext()));
                } catch (Throwable t) {
                    throw new EPException("Failed to evaluate value for parameter '" + name + "' for entry key '" + entry.getKey() + "': " + t.getMessage(), t);
                }
            } else {
                map.put(name, entry.getValue());
            }
        }
        return map;
    }

    private static <T> T tryParameterProvider(String name, DataFlowOpInitializeContext context, Class<T> clazz) throws EPException {
        if (context.getAdditionalParameters() != null && context.getAdditionalParameters().containsKey(name)) {
            return (T) context.getAdditionalParameters().get(name);
        }
        if (context.getParameterProvider() == null) {
            return null;
        }
        EPDataFlowOperatorParameterProviderContext ctx = new EPDataFlowOperatorParameterProviderContext(context, name);
        Object value = context.getParameterProvider().provide(ctx);
        if (value == null) {
            return null;
        }
        if (JavaClassHelper.isAssignmentCompatible(value.getClass(), clazz)) {
            return (T) value;
        }
        throw new EPException("Parameter provider provided an unexpected object for parameter '" + name + "' of type '" + value.getClass().getName() + "', expected type '" + clazz.getName() + "'");
    }
}
