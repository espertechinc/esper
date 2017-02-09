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
package com.espertech.esperio.jms;

import com.espertech.esper.adapter.InputAdapter;
import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.event.EventAdapterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Created for ESPER.
 */
public class JMSDefaultAnyMessageUnmarshaller implements JMSMessageUnmarshaller {
    private static final Logger log = LoggerFactory.getLogger(JMSDefaultAnyMessageUnmarshaller.class);

    public EventBean unmarshal(EventAdapterService eventAdapterService,
                               Message message) throws EPException {
        try {
            if (message instanceof ObjectMessage) {
                ObjectMessage objmsg = (ObjectMessage) message;
                Serializable obj = objmsg.getObject();
                return eventAdapterService.adapterForBean(obj);
            } else if (message instanceof MapMessage) {
                Map<String, Object> properties = new HashMap<String, Object>();
                MapMessage mapMsg = (MapMessage) message;
                Enumeration en = mapMsg.getMapNames();
                while (en.hasMoreElements()) {
                    String property = (String) en.nextElement();
                    Object mapObject = mapMsg.getObject(property);
                    properties.put(property, mapObject);
                }

                // Get event type property
                Object typeProperty = properties.get(InputAdapter.ESPERIO_MAP_EVENT_TYPE);
                if (typeProperty == null) {
                    log.warn(".unmarshal Failed to unmarshal map message, expected type property not found: '" + InputAdapter.ESPERIO_MAP_EVENT_TYPE + "'");
                    return null;
                }

                // Get event type
                String name = typeProperty.toString();
                EventType eventType = eventAdapterService.getExistsTypeByName(name);
                if (eventType == null) {
                    log.warn(".unmarshal Failed to unmarshal map message, event type name '" + name + "' is not a known type");
                    return null;
                }

                return eventAdapterService.adapterForTypedMap(properties, eventType);
            } else {
                String error = ".unmarshal Failed to unmarshal message of JMS type: " + message.getJMSType();
                log.error(error);
                throw new EPException(error);
            }
        } catch (JMSException ex) {
            throw new EPException("Error unmarshalling message", ex);
        }
    }

}
