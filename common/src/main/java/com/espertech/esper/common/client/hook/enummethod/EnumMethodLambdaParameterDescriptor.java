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
 * For use with lambda parameters, the descriptor identifies a specific lambda parameter.
 * <p>
 *     For instance <code>mymethod(1, (v, i) =&gt; 2)</code> the parameter number is 1 amd the lambda parameter number
 *     is zero for "v" and one for "i".
 * </p>
 */
public class EnumMethodLambdaParameterDescriptor {
    private final int parameterNumber;
    private final int lambdaParameterNumber;

    /**
     * Ctor.
     * @param parameterNumber overall parameter number
     * @param lambdaParameterNumber lambda parameter number
     */
    public EnumMethodLambdaParameterDescriptor(int parameterNumber, int lambdaParameterNumber) {
        this.parameterNumber = parameterNumber;
        this.lambdaParameterNumber = lambdaParameterNumber;
    }

    /**
     * Returns the overall parameter number.
     * @return number
     */
    public int getParameterNumber() {
        return parameterNumber;
    }

    /**
     * Returns the lambda parameter number.
     * @return number
     */
    public int getLambdaParameterNumber() {
        return lambdaParameterNumber;
    }
}
