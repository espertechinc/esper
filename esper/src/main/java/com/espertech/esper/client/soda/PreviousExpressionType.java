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
 * Previous function type.
 */
public enum PreviousExpressionType {
    /**
     * Returns a previous event with the index counting from the last event towards the first event.
     */
    PREV,

    /**
     * Returns the count of previous events.
     */
    PREVCOUNT,

    /**
     * Returns a previous event with the index counting from the first event towards the last event.
     */
    PREVTAIL,

    /**
     * Returns all previous events.
     */
    PREVWINDOW
}