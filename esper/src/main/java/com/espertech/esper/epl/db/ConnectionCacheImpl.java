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
 * Caches the Connection and PreparedStatement instance for reuse.
 */
public class ConnectionCacheImpl extends ConnectionCache {
    private Pair<Connection, PreparedStatement> cache;

    /**
     * Ctor.
     *
     * @param databaseConnectionFactory - connection factory
     * @param sql                       - statement sql
     */
    public ConnectionCacheImpl(DatabaseConnectionFactory databaseConnectionFactory, String sql) {
        super(databaseConnectionFactory, sql);
    }

    public Pair<Connection, PreparedStatement> getConnection() {
        if (cache == null) {
            cache = makeNew();
        }
        return cache;
    }

    public void doneWith(Pair<Connection, PreparedStatement> pair) {
        // no need to implement
    }

    public void destroy() {
        if (cache != null) {
            close(cache);
        }
        cache = null;
    }
}
