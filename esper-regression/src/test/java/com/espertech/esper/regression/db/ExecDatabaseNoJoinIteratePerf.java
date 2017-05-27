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
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.epl.SupportDatabaseService;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Properties;

import static org.junit.Assert.assertTrue;

public class ExecDatabaseNoJoinIteratePerf implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        ConfigurationDBRef configDB = new ConfigurationDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDB.setConnectionLifecycleEnum(ConfigurationDBRef.ConnectionLifecycleEnum.RETAIN);
        configDB.setLRUCache(100000);
        configDB.setConnectionCatalog("test");
        configuration.addDatabaseReference("MyDB", configDB);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().createEPL("create variable boolean queryvar_bool");
        epService.getEPAdministrator().createEPL("create variable int lower");
        epService.getEPAdministrator().createEPL("create variable int upper");
        epService.getEPAdministrator().createEPL("on SupportBean set queryvar_bool=boolPrimitive, lower=intPrimitive,upper=intBoxed");

        String stmtText = "select * from sql:MyDB ['select mybigint, mybool from mytesttable where ${queryvar_bool} = mytesttable.mybool and myint between ${lower} and ${upper} order by mybigint']";
        String[] fields = new String[]{"mybigint", "mybool"};
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        sendSupportBeanEvent(epService, true, 20, 60);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{4L, true}});
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        assertTrue("delta=" + delta, delta < 1000);

        stmt.destroy();
    }

    private void sendSupportBeanEvent(EPServiceProvider epService, boolean boolPrimitive, int intPrimitive, int intBoxed) {
        SupportBean bean = new SupportBean();
        bean.setBoolPrimitive(boolPrimitive);
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }
}
