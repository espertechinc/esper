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
package com.espertech.esper.common.internal.event.core;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.avro.EventTypeAvroHandler;
import com.espertech.esper.common.internal.event.bean.core.BeanEventBean;
import com.espertech.esper.common.internal.event.eventtyperepo.EventTypeRepository;
import com.espertech.esper.common.internal.event.map.MapEventBean;
import com.espertech.esper.common.internal.event.xml.XMLEventBean;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Map;

public class EventTypeResolvingBeanFactoryImpl implements EventTypeResolvingBeanFactory {
    private final EventTypeRepository eventTypeRepository;
    private final EventTypeAvroHandler avroHandler;

    public EventTypeResolvingBeanFactoryImpl(EventTypeRepository eventTypeRepository, EventTypeAvroHandler avroHandler) {
        this.eventTypeRepository = eventTypeRepository;
        this.avroHandler = avroHandler;
    }

    public EventBean adapterForObjectArray(Object[] theEvent, String eventTypeName) throws EPException {
        EventType type = eventTypeRepository.getTypeByName(eventTypeName);
        EventTypeUtility.validateTypeObjectArray(eventTypeName, type);
        return new ObjectArrayEventBean(theEvent, type);
    }

    public EventBean adapterForBean(Object data, String eventTypeName) {
        EventType type = eventTypeRepository.getTypeByName(eventTypeName);
        EventTypeUtility.validateTypeBean(eventTypeName, type);
        return new BeanEventBean(data, type);
    }

    public EventBean adapterForMap(Map<String, Object> map, String eventTypeName) {
        EventType type = eventTypeRepository.getTypeByName(eventTypeName);
        EventTypeUtility.validateTypeMap(eventTypeName, type);
        return new MapEventBean(map, type);
    }

    public EventBean adapterForXMLDOM(org.w3c.dom.Node node, String eventTypeName) {
        EventType type = eventTypeRepository.getTypeByName(eventTypeName);
        EventTypeUtility.validateTypeXMLDOM(eventTypeName, type);
        org.w3c.dom.Node namedNode = getXMLNodeFromDocument(node);
        return new XMLEventBean(namedNode, type);
    }

    public EventBean adapterForAvro(Object avroGenericDataDotRecord, String eventTypeName) {
        EventType type = eventTypeRepository.getTypeByName(eventTypeName);
        EventTypeUtility.validateTypeAvro(eventTypeName, type);
        return avroHandler.adapterForTypeAvro(avroGenericDataDotRecord, type);
    }

    public static Node getXMLNodeFromDocument(org.w3c.dom.Node node) {
        org.w3c.dom.Node resultNode = node;
        if (node instanceof Document) {
            resultNode = ((Document) node).getDocumentElement();
        } else if (!(node instanceof Element)) {
            throw new EPException("Unexpected DOM node of type '" + node.getClass() + "' encountered, please supply a Document or Element node");
        }
        return resultNode;
    }
}
