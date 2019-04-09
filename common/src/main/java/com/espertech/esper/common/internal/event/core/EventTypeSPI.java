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

import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

/**
 * Service provider interface for internal use for event types.
 */
public interface EventTypeSPI extends EventType {
    /**
     * Return a writer for writing a single property value.
     *
     * @param propertyName to write to
     * @return null or writer if writable
     */
    public EventPropertyWriterSPI getWriter(String propertyName);

    /**
     * Returns the writable properties.
     *
     * @return properties that can be written
     */
    public EventPropertyDescriptor[] getWriteableProperties();

    /**
     * Returns the descriptor for a writable property.
     *
     * @param propertyName to get descriptor for
     * @return descriptor
     */
    public EventPropertyDescriptor getWritableProperty(String propertyName);

    /**
     * Returns the copy method, considering only the attached properties for a write operation onto the copy
     *
     * @param properties to write after copy
     * @return copy method
     */
    public EventBeanCopyMethodForge getCopyMethodForge(String[] properties);

    /**
     * Returns the write for writing a set of properties.
     *
     * @param properties to write
     * @return writer
     */
    public EventBeanWriter getWriter(String[] properties);

    public ExprValidationException equalsCompareType(EventType eventType);

    EventPropertyGetterSPI getGetterSPI(String propertyExpression);

    EventPropertyGetterMappedSPI getGetterMappedSPI(String propertyName);

    EventPropertyGetterIndexedSPI getGetterIndexedSPI(String propertyName);

    void setMetadataId(long publicId, long protectedId);
}