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
package com.espertech.esper.event.vaevent;

import com.espertech.esper.client.ConfigurationVariantStream;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.lookup.EventTableIndexRepository;
import com.espertech.esper.epl.named.NamedWindowRootViewInstance;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventTypeIdGenerator;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.view.Viewable;

import java.util.Collection;
import java.util.Iterator;

/**
 * Represents a variant event stream, allowing events of disparate event types to be treated polymophically.
 */
public class VAEVariantProcessor implements ValueAddEventProcessor {
    /**
     * Specification for the variant stream.
     */
    protected final VariantSpec variantSpec;

    /**
     * The event type representing the variant stream.
     */
    protected VariantEventType variantEventType;

    public VAEVariantProcessor(EventAdapterService eventAdapterService, VariantSpec variantSpec, EventTypeIdGenerator eventTypeIdGenerator, ConfigurationVariantStream config) {
        this.variantSpec = variantSpec;

        VariantPropResolutionStrategy strategy;
        if (variantSpec.getTypeVariance() == ConfigurationVariantStream.TypeVariance.ANY) {
            strategy = new VariantPropResolutionStrategyAny(variantSpec);
        } else {
            strategy = new VariantPropResolutionStrategyDefault(variantSpec);
        }

        EventTypeMetadata metadata = EventTypeMetadata.createValueAdd(variantSpec.getVariantStreamName(), EventTypeMetadata.TypeClass.VARIANT);
        variantEventType = new VariantEventType(eventAdapterService, metadata, eventTypeIdGenerator.getTypeId(variantSpec.getVariantStreamName()), variantSpec, strategy, config);
    }

    public EventType getValueAddEventType() {
        return variantEventType;
    }

    public void validateEventType(EventType eventType) throws ExprValidationException {
        if (variantSpec.getTypeVariance() == ConfigurationVariantStream.TypeVariance.ANY) {
            return;
        }

        if (eventType == null) {
            throw new ExprValidationException(getMessage());
        }

        // try each permitted type
        for (EventType variant : variantSpec.getEventTypes()) {
            if (variant == eventType) {
                return;
            }
        }

        // test if any of the supertypes of the eventtype is a variant type
        for (EventType variant : variantSpec.getEventTypes()) {
            // Check all the supertypes to see if one of the matches the full or delta types
            Iterator<EventType> deepSupers = eventType.getDeepSuperTypes();
            if (deepSupers == null) {
                continue;
            }

            EventType superType;
            for (; deepSupers.hasNext(); ) {
                superType = deepSupers.next();
                if (superType == variant) {
                    return;
                }
            }
        }

        throw new ExprValidationException(getMessage());
    }

    public EventBean getValueAddEventBean(EventBean theEvent) {
        return new VariantEventBean(variantEventType, theEvent);
    }

    public void onUpdate(EventBean[] newData, EventBean[] oldData, NamedWindowRootViewInstance namedWindowRootView, EventTableIndexRepository indexRepository) {
        throw new UnsupportedOperationException();
    }

    public Collection<EventBean> getSnapshot(EPStatementAgentInstanceHandle createWindowStmtHandle, Viewable parent) {
        throw new UnsupportedOperationException();
    }

    public void removeOldData(EventBean[] oldData, EventTableIndexRepository indexRepository, AgentInstanceContext agentInstanceContext) {
        throw new UnsupportedOperationException();
    }

    private String getMessage() {
        return "Selected event type is not a valid event type of the variant stream '" + variantSpec.getVariantStreamName() + "'";
    }
}
