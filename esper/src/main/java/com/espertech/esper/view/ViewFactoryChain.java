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
package com.espertech.esper.view;

import com.espertech.esper.client.EventType;

import java.util.Collections;
import java.util.List;

/**
 * Holder for the logical chain of view factories.
 */
public class ViewFactoryChain {
    private List<ViewFactory> viewFactoryChain;
    private EventType streamEventType;

    /**
     * Ctor.
     *
     * @param streamEventType  is the event type of the event stream
     * @param viewFactoryChain is the chain of view factories
     */
    public ViewFactoryChain(EventType streamEventType, List<ViewFactory> viewFactoryChain) {
        this.streamEventType = streamEventType;
        this.viewFactoryChain = viewFactoryChain;
    }

    /**
     * Returns the final event type which is the event type of the last view factory in the chain,
     * or if the chain is empty then the stream's event type.
     *
     * @return final event type of the last view or stream
     */
    public EventType getEventType() {
        if (viewFactoryChain.isEmpty()) {
            return streamEventType;
        } else {
            return viewFactoryChain.get(viewFactoryChain.size() - 1).getEventType();
        }
    }

    /**
     * Returns the chain of view factories.
     *
     * @return view factory list
     */
    public List<ViewFactory> getViewFactoryChain() {
        return viewFactoryChain;
    }

    /**
     * Returns the number of data window factories for the chain.
     *
     * @return number of data window factories
     */
    public int getDataWindowViewFactoryCount() {
        int count = 0;
        for (ViewFactory chainElement : viewFactoryChain) {
            if (chainElement instanceof DataWindowViewFactory) {
                count++;
            }
        }
        return count;
    }

    public static ViewFactoryChain fromTypeNoViews(EventType eventType) {
        return new ViewFactoryChain(eventType, Collections.<ViewFactory>emptyList());
    }
}
