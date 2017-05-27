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
package com.espertech.esper.regression.db;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.epl.SupportDatabaseService;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecDatabaseJoinInsertInto implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        ConfigurationDBRef configDB = new ConfigurationDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDB.setConnectionLifecycleEnum(ConfigurationDBRef.ConnectionLifecycleEnum.RETAIN);
        configDB.setConnectionCatalog("test");
        configDB.setConnectionReadOnly(true);
        configDB.setConnectionTransactionIsolation(1);
        configDB.setConnectionAutoCommit(true);

        configuration.addDatabaseReference("MyDB", configDB);

        configuration.getEngineDefaults().getLogging().setEnableJDBC(true);
    }

    public void run(EPServiceProvider epService) throws Exception {

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        StringBuilder sb = new StringBuilder();
        sb.append("insert into ReservationEvents(type, cid, elapsed, series) ");
        sb.append("select istream 'type_1' as type, C.myvarchar as cid, C.myint as elapsed, C.mychar as series ");
        sb.append("from pattern [every timer:interval(20 sec)], ");
        sb.append("sql:MyDB [' select myvarchar, myint, mychar from mytesttable '] as C ");
        epService.getEPAdministrator().createEPL(sb.toString());

        // Reservation Events status change, aggregation, sla definition and DB cache update
        sb = new StringBuilder();
        sb.append("insert into SumOfReservations(cid, type, series, total, insla, bordersla, outsla) ");
        sb.append("select istream cid, type, series, ");
        sb.append("count(*) as total, ");
        sb.append("sum(case when elapsed < 600000 then 1 else 0 end) as insla, ");
        sb.append("sum(case when elapsed between 600000 and 900000 then 1 else 0 end) as bordersla, ");
        sb.append("sum(case when elapsed > 900000 then 1 else 0 end) as outsla ");
        sb.append("from ReservationEvents#time_batch(10 sec) ");
        sb.append("group by cid, type, series order by series asc");

        EPStatement stmt = epService.getEPAdministrator().createEPL(sb.toString());
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(20000));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(30000));
        EventBean[] received = listener.getLastNewData();
        assertEquals(10, received.length);
        listener.reset();

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(31000));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(39000));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(40000));
        received = listener.getLastNewData();
        assertEquals(10, received.length);
        listener.reset();

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(41000));
        assertFalse(listener.isInvoked());
    }
}
