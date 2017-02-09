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
 * Selector for use in output rate limiting.
 */
public enum OutputLimitSelector {
    /**
     * Output first event of last interval.
     */
    FIRST("first"),

    /**
     * Output last event of last interval.
     */
    LAST("last"),

    /**
     * Output all events of last interval. For group-by statements, output all groups regardless whether the group changed between the last output.
     */
    ALL("all"),

    /**
     * Output all events of last interval.
     */
    DEFAULT("default"),

    /**
     * Output all events as a snapshot considering the current state regardless of interval.
     */
    SNAPSHOT("snapshot");

    private String text;

    private OutputLimitSelector(String text) {
        this.text = text;
    }

    /**
     * Returns the text for the selector.
     *
     * @return text
     */
    public String getText() {
        return text;
    }
}
