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
package com.espertech.esper.common.internal.serde.serdeset.builtin;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class DIONullableObjectArraySerdeForge implements DataInputOutputSerdeForge {
    private final EPTypeClass componentType;
    private final DataInputOutputSerdeForge componentSerde;

    public DIONullableObjectArraySerdeForge(EPTypeClass componentType, DataInputOutputSerdeForge componentSerde) {
        this.componentType = componentType;
        this.componentSerde = componentSerde;
    }

    public String forgeClassName() {
        return DIONullableObjectArraySerde.class.getName();
    }

    public CodegenExpression codegen(CodegenMethod method, CodegenClassScope classScope, CodegenExpression optionalEventTypeResolver) {
        return newInstance(DIONullableObjectArraySerde.EPTYPE, constant(componentType.getType()), componentSerde.codegen(method, classScope, optionalEventTypeResolver));
    }

    public EPTypeClass getComponentType() {
        return componentType;
    }

    public DataInputOutputSerdeForge getComponentSerde() {
        return componentSerde;
    }
}
