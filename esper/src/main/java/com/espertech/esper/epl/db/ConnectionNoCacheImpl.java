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

import com.espertech.esper.collection.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Implementation of a connection cache that simply doesn't cache but gets
 * a new connection and statement every request, and closes these every time
 * a client indicates done.
 */
public class ConnectionNoCacheImpl extends ConnectionCache {
    /**
     * Ctor.
     *
     * @param databaseConnectionFactory is the connection factory
     * @param sql                       is the statement sql
     */
    public ConnectionNoCacheImpl(DatabaseConnectionFactory databaseConnectionFactory, String sql) {
        super(databaseConnectionFactory, sql);
    }

    public Pair<Connection, PreparedStatement> getConnection() {
        return makeNew();
    }

    public void doneWith(Pair<Connection, PreparedStatement> pair) {
        close(pair);
    }

    public void destroy() {
        // no resources held
    }
}
