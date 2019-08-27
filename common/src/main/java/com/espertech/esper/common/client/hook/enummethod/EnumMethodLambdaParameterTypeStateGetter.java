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

/**
 * A lambda parameter that assumes a value of the given type and that originates from the state object
 * by calling the provided getter-method.
 */
public class EnumMethodLambdaParameterTypeStateGetter implements EnumMethodLambdaParameterType {
    private final Class type;
    private final String getterMethodName;

    /**
     * Ctor.
     * @param type lambda parameter-assumed value type
     * @param getterMethodName getter method name
     */
    public EnumMethodLambdaParameterTypeStateGetter(Class type, String getterMethodName) {
        this.type = type;
        this.getterMethodName = getterMethodName;
    }

    /**
     * Returns the type of the value the lambda parameter assumes
     * @return types
     */
    public Class getType() {
        return type;
    }

    /**
     * Returns the name of the getter-method that the runtime invokes on the state object to obtain
     * the value of the lambda parameter
     * @return getter method name
     */
    public String getGetterMethodName() {
        return getterMethodName;
    }
}
