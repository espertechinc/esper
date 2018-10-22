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
package com.espertech.esper.common.internal.epl.historical.database.connection;

import com.espertech.esper.common.client.configuration.common.ConfigurationCommonDBRef;
import com.espertech.esper.common.internal.settings.ClasspathImportService;
import com.espertech.esper.common.internal.support.SupportClasspathImport;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

public class TestDatabaseDMConnFactory extends TestCase {
    private DatabaseDMConnFactory databaseDMConnFactoryOne;
    private DatabaseDMConnFactory databaseDMConnFactoryTwo;
    private DatabaseDMConnFactory databaseDMConnFactoryThree;

    public void setUp() throws Exception {
        ClasspathImportService engineImportService = SupportClasspathImport.INSTANCE;

        // driver-manager config 1
        ConfigurationCommonDBRef config = new ConfigurationCommonDBRef();
        config.setDriverManagerConnection(SupportDatabaseURL.DRIVER, SupportDatabaseURL.FULLURL, new Properties());
        config.setConnectionAutoCommit(true);
        config.setConnectionCatalog("test");
        config.setConnectionTransactionIsolation(1);
        config.setConnectionReadOnly(true);
        databaseDMConnFactoryOne = new DatabaseDMConnFactory((ConfigurationCommonDBRef.DriverManagerConnection) config.getConnectionFactoryDesc(), config.getConnectionSettings(), engineImportService);

        // driver-manager config 2
        config = new ConfigurationCommonDBRef();
        config.setDriverManagerConnection(SupportDatabaseURL.DRIVER, SupportDatabaseURL.PARTURL, SupportDatabaseURL.DBUSER, SupportDatabaseURL.DBPWD);
        databaseDMConnFactoryTwo = new DatabaseDMConnFactory((ConfigurationCommonDBRef.DriverManagerConnection) config.getConnectionFactoryDesc(), config.getConnectionSettings(), engineImportService);

        // driver-manager config 3
        config = new ConfigurationCommonDBRef();
        Properties properties = new Properties();
        properties.setProperty("user", SupportDatabaseURL.DBUSER);
        properties.setProperty("password", SupportDatabaseURL.DBPWD);
        config.setDriverManagerConnection(SupportDatabaseURL.DRIVER, SupportDatabaseURL.PARTURL, properties);
        databaseDMConnFactoryThree = new DatabaseDMConnFactory((ConfigurationCommonDBRef.DriverManagerConnection) config.getConnectionFactoryDesc(), config.getConnectionSettings(), engineImportService);
    }

    public void testGetConnection() throws Exception {
        Connection connection = databaseDMConnFactoryOne.getConnection();
        tryAndCloseConnection(connection);

        connection = databaseDMConnFactoryTwo.getConnection();
        tryAndCloseConnection(connection);

        connection = databaseDMConnFactoryThree.getConnection();
        tryAndCloseConnection(connection);
    }

    private void tryAndCloseConnection(Connection connection) throws Exception {
        Statement stmt = connection.createStatement();
        stmt.execute("select 1 from dual");
        ResultSet result = stmt.getResultSet();
        result.next();
        assertEquals(1, result.getInt(1));
        result.close();
        stmt.close();
        connection.close();
    }

    private final static Logger log = LoggerFactory.getLogger(TestDatabaseDMConnFactory.class);
}
