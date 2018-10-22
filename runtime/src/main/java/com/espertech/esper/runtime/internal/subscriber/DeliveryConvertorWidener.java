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

import com.espertech.esper.common.internal.util.TypeWidenerSPI;

/**
 * Implementation of a convertor for column results that renders the result as an object array itself.
 */
public class DeliveryConvertorWidener implements DeliveryConvertor {
    private final TypeWidenerSPI[] wideners;

    public DeliveryConvertorWidener(TypeWidenerSPI[] wideners) {
        this.wideners = wideners;
    }

    public Object[] convertRow(Object[] columns) {
        for (int i = 0; i < columns.length; i++) {
            if (wideners[i] == null) {
                continue;
            }
            columns[i] = wideners[i].widen(columns[i]);
        }
        return columns;
    }
}
