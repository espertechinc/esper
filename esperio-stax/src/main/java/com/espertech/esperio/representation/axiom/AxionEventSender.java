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
package com.espertech.esperio.representation.axiom;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventSender;
import com.espertech.esper.core.service.EPRuntimeEventSender;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;

/**
 * A event sender implementation that understands Apache Axiom OMNode events
 * and checks that the root element name matches the expected event type's root element name.
 * <p>
 * See {@link AxiomEventRepresentation} for more details.
 */
public class AxionEventSender implements EventSender {
    private final AxiomXMLEventType eventType;
    private final EPRuntimeEventSender runtimeEventSender;

    /**
     * Ctor.
     *
     * @param eventType          the axiom event metadata
     * @param runtimeEventSender the sender to send events into
     */
    public AxionEventSender(AxiomXMLEventType eventType, EPRuntimeEventSender runtimeEventSender) {
        this.eventType = eventType;
        this.runtimeEventSender = runtimeEventSender;
    }

    public void sendEvent(Object theEvent) {
        processEvent(theEvent, false);
    }

    public void route(Object theEvent) {
        processEvent(theEvent, true);
    }

    public void processEvent(Object node, boolean isRoute) {
        OMElement namedNode;
        if (node instanceof OMDocument) {
            namedNode = ((OMDocument) node).getOMDocumentElement();
        } else if (node instanceof OMElement) {
            namedNode = (OMElement) node;
        } else {
            throw new EPException("Unexpected AXIOM node of type '" + node.getClass() + "' encountered, please supply a Document or Element node");
        }

        String rootElementNameRequired = eventType.getConfig().getRootElementName();
        String rootElementNameFound = namedNode.getLocalName();
        if (!rootElementNameFound.equals(rootElementNameRequired)) {
            throw new EPException("Unexpected root element name '" + rootElementNameFound + "' encountered, expected '" + rootElementNameRequired + "'");
        }

        if (isRoute) {
            runtimeEventSender.routeEventBean(new AxiomEventBean(namedNode, eventType));
        } else {
            runtimeEventSender.processWrappedEvent(new AxiomEventBean(namedNode, eventType));
        }
    }
}
