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
import com.espertech.esper.common.internal.serde.serdeset.builtin.DIOSkipSerde;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.publicConstValue;

public class DataInputOutputSerdeForgeNotApplicable implements DataInputOutputSerdeForge {
    public final static DataInputOutputSerdeForgeNotApplicable INSTANCE = new DataInputOutputSerdeForgeNotApplicable();

    private DataInputOutputSerdeForgeNotApplicable() {
    }

    public CodegenExpression codegen(CodegenMethod method, CodegenClassScope classScope, CodegenExpression optionalEventTypeResolver) {
        return publicConstValue(DIOSkipSerde.class, "INSTANCE");
    }

    public String forgeClassName() {
        return DIOSkipSerde.class.getName();
    }
}
