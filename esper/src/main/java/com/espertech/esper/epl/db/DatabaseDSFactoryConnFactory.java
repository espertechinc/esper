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
import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.util.JavaClassHelper;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database connection factory using {@link javax.naming.InitialContext} and {@link javax.sql.DataSource} to obtain connections.
 */
public class DatabaseDSFactoryConnFactory implements DatabaseConnectionFactory {
    private final ConfigurationDBRef.ConnectionSettings connectionSettings;
    private DataSource dataSource;

    /**
     * Ctor.
     *
     * @param dsConfig           is the datasource object name and initial context properties.
     * @param connectionSettings are the connection-level settings
     * @param engineImportService engine imports
     * @throws DatabaseConfigException when the factory cannot be configured
     */
    public DatabaseDSFactoryConnFactory(ConfigurationDBRef.DataSourceFactory dsConfig,
                                        ConfigurationDBRef.ConnectionSettings connectionSettings,
                                        EngineImportService engineImportService)
            throws DatabaseConfigException {
        this.connectionSettings = connectionSettings;

        Class clazz;
        try {
            clazz = engineImportService.getClassForNameProvider().classForName(dsConfig.getFactoryClassname());
        } catch (ClassNotFoundException e) {
            throw new DatabaseConfigException("Class '" + dsConfig.getFactoryClassname() + "' cannot be loaded", e);
        }

        Object obj;
        try {
            obj = clazz.newInstance();
        } catch (InstantiationException e) {
            throw new ConfigurationException("Class '" + clazz + "' cannot be instantiated", e);
        } catch (IllegalAccessException e) {
            throw new ConfigurationException("Illegal access instantiating class '" + clazz + "'", e);
        }

        // find method : static DataSource createDataSource(Properties properties)
        Method method;
        try {
            method = clazz.getMethod("createDataSource", Properties.class);
        } catch (NoSuchMethodException e) {
            throw new ConfigurationException("Class '" + clazz + "' does not provide a static method by name createDataSource accepting a single Properties object as parameter", e);
        }
        if (method == null) {
            throw new ConfigurationException("Class '" + clazz + "' does not provide a static method by name createDataSource accepting a single Properties object as parameter");
        }
        if (!JavaClassHelper.isImplementsInterface(method.getReturnType(), DataSource.class)) {
            throw new ConfigurationException("On class '" + clazz + "' the static method by name createDataSource does not return a DataSource");
        }

        Object result;
        try {
            result = method.invoke(obj, dsConfig.getProperties());
        } catch (IllegalAccessException e) {
            throw new ConfigurationException("Class '" + clazz + "' failed in method createDataSource :" + e.getMessage(), e);
        } catch (InvocationTargetException e) {
            throw new ConfigurationException("Class '" + clazz + "' failed in method createDataSource :" + e.getMessage(), e);
        }
        if (result == null) {
            throw new ConfigurationException("Method createDataSource returned a null value for DataSource");
        }

        dataSource = (DataSource) result;
    }

    public Connection getConnection() throws DatabaseConfigException {
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
