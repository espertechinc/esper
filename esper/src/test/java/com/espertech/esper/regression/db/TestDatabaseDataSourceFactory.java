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
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.epl.SupportDataSourceFactory;
import com.espertech.esper.support.epl.SupportDatabaseService;
import junit.framework.TestCase;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestDatabaseDataSourceFactory extends TestCase
{
    private static final Log log = LogFactory.getLog(TestDatabaseDataSourceFactory.class);
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void testDBCP() throws Exception
    {
        Properties props = new Properties();
        props.put("driverClassName", SupportDatabaseService.DRIVER);
        props.put("url", SupportDatabaseService.FULLURL);
        props.put("username", SupportDatabaseService.DBUSER);
        props.put("password", SupportDatabaseService.DBPWD);

        ConfigurationDBRef configDB = new ConfigurationDBRef();
        // for DBCP, use setDataSourceFactoryDBCP
        configDB.setDataSourceFactory(props, SupportDataSourceFactory.class.getName());
        configDB.setConnectionLifecycleEnum(ConfigurationDBRef.ConnectionLifecycleEnum.POOLED);
        configDB.setLRUCache(100);

        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addDatabaseReference("MyDB", configDB);
    
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        runAssertion();
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    private void runAssertion()
    {
        String stmtText = "select istream myint from " +
                " sql:MyDB ['select myint from mytesttable where ${intPrimitive} = mytesttable.mybigint'] as s0," +
                SupportBean.class.getName() + " as s1";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);

        String[] fields = new String[] {"myint"};
        listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendSupportBeanEvent(10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{100});

        sendSupportBeanEvent(6);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{60});

        long startTime = System.currentTimeMillis();
        // Send 100 events which all fireStatementStopped a join
        for (int i = 0; i < 100; i++)
        {
            sendSupportBeanEvent(10);
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{100});
        }
        long endTime = System.currentTimeMillis();
        log.info("delta=" + (endTime - startTime));
        assertTrue(endTime - startTime < 5000);
    }

    private void sendSupportBeanEvent(int intPrimitive)
    {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}
