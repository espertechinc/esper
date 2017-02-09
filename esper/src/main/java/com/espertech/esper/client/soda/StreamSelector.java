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
package com.espertech.esper.client.soda;

/**
 * Enumeration for representing selection of the remove stream or the insert stream, or both.
 */
public enum StreamSelector {
    /**
     * Indicates selection of the remove stream only.
     */
    RSTREAM_ONLY("rstream"),

    /**
     * Indicates selection of the insert stream only.
     */
    ISTREAM_ONLY("istream"),

    /**
     * Indicates selection of both the insert and the remove stream.
     */
    RSTREAM_ISTREAM_BOTH("irstream");

    private final String epl;

    private StreamSelector(String epl) {
        this.epl = epl;
    }

    /**
     * Returns syntactic text
     *
     * @return epl text
     */
    public String getEpl() {
        return epl;
    }
}
