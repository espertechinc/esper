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
import java.util.Arrays;
import java.util.List;

public class GroupByRollupNodeGroupingSet extends GroupByRollupNodeBase {

    public GroupByRollupNodeGroupingSet() {
    }

    public List<int[]> evaluate(GroupByRollupEvalContext context) throws GroupByRollupDuplicateException {
        List<int[]> rollup = new ArrayList<int[]>();
        for (GroupByRollupNodeBase node : this.getChildNodes()) {
            List<int[]> result = node.evaluate(context);

            // find dups
            for (int[] row : result) {
                for (int[] existing : rollup) {
                    if (Arrays.equals(row, existing)) {
                        throw new GroupByRollupDuplicateException(row);
                    }
                }
            }

            rollup.addAll(result);
        }
        return rollup;
    }
}
