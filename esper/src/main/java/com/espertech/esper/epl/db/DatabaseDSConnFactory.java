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
package com.espertech.esper.epl.db;

import com.espertech.esper.client.ConfigurationDBRef;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database connection factory using {@link InitialContext} and {@link DataSource} to obtain connections.
 */
public class DatabaseDSConnFactory implements DatabaseConnectionFactory {
    private final ConfigurationDBRef.DataSourceConnection dsConfig;
    private final ConfigurationDBRef.ConnectionSettings connectionSettings;

    private DataSource dataSource;

    /**
     * Ctor.
     *
     * @param dsConfig           is the datasource object name and initial context properties.
     * @param connectionSettings are the connection-level settings
     */
    public DatabaseDSConnFactory(ConfigurationDBRef.DataSourceConnection dsConfig,
                                 ConfigurationDBRef.ConnectionSettings connectionSettings) {
        this.dsConfig = dsConfig;
        this.connectionSettings = connectionSettings;
    }

    public Connection getConnection() throws DatabaseConfigException {
        if (dataSource == null) {
            Properties envProps = dsConfig.getEnvProperties();
            if (envProps == null) {
                envProps = new Properties();
            }

            InitialContext ctx;
            try {
                if (!envProps.isEmpty()) {
                    ctx = new InitialContext(envProps);
                } else {
                    ctx = new InitialContext();
                }
            } catch (NamingException ex) {
                throw new DatabaseConfigException("Error instantiating initial context", ex);
            }

            DataSource ds;
            String lookupName = dsConfig.getContextLookupName();
            try {
                ds = (DataSource) ctx.lookup(lookupName);
            } catch (NamingException ex) {
                throw new DatabaseConfigException("Error looking up data source in context using name '" + lookupName + '\'', ex);
            }

            if (ds == null) {
                throw new DatabaseConfigException("Null data source obtained through context using name '" + lookupName + '\'');
            }

            dataSource = ds;
        }

        Connection connection;
        try {
            connection = dataSource.getConnection();
        } catch (SQLException ex) {
            String detail = "SQLException: " + ex.getMessage() +
                    " SQLState: " + ex.getSQLState() +
                    " VendorError: " + ex.getErrorCode();

            throw new DatabaseConfigException("Error obtaining database connection using datasource " +
                    "with detail " + detail, ex);
        }

        DatabaseDMConnFactory.setConnectionOptions(connection, connectionSettings);

        return connection;
    }
}
