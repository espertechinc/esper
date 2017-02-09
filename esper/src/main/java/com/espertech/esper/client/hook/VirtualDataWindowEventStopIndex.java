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
package com.espertech.esper.client.hook;

/**
 * Event to indicate that for a virtual data window an exitsing index is being stopped or destroyed.
 */
public class VirtualDataWindowEventStopIndex extends VirtualDataWindowEvent {

    private final String namedWindowName;
    private final String indexName;

    /**
     * Ctor.
     *
     * @param namedWindowName named window name
     * @param indexName       index name
     */
    public VirtualDataWindowEventStopIndex(String namedWindowName, String indexName) {
        this.namedWindowName = namedWindowName;
        this.indexName = indexName;
    }

    /**
     * Returns the index name.
     *
     * @return index name
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * Returns the named window name.
     *
     * @return named window name
     */
    public String getNamedWindowName() {
        return namedWindowName;
    }
}
