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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationDBRef;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanTwo;
import com.espertech.esper.supportregression.epl.SupportDatabaseService;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Properties;

public class ExecDatabase3StreamOuterJoin implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        ConfigurationDBRef configDB = new ConfigurationDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDB.setConnectionLifecycleEnum(ConfigurationDBRef.ConnectionLifecycleEnum.RETAIN);

        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        configuration.getEngineDefaults().getLogging().setEnableJDBC(true);
        configuration.addDatabaseReference("MyDB", configDB);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanTwo", SupportBeanTwo.class);

        runAssertionInnerJoinLeftS0(epService);
        runAssertionOuterJoinLeftS0(epService);
    }

    private void runAssertionInnerJoinLeftS0(EPServiceProvider epService) {
        String stmtText = "select * from SupportBean#lastevent sb" +
                " inner join " +
                " SupportBeanTwo#lastevent sbt" +
                " on sb.theString = sbt.stringTwo " +
                " inner join " +
                " sql:MyDB ['select myint from mytesttable'] as s1 " +
                "  on s1.myint = sbt.intPrimitiveTwo";

        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanTwo("T1", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("T1", -1));

        epService.getEPRuntime().sendEvent(new SupportBeanTwo("T2", 30));
        epService.getEPRuntime().sendEvent(new SupportBean("T2", -1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "sb.theString,sbt.stringTwo,s1.myint".split(","), new Object[]{"T2", "T2", 30});

        epService.getEPRuntime().sendEvent(new SupportBean("T3", -1));
        epService.getEPRuntime().sendEvent(new SupportBeanTwo("T3", 40));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "sb.theString,sbt.stringTwo,s1.myint".split(","), new Object[]{"T3", "T3", 40});

        statement.destroy();
    }

    private void runAssertionOuterJoinLeftS0(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanTwo", SupportBeanTwo.class);
        String stmtText = "select * from SupportBean#lastevent sb" +
                " left outer join " +
                " SupportBeanTwo#lastevent sbt" +
                " on sb.theString = sbt.stringTwo " +
                " left outer join " +
                " sql:MyDB ['select myint from mytesttable'] as s1 " +
                "  on s1.myint = sbt.intPrimitiveTwo";

        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanTwo("T1", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("T1", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "sb.theString,sbt.stringTwo,s1.myint".split(","), new Object[]{"T1", "T1", null});

        epService.getEPRuntime().sendEvent(new SupportBeanTwo("T2", 30));
        epService.getEPRuntime().sendEvent(new SupportBean("T2", -2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "sb.theString,sbt.stringTwo,s1.myint".split(","), new Object[]{"T2", "T2", 30});

        epService.getEPRuntime().sendEvent(new SupportBean("T3", -1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "sb.theString,sbt.stringTwo,s1.myint".split(","), new Object[]{"T3", null, null});

        epService.getEPRuntime().sendEvent(new SupportBeanTwo("T3", 40));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "sb.theString,sbt.stringTwo,s1.myint".split(","), new Object[]{"T3", "T3", 40});

        statement.destroy();
    }
}
