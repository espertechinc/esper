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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.ContextPropertyRegistry;
import com.espertech.esper.epl.spec.ContextDetailPartitionItem;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ContextPropertyRegistryImpl implements ContextPropertyRegistry {

    public final static ContextPropertyRegistry EMPTY_REGISTRY = new ContextPropertyRegistryImpl(null);

    private final List<ContextDetailPartitionItem> partitionProperties;
    private final EventType contextEventType;

    public ContextPropertyRegistryImpl(List<ContextDetailPartitionItem> partitionProperties, EventType contextEventType) {
        this.partitionProperties = partitionProperties;
        this.contextEventType = contextEventType;
    }

    public ContextPropertyRegistryImpl(EventType contextEventType) {
        partitionProperties = Collections.emptyList();
        this.contextEventType = contextEventType;
    }

    public boolean isPartitionProperty(EventType fromType, String propertyName) {
        String name = getPartitionContextPropertyName(fromType, propertyName);
        return name != null;
    }

    public String getPartitionContextPropertyName(EventType fromType, String propertyName) {
        for (ContextDetailPartitionItem item : partitionProperties) {
            if (item.getFilterSpecCompiled().getFilterForEventType() == fromType) {
                for (int i = 0; i < item.getPropertyNames().size(); i++) {
                    if (item.getPropertyNames().get(i).equals(propertyName)) {
                        return ContextPropertyEventType.PROP_CTX_KEY_PREFIX + (i + 1);
                    }
                }
            }
        }
        return null;
    }

    public boolean isContextPropertyPrefix(String prefixName) {
        return prefixName != null && prefixName.toLowerCase(Locale.ENGLISH).equals(CONTEXT_PREFIX);
    }

    public EventType getContextEventType() {
        return contextEventType;
    }
}
