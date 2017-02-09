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
package com.espertech.esper.core.context.subselect;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.activator.ViewableActivator;
import com.espertech.esper.epl.spec.StreamSpecCompiled;
import com.espertech.esper.view.ViewFactoryChain;

/**
 * Entry holding lookup resource references for use by {@link SubSelectActivationCollection}.
 */
public class SubSelectActivationHolder {
    private final int streamNumber;
    private final EventType viewableType;
    private final ViewFactoryChain viewFactoryChain;
    private final ViewableActivator activator;
    private final StreamSpecCompiled streamSpecCompiled;

    public SubSelectActivationHolder(int streamNumber, EventType viewableType, ViewFactoryChain viewFactoryChain, ViewableActivator activator, StreamSpecCompiled streamSpecCompiled) {
        this.streamNumber = streamNumber;
        this.viewableType = viewableType;
        this.viewFactoryChain = viewFactoryChain;
        this.activator = activator;
        this.streamSpecCompiled = streamSpecCompiled;
    }

    /**
     * Returns lookup stream number.
     *
     * @return stream num
     */
    public int getStreamNumber() {
        return streamNumber;
    }

    public EventType getViewableType() {
        return viewableType;
    }

    /**
     * Returns the lookup view factory chain
     *
     * @return view factory chain
     */
    public ViewFactoryChain getViewFactoryChain() {
        return viewFactoryChain;
    }

    public ViewableActivator getActivator() {
        return activator;
    }

    public StreamSpecCompiled getStreamSpecCompiled() {
        return streamSpecCompiled;
    }
}
