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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esperio.db.config.Column;
import com.espertech.esperio.db.config.ConfigurationDBAdapter;
import com.espertech.esperio.db.config.Executor;
import com.espertech.esperio.db.config.UpsertQuery;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class TestDBAdapterUpsert extends TestCase {
    private final static String ENGINE_URI = "TESTDBURI";

    private final static String TABLE_NAME = "mytestupsert";

    public void setUp() throws Exception {
        SupportDatabaseService.truncateTable(TABLE_NAME);
    }

    public void testUpsert() throws InterruptedException {
        ConfigurationDBAdapter adapterConfig = new ConfigurationDBAdapter();
        ConfigurationDBRef conn = SupportDatabaseService.makeDBConfig();
        adapterConfig.getJdbcConnections().put("conn1", conn);

        UpsertQuery upsertQuery = new UpsertQuery();
        upsertQuery.setName("MyTestUpsert");
        upsertQuery.setStream("UpsertDBOutputStream");
        upsertQuery.setConnection("conn1");
        upsertQuery.getKeys().add(new Column("k1", "key1", "varchar"));
        upsertQuery.getKeys().add(new Column("k2", "key2", "integer"));
        upsertQuery.getValues().add(new Column("v1", "value1", "varchar"));
        upsertQuery.getValues().add(new Column("v2", "value2", "double"));
        upsertQuery.setTableName(TABLE_NAME);
        upsertQuery.setExecutorName("queue1");
        adapterConfig.getUpsertQueries().add(upsertQuery);

        adapterConfig.getExecutors().put("queue1", new Executor(2));

        EsperIODBAdapter dbAdapter = new EsperIODBAdapter(adapterConfig, ENGINE_URI);

        Configuration engineConfig = new Configuration();
        engineConfig.addDatabaseReference("testdb", SupportDatabaseService.makeDBConfig());
        engineConfig.addEventType("SupportDBBean", SupportDBBean.class);
        engineConfig.addEventType("UpsertDBOutputStream", getUpsertType());
        EPServiceProvider provider = EPServiceProviderManager.getProvider(ENGINE_URI, engineConfig);

        dbAdapter.start();

        provider.getEPAdministrator().createEPL("insert into UpsertDBOutputStream(k1, k2, v1, v2) select key1 as k1, key2 as k2, value1 as v1, value2 as v2 from SupportDBBean");
        provider.getEPRuntime().sendEvent(new SupportDBBean("myk1", 10, "myv1", 20.2d));

        String[] fields = "key1,key2,value1,value2".split(",");
        EPStatement stmt = provider.getEPAdministrator().createEPL("select * from sql:testdb ['select * from mytestupsert order by key1']");
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"myk1", 10, "myv1", 20.2d}});

        provider.getEPRuntime().sendEvent(new SupportDBBean("myk2", 11, "myv2", 23.2d));
        Thread.sleep(500); // required since configured for threadpool exec
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]
                {{"myk1", 10, "myv1", 20.2d}, {"myk2", 11, "myv2", 23.2d}});

        provider.getEPRuntime().sendEvent(new SupportDBBean("myk3", null, null, null));
        Thread.sleep(500); // required since configured for threadpool exec
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]
                {{"myk1", 10, "myv1", 20.2d}, {"myk2", 11, "myv2", 23.2d}, {"myk3", null, null, null}});

        dbAdapter.destroy();
    }

    private Map<String, Object> getUpsertType() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("k1", "string");
        map.put("k2", "int");
        map.put("v1", "string");
        map.put("v2", "double");
        return map;
    }
}
