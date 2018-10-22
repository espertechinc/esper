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
package com.espertech.esper.regressionlib.support.extend.view;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.view.core.ViewFactoryForge;
import com.espertech.esper.common.internal.view.core.ViewForgeEnv;
import com.espertech.esper.common.internal.view.core.ViewParameterException;
import com.espertech.esper.common.internal.view.derived.DerivedViewTypeUtil;
import com.espertech.esper.common.internal.view.util.ViewForgeSupport;

import java.util.LinkedHashMap;
import java.util.List;

public class MyTrendSpotterViewForge implements ViewFactoryForge {
    private List<ExprNode> viewParameters;

    private ExprNode parameter;
    private EventType eventType;

    public void setViewParameters(List<ExprNode> parameters, ViewForgeEnv viewForgeEnv, int streamNumber) throws ViewParameterException {
        this.viewParameters = parameters;
    }

    public void attach(EventType parentEventType, int streamNumber, ViewForgeEnv viewForgeEnv) throws ViewParameterException {
        ExprNode[] validated = ViewForgeSupport.validate("Trend spotter view", parentEventType, viewParameters, false, viewForgeEnv, streamNumber);
        String message = "Trend spotter view accepts a single integer or double value";
        if (validated.length != 1) {
            throw new ViewParameterException(message);
        }
        Class resultType = validated[0].getForge().getEvaluationType();
        if ((resultType != Integer.class) && (resultType != int.class) &&
            (resultType != Double.class) && (resultType != double.class)) {
            throw new ViewParameterException(message);
        }
        parameter = validated[0];

        LinkedHashMap<String, Object> eventTypeMap = new LinkedHashMap<String, Object>();
        eventTypeMap.put("trendcount", Long.class);

        eventType = DerivedViewTypeUtil.newType("trendview", eventTypeMap, viewForgeEnv, streamNumber);
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getViewName() {
        return "Trend-spotter";
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return new SAIFFInitializeBuilder(MyTrendSpotterViewFactory.class, this.getClass(), "factory", parent, symbols, classScope)
            .eventtype("eventType", eventType)
            .exprnode("parameter", parameter)
            .build();
    }
}
