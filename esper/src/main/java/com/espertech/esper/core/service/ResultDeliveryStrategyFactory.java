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
package com.espertech.esper.core.service;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPSubscriberException;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.TypeWidener;
import com.espertech.esper.util.TypeWidenerException;
import com.espertech.esper.util.TypeWidenerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Factory for creating a dispatch strategy based on the subscriber object
 * and the columns produced by a select-clause.
 */
public class ResultDeliveryStrategyFactory {
    private static final Comparator<? super Method> METHOD_PREFERECE_COMPARATOR = new Comparator<Method>() {
        public int compare(Method o1, Method o2) {
            int v1 = value(o1);
            int v2 = value(o2);
            return v1 > v2 ? 1 : (v1 == v2 ? 0 : -1);
        }

        private int value(Method m) {
            return isFirstParameterEPStatement(m) ? 0 : 1;
        }
    };

    /**
     * Creates a strategy implementation that indicates to subscribers
     * the statement results based on the select-clause columns.
     *
     * @param subscriber          to indicate to
     * @param selectClauseTypes   are the types of each column in the select clause
     * @param selectClauseColumns the names of each column in the select clause
     * @param statement           statement
     * @param methodName          method name
     * @param engineURI           engine URI
     * @param engineImportService engine imports
     * @return strategy for dispatching naturals
     * @throws EPSubscriberException if the subscriber is invalid
     */
    public static ResultDeliveryStrategy create(EPStatement statement, Object subscriber, String methodName,
                                                Class[] selectClauseTypes,
                                                String[] selectClauseColumns,
                                                String engineURI,
                                                EngineImportService engineImportService)
            throws EPSubscriberException {
        if (selectClauseTypes == null) {
            selectClauseTypes = new Class[0];
            selectClauseColumns = new String[0];
        }

        if (methodName == null) {
            methodName = "update";
        }

        // sort by presence of EPStatement as the first parameter
        List<Method> sorted = Arrays.asList(subscriber.getClass().getMethods());
        Collections.sort(sorted, METHOD_PREFERECE_COMPARATOR);

        // Locate update methods
        Method subscriptionMethod = null;
        Map<Method, Class[]> updateMethods = new LinkedHashMap<Method, Class[]>();
        for (Method method : sorted) {
            if ((method.getName().equals(methodName)) &&
                    (Modifier.isPublic(method.getModifiers()))) {
                // Determine parameter types without EPStatement (the normalized parameters)
                Class[] normalizedParameters = getMethodParameterTypesWithoutEPStatement(method);
                updateMethods.put(method, normalizedParameters);
            }
        }

        // none found
        if (updateMethods.size() == 0) {
            String message = "Subscriber object does not provide a public method by name 'update'";
            throw new EPSubscriberException(message);
        }

        // match to parameters
        boolean isMapArrayDelivery = false;
        boolean isObjectArrayDelivery = false;
        boolean isSingleRowMap = false;
        boolean isSingleRowObjectArr = false;
        boolean isTypeArrayDelivery = false;

        // find an exact-matching method: no conversions and not even unboxing/boxing
        for (Map.Entry<Method, Class[]> methodNormParameterEntry : updateMethods.entrySet()) {
            Class[] normalized = methodNormParameterEntry.getValue();
            if (normalized.length == selectClauseTypes.length) {
                boolean fits = true;
                for (int i = 0; i < normalized.length; i++) {
                    if ((selectClauseTypes[i] != null) && (selectClauseTypes[i] != normalized[i])) {
                        fits = false;
                        break;
                    }
                }
                if (fits) {
                    subscriptionMethod = methodNormParameterEntry.getKey();
                    break;
                }
            }
        }

        // when not yet resolved, find an exact-matching method with boxing/unboxing
        if (subscriptionMethod == null) {
            for (Map.Entry<Method, Class[]> methodNormParameterEntry : updateMethods.entrySet()) {
                Class[] normalized = methodNormParameterEntry.getValue();
                if (normalized.length == selectClauseTypes.length) {
                    boolean fits = true;
                    for (int i = 0; i < normalized.length; i++) {
                        Class boxedExpressionType = JavaClassHelper.getBoxedType(selectClauseTypes[i]);
                        Class boxedParameterType = JavaClassHelper.getBoxedType(normalized[i]);
                        if ((boxedExpressionType != null) && (boxedExpressionType != boxedParameterType)) {
                            fits = false;
                            break;
                        }
                    }
                    if (fits) {
                        subscriptionMethod = methodNormParameterEntry.getKey();
                        break;
                    }
                }
            }
        }

        // when not yet resolved, find assignment-compatible methods that may require widening (including Integer to Long etc.)
        boolean checkWidening = false;
        if (subscriptionMethod == null) {
            for (Map.Entry<Method, Class[]> methodNormParameterEntry : updateMethods.entrySet()) {
                Class[] normalized = methodNormParameterEntry.getValue();
                if (normalized.length == selectClauseTypes.length) {
                    boolean fits = true;
                    for (int i = 0; i < normalized.length; i++) {
                        Class boxedExpressionType = JavaClassHelper.getBoxedType(selectClauseTypes[i]);
                        Class boxedParameterType = JavaClassHelper.getBoxedType(normalized[i]);
                        if ((boxedExpressionType != null) && (!JavaClassHelper.isAssignmentCompatible(boxedExpressionType, boxedParameterType))) {
                            fits = false;
                            break;
                        }
                    }
                    if (fits) {
                        subscriptionMethod = methodNormParameterEntry.getKey();
                        checkWidening = true;
                        break;
                    }
                }
            }
        }

        // when not yet resolved, find first-fit wildcard method
        if (subscriptionMethod == null) {
            for (Map.Entry<Method, Class[]> methodNormParameterEntry : updateMethods.entrySet()) {
                Class[] normalized = methodNormParameterEntry.getValue();
                if ((normalized.length == 1) && (normalized[0] == Map.class)) {
                    isSingleRowMap = true;
                    subscriptionMethod = methodNormParameterEntry.getKey();
                    break;
                }
                if ((normalized.length == 1) && (normalized[0] == Object[].class)) {
                    isSingleRowObjectArr = true;
                    subscriptionMethod = methodNormParameterEntry.getKey();
                    break;
                }

                if ((normalized.length == 2) && (normalized[0] == Map[].class) && (normalized[1] == Map[].class)) {
                    subscriptionMethod = methodNormParameterEntry.getKey();
                    isMapArrayDelivery = true;
                    break;
                }
                if ((normalized.length == 2) && (normalized[0] == Object[][].class) && (normalized[1] == Object[][].class)) {
                    subscriptionMethod = methodNormParameterEntry.getKey();
                    isObjectArrayDelivery = true;
                    break;
                }
                // Handle uniform underlying or column type array dispatch
                if ((normalized.length == 2) && (normalized[0].equals(normalized[1])) && (normalized[0].isArray())
                        && (selectClauseTypes.length == 1)) {
                    Class componentType = normalized[0].getComponentType();
                    if (JavaClassHelper.isAssignmentCompatible(selectClauseTypes[0], componentType)) {
                        subscriptionMethod = methodNormParameterEntry.getKey();
                        isTypeArrayDelivery = true;
                        break;
                    }
                }

                if ((normalized.length == 0) && (selectClauseTypes.length == 1) && (selectClauseTypes[0] == null)) {
                    subscriptionMethod = methodNormParameterEntry.getKey();
                }
            }
        }

        if (subscriptionMethod == null) {
            if (updateMethods.size() > 1) {
                String parametersDesc = JavaClassHelper.getParameterAsString(selectClauseTypes);
                String message = "No suitable subscriber method named 'update' found, expecting a method that takes " +
                        selectClauseTypes.length + " parameter of type " + parametersDesc;
                throw new EPSubscriberException(message);
            } else {
                Map.Entry<Method, Class[]> firstUpdateMethod = updateMethods.entrySet().iterator().next();
                Class[] parametersNormalized = firstUpdateMethod.getValue();
                String parametersDescNormalized = JavaClassHelper.getParameterAsString(selectClauseTypes);
                if (parametersNormalized.length != selectClauseTypes.length) {
                    if (selectClauseTypes.length > 0) {
                        String message = "No suitable subscriber method named 'update' found, expecting a method that takes " +
                                selectClauseTypes.length + " parameter of type " + parametersDescNormalized;
                        throw new EPSubscriberException(message);
                    } else {
                        String message = "No suitable subscriber method named 'update' found, expecting a method that takes no parameters";
                        throw new EPSubscriberException(message);
                    }
                }
                for (int i = 0; i < parametersNormalized.length; i++) {
                    Class boxedExpressionType = JavaClassHelper.getBoxedType(selectClauseTypes[i]);
                    Class boxedParameterType = JavaClassHelper.getBoxedType(parametersNormalized[i]);
                    if ((boxedExpressionType != null) && (!JavaClassHelper.isAssignmentCompatible(boxedExpressionType, boxedParameterType))) {
                        String message = "Subscriber method named 'update' for parameter number " + (i + 1) + " is not assignable, " +
                                "expecting type '" + JavaClassHelper.getParameterAsString(selectClauseTypes[i]) + "' but found type '"
                                + JavaClassHelper.getParameterAsString(parametersNormalized[i]) + "'";
                        throw new EPSubscriberException(message);
                    }
                }
            }
        }

        // Invalid if there is a another footprint for the subscription method that does not include EPStatement if present
        boolean firstParameterIsEPStatement = isFirstParameterEPStatement(subscriptionMethod);
        if (isMapArrayDelivery) {
            return firstParameterIsEPStatement ?
                    new ResultDeliveryStrategyMapWStmt(statement, subscriber, subscriptionMethod, selectClauseColumns, engineImportService) :
                    new ResultDeliveryStrategyMap(statement, subscriber, subscriptionMethod, selectClauseColumns, engineImportService);
        } else if (isObjectArrayDelivery) {
            return firstParameterIsEPStatement ?
                    new ResultDeliveryStrategyObjectArrWStmt(statement, subscriber, subscriptionMethod, engineImportService) :
                    new ResultDeliveryStrategyObjectArr(statement, subscriber, subscriptionMethod, engineImportService);
        } else if (isTypeArrayDelivery) {
            return firstParameterIsEPStatement ?
                    new ResultDeliveryStrategyTypeArrWStmt(statement, subscriber, subscriptionMethod, subscriptionMethod.getParameterTypes()[1].getComponentType(), engineImportService) :
                    new ResultDeliveryStrategyTypeArr(statement, subscriber, subscriptionMethod, subscriptionMethod.getParameterTypes()[0].getComponentType(), engineImportService);
        }

        // Try to find the "start", "end" and "updateRStream" methods
        Method startMethod = null;
        Method endMethod = null;
        Method rStreamMethod = null;
        try {
            startMethod = subscriber.getClass().getMethod("updateStart", EPStatement.class, int.class, int.class);
        } catch (NoSuchMethodException e) {
            try {
                startMethod = subscriber.getClass().getMethod("updateStart", int.class, int.class);
            } catch (NoSuchMethodException ex) {
                // expected
            }
        }

        try {
            endMethod = subscriber.getClass().getMethod("updateEnd", EPStatement.class);
        } catch (NoSuchMethodException e) {
            try {
                endMethod = subscriber.getClass().getMethod("updateEnd");
            } catch (NoSuchMethodException ex) {
                // expected
            }
        }

        try {
            // must be exactly the same footprint (may include EPStatement), since delivery convertor used for both
            rStreamMethod = subscriber.getClass().getMethod("updateRStream", subscriptionMethod.getParameterTypes());
        } catch (NoSuchMethodException e) {
            // we don't have an "updateRStream" expected, make sure there isn't one with/without EPStatement
            if (isFirstParameterEPStatement(subscriptionMethod)) {
                Class[] classes = updateMethods.get(subscriptionMethod);
                validateNonMatchUpdateRStream(subscriber, classes);
            } else {
                Class[] classes = new Class[subscriptionMethod.getParameterTypes().length + 1];
                classes[0] = EPStatement.class;
                System.arraycopy(subscriptionMethod.getParameterTypes(), 0, classes, 1, subscriptionMethod.getParameterTypes().length);
                validateNonMatchUpdateRStream(subscriber, classes);
            }
        }

        DeliveryConvertor convertor;
        if (isSingleRowMap) {
            convertor = firstParameterIsEPStatement ?
                    new DeliveryConvertorMapWStatement(selectClauseColumns, statement) :
                    new DeliveryConvertorMap(selectClauseColumns);
        } else if (isSingleRowObjectArr) {
            convertor = firstParameterIsEPStatement ?
                    new DeliveryConvertorObjectArrWStatement(statement) :
                    DeliveryConvertorObjectArr.INSTANCE;
        } else {
            if (checkWidening) {
                Class[] normalizedParameters = updateMethods.get(subscriptionMethod);
                convertor = determineWideningDeliveryConvertor(firstParameterIsEPStatement, statement, selectClauseTypes, normalizedParameters, subscriptionMethod, engineURI);
            } else {
                convertor = firstParameterIsEPStatement ?
                        new DeliveryConvertorNullWStatement(statement) :
                        DeliveryConvertorNull.INSTANCE;
            }
        }

        return new ResultDeliveryStrategyImpl(statement, subscriber, convertor, subscriptionMethod, startMethod, endMethod, rStreamMethod, engineImportService);
    }

