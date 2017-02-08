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
package com.espertech.esper.example.terminal.jse.listener;

import com.espertech.esper.client.UpdateListener;

/**
 * The parent class of our EPA - event processing agents
 * <p/>
 * Our EPA will get triggered based on the ESP/CEP queries registered and can
 * pipe complex composite events to the bound ComplexEventListener
 */
public abstract class BaseTerminalListener implements UpdateListener {

    protected ComplexEventListener complexEventListener;

    public BaseTerminalListener(ComplexEventListener complexEventListener) {
        this.complexEventListener = complexEventListener;
    }

}
