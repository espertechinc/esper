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
package com.espertech.esper.regressionlib.suite.infra.tbl;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableMTGroupedWContextIntoTableWriteAsContextTable implements RegressionExecution {

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    /**
     * Multiple writers share a key space that they aggregate into.
     * Writer utilize a hash partition context.
     * After all writers are done validate the space.
     */
    public void run(RegressionEnvironment env) {
        // with T, N, G:  Each of T threads loops N times and sends for each loop G events for each group.
        // for a total of T*N*G events being processed, and G aggregations retained in a shared variable.
        // Group is the innermost loop.
        try {
            tryMT(env, 8, 1000, 64);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void tryMT(RegressionEnvironment env, int numThreads, int numLoops, int numGroups) throws InterruptedException {
        String eplDeclare =
            "create context ByStringHash\n" +
                "  coalesce by consistent_hash_crc32(theString) from SupportBean, " +
                "    consistent_hash_crc32(p00) from SupportBean_S0 " +
                "  granularity 16 preallocate\n;" +
                "context ByStringHash create table varTotal (key string primary key, total sum(int));\n" +
                "context ByStringHash into table varTotal select theString, sum(intPrimitive) as total from SupportBean group by theString;\n";
        String eplAssert = "context ByStringHash select varTotal[p00].total as c0 from SupportBean_S0";

        InfraTableMTGroupedWContextIntoTableWriteAsSharedTable.runAndAssert(env, eplDeclare, eplAssert, numThreads, numLoops, numGroups);
    }
}
