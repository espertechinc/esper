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

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newArrayWithInit;

/**
 * Encapsulates information on what serde should be used, for byte code production.
 * Byte code production produces the equivalent {@link com.espertech.esper.common.client.serde.DataInputOutputSerde}.
 */
public interface DataInputOutputSerdeForge {
    static CodegenExpression codegenArray(DataInputOutputSerdeForge[] serdes, CodegenMethod method, CodegenClassScope classScope, CodegenExpression optionalEventTypeResolver) {
        if (serdes == null) {
            return constantNull();
        }
        CodegenExpression[] expressions = new CodegenExpression[serdes.length];
        for (int i = 0; i < serdes.length; i++) {
            expressions[i] = serdes[i].codegen(method, classScope, optionalEventTypeResolver);
        }
        return newArrayWithInit(DataInputOutputSerde.class, expressions);
    }

    String forgeClassName();
    CodegenExpression codegen(CodegenMethod method, CodegenClassScope classScope, CodegenExpression optionalEventTypeResolver);
}
