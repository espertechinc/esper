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
 * Skip-enum for match_recognize.
 */
public enum MatchRecognizeSkipEnum {
    /**
     * Skip to current row.
     */
    TO_CURRENT_ROW,

    /**
     * Skip to next row.
     */
    TO_NEXT_ROW,

    /**
     * Skip past last row.
     */
    PAST_LAST_ROW
}