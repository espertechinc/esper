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

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.util.ExecutionPathDebugLog;
import com.espertech.esper.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Session;

/**
 * Marshals the response out of the event bean into a jms map message.
 */
public class JMSDefaultMapMessageMarshaller implements JMSMessageMarshaller {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public Message marshal(EventBean eventBean, Session session,
                           long timestamp) throws EPException {
        EventType eventType = eventBean.getEventType();
        MapMessage mapMessage = null;
        try {
            mapMessage = session.createMapMessage();
            String[] properties = eventType.getPropertyNames();
            for (String property : properties) {
                if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                    log.debug(
                            ".Marshal EventProperty property==" + property + ", value=" +
                                    eventBean.get(property));
                }
                Class clazz = eventType.getPropertyType(property);
                if (JavaClassHelper.isNumeric(clazz)) {
                    Class boxedClazz = JavaClassHelper.getBoxedType(clazz);
                    if (boxedClazz == Double.class) {
                        mapMessage.setDouble(property, (Double) eventBean.get(property));
                    }
                    if (boxedClazz == Float.class) {
                        mapMessage.setFloat(property, (Float) eventBean.get(property));
                    }
                    if (boxedClazz == Byte.class) {
                        mapMessage.setFloat(property, (Byte) eventBean.get(property));
                    }
                    if (boxedClazz == Short.class) {
                        mapMessage.setShort(property, (Short) eventBean.get(property));
                    }
                    if (boxedClazz == Integer.class) {
                        mapMessage.setInt(property, (Integer) eventBean.get(property));
                    }
                    if (boxedClazz == Long.class) {
                        mapMessage.setLong(property, (Long) eventBean.get(property));
                    }
                } else if ((clazz == boolean.class) || (clazz == Boolean.class)) {
                    mapMessage.setBoolean(property, (Boolean) eventBean.get(property));
                } else if ((clazz == Character.class) || (clazz == char.class)) {
                    mapMessage.setChar(property, (Character) eventBean.get(property));
                } else if (clazz == String.class) {
                    mapMessage.setString(property, (String) eventBean.get(property));
                } else {
                    mapMessage.setObject(property, eventBean.get(property));
                }
            }
            mapMessage.setJMSTimestamp(timestamp);
        } catch (JMSException ex) {
            throw new EPException(ex);
        }
        return mapMessage;
    }
}
