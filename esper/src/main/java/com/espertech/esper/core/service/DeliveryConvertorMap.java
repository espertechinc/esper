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

import java.util.HashMap;
import java.util.Map;

/**
 * Converts column results into a Map of key-value pairs.
 */
public class DeliveryConvertorMap implements DeliveryConvertor {
    private final String[] columnNames;

    /**
     * Ctor.
     *
     * @param columnNames the names for columns
     */
    public DeliveryConvertorMap(String[] columnNames) {
        this.columnNames = columnNames;
    }

    public Object[] convertRow(Object[] columns) {
        Map<String, Object> map = new HashMap<String, Object>();
        for (int i = 0; i < columns.length; i++) {
            map.put(columnNames[i], columns[i]);
        }
        return new Object[]{map};
    }
}
