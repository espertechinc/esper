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
package com.espertech.esperio.db;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esperio.db.core.ExecutorServices;
import junit.framework.TestCase;

import javax.naming.Context;
import javax.naming.NamingException;
import java.sql.SQLException;
import java.util.Properties;

public class TestDBAdapterDML extends TestCase {
    private final static String ENGINE_URI = "TestDBAdapterDML";

    private final static String TABLE_NAME = "mytestupsert";

    public void setUp() throws Exception {
        SupportDatabaseService.truncateTable(TABLE_NAME);
    }

    public void testDML() throws InterruptedException, SQLException, NamingException {
        String esperIODBConfig = "<esperio-db-configuration>\n" +
                "<jdbc-connection name=\"db1\">\n" +
                "<drivermanager-connection class-name=\"com.mysql.jdbc.Driver\" url=\"jdbc:mysql://localhost/test\" user=\"root\" password=\"password\"/>\n" +
                "<connection-settings auto-commit=\"true\" catalog=\"TEST\"/>" +
                "</jdbc-connection>\n" +
                "<dml connection=\"db1\" stream=\"SupportDBBean\" name=\"MyInsertQuery\" retry=\"10\" retry-interval-sec=\"0.1\">\n" +
                "<sql>insert into mytestupsert(key1, key2, value1, value2)\n" +
                "values (?, ?, ?, ?)</sql>\n" +
                "<bindings>\n" +
                "<parameter pos=\"1\" property=\"key1\"/>\n" +
                "<parameter pos=\"2\" property=\"key2\"/>\n" +
                "<parameter pos=\"3\" property=\"value1\"/>\n" +
                "<parameter pos=\"4\" property=\"value2\"/>\n" +
                "</bindings>\n" +
                "</dml>\n" +
                "</esperio-db-configuration>";

        Configuration engineConfig = new Configuration();
        engineConfig.addPluginLoader("EsperIODBAdapter", EsperIODBAdapterPlugin.class.getName(), new Properties(), esperIODBConfig);

        engineConfig.addEventType("SupportDBBean", SupportDBBean.class);
        engineConfig.addEventType("SupportBean", SupportBean.class);

        EPServiceProvider provider = EPServiceProviderManager.getProvider(ENGINE_URI, engineConfig);

        provider.getEPAdministrator().createEPL("insert into SupportDBBean select 'A' as key1, intProp as key2, stringProp as value1, intProp as value2 from SupportBean");
        provider.getEPRuntime().sendEvent(new SupportBean("a", 10));

        Object[][] rows = SupportDatabaseService.readAll(TABLE_NAME);
        EPAssertionUtil.assertEqualsExactOrder(rows, new Object[][]{{"A", 10, "a", 10.0d}});

        provider.getEPRuntime().sendEvent(new SupportBean("b", 15));
        rows = SupportDatabaseService.readAll(TABLE_NAME);
        EPAssertionUtil.assertEqualsExactOrder(rows, new Object[][]{{"A", 10, "a", 10.0d}, {"A", 15, "b", 15.0d}});

        // look up executor services
        Context context = provider.getContext();
        ExecutorServices result = (ExecutorServices) context.lookup("EsperIODBAdapter/ExecutorServices");
        assertNotNull(result);

        provider.destroy();
    }
}