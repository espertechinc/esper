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
import com.espertech.esper.epl.core.engineimport.EngineImportService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database connection factory using {@link DriverManager} to obtain connections.
 */
public class DatabaseDMConnFactory implements DatabaseConnectionFactory {
    private final ConfigurationDBRef.DriverManagerConnection driverConfig;
    private final ConfigurationDBRef.ConnectionSettings connectionSettings;

    /**
     * Ctor.
     *
     * @param driverConfig       is the driver manager configuration
     * @param connectionSettings are connection-level settings
     * @param engineImportService engine imports
     * @throws DatabaseConfigException thrown if the driver class cannot be loaded
     */
    public DatabaseDMConnFactory(ConfigurationDBRef.DriverManagerConnection driverConfig,
                                 ConfigurationDBRef.ConnectionSettings connectionSettings,
                                 EngineImportService engineImportService)
            throws DatabaseConfigException {
        this.driverConfig = driverConfig;
        this.connectionSettings = connectionSettings;

        // load driver class
        String driverClassName = driverConfig.getClassName();
        try {
            engineImportService.getClassForNameProvider().classForName(driverClassName);
        } catch (ClassNotFoundException ex) {
            throw new DatabaseConfigException("Error loading driver class '" + driverClassName + '\'', ex);
        } catch (RuntimeException ex) {
            throw new DatabaseConfigException("Error loading driver class '" + driverClassName + '\'', ex);
        }
    }

    public Connection getConnection() throws DatabaseConfigException {
        // use driver manager to get a connection
        Connection connection;
        String url = driverConfig.getUrl();
        Properties properties = driverConfig.getOptionalProperties();
        if (properties == null) {
            properties = new Properties();
        }
        try {
            String user = driverConfig.getOptionalUserName();
            String pwd = driverConfig.getOptionalPassword();
            if ((user == null) && (pwd == null) && (properties.isEmpty())) {
                connection = DriverManager.getConnection(url);
            } else if (!properties.isEmpty()) {
                connection = DriverManager.getConnection(url, properties);
            } else {
                connection = DriverManager.getConnection(url, user, pwd);
            }
        } catch (SQLException ex) {
            String detail = "SQLException: " + ex.getMessage() +
                    " SQLState: " + ex.getSQLState() +
                    " VendorError: " + ex.getErrorCode();

            throw new DatabaseConfigException("Error obtaining database connection using url '" + url +
                    "' with detail " + detail, ex);
        }

        setConnectionOptions(connection, connectionSettings);

        return connection;
    }

    /**
     * Method to set connection-level configuration settings.
     *
     * @param connection         is the connection to set on
     * @param connectionSettings are the settings to apply
     * @throws DatabaseConfigException is thrown if an SQLException is thrown
     */
    protected static void setConnectionOptions(Connection connection,
                                               ConfigurationDBRef.ConnectionSettings connectionSettings)
            throws DatabaseConfigException {
        try {
            if (connectionSettings.getReadOnly() != null) {
                connection.setReadOnly(connectionSettings.getReadOnly());
            }
        } catch (SQLException ex) {
            throw new DatabaseConfigException("Error setting read-only to " + connectionSettings.getReadOnly() +
                    " on connection with detail " + getDetail(ex), ex);
        }

        try {
            if (connectionSettings.getTransactionIsolation() != null) {
                connection.setTransactionIsolation(connectionSettings.getTransactionIsolation());
            }
        } catch (SQLException ex) {
            throw new DatabaseConfigException("Error setting transaction isolation level to " +
                    connectionSettings.getTransactionIsolation() + " on connection with detail " + getDetail(ex), ex);
        }

        try {
            if (connectionSettings.getCatalog() != null) {
                connection.setCatalog(connectionSettings.getCatalog());
            }
        } catch (SQLException ex) {
            throw new DatabaseConfigException("Error setting catalog to '" + connectionSettings.getCatalog() +
                    "' on connection with detail " + getDetail(ex), ex);
        }

        try {
            if (connectionSettings.getAutoCommit() != null) {
                connection.setCatalog(connectionSettings.getCatalog());
            }
        } catch (SQLException ex) {
            throw new DatabaseConfigException("Error setting auto-commit to " + connectionSettings.getAutoCommit() +
                    " on connection with detail " + getDetail(ex), ex);
        }
    }

    private static String getDetail(SQLException ex) {
        return "SQLException: " + ex.getMessage() +
                " SQLState: " + ex.getSQLState() +
                " VendorError: " + ex.getErrorCode();
    }
}
