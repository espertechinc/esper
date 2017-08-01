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
package com.espertech.esper.util;

import com.espertech.esper.codegen.model.expression.CodegenExpression;

import java.math.BigInteger;

/**
 * Interface for number coercion resulting in BigInteger.
 */
public interface SimpleNumberBigIntegerCoercer {
    /**
     * Widen the number to BigInteger, if widening is required.
     *
     * @param numToCoerce number to widen
     * @return widened number
     */
    public BigInteger coerceBoxedBigInt(Number numToCoerce);

    CodegenExpression coerceBoxedBigIntCodegen(CodegenExpression expr, Class type);
}
