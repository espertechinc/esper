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

/**
 * Interface for use with multi-keys made up of multiple values and providing hashcode and equals semantics
 */
public interface MultiKey {
    /**
     * Returns the number of keys available
     * @return number of keys
     */
    int getNumKeys();

    /**
     * Returns the key value at the given index
     * @param num key number
     * @return key value
     */
    Object getKey(int num);

    /**
     * Convert the multi-key to an object array
     * @param mk to convert
     * @return object-array
     */
    static Object[] toObjectArray(MultiKey mk) {
        Object[] keys = new Object[mk.getNumKeys()];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = mk.getKey(i);
        }
        return keys;
    }
}
