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

import com.espertech.esper.client.EPStatement;

/**
 * Implementation of a convertor for column results that renders the result as an object array itself.
 */
public class DeliveryConvertorObjectArrWStatement implements DeliveryConvertor {
    private final EPStatement statement;

    public DeliveryConvertorObjectArrWStatement(EPStatement statement) {
        this.statement = statement;
    }

    public Object[] convertRow(Object[] columns) {
        return new Object[]{statement, columns};
    }
}
