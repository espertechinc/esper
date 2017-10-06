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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.agg.service.common.AggregationGroupByRollupLevel;

public class GroupByRollupKey {
    private final EventBean[] generator;
    private final AggregationGroupByRollupLevel level;
    private final Object groupKey;

    public GroupByRollupKey(EventBean[] generator, AggregationGroupByRollupLevel level, Object groupKey) {
        this.generator = generator;
        this.level = level;
        this.groupKey = groupKey;
    }

    public EventBean[] getGenerator() {
        return generator;
    }

    public AggregationGroupByRollupLevel getLevel() {
        return level;
    }

    public Object getGroupKey() {
        return groupKey;
    }

    public String toString() {
        return "GroupRollupKey{" +
                "level=" + level +
                ", groupKey=" + groupKey +
                '}';
    }
}
