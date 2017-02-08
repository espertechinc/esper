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
package com.espertech.esper.epl.table.mgmt;

import com.espertech.esper.collection.MultiKeyUntyped;

public class TableRowKeyFactory {
    private final int[] keyColIndexes;

    public TableRowKeyFactory(int[] keyColIndexes) {
        if (keyColIndexes.length == 0) {
            throw new IllegalArgumentException("No key indexed provided");
        }
        this.keyColIndexes = keyColIndexes;
    }

    public Object getTableRowKey(Object[] data) {
        if (keyColIndexes.length == 1) {
            return data[keyColIndexes[0]];
        }
        Object[] key = new Object[keyColIndexes.length];
        for (int i = 0; i < keyColIndexes.length; i++) {
            key[i] = data[keyColIndexes[i]];
        }
        return new MultiKeyUntyped(key);
    }
}
