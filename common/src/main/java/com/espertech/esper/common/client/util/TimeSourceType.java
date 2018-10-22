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
package com.espertech.esper.common.client.util;

/**
 * Time source type.
 */
public enum TimeSourceType {
    /**
     * Millisecond time source type with time originating from System.currentTimeMillis
     */
    MILLI,

    /**
     * Nanosecond time source from a wallclock-adjusted System.nanoTime
     */
    NANO
}
