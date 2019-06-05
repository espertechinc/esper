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
package com.espertech.esper.common.internal.context.module;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.event.eventtyperepo.EventTypeRepository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class EventTypeCompileTimeRegistry {
    private final Map<String, EventType> moduleTypesAdded = new LinkedHashMap<>();
    private final Map<String, EventType> newTypesAdded = new LinkedHashMap<>();
    private final EventTypeRepository eventTypeRepositoryPreconfigured;

    public EventTypeCompileTimeRegistry(EventTypeRepository eventTypeRepositoryPreconfigured) {
        this.eventTypeRepositoryPreconfigured = eventTypeRepositoryPreconfigured;
    }

    public void newType(EventType type)  {
        try {
            EventTypeUtility.validateModifiers(type.getName(), type.getMetadata().getBusModifier(), type.getMetadata().getAccessModifier());
        } catch (ExprValidationException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        if (type.getMetadata().getAccessModifier() == NameAccessModifier.PRECONFIGURED) {
            if (type.getMetadata().getApplicationType() != EventTypeApplicationType.XML) {
                throw new IllegalArgumentException("Preconfigured-visibility is not allowed here");
            }
            eventTypeRepositoryPreconfigured.addType(type);
        }
        if (moduleTypesAdded.containsKey(type.getName())) {
            throw new IllegalArgumentException("Event type '" + type.getName() + "' has already been added by the module");
        }
        if (type.getMetadata().getAccessModifier() == NameAccessModifier.PRIVATE ||
            type.getMetadata().getAccessModifier() == NameAccessModifier.PROTECTED ||
            type.getMetadata().getAccessModifier() == NameAccessModifier.PUBLIC) {
            moduleTypesAdded.put(type.getName(), type);
        }

        // We allow private types to register multiple times, the first one counts (i.e. rollup with multiple select-clauses active)
        if (!newTypesAdded.containsKey(type.getName())) {
            newTypesAdded.put(type.getName(), type);
        } else {
            throw new IllegalArgumentException("Event type '" + type.getName() + "' has already been added by the module");
        }
    }

    public EventType getModuleTypes(String typeName) {
        return moduleTypesAdded.get(typeName);
    }

    public Collection<EventType> getNewTypesAdded() {
        return newTypesAdded.values();
    }
}
