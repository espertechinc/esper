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
package com.espertech.esper.common.client.hook.enummethod;

import com.espertech.esper.common.internal.rettype.EPType;

import java.security.InvalidParameterException;
import java.util.function.Function;

/**
 * Provides information about the public static method that implements the logic for the enumeration method.
 */
public class EnumMethodModeStaticMethod implements EnumMethodMode {
    private Class stateClass;
    private Class serviceClass;
    private String methodName;
    private EPType returnType;
    private boolean earlyExit;
    private Function<EnumMethodLambdaParameterDescriptor, EnumMethodLambdaParameterType> lambdaParameters = new Function<EnumMethodLambdaParameterDescriptor, EnumMethodLambdaParameterType>() {
        public EnumMethodLambdaParameterType apply(EnumMethodLambdaParameterDescriptor enumMethodLambdaParameterDescriptor) {
            return EnumMethodLambdaParameterTypeValue.INSTANCE;
        }
    };

    /**
     * Ctor.
     */
    public EnumMethodModeStaticMethod() {
    }

    /**
     * Ctor.
     * @param stateClass class
     * @param serviceClass class
     * @param methodName method
     * @param returnType return type
     * @param earlyExit early-exit indicator, when the compiler should generate code to check for early-exit by calling the "completed" method of the state
     */
    public EnumMethodModeStaticMethod(Class stateClass, Class serviceClass, String methodName, EPType returnType, boolean earlyExit) {
        if (stateClass == null) {
            throw new InvalidParameterException("Required parameter state-class is not provided");
        }
        if (serviceClass == null) {
            throw new InvalidParameterException("Required parameter service-class is not provided");
        }
        if (methodName == null) {
            throw new InvalidParameterException("Required parameter method-name is not provided");
        }
        if (returnType == null) {
            throw new InvalidParameterException("Required parameter return-type is not provided");
        }
        this.stateClass = stateClass;
        this.serviceClass = serviceClass;
        this.methodName = methodName;
        this.returnType = returnType;
        this.earlyExit = earlyExit;
    }

    /**
     * Returns the method name of the public static processing method provided by the service class
     * @return method
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Returns the class providing state
     * @return state class
     */
    public Class getStateClass() {
        return stateClass;
    }

    /**
     * Returns the class providing the processing method
     * @return class providing the public static processing method
     */
    public Class getServiceClass() {
        return serviceClass;
    }

    /**
     * Returns the return type of the enumeration method.
     * @return type
     */
    public EPType getReturnType() {
        return returnType;
    }

    /**
     * Returns indicator whether the compiler should consider the
     * enumeration method as doing early-exit checking
     * @return early-exit indicator
     */
    public boolean isEarlyExit() {
        return earlyExit;
    }

    /**
     * Sets the function that determines, for each lambda parameter, the lambda parameter type.
     * @param lambdaParameters function
     */
    public void setLambdaParameters(Function<EnumMethodLambdaParameterDescriptor, EnumMethodLambdaParameterType> lambdaParameters) {
        this.lambdaParameters = lambdaParameters;
    }

    /**
     * Returns the function that determines, for each lambda parameter, the lambda parameter type.
     * <p>
     *     This function defaults to a function that assumes a value-type for all lambda parameters.
     * </p>
     * @return function
     */
    public Function<EnumMethodLambdaParameterDescriptor, EnumMethodLambdaParameterType> getLambdaParameters() {
        return lambdaParameters;
    }

    /**
     * Sets the class providing state
     * @param stateClass state class
     */
    public void setStateClass(Class stateClass) {
        this.stateClass = stateClass;
    }

    /**
     * Sets the class providing the processing method
     * @param serviceClass class providing the public static processing method
     */
    public void setServiceClass(Class serviceClass) {
        this.serviceClass = serviceClass;
    }

    /**
     * Sets the method name of the public static processing method provided by the service class
     * @param methodName method name
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    /**
     * Sets the return type of the enumeration method.
     * @param returnType return type
     */
    public void setReturnType(EPType returnType) {
        this.returnType = returnType;
    }

    /**
     * Sets the indicator whether the compiler should consider the
     * enumeration method as doing early-exit checking
     * @param earlyExit early-exit indicator
     */
    public void setEarlyExit(boolean earlyExit) {
        this.earlyExit = earlyExit;
    }
}
