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
package com.espertech.esper.common.client.annotation;

/**
 * Annotation to target certain constructs.
 */
public enum AppliesTo {
    /**
     * Undefined
     */
    UNDEFINED,

    /**
     * Unique-view
     */
    UNIQUE,

    /**
     * Group-by
     */
    GROUPBY,

    /**
     * Index
     */
    INDEX,

    /**
     * Output rate limiting
     */
    OUTPUTLIMIT,

    /**
     * Match-recognize
     */
    MATCHRECOGNIZE,

    /**
     * Contexts
     */
    CONTEXT,

    /**
     * Prior
     */
    PRIOR,

    /**
     * Rank window
     */
    RANK,

    /**
     * Pattern every-distinct
     */
    EVERYDISTINCT,

    /**
     * Sorted window
     */
    SORTEDWIN,

    /**
     * Time order window
     */
    TIMEORDERWIN,

    /**
     * Time-to-live window
     */
    TIMETOLIVEWIN,

    /**
     * Keep-all window
     */
    KEEPALLWIN,

    /**
     * Pattern
     */
    PATTERN,

    /**
     * Time-accumulative window
     */
    TIMEACCUMWIN,

    /**
     * Time-batch window
     */
    TIMEBATCHWIN,

    /**
     * Length-batch window
     */
    TIMELENGTHBATCHWIN,

    /**
     * Grouped window
     */
    GROUPWIN,

    /**
     * Length window
     */
    LENGTHWIN,

    /**
     * Time window
     */
    TIMEWIN,

    /**
     * Length-batch window
     */
    LENGTHBATCHWIN,

    /**
     * Previous functions
     */
    PREV,

    /**
     * Expression window
     */
    EXPRESSIONWIN,

    /**
     * Expression batch window
     */
    EXPRESSIONBATCHWIN,

    /**
     * Pattern followed-by
     */
    FOLLOWEDBY,

    /**
     * First-length window
     */
    FIRSTLENGTHWIN,

    /**
     * Externally-timed window
     */
    EXTTIMEDWIN,

    /**
     * Externally-timed batch window
     */
    EXTTIMEDBATCHWIN
}
