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

import com.espertech.esper.common.internal.epl.historical.database.core.ColumnSettings;

/**
 * Service providing database connection factory and configuration information
 * for use with historical data polling.
 */
public interface DatabaseConfigServiceCompileTime extends DatabaseConfigService {
    /**
     * Returns the column metadata settings for the database.
     *
     * @param databaseName is the database name
     * @return indicators for change case, metadata retrieval strategy and others
     * @throws DatabaseConfigException if the name was not configured
     */
    public ColumnSettings getQuerySetting(String databaseName) throws DatabaseConfigException;
}
