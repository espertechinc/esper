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

package com.espertech.esper.regression.db;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBeanTwo;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.epl.SupportDatabaseService;
import junit.framework.TestCase;

import java.util.Properties;

public class TestDatabase3StreamOuterJoin extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        ConfigurationDBRef configDB = new ConfigurationDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDB.setConnectionLifecycleEnum(ConfigurationDBRef.ConnectionLifecycleEnum.RETAIN);

        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        configuration.getEngineDefaults().getLogging().setEnableJDBC(true);
        configuration.addDatabaseReference("MyDB", configDB);

        epService = EPServiceProviderManager.getProvider("TestDatabaseJoinRetained", configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanTwo", SupportBeanTwo.class);
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
        epService.destroy();
    }

    public void testInnerJoinLeftS0()
    {
        String stmtText = "select * from SupportBean.std:lastevent() sb" +
                " inner join " +
                " SupportBeanTwo.std:lastevent() sbt" +
                " on sb.theString = sbt.stringTwo " +
                " inner join " +
                " sql:MyDB ['select myint from mytesttable'] as s1 " +
                "  on s1.myint = sbt.intPrimitiveTwo";

        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanTwo("T1", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("T1", -1));

        epService.getEPRuntime().sendEvent(new SupportBeanTwo("T2", 30));
        epService.getEPRuntime().sendEvent(new SupportBean("T2", -1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "sb.theString,sbt.stringTwo,s1.myint".split(","), new Object[]{"T2", "T2", 30});

        epService.getEPRuntime().sendEvent(new SupportBean("T3", -1));
        epService.getEPRuntime().sendEvent(new SupportBeanTwo("T3", 40));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "sb.theString,sbt.stringTwo,s1.myint".split(","), new Object[]{"T3", "T3", 40});
    }

    public void testOuterJoinLeftS0()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanTwo", SupportBeanTwo.class);
        String stmtText = "select * from SupportBean.std:lastevent() sb" +
                " left outer join " +
                " SupportBeanTwo.std:lastevent() sbt" +
                " on sb.theString = sbt.stringTwo " +
                " left outer join " +
                " sql:MyDB ['select myint from mytesttable'] as s1 " +
                "  on s1.myint = sbt.intPrimitiveTwo";

        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        listener = new SupportUpdateListener();
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
    }
}
