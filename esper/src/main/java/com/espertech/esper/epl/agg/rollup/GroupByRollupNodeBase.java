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

import java.util.ArrayList;
import java.util.List;

public abstract class GroupByRollupNodeBase {
    private final List<GroupByRollupNodeBase> childNodes = new ArrayList<GroupByRollupNodeBase>();

    public abstract List<int[]> evaluate(GroupByRollupEvalContext context) throws GroupByRollupDuplicateException;

    public List<GroupByRollupNodeBase> getChildNodes() {
        return childNodes;
    }

    public void add(GroupByRollupNodeBase child) {
        childNodes.add(child);
    }
}
