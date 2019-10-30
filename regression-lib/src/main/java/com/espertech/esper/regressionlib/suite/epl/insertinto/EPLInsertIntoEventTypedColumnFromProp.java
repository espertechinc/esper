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
package com.espertech.esper.regressionlib.suite.epl.insertinto;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;

public class EPLInsertIntoEventTypedColumnFromProp {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLInsertIntoEventTypedColumnOnMerge());
        return execs;
    }

    private static class EPLInsertIntoEventTypedColumnOnMerge implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create schema CarEvent(carId string, tracked boolean);\n" +
                "create table StatusTable(carId string primary key, lastevent CarEvent);\n" +
                "on CarEvent(tracked=true) as ce merge StatusTable as st where ce.carId = st.carId \n" +
                "  when matched \n" +
                "    then update set lastevent = ce \n" +
                "  when not matched \n" +
                "    then insert(carId, lastevent) select ce.carId, ce \n" +
                "    then insert into CarOutputStream select 'online' as status, ce as outputevent;\n" +
                "insert into CarTimeoutStream select e.* \n" +
                "  from pattern[every e=CarEvent(tracked=true) -> (timer:interval(1 minutes) and not CarEvent(carId = e.carId, tracked=true))];\n" +
                "on CarTimeoutStream as cts merge StatusTable as st where cts.carId = st.carId \n" +
                "  when matched \n" +
                "    then delete \n" +
                "    then insert into CarOutputStream select 'offline' as status, lastevent as outputevent;\n" +
                "@name('s0') select * from CarOutputStream";
            env.advanceTime(0);
            env.compileDeploy(epl).addListener("s0");

            sendCar(env, "C1");
            assertReceived(env.listener("s0").assertOneGetNewAndReset(), "online", "C1");

            env.advanceTime(60000);
            assertReceived(env.listener("s0").assertOneGetNewAndReset(), "offline", "C1");

            env.undeployAll();
        }
    }

    private static void assertReceived(EventBean received, String status, String carId) {
        assertEquals(status, received.get("status"));
        assertEquals(carId, ((Map) received.get("outputevent")).get("carId"));
    }

    private static void sendCar(RegressionEnvironment env, String carId) {
        env.sendEventMap(CollectionUtil.buildMap("carId", carId, "tracked", true), "CarEvent");
    }
}
