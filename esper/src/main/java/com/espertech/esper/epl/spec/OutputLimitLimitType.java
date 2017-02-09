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
package com.espertech.esper.epl.spec;

/**
 * Enum for describing the type of output limit within an interval.
 */
public enum OutputLimitLimitType {
    /**
     * Output first event, relative to the output batch.
     */
    FIRST,

    /**
     * Output last event, relative to the output batch.
     */
    LAST,

    /**
     * The ALL keyword has been explicitly specified: Output all events, relative to the output batch.
     * <p>
     * In the fully-grouped and aggregated case, the explicit ALL outputs one row for each group.
     */
    ALL,

    /**
     * The ALL keyword has not been explicitly specified: Output all events, relative to the output batch.
     * <p>
     * In the fully-grouped and aggregated case, the default ALL outputs all events of the batch row-by-row, multiple per group.
     */
    DEFAULT,

    /**
     * Output a snapshot of the current state, relative to the full historical state of a statement.
     */
    SNAPSHOT
}
