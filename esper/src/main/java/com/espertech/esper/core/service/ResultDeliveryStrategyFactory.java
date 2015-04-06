/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.core.service;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPSubscriberException;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.TypeWidener;
import com.espertech.esper.util.TypeWidenerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Map;

/**
 * Factory for creating a dispatch strategy based on the subscriber object
 * and the columns produced by a select-clause.
 */
public class ResultDeliveryStrategyFactory
{
    /**
     * Creates a strategy implementation that indicates to subscribers
     * the statement results based on the select-clause columns.
     * @param subscriber to indicate to
     * @param selectClauseTypes are the types of each column in the select clause
     * @param selectClauseColumns the names of each column in the select clause
     * @return strategy for dispatching naturals
     * @throws EPSubscriberException if the subscriber is invalid
     */
    public static ResultDeliveryStrategy create(String statementName, Object subscriber, String methodName,
                                                        Class[] selectClauseTypes,
                                                        String[] selectClauseColumns)
            throws EPSubscriberException
    {
        if (selectClauseTypes == null) {
            selectClauseTypes = new Class[0];
            selectClauseColumns = new String[0];
        }

        if (methodName == null) {
            methodName = "update";
        }

        // Locate update methods
        Method subscriptionMethod = null;
        ArrayList<Method> updateMethods = new ArrayList<Method>();
        for (Method method : subscriber.getClass().getMethods())
        {
            if ((method.getName().equals(methodName)) &&
                (Modifier.isPublic(method.getModifiers())))
            {
                updateMethods.add(method);
            }
        }

        // none found
        if (updateMethods.size() == 0)
        {
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
        for (Method method : updateMethods)
        {
            Class[] parameters = method.getParameterTypes();
            if (parameters.length == selectClauseTypes.length) {
                boolean fits = true;
                for (int i = 0; i < parameters.length; i++) {
                    if ((selectClauseTypes[i] != null) && (selectClauseTypes[i] != parameters[i])) {
                        fits = false;
                        break;
                    }
                }
                if (fits) {
                    subscriptionMethod = method;
                    break;
                }
            }
        }

        // when not yet resolved, find an exact-matching method with boxing/unboxing
        if (subscriptionMethod == null) {
            for (Method method : updateMethods)
            {
                Class[] parameters = method.getParameterTypes();
                if (parameters.length == selectClauseTypes.length) {
                    boolean fits = true;
                    for (int i = 0; i < parameters.length; i++) {
                        Class boxedExpressionType = JavaClassHelper.getBoxedType(selectClauseTypes[i]);
                        Class boxedParameterType = JavaClassHelper.getBoxedType(parameters[i]);
                        if ((boxedExpressionType != null) && (boxedExpressionType != boxedParameterType)) {
                            fits = false;
                            break;
                        }
                    }
                    if (fits) {
                        subscriptionMethod = method;
                        break;
                    }
                }
            }
        }

        // when not yet resolved, find assignment-compatible methods that may require widening (including Integer to Long etc.)
        boolean checkWidening = false;
        if (subscriptionMethod == null) {
            for (Method method : updateMethods) {
                Class[] parameters = method.getParameterTypes();
                if (parameters.length == selectClauseTypes.length) {
                    boolean fits = true;
                    for (int i = 0; i < parameters.length; i++) {
                        Class boxedExpressionType = JavaClassHelper.getBoxedType(selectClauseTypes[i]);
                        Class boxedParameterType = JavaClassHelper.getBoxedType(parameters[i]);
                        if ((boxedExpressionType != null) && (!JavaClassHelper.isAssignmentCompatible(boxedExpressionType, boxedParameterType))) {
                            fits = false;
                            break;
                        }
                    }
                    if (fits) {
                        subscriptionMethod = method;
                        checkWidening = true;
                        break;
                    }
                }
            }
        }

        // when not yet resolved, find first-fit wildcard method
        if (subscriptionMethod == null) {
            for (Method method : updateMethods)
            {
                Class[] parameters = method.getParameterTypes();
                if ((parameters.length == 1) && (parameters[0] == Map.class))
                {
                    isSingleRowMap = true;
                    subscriptionMethod = method;
                    break;
                }
                if ((parameters.length == 1) && (parameters[0] == Object[].class))
                {
                    isSingleRowObjectArr = true;
                    subscriptionMethod = method;
                    break;
                }

                if ((parameters.length == 2) && (parameters[0] == Map[].class) && (parameters[1] == Map[].class))
                {
                    subscriptionMethod = method;
                    isMapArrayDelivery = true;
                    break;
                }
                if ((parameters.length == 2) && (parameters[0] == Object[][].class) && (parameters[1] == Object[][].class))
                {
                    subscriptionMethod = method;
                    isObjectArrayDelivery = true;
                    break;
                }
                // Handle uniform underlying or column type array dispatch
                if ((parameters.length == 2) && (parameters[0].equals(parameters[1])) && (parameters[0].isArray())
                        && (selectClauseTypes.length == 1))
                {
                    Class componentType = parameters[0].getComponentType();
                    if (JavaClassHelper.isAssignmentCompatible(selectClauseTypes[0], componentType))
                    {
                        subscriptionMethod = method;
                        isTypeArrayDelivery = true;
                        break;
                    }
                }

                if ((parameters.length == 0) && (selectClauseTypes.length == 1) && (selectClauseTypes[0] == null)) {
                    subscriptionMethod = method;
                }
            }
        }

        if (subscriptionMethod == null)
        {
            if (updateMethods.size() > 1)
            {
                String parametersDesc = JavaClassHelper.getParameterAsString(selectClauseTypes);
                String message = "No suitable subscriber method named 'update' found, expecting a method that takes " +
                        selectClauseTypes.length + " parameter of type " + parametersDesc;
                throw new EPSubscriberException(message);
            }
            else
            {
                Class[] parameters = updateMethods.get(0).getParameterTypes();
                String parametersDesc = JavaClassHelper.getParameterAsString(selectClauseTypes);
                if (parameters.length != selectClauseTypes.length)
                {
                    if (selectClauseTypes.length > 0) {
                        String message = "No suitable subscriber method named 'update' found, expecting a method that takes " +
                                selectClauseTypes.length + " parameter of type " + parametersDesc;
                        throw new EPSubscriberException(message);
                    }
                    else {
                        String message = "No suitable subscriber method named 'update' found, expecting a method that takes no parameters";
                        throw new EPSubscriberException(message);
                    }
                }
                for (int i = 0; i < parameters.length; i++)
                {
                    Class boxedExpressionType = JavaClassHelper.getBoxedType(selectClauseTypes[i]);
                    Class boxedParameterType = JavaClassHelper.getBoxedType(parameters[i]);
                    if ((boxedExpressionType != null) && (!JavaClassHelper.isAssignmentCompatible(boxedExpressionType, boxedParameterType)))
                    {
                        String message = "Subscriber method named 'update' for parameter number " + (i + 1) + " is not assignable, " +
                                "expecting type '" + JavaClassHelper.getParameterAsString(selectClauseTypes[i]) + "' but found type '"
                                + JavaClassHelper.getParameterAsString(parameters[i]) + "'";
                        throw new EPSubscriberException(message);
                    }
                }
            }
        }

        if (isMapArrayDelivery)
        {
            return new ResultDeliveryStrategyMap(statementName, subscriber, subscriptionMethod, selectClauseColumns);
        }
        else if (isObjectArrayDelivery)
        {
            return new ResultDeliveryStrategyObjectArr(statementName, subscriber, subscriptionMethod);
        }
        else if (isTypeArrayDelivery)
        {
            return new ResultDeliveryStrategyTypeArr(statementName, subscriber, subscriptionMethod);
        }

        // Try to find the "start", "end" and "updateRStream" methods
        Method startMethod = null;
        Method endMethod = null;
        Method rStreamMethod = null;
        try {
            startMethod = subscriber.getClass().getMethod("updateStart", int.class, int.class);
        }
        catch (NoSuchMethodException e) {
            // expected
        }

        try {
            endMethod = subscriber.getClass().getMethod("updateEnd");
        }
        catch (NoSuchMethodException e) {
            // expected
        }

        try {
            rStreamMethod = subscriber.getClass().getMethod("updateRStream", subscriptionMethod.getParameterTypes());
        }
        catch (NoSuchMethodException e) {
            // expected
        }

        DeliveryConvertor convertor;
        if (isSingleRowMap)
        {
            convertor = new DeliveryConvertorMap(selectClauseColumns);
        }
        else if (isSingleRowObjectArr)
        {
            convertor = new DeliveryConvertorObjectArr();
        }
        else
        {
            if (checkWidening) {
                convertor = determineWideningDeliveryConvertor(selectClauseTypes, subscriptionMethod.getParameterTypes(), subscriptionMethod);
            }
            else {
                convertor = DeliveryConvertorNull.INSTANCE;
            }
        }

        return new ResultDeliveryStrategyImpl(statementName, subscriber, convertor, subscriptionMethod, startMethod, endMethod, rStreamMethod);
    }

    private static DeliveryConvertor determineWideningDeliveryConvertor(Class[] selectClauseTypes, Class[] parameterTypes, Method method) {
        boolean needWidener = false;
        for (int i = 0; i < selectClauseTypes.length; i++) {
            TypeWidener optionalWidener = getWidener(i, selectClauseTypes[i], parameterTypes[i], method);
            if (optionalWidener != null) {
                needWidener = true;
                break;
            }
        }
        if (!needWidener) {
            return DeliveryConvertorNull.INSTANCE;
        }
        TypeWidener[] wideners = new TypeWidener[selectClauseTypes.length];
        for (int i = 0; i < selectClauseTypes.length; i++) {
            wideners[i] = getWidener(i, selectClauseTypes[i], parameterTypes[i], method);
        }
        return new DeliveryConvertorWidener(wideners);
    }

    private static TypeWidener getWidener(int columnNum, Class selectClauseType, Class parameterType, Method method) {
        if (selectClauseType == null || parameterType == null) {
            return null;
        }
        if (selectClauseType == parameterType) {
            return null;
        }
        try {
            return TypeWidenerFactory.getCheckPropertyAssignType("Select-Clause Column " + columnNum, selectClauseType, parameterType, "Method Parameter " + columnNum);
        }
        catch (ExprValidationException e) {
            throw new EPException("Unexpected exception assigning select clause columns to subscriber method " + method + ": " + e.getMessage(), e);
        }
    }


}
