/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.nwtable;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestTableMTGroupedWContextIntoTableWriteAsContextTable extends TestCase
{
    private EPServiceProvider epService;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        config.addEventType("SupportBean_S0", SupportBean_S0.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
    }

    /**
     * Multiple writers share a key space that they aggregate into.
     * Writer utilize a hash partition context.
     * After all writers are done validate the space.
     */
    public void testMT() throws Exception
    {
        // with T, N, G:  Each of T threads loops N times and sends for each loop G events for each group.
        // for a total of T*N*G events being processed, and G aggregations retained in a shared variable.
        // Group is the innermost loop.
        tryMT(8, 1000, 64);
    }

    private void tryMT(int numThreads, int numLoops, int numGroups) throws Exception
    {
        String eplDeclare =
                "create context ByStringHash\n" +
                "  coalesce by consistent_hash_crc32(theString) from SupportBean, " +
                "    consistent_hash_crc32(p00) from SupportBean_S0 " +
                "  granularity 16 preallocate\n;" +
                "context ByStringHash create table varTotal (key string primary key, total sum(int));\n" +
                "context ByStringHash into table varTotal select theString, sum(intPrimitive) as total from SupportBean group by theString;\n";
        String eplAssert = "context ByStringHash select varTotal[p00].total as c0 from SupportBean_S0";

        TestTableMTGroupedWContextIntoTableWriteAsSharedTable.runAndAssert(epService, eplDeclare, eplAssert, numThreads, numLoops, numGroups);
    }
}
