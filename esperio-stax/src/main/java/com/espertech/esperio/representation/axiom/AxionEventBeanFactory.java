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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.plugin.PlugInEventBeanFactory;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;

import java.net.URI;
import java.util.Map;

/**
 * A event bean factory implementation that understands Apache Axiom OMNode events
 * and that can look up the root element name to determine the right event type.
 * <p>
 * See {@link AxiomEventRepresentation} for more details.
 */
public class AxionEventBeanFactory implements PlugInEventBeanFactory {
    private final Map<String, AxiomXMLEventType> types;

    /**
     * Ctor.
     *
     * @param types the currently known event type name and their types
     */
    public AxionEventBeanFactory(Map<String, AxiomXMLEventType> types) {
        this.types = types;
    }

    public EventBean create(Object theEvent, URI resolutionURI) {
        // Check event type - only handle the Axiom types of OMDocument and OMElement
        OMElement namedNode;
        if (theEvent instanceof OMDocument) {
            namedNode = ((OMDocument) theEvent).getOMDocumentElement();
        } else if (theEvent instanceof OMElement) {
            namedNode = (OMElement) theEvent;
        } else {
            return null;    // not the right event type, return null and let others handle it, or ignore
        }

        // Look up the root element name and map to a known event type
        String rootElementName = namedNode.getLocalName();
        EventType eventType = types.get(rootElementName);
        if (eventType == null) {
            return null;    // not a root element name, let others handle it
        }

        return new AxiomEventBean(namedNode, eventType);
    }
}
