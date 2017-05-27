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
import com.espertech.esper.supportregression.epl.SupportDataSourceFactory;
import com.espertech.esper.supportregression.epl.SupportDatabaseService;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static org.junit.Assert.assertTrue;

public class ExecDatabaseDataSourceFactory implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(ExecDatabaseDataSourceFactory.class);

    public void configure(Configuration configuration) throws Exception {
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

        configuration.addDatabaseReference("MyDB", configDB);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String stmtText = "select istream myint from " +
                " sql:MyDB ['select myint from mytesttable where ${intPrimitive} = mytesttable.mybigint'] as s0," +
                SupportBean.class.getName() + " as s1";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);

        String[] fields = new String[]{"myint"};
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendSupportBeanEvent(epService, 10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{100});

        sendSupportBeanEvent(epService, 6);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{60});

        long startTime = System.currentTimeMillis();
        // Send 100 events which all fireStatementStopped a join
        for (int i = 0; i < 100; i++) {
            sendSupportBeanEvent(epService, 10);
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{100});
        }
        long endTime = System.currentTimeMillis();
        log.info("delta=" + (endTime - startTime));
        assertTrue(endTime - startTime < 5000);
    }

    private void sendSupportBeanEvent(EPServiceProvider epService, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}