    private static DeliveryConvertor determineWideningDeliveryConvertor(boolean firstParameterIsEPStatement, EPStatement statement, Class[] selectClauseTypes, Class[] parameterTypes, Method method, String engineURI) {
        boolean needWidener = false;
        for (int i = 0; i < selectClauseTypes.length; i++) {
            TypeWidener optionalWidener = getWidener(i, selectClauseTypes[i], parameterTypes[i], method, statement.getName(), engineURI);
            if (optionalWidener != null) {
                needWidener = true;
                break;
            }
        }
        if (!needWidener) {
            return firstParameterIsEPStatement ?
                    new DeliveryConvertorNullWStatement(statement) :
                    DeliveryConvertorNull.INSTANCE;
        }
        TypeWidener[] wideners = new TypeWidener[selectClauseTypes.length];
        for (int i = 0; i < selectClauseTypes.length; i++) {
            wideners[i] = getWidener(i, selectClauseTypes[i], parameterTypes[i], method, statement.getName(), engineURI);
        }
        return firstParameterIsEPStatement ?
                new DeliveryConvertorWidenerWStatement(wideners, statement) :
                new DeliveryConvertorWidener(wideners);
    }

    private static TypeWidener getWidener(int columnNum, Class selectClauseType, Class parameterType, Method method, String statementName, String engineURI) {
        if (selectClauseType == null || parameterType == null) {
            return null;
        }
        if (selectClauseType == parameterType) {
            return null;
        }
        try {
            return TypeWidenerFactory.getCheckPropertyAssignType("Select-Clause Column " + columnNum, selectClauseType, parameterType, "Method Parameter " + columnNum, false, null, statementName, engineURI);
        } catch (TypeWidenerException e) {
            throw new EPException("Unexpected exception assigning select clause columns to subscriber method " + method + ": " + e.getMessage(), e);
        }
    }

    private static void validateNonMatchUpdateRStream(Object subscriber, Class[] classes) {
        try {
            Method m = subscriber.getClass().getMethod("updateRStream", classes);
            if (m != null) {
                throw new EPSubscriberException("Subscriber 'updateRStream' method footprint must match 'update' method footprint");
            }
        } catch (NoSuchMethodException ex) {
            // expected
        }
    }

    private static Class[] getMethodParameterTypesWithoutEPStatement(Method method) {
        Class[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0 || parameterTypes[0] != EPStatement.class) {
            return parameterTypes;
        }
        Class[] normalized = new Class[parameterTypes.length - 1];
        System.arraycopy(parameterTypes, 1, normalized, 0, parameterTypes.length - 1);
        return normalized;
    }

    private static boolean isFirstParameterEPStatement(Method method) {
        return method.getParameterTypes().length > 0 && method.getParameterTypes()[0] == EPStatement.class;
    }
}
