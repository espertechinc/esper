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
package com.espertech.esper.regressionlib.suite.infra.nwtable;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;

import java.util.ArrayList;
import java.util.Collection;

public class InfraNWTableContext {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraContext(true));
        execs.add(new InfraContext(false));
        return execs;
    }

    private static class InfraContext implements RegressionExecution {
        private final boolean namedWindow;

        public InfraContext(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context ContextOne start SupportBean_S0 end SupportBean_S1", path);

            String eplCreate = namedWindow ?
                "context ContextOne create window MyInfra#keepall as (pkey0 string, pkey1 int, c0 long)" :
                "context ContextOne create table MyInfra as (pkey0 string primary key, pkey1 int primary key, c0 long)";
            env.compileDeploy(eplCreate, path);

            env.compileDeploy("context ContextOne insert into MyInfra select theString as pkey0, intPrimitive as pkey1, longPrimitive as c0 from SupportBean", path);

            env.sendEventBean(new SupportBean_S0(0));  // start

            makeSendSupportBean(env, "E1", 10, 100);
            makeSendSupportBean(env, "E2", 20, 200);

            register(env, path, 1, "context ContextOne select * from MyInfra output snapshot when terminated");
            register(env, path, 2, "context ContextOne select count(*) as thecnt from MyInfra output snapshot when terminated");
            register(env, path, 3, "context ContextOne select pkey0, count(*) as thecnt from MyInfra output snapshot when terminated");
            register(env, path, 4, "context ContextOne select pkey0, count(*) as thecnt from MyInfra group by pkey0 output snapshot when terminated");
            register(env, path, 5, "context ContextOne select pkey0, pkey1, count(*) as thecnt from MyInfra group by pkey0 output snapshot when terminated");
            register(env, path, 6, "context ContextOne select pkey0, pkey1, count(*) as thecnt from MyInfra group by rollup (pkey0, pkey1) output snapshot when terminated");

            env.milestone(0);

            env.sendEventBean(new SupportBean_S1(0));  // end

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s1").getAndResetLastNewData(), "pkey0,pkey1,c0".split(","), new Object[][]{{"E1", 10, 100L}, {"E2", 20, 200L}});
            EPAssertionUtil.assertProps(env.listener("s2").assertOneGetNewAndReset(), "thecnt".split(","), new Object[]{2L});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s3").getAndResetLastNewData(), "pkey0,thecnt".split(","), new Object[][]{{"E1", 2L}, {"E2", 2L}});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s4").getAndResetLastNewData(), "pkey0,thecnt".split(","), new Object[][]{{"E1", 1L}, {"E2", 1L}});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s5").getAndResetLastNewData(), "pkey0,pkey1,thecnt".split(","), new Object[][]{{"E1", 10, 1L}, {"E2", 20, 1L}});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s6").getAndResetLastNewData(), "pkey0,pkey1,thecnt".split(","), new Object[][]{
                {"E1", 10, 1L}, {"E2", 20, 1L}, {"E1", null, 1L}, {"E2", null, 1L}, {null, null, 2L}});

            env.undeployAll();
        }
    }

    private static void register(RegressionEnvironment env, RegressionPath path, int num, String epl) {
        env.compileDeploy("@name('s" + num + "')" + epl, path).addListener("s" + num);
    }

    private static void makeSendSupportBean(RegressionEnvironment env, String theString, int intPrimitive, long longPrimitive) {
        SupportBean b = new SupportBean(theString, intPrimitive);
        b.setLongPrimitive(longPrimitive);
        env.sendEventBean(b);
    }
}
