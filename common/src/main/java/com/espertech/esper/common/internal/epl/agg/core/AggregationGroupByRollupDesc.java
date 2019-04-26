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

import com.espertech.esper.common.client.serde.DataInputOutputSerde;

public class AggregationGroupByRollupDesc {
    private final AggregationGroupByRollupLevel[] levels;
    private final int numLevelsAggregation;

    public AggregationGroupByRollupDesc(AggregationGroupByRollupLevel[] levels) {
        this.levels = levels;

        int count = 0;
        for (AggregationGroupByRollupLevel level : levels) {
            if (!level.isAggregationTop()) {
                count++;
            }
        }
        numLevelsAggregation = count;
    }

    public AggregationGroupByRollupLevel[] getLevels() {
        return levels;
    }

    public int getNumLevelsAggregation() {
        return numLevelsAggregation;
    }

    public int getNumLevels() {
        return levels.length;
    }

    public DataInputOutputSerde[] getKeySerdes() {
        DataInputOutputSerde[] serdes = new DataInputOutputSerde[levels.length];
        for (int i = 0; i < serdes.length; i++) {
            serdes[i] = levels[i].getSubkeySerde();
        }
        return serdes;
    }
}
