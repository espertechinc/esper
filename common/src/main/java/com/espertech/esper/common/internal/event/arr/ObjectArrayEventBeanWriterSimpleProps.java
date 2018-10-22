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
package com.espertech.esper.common.internal.event.arr;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.event.core.EventBeanWriter;
import com.espertech.esper.common.internal.event.core.ObjectArrayBackedEventBean;

/**
 * Writer method for writing to Object-Array-type events.
 */
public class ObjectArrayEventBeanWriterSimpleProps implements EventBeanWriter {
    private final int[] indexes;

    /**
     * Ctor.
     *
     * @param indexes indexes of properties to write
     */
    public ObjectArrayEventBeanWriterSimpleProps(int[] indexes) {
        this.indexes = indexes;
    }

    /**
     * Write values to an event.
     *
     * @param values   to write
     * @param theEvent to write to
     */
    public void write(Object[] values, EventBean theEvent) {
        ObjectArrayBackedEventBean arrayEvent = (ObjectArrayBackedEventBean) theEvent;
        Object[] array = arrayEvent.getProperties();

        for (int i = 0; i < indexes.length; i++) {
            array[indexes[i]] = values[i];
        }
    }
}
