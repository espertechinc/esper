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

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonDBRef;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esperio.db.config.Column;
import com.espertech.esperio.db.config.ConfigurationDBAdapter;
import com.espertech.esperio.db.config.Executor;
import com.espertech.esperio.db.config.UpsertQuery;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

import static com.espertech.esperio.db.SupportCompileUtil.compileDeploy;

public class TestDBAdapterUpsert extends TestCase {
    private final static String RUNTIME_URI = "TESTDBURI";

    private final static String TABLE_NAME = "mytestupsert";

    public void setUp() throws Exception {
        SupportDatabaseService.truncateTable(TABLE_NAME);
    }

    public void testUpsert() throws InterruptedException {
        ConfigurationDBAdapter adapterConfig = new ConfigurationDBAdapter();
        ConfigurationCommonDBRef conn = SupportDatabaseService.makeDBConfig();
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

        EsperIODBAdapter dbAdapter = new EsperIODBAdapter(adapterConfig, RUNTIME_URI);

        Configuration config = new Configuration();
        config.getCommon().addDatabaseReference("testdb", SupportDatabaseService.makeDBConfig());
        config.getCommon().addEventType("SupportDBBean", SupportDBBean.class);
        config.getCommon().addEventType("UpsertDBOutputStream", getUpsertType());
        EPRuntime runtime = EPRuntimeProvider.getRuntime(RUNTIME_URI, config);

        dbAdapter.start();

        compileDeploy(runtime, "insert into UpsertDBOutputStream(k1, k2, v1, v2) select key1 as k1, key2 as k2, value1 as v1, value2 as v2 from SupportDBBean");
        runtime.getEventService().sendEventBean(new SupportDBBean("myk1", 10, "myv1", 20.2d), "SupportDBBean");

        String[] fields = "key1,key2,value1,value2".split(",");
        EPStatement stmt = compileDeploy(runtime, "select * from sql:testdb ['select * from mytestupsert order by key1']").getStatements()[0];
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"myk1", 10, "myv1", 20.2d}});

        runtime.getEventService().sendEventBean(new SupportDBBean("myk2", 11, "myv2", 23.2d), "SupportDBBean");
        Thread.sleep(500); // required since configured for threadpool exec
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]
            {{"myk1", 10, "myv1", 20.2d}, {"myk2", 11, "myv2", 23.2d}});

        runtime.getEventService().sendEventBean(new SupportDBBean("myk3", null, null, null), "SupportDBBean");
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
