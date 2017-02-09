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
 * For use in select clauses for specifying a selected stream: select a.* from MyEvent as a, MyOther as b
 */
public class SelectClauseStreamRawSpec implements SelectClauseElementRaw {
    private String streamName;
    private String optionalAsName;
    private static final long serialVersionUID = 3679315688549186789L;

    /**
     * Ctor.
     *
     * @param streamName     is the stream name of the stream to select
     * @param optionalAsName is the column name
     */
    public SelectClauseStreamRawSpec(String streamName, String optionalAsName) {
        this.streamName = streamName;
        this.optionalAsName = optionalAsName;
    }

    /**
     * Returns the stream name (e.g. select streamName from MyEvent as streamName).
     *
     * @return name
     */
    public String getStreamName() {
        return streamName;
    }

    /**
     * Returns the column name.
     *
     * @return name
     */
    public String getOptionalAsName() {
        return optionalAsName;
    }
}
