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
package com.espertech.esper.core.context.util;

import com.espertech.esper.client.EventType;

public interface ContextPropertyRegistry {

    public final static String CONTEXT_PREFIX = "context";

    public boolean isContextPropertyPrefix(String prefixName);

    public EventType getContextEventType();

    public boolean isPartitionProperty(EventType fromType, String propertyName);

    public String getPartitionContextPropertyName(EventType fromType, String propertyName);
}
