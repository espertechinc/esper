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
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.junit.Assert;

import static com.espertech.esper.regression.db.ExecDatabaseJoinOptionUppercase.getDBConfig;

public class ExecDatabaseJoinOptionLowercase implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        ConfigurationDBRef dbconfig = getDBConfig();
        dbconfig.setColumnChangeCase(ConfigurationDBRef.ColumnChangeCaseEnum.LOWERCASE);
        dbconfig.addSqlTypesBinding(java.sql.Types.INTEGER, "string");
        configuration.addDatabaseReference("MyDB", dbconfig);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String sql = "select myint from mytesttable where ${intPrimitive} = myint'" +
                "metadatasql 'select myint from mytesttable'";
        String stmtText = "select myint from " +
                " sql:MyDB ['" + sql + "] as s0," +
                SupportBean.class.getName() + "#length(100) as s1";

        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        Assert.assertEquals(String.class, statement.getEventType().getPropertyType("myint"));

        sendSupportBeanEvent(epService, 10);
        Assert.assertEquals("10", listener.assertOneGetNewAndReset().get("myint"));

        sendSupportBeanEvent(epService, 80);
        Assert.assertEquals("80", listener.assertOneGetNewAndReset().get("myint"));
    }

    private void sendSupportBeanEvent(EPServiceProvider epService, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}
