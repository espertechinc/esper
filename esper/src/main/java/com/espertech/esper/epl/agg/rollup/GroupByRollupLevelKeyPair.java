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
package com.espertech.esper.epl.agg.rollup;

import com.espertech.esper.epl.agg.service.common.AggregationGroupByRollupLevel;

public class GroupByRollupLevelKeyPair {
    private final AggregationGroupByRollupLevel level;
    private final Object key;

    public GroupByRollupLevelKeyPair(AggregationGroupByRollupLevel level, Object key) {
        this.level = level;
        this.key = key;
    }

    public AggregationGroupByRollupLevel getLevel() {
        return level;
    }

    public Object getKey() {
        return key;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GroupByRollupLevelKeyPair that = (GroupByRollupLevelKeyPair) o;

        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        if (!level.equals(that.level)) return false;

        return true;
    }

    public int hashCode() {
        int result = level.hashCode();
        result = 31 * result + (key != null ? key.hashCode() : 0);
        return result;
    }
}
