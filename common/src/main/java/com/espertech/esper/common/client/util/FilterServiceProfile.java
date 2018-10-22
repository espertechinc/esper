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
 * Filter service profile.
 */
public enum FilterServiceProfile {
    /**
     * If filters are mostly static, the default.
     */
    READMOSTLY,

    /**
     * For very dynamic filters that come and go in a highly threaded environment.
     */
    READWRITE
}
