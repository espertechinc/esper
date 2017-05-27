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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.epl.SupportDatabaseService;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.junit.Assert;

import java.util.Properties;

public class ExecDatabaseJoinOptionUppercase implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        ConfigurationDBRef dbconfig = getDBConfig();
        dbconfig.setColumnChangeCase(ConfigurationDBRef.ColumnChangeCaseEnum.UPPERCASE);
        configuration.addDatabaseReference("MyDB", dbconfig);
        configuration.getEngineDefaults().getLogging().setEnableExecutionDebug(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String sql = "select myint from mytesttable where ${theString} = myvarchar'" +
                "metadatasql 'select myint from mytesttable'";
        String stmtText = "select MYINT from " +
                " sql:MyDB ['" + sql + "] as s0," +
                SupportBean.class.getName() + "#length(100) as s1";

        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        Assert.assertEquals(Integer.class, statement.getEventType().getPropertyType("MYINT"));

        sendSupportBeanEvent(epService, "A");
        Assert.assertEquals(10, listener.assertOneGetNewAndReset().get("MYINT"));

        sendSupportBeanEvent(epService, "H");
        Assert.assertEquals(80, listener.assertOneGetNewAndReset().get("MYINT"));
    }

    protected static ConfigurationDBRef getDBConfig() {
        ConfigurationDBRef configDB = new ConfigurationDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDB.setConnectionLifecycleEnum(ConfigurationDBRef.ConnectionLifecycleEnum.RETAIN);
        configDB.setConnectionCatalog("test");
        configDB.setConnectionReadOnly(true);
        configDB.setConnectionAutoCommit(true);
        return configDB;
    }

    private void sendSupportBeanEvent(EPServiceProvider epService, String theString) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        epService.getEPRuntime().sendEvent(bean);
    }
}
