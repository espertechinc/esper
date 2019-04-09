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
package com.espertech.esper.common.client.util;

public interface MultiKeyGenerated {
    int getNumKeys();
    Object getKey(int num);

    static Object[] toObjectArray(MultiKeyGenerated mk) {
        Object[] keys = new Object[mk.getNumKeys()];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = mk.getKey(i);
        }
        return keys;
    }
}
