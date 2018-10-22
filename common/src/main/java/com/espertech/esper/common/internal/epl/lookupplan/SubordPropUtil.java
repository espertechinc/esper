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
package com.espertech.esper.common.internal.epl.lookupplan;

import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryHashKeyedForgeProp;

import java.util.Collection;

public class SubordPropUtil {

    public static boolean isStrictKeyJoin(Collection<SubordPropHashKeyForge> hashKeys) {
        for (SubordPropHashKeyForge hashKey : hashKeys) {
            if (!(hashKey.getHashKey() instanceof QueryGraphValueEntryHashKeyedForgeProp)) {
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
    public static int[] getKeyStreamNums(Collection<SubordPropHashKeyForge> descList) {
        int[] streamIds = new int[descList.size()];
        int count = 0;
        for (SubordPropHashKeyForge desc : descList) {
            if (!(desc.getHashKey() instanceof QueryGraphValueEntryHashKeyedForgeProp)) {
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
    public static String[] getKeyProperties(Collection<SubordPropHashKeyForge> descList) {
        String[] result = new String[descList.size()];
        int count = 0;
        for (SubordPropHashKeyForge desc : descList) {
            if (!(desc.getHashKey() instanceof QueryGraphValueEntryHashKeyedForgeProp)) {
                throw new UnsupportedOperationException("Not a strict key compare");
            }
            QueryGraphValueEntryHashKeyedForgeProp keyed = (QueryGraphValueEntryHashKeyedForgeProp) desc.getHashKey();
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
    public static String[] getKeyProperties(SubordPropHashKeyForge[] descList) {
        String[] result = new String[descList.length];
        int count = 0;
        for (SubordPropHashKeyForge desc : descList) {
            if (!(desc.getHashKey() instanceof QueryGraphValueEntryHashKeyedForgeProp)) {
                throw new UnsupportedOperationException("Not a strict key compare");
            }
            QueryGraphValueEntryHashKeyedForgeProp keyed = (QueryGraphValueEntryHashKeyedForgeProp) desc.getHashKey();
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
    public static Class[] getCoercionTypes(Collection<SubordPropHashKeyForge> descList) {
        Class[] result = new Class[descList.size()];
        int count = 0;
        for (SubordPropHashKeyForge desc : descList) {
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
    public static Class[] getCoercionTypes(SubordPropHashKeyForge[] descList) {
        Class[] result = new Class[descList.length];
        int count = 0;
        for (SubordPropHashKeyForge desc : descList) {
            result[count++] = desc.getCoercionType();
        }
        return result;
    }
}
