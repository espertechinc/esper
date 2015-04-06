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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.epl.SupportDatabaseService;
import org.junit.Assert;
import junit.framework.TestCase;

import java.util.Properties;

public class TestDatabaseJoinOptions extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void testHasMetaSQLStringParam()
    {
        ConfigurationDBRef dbconfig = getDBConfig();
        dbconfig.setColumnChangeCase(ConfigurationDBRef.ColumnChangeCaseEnum.UPPERCASE);
        Configuration configuration = getConfig(dbconfig);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String sql = "select myint from mytesttable where ${theString} = myvarchar'" +
                "metadatasql 'select myint from mytesttable'";
        String stmtText = "select MYINT from " +
                " sql:MyDB ['" + sql + "] as s0," +
                SupportBean.class.getName() + ".win:length(100) as s1";

        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        listener = new SupportUpdateListener();
        statement.addListener(listener);

        Assert.assertEquals(Integer.class, statement.getEventType().getPropertyType("MYINT"));

        sendSupportBeanEvent("A");
        Assert.assertEquals(10, listener.assertOneGetNewAndReset().get("MYINT"));

        sendSupportBeanEvent("H");
        Assert.assertEquals(80, listener.assertOneGetNewAndReset().get("MYINT"));

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void tearDown() {
        listener = null;
    }

    public void testTypeMapped()
    {
        ConfigurationDBRef dbconfig = getDBConfig();
        dbconfig.setColumnChangeCase(ConfigurationDBRef.ColumnChangeCaseEnum.LOWERCASE);
        dbconfig.addSqlTypesBinding(java.sql.Types.INTEGER, "string");
        Configuration configuration = getConfig(dbconfig);
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String sql = "select myint from mytesttable where ${intPrimitive} = myint'" +
                "metadatasql 'select myint from mytesttable'";
        String stmtText = "select myint from " +
                " sql:MyDB ['" + sql + "] as s0," +
                SupportBean.class.getName() + ".win:length(100) as s1";

        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        listener = new SupportUpdateListener();
        statement.addListener(listener);

        Assert.assertEquals(String.class, statement.getEventType().getPropertyType("myint"));

        sendSupportBeanEvent(10);
        Assert.assertEquals("10", listener.assertOneGetNewAndReset().get("myint"));

        sendSupportBeanEvent(80);
        Assert.assertEquals("80", listener.assertOneGetNewAndReset().get("myint"));

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testNoMetaLexAnalysis()
    {
        ConfigurationDBRef dbconfig = getDBConfig();
        Configuration configuration = getConfig(dbconfig);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String sql = "select mydouble from mytesttable where ${intPrimitive} = myint";
        run(sql);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testNoMetaLexAnalysisGroup()
    {
        ConfigurationDBRef dbconfig = getDBConfig();
        Configuration configuration = getConfig(dbconfig);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String sql = "select mydouble, sum(myint) from mytesttable where ${intPrimitive} = myint group by mydouble";
        run(sql);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testPlaceholderWhere()
    {
        ConfigurationDBRef dbconfig = getDBConfig();
        Configuration configuration = getConfig(dbconfig);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String sql = "select mydouble from mytesttable ${$ESPER-SAMPLE-WHERE} where ${intPrimitive} = myint";
        run(sql);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private void run(String sql)
    {
        String stmtText = "select mydouble from " +
                " sql:MyDB ['" + sql + "'] as s0," +
                SupportBean.class.getName() + ".win:length(100) as s1";

        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        listener = new SupportUpdateListener();
        statement.addListener(listener);

        Assert.assertEquals(Double.class, statement.getEventType().getPropertyType("mydouble"));

        sendSupportBeanEvent(10);
        Assert.assertEquals(1.2, listener.assertOneGetNewAndReset().get("mydouble"));

        sendSupportBeanEvent(80);
        Assert.assertEquals(8.2, listener.assertOneGetNewAndReset().get("mydouble"));
    }

    private ConfigurationDBRef getDBConfig()
    {
        ConfigurationDBRef configDB = new ConfigurationDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDB.setConnectionLifecycleEnum(ConfigurationDBRef.ConnectionLifecycleEnum.RETAIN);
        configDB.setConnectionCatalog("test");
        configDB.setConnectionReadOnly(true);
        configDB.setConnectionAutoCommit(true);
        return configDB;
    }

    private Configuration getConfig(ConfigurationDBRef configOracle)
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addDatabaseReference("MyDB", configOracle);
        configuration.getEngineDefaults().getLogging().setEnableExecutionDebug(true);

        return configuration;
    }

    private void sendSupportBeanEvent(String theString)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendSupportBeanEvent(int intPrimitive)
    {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}
