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
package com.espertech.esper.epl.named;

import com.espertech.esper.client.EventType;

/**
 * View for the on-delete statement that handles removing events from a named window.
 */
public abstract class NamedWindowOnExprBaseViewFactory implements NamedWindowOnExprFactory {
    protected final EventType namedWindowEventType;

    protected NamedWindowOnExprBaseViewFactory(EventType namedWindowEventType) {
        this.namedWindowEventType = namedWindowEventType;
    }
}
