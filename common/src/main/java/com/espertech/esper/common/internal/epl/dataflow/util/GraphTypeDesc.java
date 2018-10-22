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
package com.espertech.esper.common.internal.epl.dataflow.util;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;

public class GraphTypeDesc {
    private boolean wildcard;
    private boolean underlying;
    private EventType eventType;

    public GraphTypeDesc() {
    }

    public GraphTypeDesc(boolean wildcard, boolean underlying, EventType eventType) {
        this.wildcard = wildcard;
        this.underlying = underlying;
        this.eventType = eventType;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return new SAIFFInitializeBuilder(GraphTypeDesc.class, this.getClass(), "gtd", parent, symbols, classScope)
                .constant("wildcard", wildcard)
                .constant("underlying", underlying)
                .eventtype("eventType", eventType)
                .build();
    }

    public boolean isWildcard() {
        return wildcard;
    }

    public boolean isUnderlying() {
        return underlying;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setWildcard(boolean wildcard) {
        this.wildcard = wildcard;
    }

    public void setUnderlying(boolean underlying) {
        this.underlying = underlying;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }
}
