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
 * Threading profile.
 */
public enum ThreadingProfile {
    /**
     * Large for use with 100 threads or more. Please see the documentation for more information.
     */
    LARGE,

    /**
     * For use with 100 threads or less.
     */
    NORMAL
}
