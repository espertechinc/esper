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
 * A lambda parameter that assumes an index value.
 */
public class EnumMethodLambdaParameterTypeIndex implements EnumMethodLambdaParameterType {
    /**
     * Instance.
     */
    public final static EnumMethodLambdaParameterTypeIndex INSTANCE = new EnumMethodLambdaParameterTypeIndex();

    private EnumMethodLambdaParameterTypeIndex() {
    }
}
