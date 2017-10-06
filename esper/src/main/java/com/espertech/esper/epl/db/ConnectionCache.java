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

import com.espertech.esper.client.EPException;
import com.espertech.esper.collection.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Base class for a Connection and PreparedStatement cache.
 * <p>
 * Implementations control the lifecycle via lifecycle methods, or
 * may simple obtain new resources and close new resources every time.
 * <p>
 * This is not a pool - a cache is associated with one client class and that
 * class is expected to use cache methods in well-defined order of get, done-with and destroy.
 */
public abstract class ConnectionCache {
    private DatabaseConnectionFactory databaseConnectionFactory;
    private String sql;

    /**
     * Returns a cached or new connection and statement pair.
     *
     * @return connection and statement pair
     */
    public abstract Pair<Connection, PreparedStatement> getConnection();

    /**
     * Indicate to return the connection and statement pair after use.
     *
     * @param pair is the resources to return
     */
    public abstract void doneWith(Pair<Connection, PreparedStatement> pair);

    /**
     * Destroys cache closing all resources cached, if any.
     */
    public abstract void destroy();

    /**
     * Ctor.
     *
     * @param databaseConnectionFactory - connection factory
     * @param sql                       - statement sql
     */
    protected ConnectionCache(DatabaseConnectionFactory databaseConnectionFactory, String sql) {
        this.databaseConnectionFactory = databaseConnectionFactory;
        this.sql = sql;
    }

    /**
     * Close resources.
     *
     * @param pair is the resources to close.
     */
    protected static void close(Pair<Connection, PreparedStatement> pair) {
        log.info(".close Closing statement and connection");
        try {
            pair.getSecond().close();
        } catch (SQLException ex) {
            try {
                pair.getFirst().close();
            } catch (SQLException e) {
                log.error("Error closing JDBC connection:" + e.getMessage(), e);
            }
            throw new EPException("Error closing statement", ex);
        }

        try {
            pair.getFirst().close();
        } catch (SQLException ex) {
            throw new EPException("Error closing statement", ex);
        }
    }

    /**
     * Make a new pair of resources.
     *
     * @return pair of resources
     */
    protected Pair<Connection, PreparedStatement> makeNew() {
        log.info(".makeNew Obtaining new connection and statement");
        Connection connection;
        try {
            connection = databaseConnectionFactory.getConnection();
        } catch (DatabaseConfigException ex) {
            throw new EPException("Error obtaining connection", ex);
        }

        PreparedStatement preparedStatement;
        try {
            preparedStatement = connection.prepareStatement(sql);
        } catch (SQLException ex) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.warn("Error closing connection: " + e.getMessage(), e);
            }

            throw new EPException("Error preparing statement '" + sql + '\'', ex);
        }

        return new Pair<Connection, PreparedStatement>(connection, preparedStatement);
    }

    private final static Logger log = LoggerFactory.getLogger(ConnectionCache.class);
}
