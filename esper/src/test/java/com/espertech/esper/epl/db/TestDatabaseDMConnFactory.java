/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.db;

import junit.framework.TestCase;
import com.espertech.esper.support.epl.SupportDatabaseService;
import com.espertech.esper.client.ConfigurationDBRef;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestDatabaseDMConnFactory extends TestCase
{
    private DatabaseDMConnFactory databaseDMConnFactoryOne;
    private DatabaseDMConnFactory databaseDMConnFactoryTwo;
    private DatabaseDMConnFactory databaseDMConnFactoryThree;

    public void setUp() throws Exception
    {
        // driver-manager config 1
        ConfigurationDBRef config = new ConfigurationDBRef();
        config.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        config.setConnectionAutoCommit(true);
        config.setConnectionCatalog("test");
        config.setConnectionTransactionIsolation(1);
        config.setConnectionReadOnly(true);
        databaseDMConnFactoryOne = new DatabaseDMConnFactory((ConfigurationDBRef.DriverManagerConnection) config.getConnectionFactoryDesc(), config.getConnectionSettings());

        // driver-manager config 2
        config = new ConfigurationDBRef();
        config.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.PARTURL, SupportDatabaseService.DBUSER, SupportDatabaseService.DBPWD);
        databaseDMConnFactoryTwo = new DatabaseDMConnFactory((ConfigurationDBRef.DriverManagerConnection) config.getConnectionFactoryDesc(), config.getConnectionSettings());

        // driver-manager config 3
        config = new ConfigurationDBRef();
        Properties properties = new Properties();
        properties.setProperty("user", SupportDatabaseService.DBUSER);
        properties.setProperty("password", SupportDatabaseService.DBPWD);
        config.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.PARTURL, properties);
        databaseDMConnFactoryThree = new DatabaseDMConnFactory((ConfigurationDBRef.DriverManagerConnection) config.getConnectionFactoryDesc(), config.getConnectionSettings());
    }

    public void testGetConnection() throws Exception
    {
        Connection connection = databaseDMConnFactoryOne.getConnection();
        tryAndCloseConnection(connection);

        connection = databaseDMConnFactoryTwo.getConnection();
        tryAndCloseConnection(connection);

        connection = databaseDMConnFactoryThree.getConnection();
        tryAndCloseConnection(connection);
    }

    private void tryAndCloseConnection(Connection connection) throws Exception
    {
        Statement stmt = connection.createStatement();
        stmt.execute("select 1 from dual");
        ResultSet result = stmt.getResultSet();
        result.next();
        assertEquals(1, result.getInt(1));
        result.close();
        stmt.close();
        connection.close();
    }

    private static Log log = LogFactory.getLog(TestDatabaseDMConnFactory.class);
}
