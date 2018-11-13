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
package com.espertech.esper.common.internal.view.firstevent;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.view.core.*;
import com.espertech.esper.common.internal.view.util.ViewForgeSupport;

import java.util.List;

public class FirstEventViewForge extends ViewFactoryForgeBase implements DataWindowViewForge, AsymetricDataWindowViewForge {
    public void setViewParameters(List<ExprNode> parameters, ViewForgeEnv viewForgeEnv, int streamNumber) throws ViewParameterException {
        ViewForgeSupport.validateNoParameters(getViewName(), parameters);
    }

    public void attach(EventType parentEventType, int streamNumber, ViewForgeEnv viewForgeEnv) throws ViewParameterException {
        this.eventType = parentEventType;
    }

    protected Class typeOfFactory() {
        return ViewFactory.class;
    }

    protected String factoryMethod() {
        return "firstevent";
    }

    protected void assign(CodegenMethod method, CodegenExpressionRef factory, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
    }

    public String getViewName() {
        return ViewEnum.FIRST_EVENT.getName();
    }
}
