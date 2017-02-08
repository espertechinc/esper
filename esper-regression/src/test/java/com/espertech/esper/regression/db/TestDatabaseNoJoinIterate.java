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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.epl.SupportDatabaseService;
import junit.framework.TestCase;

import java.util.Properties;

public class TestDatabaseNoJoinIterate extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        ConfigurationDBRef configDB = new ConfigurationDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDB.setConnectionLifecycleEnum(ConfigurationDBRef.ConnectionLifecycleEnum.RETAIN);
        configDB.setConnectionCatalog("test");
        configDB.setConnectionReadOnly(true);
        configDB.setConnectionTransactionIsolation(1);
        configDB.setConnectionAutoCommit(true);

        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addDatabaseReference("MyDB", configDB);

        epService = EPServiceProviderManager.getProvider("TestDatabaseJoinRetained", configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
        epService.destroy();
    }

    public void testExpressionPoll()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().createEPL("create variable boolean queryvar_bool");
        epService.getEPAdministrator().createEPL("create variable int queryvar_int");
        epService.getEPAdministrator().createEPL("create variable int lower");
        epService.getEPAdministrator().createEPL("create variable int upper");
        epService.getEPAdministrator().createEPL("on SupportBean set queryvar_int=intPrimitive, queryvar_bool=boolPrimitive, lower=intPrimitive,upper=intBoxed");

        // Test int and singlerow
        String stmtText = "select myint from sql:MyDB ['select myint from mytesttable where ${queryvar_int -2} = mytesttable.mybigint']";
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL(stmtText);
        assertFalse(stmt.getStatementContext().isStatelessSelect());
        listener = new SupportUpdateListener();
        stmt.addListener(listener);

        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), new String[]{"myint"}, null);

        sendSupportBeanEvent(5);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), new String[]{"myint"}, new Object[][]{{30}});

        stmt.destroy();
        assertFalse(listener.isInvoked());

        // Test multi-parameter and multi-row
        stmtText = "select myint from sql:MyDB ['select myint from mytesttable where mytesttable.mybigint between ${queryvar_int-2} and ${queryvar_int+2}'] order by myint";
        stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL(stmtText);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), new String[]{"myint"}, new Object[][]{{30}, {40}, {50}, {60}, {70}});
        stmt.destroy();

        // Test substitution parameters
        try {
            stmtText = "select myint from sql:MyDB ['select myint from mytesttable where mytesttable.mybigint between ${?} and ${queryvar_int+?}'] order by myint";
            epService.getEPAdministrator().prepareEPL(stmtText);
            fail();
        }
        catch (EPStatementException ex) {
            assertEquals("EPL substitution parameters are not allowed in SQL ${...} expressions, consider using a variable instead [select myint from sql:MyDB ['select myint from mytesttable where mytesttable.mybigint between ${?} and ${queryvar_int+?}'] order by myint]", ex.getMessage());
        }
    }

    public void testVariablesPoll()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().createEPL("create variable boolean queryvar_bool");
        epService.getEPAdministrator().createEPL("create variable int queryvar_int");
        epService.getEPAdministrator().createEPL("create variable int lower");
        epService.getEPAdministrator().createEPL("create variable int upper");
        epService.getEPAdministrator().createEPL("on SupportBean set queryvar_int=intPrimitive, queryvar_bool=boolPrimitive, lower=intPrimitive,upper=intBoxed");

        // Test int and singlerow
        String stmtText = "select myint from sql:MyDB ['select myint from mytesttable where ${queryvar_int} = mytesttable.mybigint']";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        listener = new SupportUpdateListener();
        stmt.addListener(listener);

        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), new String[]{"myint"}, null);

        sendSupportBeanEvent(5);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), new String[]{"myint"}, new Object[][]{{50}});

        stmt.destroy();
        assertFalse(listener.isInvoked());

        // Test boolean and multirow
        stmtText = "select * from sql:MyDB ['select mybigint, mybool from mytesttable where ${queryvar_bool} = mytesttable.mybool and myint between ${lower} and ${upper} order by mybigint']";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String[] fields = new String[] {"mybigint", "mybool"};
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendSupportBeanEvent(true, 10, 40);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{1L, true}, {4L, true}});

        sendSupportBeanEvent(false, 30, 80);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{3L, false}, {5L, false}, {6L, false}});

        sendSupportBeanEvent(true, 20, 30);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendSupportBeanEvent(true, 20, 60);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{4L, true}});
    }

    private void sendSupportBeanEvent(int intPrimitive)
    {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendSupportBeanEvent(boolean boolPrimitive, int intPrimitive, int intBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setBoolPrimitive(boolPrimitive);
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }
}
