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
package com.espertech.esper.epl.lookup;

import com.espertech.esper.epl.join.plan.QueryGraphValueEntryHashKeyedProp;

import java.util.Collection;

public class SubordPropUtil {

    public static boolean isStrictKeyJoin(Collection<SubordPropHashKey> hashKeys) {
        for (SubordPropHashKey hashKey : hashKeys) {
            if (!(hashKey.getHashKey() instanceof QueryGraphValueEntryHashKeyedProp)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the key stream numbers.
     *
     * @param descList a list of descriptors
     * @return key stream numbers
     */
    public static int[] getKeyStreamNums(Collection<SubordPropHashKey> descList) {
        int[] streamIds = new int[descList.size()];
        int count = 0;
        for (SubordPropHashKey desc : descList) {
            if (!(desc.getHashKey() instanceof QueryGraphValueEntryHashKeyedProp)) {
                throw new UnsupportedOperationException("Not a strict key compare");
            }
            streamIds[count++] = desc.getOptionalKeyStreamNum();
        }
        return streamIds;
    }

    /**
     * Returns the key property names.
     *
     * @param descList a list of descriptors
     * @return key property names
     */
    public static String[] getKeyProperties(Collection<SubordPropHashKey> descList) {
        String[] result = new String[descList.size()];
        int count = 0;
        for (SubordPropHashKey desc : descList) {
            if (!(desc.getHashKey() instanceof QueryGraphValueEntryHashKeyedProp)) {
                throw new UnsupportedOperationException("Not a strict key compare");
            }
            QueryGraphValueEntryHashKeyedProp keyed = (QueryGraphValueEntryHashKeyedProp) desc.getHashKey();
            result[count++] = keyed.getKeyProperty();
        }
        return result;
    }

    /**
     * Returns the key property names.
     *
     * @param descList a list of descriptors
     * @return key property names
     */
    public static String[] getKeyProperties(SubordPropHashKey[] descList) {
        String[] result = new String[descList.length];
        int count = 0;
        for (SubordPropHashKey desc : descList) {
            if (!(desc.getHashKey() instanceof QueryGraphValueEntryHashKeyedProp)) {
                throw new UnsupportedOperationException("Not a strict key compare");
            }
            QueryGraphValueEntryHashKeyedProp keyed = (QueryGraphValueEntryHashKeyedProp) desc.getHashKey();
            result[count++] = keyed.getKeyProperty();
        }
        return result;
    }

    /**
     * Returns the key coercion types.
     *
     * @param descList a list of descriptors
     * @return key coercion types
     */
    public static Class[] getCoercionTypes(Collection<SubordPropHashKey> descList) {
        Class[] result = new Class[descList.size()];
        int count = 0;
        for (SubordPropHashKey desc : descList) {
            result[count++] = desc.getCoercionType();
        }
        return result;
    }

    /**
     * Returns the key coercion types.
     *
     * @param descList a list of descriptors
     * @return key coercion types
     */
    public static Class[] getCoercionTypes(SubordPropHashKey[] descList) {
        Class[] result = new Class[descList.length];
        int count = 0;
        for (SubordPropHashKey desc : descList) {
            result[count++] = desc.getCoercionType();
        }
        return result;
    }
}
