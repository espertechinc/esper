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
package com.espertech.esper.client.context;

/**
 * Context state event indicating a context deactivated.
 */
public class ContextStateEventContextDeactivated extends ContextStateEvent {

    /**
     * Ctor.
     * @param engineURI engine URI
     * @param contextName context name
     */
    public ContextStateEventContextDeactivated(String engineURI, String contextName) {
        super(engineURI, contextName);
    }
}
