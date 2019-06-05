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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.runtime.client.util.InputAdapter;
import com.espertech.esper.runtime.internal.kernel.service.EPRuntimeSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Created for ESPER.
 */
public class JMSDefaultAnyMessageUnmarshaller implements JMSMessageUnmarshaller {
    private static final Logger log = LoggerFactory.getLogger(JMSDefaultAnyMessageUnmarshaller.class);

    public EventBean unmarshal(EPRuntimeSPI runtime,
                               Message message) throws EPException {
        try {
            if (message instanceof ObjectMessage) {
                ObjectMessage objmsg = (ObjectMessage) message;
                Serializable obj = objmsg.getObject();

                String name = obj.getClass().getSimpleName();
                EventType eventType = runtime.getServicesContext().getEventTypeRepositoryBus().getNameToTypeMap().get(name);
                if (eventType == null) {
                    log.warn(".unmarshal Failed to unmarshal map message, event type name '" + name + "' is not a known type");
                    return null;
                }

                return runtime.getServicesContext().getEventBeanTypedEventFactory().adapterForTypedBean(obj, eventType);
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
                EventType eventType = runtime.getServicesContext().getEventTypeRepositoryBus().getNameToTypeMap().get(name);
                if (eventType == null) {
                    log.warn(".unmarshal Failed to unmarshal map message, event type name '" + name + "' is not a known type");
                    return null;
                }

                return runtime.getServicesContext().getEventBeanTypedEventFactory().adapterForTypedMap(properties, eventType);
            } else if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;

                // Get event type property
                Object typeProperty = textMessage.getStringProperty(InputAdapter.ESPERIO_JSON_EVENT_TYPE);
                if (typeProperty == null) {
                    log.warn(".unmarshal Failed to unmarshal text message, expected type property not found: '" + InputAdapter.ESPERIO_JSON_EVENT_TYPE + "'");
                    return null;
                }

                // Get event type
                String name = typeProperty.toString();
                EventType eventType = runtime.getServicesContext().getEventTypeRepositoryBus().getNameToTypeMap().get(name);
                if (eventType == null || !(eventType instanceof JsonEventType)) {
                    log.warn(".unmarshal Failed to unmarshal text message, event type name '" + name + "' is not a known type");
                    return null;
                }

                JsonEventType jsonEventType = (JsonEventType) eventType;
                Object underlying = jsonEventType.parse(textMessage.getText());

                return runtime.getServicesContext().getEventBeanTypedEventFactory().adapterForTypedJson(underlying, eventType);
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
