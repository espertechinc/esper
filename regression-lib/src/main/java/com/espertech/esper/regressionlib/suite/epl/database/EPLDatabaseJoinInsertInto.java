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
package com.espertech.esper.regressionlib.suite.epl.database;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import static com.espertech.esper.common.client.scopetest.ScopeTestHelper.assertEquals;
import static com.espertech.esper.common.client.scopetest.ScopeTestHelper.assertFalse;

public class EPLDatabaseJoinInsertInto implements RegressionExecution {

    public void run(RegressionEnvironment env) {

        env.advanceTime(0);
        RegressionPath path = new RegressionPath();

        StringBuilder sb = new StringBuilder();
        sb.append("insert into ReservationEvents(type, cid, elapsed, series) ");
        sb.append("select istream 'type_1' as type, C.myvarchar as cid, C.myint as elapsed, C.mychar as series ");
        sb.append("from pattern [every timer:interval(20 sec)], ");
        sb.append("sql:MyDBWithTxnIso1WithReadOnly [' select myvarchar, myint, mychar from mytesttable '] as C ");
        env.compileDeploy(sb.toString(), path);

        // Reservation Events status change, aggregation, sla definition and DB cache update
        sb = new StringBuilder();
        sb.append("@name('s0') insert into SumOfReservations(cid, type, series, total, insla, bordersla, outsla) ");
        sb.append("select istream cid, type, series, ");
        sb.append("count(*) as total, ");
        sb.append("sum(case when elapsed < 600000 then 1 else 0 end) as insla, ");
        sb.append("sum(case when elapsed between 600000 and 900000 then 1 else 0 end) as bordersla, ");
        sb.append("sum(case when elapsed > 900000 then 1 else 0 end) as outsla ");
        sb.append("from ReservationEvents#time_batch(10 sec) ");
        sb.append("group by cid, type, series order by series asc");

        env.compileDeploy(sb.toString(), path).addListener("s0");

        env.advanceTime(20000);
        assertFalse(env.listener("s0").isInvoked());

        env.advanceTime(30000);
        EventBean[] received = env.listener("s0").getLastNewData();
        assertEquals(10, received.length);
        env.listener("s0").reset();

        env.advanceTime(31000);
        assertFalse(env.listener("s0").isInvoked());
        env.advanceTime(39000);
        assertFalse(env.listener("s0").isInvoked());

        env.advanceTime(40000);
        received = env.listener("s0").getLastNewData();
        assertEquals(10, received.length);
        env.listener("s0").reset();

        env.advanceTime(41000);
        assertFalse(env.listener("s0").isInvoked());

        env.undeployAll();
    }
}
