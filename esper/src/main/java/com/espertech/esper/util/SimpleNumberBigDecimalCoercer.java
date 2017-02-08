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

import com.espertech.esper.event.EventAdapterException;
import com.espertech.esper.type.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.math.BigInteger;
import java.math.BigDecimal;

/**
 * Interface for number coercion resulting in BigInteger.
 */
public interface SimpleNumberBigDecimalCoercer
{
    /**
     * Widen the number to BigDecimal, if widening is required.
     * @param numToCoerce number to widen
     * @return widened number
     */
    public BigDecimal coerceBoxedBigDec(Number numToCoerce);
}
