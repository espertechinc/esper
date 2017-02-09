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
package com.espertech.esper.event;

/**
 * Marker interface for event types that need not transpose their property.
 * <p>
 * Transpose is the process of taking a fragment event property and adding the fragment to the
 * resulting type rather then the underlying property object.
 */
public interface NativeEventType {
}
