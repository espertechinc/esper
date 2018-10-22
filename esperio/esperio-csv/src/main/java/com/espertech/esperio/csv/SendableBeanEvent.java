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
package com.espertech.esperio.csv;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.internal.event.bean.core.BeanEventPropertyWriter;
import com.espertech.esper.common.internal.event.bean.core.PropertyHelper;
import com.espertech.esper.common.internal.event.core.WriteablePropertyDescriptor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of SendableEvent that wraps a Map event for
 * sending into the runtime.
 */
public class SendableBeanEvent extends AbstractSendableEvent {
    private final Object beanToSend;
    private final String eventTypeName;
    private final Map<Class, Map<String, BeanEventPropertyWriter>> writersMap = new HashMap<Class, Map<String, BeanEventPropertyWriter>>();

    /**
     * Converts mapToSend to an instance of beanClass
     *
     * @param mapToSend     - the map containing data to send into the runtime
     * @param beanClass     - type of the bean to create from mapToSend
     * @param eventTypeName - the event type name for the map event
     * @param timestamp     - the timestamp for this event
     * @param scheduleSlot  - the schedule slot for the entity that created this event
     */
    public SendableBeanEvent(Map<String, Object> mapToSend, Class beanClass, String eventTypeName, long timestamp, long scheduleSlot) {
        super(timestamp, scheduleSlot);
        this.eventTypeName = eventTypeName;

        try {
            Map<String, BeanEventPropertyWriter> writers = writersMap.get(beanClass);
            if (writers == null) {
                Set<WriteablePropertyDescriptor> props = PropertyHelper.getWritableProperties(beanClass);
                writers = new HashMap<String, BeanEventPropertyWriter>();
                writersMap.put(beanClass, writers);
                for (WriteablePropertyDescriptor prop : props) {
                    Method writerMethod = prop.getWriteMethod();
                    writers.put(prop.getPropertyName(), new BeanEventPropertyWriter(beanClass, writerMethod));
                }
                // populate writers
            }

            beanToSend = beanClass.newInstance();

            for (Map.Entry<String, Object> entry : mapToSend.entrySet()) {
                BeanEventPropertyWriter writer = writers.get(entry.getKey());
                if (writer != null) {
                    writer.writeValue(entry.getValue(), beanToSend);
                }
            }
        } catch (Exception e) {
            throw new EPException("Cannot populate bean instance", e);
        }
    }

    /* (non-Javadoc)
     * @see com.espertech.esperio.csv.SendableEvent#send(com.espertech.esper.client.EPRuntime)
     */
    public void send(AbstractSender sender) {
        sender.sendEvent(this, beanToSend, eventTypeName);
    }

    public String toString() {
        return beanToSend.toString();
    }
}
