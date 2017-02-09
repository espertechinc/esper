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
package com.espertech.esper.client.hook;

/**
 * Event indicating a named-window consuming statement is being removed.
 */
public class VirtualDataWindowEventConsumerRemove extends VirtualDataWindowEventConsumerBase {

    /**
     * Ctor.
     *
     * @param namedWindowName named window name
     * @param consumerObject  identifying object for consumer
     * @param statementName   statement name
     * @param agentInstanceId agent instance id
     */
    public VirtualDataWindowEventConsumerRemove(String namedWindowName, Object consumerObject, String statementName, int agentInstanceId) {
        super(namedWindowName, consumerObject, statementName, agentInstanceId);
    }
}
