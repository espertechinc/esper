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
package com.espertech.esper.common.internal.epl.updatehelper;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprArrayElementForgeProperty;
import com.espertech.esper.common.internal.epl.expression.core.ExprArrayElementIdentNodeExpressions;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.event.core.EventPropertyWriterSPI;
import com.espertech.esper.common.internal.util.TypeWidenerSPI;

public class EventBeanUpdateItemForge {
    private final ExprForge expression;
    private final String optionalPropertyName;
    private final EventPropertyWriterSPI optionalWriter;
    private final boolean notNullableField;
    private final TypeWidenerSPI optionalWidener;
    private final ExprArrayElementForgeProperty optionalArrayNode;

    public EventBeanUpdateItemForge(ExprForge expression, String optionalPropertyName, EventPropertyWriterSPI optionalWriter, boolean notNullableField, TypeWidenerSPI optionalWidener, ExprArrayElementForgeProperty optionalArrayNode) {
        this.expression = expression;
        this.optionalPropertyName = optionalPropertyName;
        this.optionalWriter = optionalWriter;
        this.notNullableField = notNullableField;
        this.optionalWidener = optionalWidener;
        this.optionalArrayNode = optionalArrayNode;
    }

    public ExprForge getExpression() {
        return expression;
    }

    public String getOptionalPropertyName() {
        return optionalPropertyName;
    }

    public EventPropertyWriterSPI getOptionalWriter() {
        return optionalWriter;
    }

    public boolean isNotNullableField() {
        return notNullableField;
    }

    public TypeWidenerSPI getOptionalWidener() {
        return optionalWidener;
    }

    public ExprArrayElementForgeProperty getOptionalArrayNode() {
        return optionalArrayNode;
    }

    public EventBeanUpdateItemForgeWExpressions toExpression(Class type, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        CodegenExpression rhs = expression.evaluateCodegen(type, parent, symbols, classScope);
        ExprArrayElementIdentNodeExpressions arrayExpressions = null;
        if (optionalArrayNode != null) {
            arrayExpressions = optionalArrayNode.getArrayExpressions(parent, symbols, classScope);
        }
        return new EventBeanUpdateItemForgeWExpressions(rhs, arrayExpressions);
    }
}