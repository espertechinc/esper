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
package com.espertech.esper.client.annotation;

/**
 * An execution directive for use in an EPL statement, that causes processing of an event to stop after the EPL statement
 * marked with @Drop has processed the event, applicable only if multiple statements must process the same event.
 * <p>
 * Ensure the engine configuration for prioritized execution is set before using this annotation.
 */
public @interface Drop {
}
