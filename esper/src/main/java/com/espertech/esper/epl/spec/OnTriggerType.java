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
 * Enum for the type of on-trigger statement.
 */
public enum OnTriggerType {
    /**
     * For on-delete triggers that delete from a named window when a triggering event arrives.
     */
    ON_DELETE("on-delete"),

    /**
     * For on-select triggers that selected from a named window when a triggering event arrives.
     */
    ON_SELECT("on-select"),

    /**
     * For the on-insert split-stream syntax allowing multiple insert-into streams.
     */
    ON_SPLITSTREAM("on-insert-multiple"),

    /**
     * For on-set triggers that set variable values when a triggering event arrives.
     */
    ON_SET("on-set"),

    /**
     * For on-update triggers that update an event in a named window when a triggering event arrives.
     */
    ON_UPDATE("on-update"),

    /**
     * For on-merge triggers that insert/update an event in a named window when a triggering event arrives.
     */
    ON_MERGE("on-merge");

    private final String textual;

    private OnTriggerType(String textual) {
        this.textual = textual;
    }

    public String getTextual() {
        return textual;
    }
}
