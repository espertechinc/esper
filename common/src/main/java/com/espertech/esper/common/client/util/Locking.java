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
 * Enumeration of blocking techniques.
 */
public enum Locking {
    /**
     * Spin lock blocking is good for locks held very shortly or generally uncontended locks and
     * is therefore the default.
     */
    SPIN,

    /**
     * Blocking that suspends a thread and notifies a thread to wake up can be
     * more expensive then spin locks.
     */
    SUSPEND
}
