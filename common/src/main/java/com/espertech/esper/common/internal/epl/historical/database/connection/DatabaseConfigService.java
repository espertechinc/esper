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

/**
 * Service providing database connection factory and configuration information
 * for use with historical data polling.
 */
public interface DatabaseConfigService {
    /**
     * Returns a connection factory for a configured database.
     *
     * @param databaseName is the name of the database
     * @return is a connection factory to use to get connections to the database
     * @throws DatabaseConfigException is thrown to indicate database configuration errors
     */
    DatabaseConnectionFactory getConnectionFactory(String databaseName) throws DatabaseConfigException;
}
