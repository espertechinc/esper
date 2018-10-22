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
package com.espertech.esper.runtime.internal.subscriber;

/**
 * Implementation that does not convert columns.
 */
public class DeliveryConvertorZeroLengthParam implements DeliveryConvertor {
    public final static DeliveryConvertorZeroLengthParam INSTANCE = new DeliveryConvertorZeroLengthParam();

    private DeliveryConvertorZeroLengthParam() {
    }

    public Object[] convertRow(Object[] columns) {
        return null;
    }
}
