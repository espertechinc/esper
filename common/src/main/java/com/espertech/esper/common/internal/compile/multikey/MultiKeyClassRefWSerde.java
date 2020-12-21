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
package com.espertech.esper.common.internal.compile.multikey;

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

public class MultiKeyClassRefWSerde implements MultiKeyClassRef {
    private final DataInputOutputSerdeForge forge;
    private final EPType[] types;

    public MultiKeyClassRefWSerde(DataInputOutputSerdeForge forge, EPType[] types) {
        this.forge = forge;
        this.types = types;
    }

    public String getClassNameMK() {
        return null;
    }

    public EPType[] getMKTypes() {
        return types;
    }

    public DataInputOutputSerdeForge getForge() {
        return forge;
    }

    public CodegenExpression getExprMKSerde(CodegenMethod method, CodegenClassScope classScope) {
        return forge.codegen(method, classScope, null);
    }

    public DataInputOutputSerdeForge[] getSerdeForges() {
        return new DataInputOutputSerdeForge[] {forge};
    }

    public <T> T accept(MultiKeyClassRefVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
