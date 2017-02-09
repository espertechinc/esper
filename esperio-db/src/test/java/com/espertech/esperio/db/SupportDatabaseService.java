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

import com.espertech.esper.client.ConfigurationDBRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SupportDatabaseService {
    private static final Logger log = LoggerFactory.getLogger(SupportDatabaseService.class);

    public final static String DBNAME_FULLURL = "mydb";
    public final static String DBNAME_PARTURL = "mydb_part";

    public final static String DBUSER = "root";
    public final static String DBPWD = "password";
    public final static String DRIVER = "com.mysql.jdbc.Driver";
    public final static String FULLURL = "jdbc:mysql://localhost/test?user=root&password=password";
    public final static String PARTURL = "jdbc:mysql://localhost/test";

    public static ConfigurationDBRef makeDBConfig() {
        ConfigurationDBRef configDB = new ConfigurationDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        return configDB;
    }

    public static void truncateTable(String tableName) throws SQLException {
        Connection connection = getConnection(PARTURL, DBUSER, DBPWD);
        connection.setAutoCommit(true);
        Statement stmt = connection.createStatement();
        String sql = "delete from " + tableName;
        log.info("Executing sql : " + sql);
        stmt.executeUpdate(sql);
        stmt.close();
        connection.close();
    }

    public static Object[][] readAll(String tableName) throws SQLException {
        Connection connection = getConnection(PARTURL, DBUSER, DBPWD);
        connection.setAutoCommit(true);
        Statement stmt = connection.createStatement();
        String sql = "select * from " + tableName;
        log.info("Executing sql : " + sql);
        ResultSet resultSet = stmt.executeQuery(sql);

        List<Object[]> rows = new ArrayList<Object[]>();
        while (resultSet.next()) {
            List<Object> row = new ArrayList<Object>();
            for (int i = 0; i < resultSet.getMetaData().getColumnCount(); i++) {
                row.add(resultSet.getObject(i + 1));
            }
            rows.add(row.toArray());
        }

        Object[][] arr = new Object[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            arr[i] = rows.get(i);
        }

        stmt.close();
        connection.close();
        return arr;
    }

    /**
     * Strictly for use in regression testing, this method provides a connection via driver manager.
     *
     * @param url      url
     * @param username user
     * @param password password
     * @return connection
     */
    public static Connection getConnection(String url, String username, String password) {
        log.info("Creating new connection instance for pool for url " + url);

        Driver d;
        try {
            d = (Driver) Class.forName(DRIVER).newInstance();
            DriverManager.registerDriver(d);
        } catch (Exception e) {
            String message = "Failed to load and register driver class:" + e.getMessage();
            log.error(message, e);
            throw new RuntimeException(message, e);
        }

        Connection connection;
        try {
            connection = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            String message = "Failed to obtain a database connection using url '" +
                    url + "' and user '" + username + "' :" + e.getMessage();
            log.error(message, e);
            throw new RuntimeException(message, e);
        }

        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            String message = "Failed to set auto-commit on connection '" +
                    url + "' and user '" + username + "' :" + e.getMessage();
            log.error(message, e);
            throw new RuntimeException(message, e);
        }

        return connection;
    }
}
