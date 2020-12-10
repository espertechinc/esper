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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.serde.serdeset.additional.DIOMapPropertySerde;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class DataInputOutputSerdeForgeMap implements DataInputOutputSerdeForge {
    private final String[] keys;
    private final DataInputOutputSerdeForge[] valueSerdes;

    public DataInputOutputSerdeForgeMap(String[] keys, DataInputOutputSerdeForge[] valueSerdes) {
        this.keys = keys;
        this.valueSerdes = valueSerdes;
    }

    public String forgeClassName() {
        return DIOMapPropertySerde.class.getName();
    }

    public CodegenExpression codegen(CodegenMethod method, CodegenClassScope classScope, CodegenExpression optionalEventTypeResolver) {
        return newInstance(DIOMapPropertySerde.EPTYPE, constant(keys), DataInputOutputSerdeForge.codegenArray(valueSerdes, method, classScope, optionalEventTypeResolver));
    }

    public String[] getKeys() {
        return keys;
    }

    public DataInputOutputSerdeForge[] getValueSerdes() {
        return valueSerdes;
    }
}
