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
package com.espertech.esper.common.internal.serde.compiletime.resolve;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.util.function.Function;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethodChain;
import static com.espertech.esper.common.internal.event.path.EventTypeResolver.GETEVENTSERDEFACTORY;

public class DataInputOutputSerdeForgeEventSerde implements DataInputOutputSerdeForge {
    private final DataInputOutputSerdeForgeEventSerdeMethod method;
    private final EventType eventType;
    private final Function<DataInputOutputSerdeForgeParameterizedVars, CodegenExpression>[] functions;

    public DataInputOutputSerdeForgeEventSerde(DataInputOutputSerdeForgeEventSerdeMethod method, EventType eventType, Function<DataInputOutputSerdeForgeParameterizedVars, CodegenExpression>... functions) {
        this.method = method;
        this.eventType = eventType;
        this.functions = functions;
    }

    public String forgeClassName() {
        return DataInputOutputSerde.class.getName();
    }

    public EventType getEventType() {
        return eventType;
    }

    public DataInputOutputSerdeForgeEventSerdeMethod getMethod() {
        return method;
    }

    public CodegenExpression codegen(CodegenMethod method, CodegenClassScope classScope, CodegenExpression optionalEventTypeResolver) {
        CodegenExpression[] params = new CodegenExpression[functions.length];
        DataInputOutputSerdeForgeParameterizedVars vars = new DataInputOutputSerdeForgeParameterizedVars(method, classScope, optionalEventTypeResolver);
        for (int i = 0; i < params.length; i++) {
            params[i] = functions[i].apply(vars);
        }
        return exprDotMethodChain(optionalEventTypeResolver).add(GETEVENTSERDEFACTORY).add(this.method.getMethodName(), params);
    }

}
