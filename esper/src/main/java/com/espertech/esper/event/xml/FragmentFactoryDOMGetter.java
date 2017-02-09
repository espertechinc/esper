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
package com.espertech.esper.event.xml;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.FragmentEventType;
import com.espertech.esper.event.EventAdapterService;
import org.w3c.dom.Node;

/**
 * Factory for fragments for DOM getters.
 */
public class FragmentFactoryDOMGetter implements FragmentFactory {
    private final EventAdapterService eventAdapterService;
    private final BaseXMLEventType xmlEventType;
    private final String propertyExpression;

    private volatile EventType fragmentType;

    /**
     * Ctor.
     *
     * @param eventAdapterService for event type lookup
     * @param xmlEventType        the originating type
     * @param propertyExpression  property expression
     */
    public FragmentFactoryDOMGetter(EventAdapterService eventAdapterService, BaseXMLEventType xmlEventType, String propertyExpression) {
        this.eventAdapterService = eventAdapterService;
        this.xmlEventType = xmlEventType;
        this.propertyExpression = propertyExpression;
    }

    public EventBean getEvent(Node result) {
        if (fragmentType == null) {
            FragmentEventType type = xmlEventType.getFragmentType(propertyExpression);
            if (type == null) {
                return null;
            }
            fragmentType = type.getFragmentType();
        }

        return eventAdapterService.adapterForTypedDOM(result, fragmentType);
    }
}
