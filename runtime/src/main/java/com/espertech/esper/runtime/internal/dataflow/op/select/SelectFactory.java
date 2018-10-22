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

package com.espertech.esper.runtime.internal.dataflow.op.select;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.aifactory.select.StatementAgentInstanceFactorySelect;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryFactoryMap;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryRequirements;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryUtil;
import com.espertech.esper.common.internal.context.airegistry.StatementAIResourceRegistry;
import com.espertech.esper.common.internal.context.module.StatementAIFactoryAssignmentContext;
import com.espertech.esper.common.internal.context.module.StatementAIFactoryProvider;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpFactoryInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperator;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorFactory;
import com.espertech.esper.common.internal.event.core.EventBeanAdapterFactory;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.view.core.ViewFactoryServiceImpl;

public class SelectFactory implements DataFlowOperatorFactory {
    private boolean submitEventBean;
    private EventType[] eventTypes;
    private EventBeanAdapterFactory[] adapterFactories;
    private StatementAIFactoryProvider factoryProvider;
    private StatementAgentInstanceFactorySelect factorySelect;
    private StatementAIResourceRegistry resourceRegistry;
    private boolean iterate;
    private int[] originatingStreamToViewableStream;

    public void initializeFactory(DataFlowOpFactoryInitializeContext context) {
        boolean ha = context.getStatementContext().getViewFactoryService() != ViewFactoryServiceImpl.INSTANCE;
        if (ha) {
            throw new EPException("The select-operator is not supported in the HA environment");
        }

        adapterFactories = new EventBeanAdapterFactory[eventTypes.length];
        for (int i = 0; i < eventTypes.length; i++) {
            if (eventTypes[i] != null) {
                adapterFactories[i] = EventTypeUtility.getAdapterFactoryForType(eventTypes[i], context.getStatementContext().getEventBeanTypedEventFactory(),
                        context.getStatementContext().getEventTypeAvroHandler());
            }
        }

        factorySelect = (StatementAgentInstanceFactorySelect) factoryProvider.getFactory();
        AIRegistryRequirements registryRequirements = factorySelect.getRegistryRequirements();
        resourceRegistry = AIRegistryUtil.allocateRegistries(registryRequirements, AIRegistryFactoryMap.INSTANCE);
        factoryProvider.assign(new StatementAIFactoryAssignmentContext(resourceRegistry));
    }

    public DataFlowOperator operator(DataFlowOpInitializeContext context) {
        return new SelectOp(this, context.getAgentInstanceContext());
    }

    public void setEventTypes(EventType[] eventTypes) {
        this.eventTypes = eventTypes;
    }

    public EventBeanAdapterFactory[] getAdapterFactories() {
        return adapterFactories;
    }

    public void setFactoryProvider(StatementAIFactoryProvider factoryProvider) {
        this.factoryProvider = factoryProvider;
    }

    public StatementAIResourceRegistry getResourceRegistry() {
        return resourceRegistry;
    }

    public EventType[] getEventTypes() {
        return eventTypes;
    }

    public StatementAIFactoryProvider getFactoryProvider() {
        return factoryProvider;
    }

    public StatementAgentInstanceFactorySelect getFactorySelect() {
        return factorySelect;
    }

    public boolean isSubmitEventBean() {
        return submitEventBean;
    }

    public void setSubmitEventBean(boolean submitEventBean) {
        this.submitEventBean = submitEventBean;
    }

    public boolean isIterate() {
        return iterate;
    }

    public void setIterate(boolean iterate) {
        this.iterate = iterate;
    }

    public int[] getOriginatingStreamToViewableStream() {
        return originatingStreamToViewableStream;
    }

    public void setOriginatingStreamToViewableStream(int[] originatingStreamToViewableStream) {
        this.originatingStreamToViewableStream = originatingStreamToViewableStream;
    }
}
