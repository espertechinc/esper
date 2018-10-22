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
package com.espertech.esper.regressionlib.support.util;

import com.espertech.esper.common.internal.epl.agg.rollup.GroupByRollupPlanDesc;
import com.espertech.esper.common.internal.epl.agg.rollup.GroupByRollupPlanHook;

public class SupportGroupRollupPlanHook implements GroupByRollupPlanHook {

    private static GroupByRollupPlanDesc plan;

    public static void reset() {
        plan = null;
    }

    public void query(GroupByRollupPlanDesc desc) {
        if (plan != null) {
            throw new IllegalStateException();
        }
        plan = desc;
    }

    public static GroupByRollupPlanDesc getPlan() {
        return plan;
    }
}
