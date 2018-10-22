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
package com.espertech.esper.example.ohlc;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.introspect.BeanEventTypeStem;
import com.espertech.esper.common.internal.view.core.ViewFactoryForge;
import com.espertech.esper.common.internal.view.core.ViewForgeEnv;
import com.espertech.esper.common.internal.view.core.ViewParameterException;
import com.espertech.esper.common.internal.view.util.ViewForgeSupport;

import java.util.List;

public class OHLCBarPlugInViewForge implements ViewFactoryForge {
    private List<ExprNode> viewParameters;
    private ExprNode timestampExpression;
    private ExprNode valueExpression;
    private EventType eventType;

    public void setViewParameters(List<ExprNode> parameters, ViewForgeEnv viewForgeEnv, int streamNumber) throws ViewParameterException {
        this.viewParameters = parameters;
    }

    public void attach(EventType parentEventType, int streamNumber, ViewForgeEnv env) throws ViewParameterException {
        if (viewParameters.size() != 2) {
            throw new ViewParameterException("View requires a two parameters: the expression returning timestamps and the expression supplying OHLC data points");
        }
        ExprNode[] validatedNodes = ViewForgeSupport.validate("OHLC view", parentEventType, viewParameters, false, env, streamNumber);

        timestampExpression = validatedNodes[0];
        valueExpression = validatedNodes[1];

        if ((timestampExpression.getForge().getEvaluationType() != long.class) && (timestampExpression.getForge().getEvaluationType() != Long.class)) {
            throw new ViewParameterException("View requires long-typed timestamp values in parameter 1");
        }
        if ((valueExpression.getForge().getEvaluationType() != double.class) && (valueExpression.getForge().getEvaluationType() != Double.class)) {
            throw new ViewParameterException("View requires double-typed values for in parameter 2");
        }

        /*
         * Allocate a custom event type for this example. This event type will be a Bean event type.
         */
        // make event type name
        String outputEventTypeName = env.getStatementCompileTimeServices().getEventTypeNameGeneratorStatement().getViewDerived(getViewName(), streamNumber);

        // make event type metadata
        EventTypeMetadata metadata = new EventTypeMetadata(outputEventTypeName, env.getModuleName(), EventTypeTypeClass.VIEWDERIVED, EventTypeApplicationType.CLASS, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());

        // for Bean event types, make a stem
        BeanEventTypeStem stem = env.getStatementCompileTimeServices().getBeanEventTypeStemService().getCreateStem(OHLCBarValue.class, null);

        // make bean event type
        eventType = new BeanEventType(stem, metadata, env.getBeanEventTypeFactoryProtected(), null, null, null, null);

        // register bean type
        env.getEventTypeModuleCompileTimeRegistry().newType(eventType);
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getViewName() {
        return OHLCBarPlugInView.class.getSimpleName();
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return new SAIFFInitializeBuilder(OHLCBarPlugInViewFactory.class, this.getClass(), "factory", parent, symbols, classScope)
            .exprnode("timestampExpression", timestampExpression)
            .exprnode("valueExpression", valueExpression)
            .build();
    }
}
