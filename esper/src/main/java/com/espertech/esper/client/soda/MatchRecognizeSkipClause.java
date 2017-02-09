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
 * Skip clause enum for match recognize.
 */
public enum MatchRecognizeSkipClause {
    /**
     * Skip to current row.
     */
    TO_CURRENT_ROW("to current row"),

    /**
     * Skip to next row.
     */
    TO_NEXT_ROW("to next row"),

    /**
     * Skip past last row.
     */
    PAST_LAST_ROW("past last row");

    private String text;

    private MatchRecognizeSkipClause(String text) {
        this.text = text;
    }

    /**
     * Returns clause text.
     *
     * @return textual
     */
    public String getText() {
        return text;
    }
}
