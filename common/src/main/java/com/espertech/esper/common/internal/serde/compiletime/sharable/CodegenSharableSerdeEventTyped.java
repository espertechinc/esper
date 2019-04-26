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
package com.espertech.esper.common.internal.serde.compiletime.sharable;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenFieldSharable;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethodChain;
import static com.espertech.esper.common.internal.event.path.EventTypeResolver.GETEVENTSERDEFACTORY;

public class CodegenSharableSerdeEventTyped implements CodegenFieldSharable {
    private final CodegenSharableSerdeName name;
    private final EventType eventType;

    public CodegenSharableSerdeEventTyped(CodegenSharableSerdeName name, EventType eventType) {
        this.name = name;
        this.eventType = eventType;
        if (eventType == null || name == null) {
            throw new IllegalArgumentException();
        }
    }

    public Class type() {
        return DataInputOutputSerde.class;
    }

    public CodegenExpression initCtorScoped() {
        CodegenExpression type = EventTypeUtility.resolveTypeCodegen(eventType, EPStatementInitServices.REF);
        return exprDotMethodChain(EPStatementInitServices.REF).add(EPStatementInitServices.GETEVENTTYPERESOLVER).add(GETEVENTSERDEFACTORY).add(name.methodName, type);
    }

    public enum CodegenSharableSerdeName {
        NULLABLEEVENTMAYCOLLATE("nullableEventMayCollate"),
        LISTEVENTS("listEvents"),
        LINKEDHASHMAPEVENTSANDINT("linkedHashMapEventsAndInt"),
        REFCOUNTEDSETATOMICINTEGER("refCountedSetAtomicInteger");

        private final String methodName;

        CodegenSharableSerdeName(String methodName) {
            this.methodName = methodName;
        }

        public String getMethodName() {
            return methodName;
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CodegenSharableSerdeEventTyped that = (CodegenSharableSerdeEventTyped) o;

        if (name != that.name) return false;
        return eventType.getName().equals(that.eventType.getName());
    }

    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + eventType.getName().hashCode();
        return result;
    }
}
