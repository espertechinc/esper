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
package com.espertech.esper.common.internal.epl.virtualdw;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBeanFactory;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindow;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowContext;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowFactory;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowFactoryContext;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.view.core.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.common.internal.view.core.DataWindowViewFactory;
import com.espertech.esper.common.internal.view.core.View;
import com.espertech.esper.common.internal.view.core.ViewFactoryContext;

import java.io.Serializable;

public class VirtualDWViewFactory implements DataWindowViewFactory {
    private EventType eventType;
    private VirtualDataWindowFactory factory;
    private Object[] parameters;
    private ExprEvaluator[] parameterExpressions;
    private String namedWindowName;
    private Serializable compileTimeConfiguration;
    private EventBeanFactory eventBeanFactory;

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void init(ViewFactoryContext viewFactoryContext, EPStatementInitServices services) {
        try {
            eventBeanFactory = EventTypeUtility.getFactoryForType(eventType, services.getEventBeanTypedEventFactory(), services.getEventTypeAvroHandler());
            VirtualDataWindowFactoryContext factoryContext = new VirtualDataWindowFactoryContext(eventType, parameters, parameterExpressions, namedWindowName, compileTimeConfiguration, viewFactoryContext, services);
            factory.initialize(factoryContext);
        } catch (RuntimeException ex) {
            throw new EPException("Validation exception initializing virtual data window '" + namedWindowName + "': " + ex.getMessage(), ex);
        }
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        VirtualDataWindowOutStreamImpl outputStream = new VirtualDataWindowOutStreamImpl();
        VirtualDataWindow window;
        try {
            VirtualDataWindowContext context = new VirtualDataWindowContext(this, agentInstanceViewFactoryContext, eventBeanFactory, outputStream);
            window = factory.create(context);
        } catch (Exception ex) {
            throw new EPException("Exception returned by virtual data window factory upon creation: " + ex.getMessage(), ex);
        }
        VirtualDWViewImpl view = new VirtualDWViewImpl(this, agentInstanceViewFactoryContext.getAgentInstanceContext(), window);
        outputStream.setView(view);
        return view;
    }

    public String getViewName() {
        return "virtual-data-window";
    }

    public VirtualDataWindowFactory getFactory() {
        return factory;
    }

    public void setFactory(VirtualDataWindowFactory factory) {
        this.factory = factory;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public void setParameterExpressions(ExprEvaluator[] parameterExpressions) {
        this.parameterExpressions = parameterExpressions;
    }

    public void setNamedWindowName(String namedWindowName) {
        this.namedWindowName = namedWindowName;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public ExprEvaluator[] getParameterExpressions() {
        return parameterExpressions;
    }

    public String getNamedWindowName() {
        return namedWindowName;
    }

    public Serializable getCompileTimeConfiguration() {
        return compileTimeConfiguration;
    }

    public void setCompileTimeConfiguration(Serializable compileTimeConfiguration) {
        this.compileTimeConfiguration = compileTimeConfiguration;
    }

    public void destroy() {
        factory.destroy();
    }
}
