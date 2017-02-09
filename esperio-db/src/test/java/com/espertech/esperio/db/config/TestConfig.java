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
package com.espertech.esperio.db.config;

import com.espertech.esper.client.ConfigurationDBRef;
import junit.framework.TestCase;

import java.net.URL;

public class TestConfig extends TestCase {
    private ConfigurationDBAdapter config;

    public void setUp() {
        config = new ConfigurationDBAdapter();
    }

    public void testConfigureFromStream() throws Exception {
        URL url = this.getClass().getClassLoader().getResource("esperio-db-sample-config.xml");
        ConfigurationDBAdapterParser.doConfigure(config, url.openStream(), url.toString());
        assertFileConfig(config);
    }

    public void testEngineDefaults() {
        config = new ConfigurationDBAdapter();
    }

    protected static void assertFileConfig(ConfigurationDBAdapter config) throws Exception {
        assertEquals(3, config.getJdbcConnections().size());

        ConfigurationDBRef connection = config.getJdbcConnections().get("db1");
        ConfigurationDBRef.DataSourceConnection dsDef = (ConfigurationDBRef.DataSourceConnection) connection.getConnectionFactoryDesc();
        assertEquals("java:comp/env/jdbc/mydb", dsDef.getContextLookupName());
        assertEquals("{java.naming.provider.url=iiop://localhost:1050, java.naming.factory.initial=com.myclass.CtxFactory}", dsDef.getEnvProperties().toString());
        assertNull(connection.getConnectionSettings().getAutoCommit());
        assertNull(connection.getConnectionSettings().getCatalog());
        assertNull(connection.getConnectionSettings().getReadOnly());
        assertNull(connection.getConnectionSettings().getTransactionIsolation());

        // assert database reference - data source factory and DBCP config
        connection = config.getJdbcConnections().get("db2");
        ConfigurationDBRef.DataSourceFactory dsFactory = (ConfigurationDBRef.DataSourceFactory) connection.getConnectionFactoryDesc();
        assertEquals("org.apache.commons.dbcp.BasicDataSourceFactory", dsFactory.getFactoryClassname());
        assertEquals("jdbc:mysql://localhost/test", dsFactory.getProperties().getProperty("url"));
        assertEquals("myusername", dsFactory.getProperties().getProperty("username"));
        assertEquals("mypassword", dsFactory.getProperties().getProperty("password"));
        assertEquals("com.mysql.jdbc.Driver", dsFactory.getProperties().getProperty("driverClassName"));
        assertEquals("2", dsFactory.getProperties().getProperty("initialSize"));
        assertEquals((Boolean) true, connection.getConnectionSettings().getAutoCommit());
        assertEquals("TEST", connection.getConnectionSettings().getCatalog());
        assertEquals(Boolean.TRUE, connection.getConnectionSettings().getReadOnly());
        assertEquals(new Integer(0), connection.getConnectionSettings().getTransactionIsolation());

        // assert database reference - driver manager config
        connection = config.getJdbcConnections().get("db3");
        ConfigurationDBRef.DriverManagerConnection dmDef = (ConfigurationDBRef.DriverManagerConnection) connection.getConnectionFactoryDesc();
        assertEquals("my.sql.Driver", dmDef.getClassName());
        assertEquals("jdbc:mysql://localhost/test?user=root&password=welcome", dmDef.getUrl());
        assertEquals("myuser", dmDef.getOptionalUserName());
        assertEquals("mypassword", dmDef.getOptionalPassword());
        assertEquals("{user=myuser, password=mypassword, somearg=someargvalue}", dmDef.getOptionalProperties().toString());
        assertEquals(null, connection.getConnectionSettings().getAutoCommit());
        assertEquals(null, connection.getConnectionSettings().getCatalog());
        assertEquals(null, connection.getConnectionSettings().getReadOnly());
        assertEquals(null, connection.getConnectionSettings().getTransactionIsolation());

        assertEquals(1, config.getDmlQueries().size());
        DMLQuery dmlQuery = config.getDmlQueries().get(0);
        assertEquals("db1", dmlQuery.getConnection());
        assertEquals("insert into MyEventStore(key1, value1, value2)\nvalues (?, ?, ?)", dmlQuery.getSql());
        assertEquals("InsertToDBStream", dmlQuery.getStream());
        assertEquals("MyInsertQuery", dmlQuery.getName());
        assertEquals("queue1", dmlQuery.getExecutorName());
        assertEquals((int) 2, (int) dmlQuery.getRetry());
        assertEquals(1d, dmlQuery.getRetryIntervalSec());
        assertEquals(3, dmlQuery.getBindings().size());
        BindingParameter binding = dmlQuery.getBindings().get(0);
        assertEquals(1, binding.getPosition());
        assertEquals("eventProperty1", binding.getPropertyName());
        binding = dmlQuery.getBindings().get(1);
        assertEquals(2, binding.getPosition());
        assertEquals("eventProperty2", binding.getPropertyName());

        UpsertQuery upsertQuery = config.getUpsertQueries().get(0);
        assertEquals("db1", upsertQuery.getConnection());
        assertEquals("MyKeyedTable", upsertQuery.getTableName());
        assertEquals("UpdateInsertDBTableTrigger", upsertQuery.getStream());
        assertEquals("UpdateInsertSample", upsertQuery.getName());
        assertEquals("queue1", upsertQuery.getExecutorName());
        assertEquals((int) 3, (int) upsertQuery.getRetry());
        assertNull(upsertQuery.getRetryIntervalSec());
        assertEquals(2, upsertQuery.getKeys().size());
        Column col = upsertQuery.getKeys().get(0);
        assertEquals("eventProperty1", col.getProperty());
        assertEquals("keyColumn1", col.getColumn());
        assertEquals("varchar", col.getType());
        assertEquals(2, upsertQuery.getValues().size());
        col = upsertQuery.getValues().get(0);
        assertEquals("eventProperty3", col.getProperty());
        assertEquals("valueColumn1", col.getColumn());
        assertEquals("varchar", col.getType());

        assertEquals(1, config.getExecutors().size());
        Executor workQueue = config.getExecutors().get("queue1");
        assertEquals(2, workQueue.getNumThreads());
    }
}
