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
package com.espertech.esper.common.internal.epl.agg.core;

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.collection.HashableMultiKey;

import java.util.Arrays;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class AggregationGroupByRollupLevel {
    private final int levelNumber;
    private final int levelOffset;
    private final int[] rollupKeys;

    public AggregationGroupByRollupLevel(int levelNumber, int levelOffset, int[] rollupKeys) {
        this.levelNumber = levelNumber;
        this.levelOffset = levelOffset;
        this.rollupKeys = rollupKeys;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public int getAggregationOffset() {
        if (isAggregationTop()) {
            throw new IllegalArgumentException();
        }
        return levelOffset;
    }

    public CodegenExpression toExpression() {
        return newInstance(AggregationGroupByRollupLevel.class, constant(levelNumber), constant(levelOffset), constant(rollupKeys));
    }

    public boolean isAggregationTop() {
        return levelOffset == -1;
    }

    public int[] getRollupKeys() {
        return rollupKeys;
    }

    public Object computeSubkey(Object groupKey) {
        if (isAggregationTop()) {
            return null;
        }
        if (groupKey instanceof HashableMultiKey) {
            HashableMultiKey mk = (HashableMultiKey) groupKey;
            Object[] keys = mk.getKeys();
            if (rollupKeys.length == keys.length) {
                return mk;
            } else if (rollupKeys.length == 1) {
                return keys[rollupKeys[0]];
            } else {
                Object[] subkeys = new Object[rollupKeys.length];
                int count = 0;
                for (int rollupKey : rollupKeys) {
                    subkeys[count++] = keys[rollupKey];
                }
                return new HashableMultiKey(subkeys);
            }
        } else {
            return groupKey;
        }
    }

    public String toString() {
        return "GroupByRollupLevel{" +
                "levelOffset=" + levelOffset +
                ", rollupKeys=" + Arrays.toString(rollupKeys) +
                '}';
    }

    public HashableMultiKey computeMultiKey(Object subkey, int numExpected) {
        if (subkey instanceof HashableMultiKey) {
            HashableMultiKey mk = (HashableMultiKey) subkey;
            if (mk.getKeys().length == numExpected) {
                return mk;
            }
            Object[] keys = new Object[]{numExpected};
            for (int i = 0; i < rollupKeys.length; i++) {
                keys[rollupKeys[i]] = mk.getKeys()[i];
            }
            return new HashableMultiKey(keys);
        }
        Object[] keys = new Object[numExpected];
        if (subkey == null) {
            return new HashableMultiKey(keys);
        }
        keys[rollupKeys[0]] = subkey;
        return new HashableMultiKey(keys);
    }
}
