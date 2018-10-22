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
package com.espertech.esper.common.internal.event.xml;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventSender;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.util.EPRuntimeEventProcessWrapped;
import com.espertech.esper.common.internal.statement.thread.ThreadingCommon;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Event sender for XML DOM-backed events.
 * <p>
 * Allows sending only event objects of type Node or Document, does check the root name of the XML document
 * which must match the event type root name as configured. Any other event object generates an error.
 */
public class EventSenderXMLDOM implements EventSender {
    private final EPRuntimeEventProcessWrapped runtimeEventSender;
    private final BaseXMLEventType baseXMLEventType;
    private final boolean validateRootElement;
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;
    private final ThreadingCommon threadingService;

    /**
     * Ctor.
     *
     * @param runtimeEventSender         for processing events
     * @param baseXMLEventType           the event type
     * @param threadingService           for inbound threading
     * @param eventBeanTypedEventFactory for event bean creation
     */
    public EventSenderXMLDOM(EPRuntimeEventProcessWrapped runtimeEventSender, BaseXMLEventType baseXMLEventType, EventBeanTypedEventFactory eventBeanTypedEventFactory, ThreadingCommon threadingService) {
        this.runtimeEventSender = runtimeEventSender;
        this.baseXMLEventType = baseXMLEventType;
        this.validateRootElement = baseXMLEventType.getConfigurationEventTypeXMLDOM().isEventSenderValidatesRoot();
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
        this.threadingService = threadingService;
    }

    public void sendEvent(Object theEvent) throws EPException {
        sendEvent(theEvent, false);
    }

    public void routeEvent(Object theEvent) throws EPException {
        sendEvent(theEvent, true);
    }

    private void sendEvent(Object node, boolean isRoute) throws EPException {
        Node namedNode;
        if (node instanceof Document) {
            namedNode = ((Document) node).getDocumentElement();
        } else if (node instanceof Element) {
            namedNode = (Element) node;
        } else {
            throw new EPException("Unexpected event object type '" + node.getClass().getName() + "' encountered, please supply a org.w3c.dom.Document or Element node");
        }

        if (validateRootElement) {
            String getNodeName = namedNode.getLocalName();
            if (getNodeName == null) {
                getNodeName = namedNode.getNodeName();
            }

            if (!getNodeName.equals(baseXMLEventType.getRootElementName())) {
                throw new EPException("Unexpected root element name '" + getNodeName + "' encountered, expected a root element name of '" + baseXMLEventType.getRootElementName() + "'");
            }
        }

        EventBean theEvent = eventBeanTypedEventFactory.adapterForTypedDOM(namedNode, baseXMLEventType);
        if (isRoute) {
            runtimeEventSender.routeEventBean(theEvent);
        } else {
            if (threadingService.isInboundThreading()) {
                threadingService.submitInbound(theEvent, runtimeEventSender);
            } else {
                runtimeEventSender.processWrappedEvent(theEvent);
            }
        }
    }
}
