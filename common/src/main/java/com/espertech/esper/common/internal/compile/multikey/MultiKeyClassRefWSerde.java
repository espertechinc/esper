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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

public class MultiKeyClassRefWSerde implements MultiKeyClassRef {
    private final DataInputOutputSerdeForge forge;
    private final Class[] types;

    public MultiKeyClassRefWSerde(DataInputOutputSerdeForge forge, Class[] types) {
        this.forge = forge;
        this.types = types;
    }

    public String getClassNameMK() {
        return null;
    }

    public Class[] getMKTypes() {
        return types;
    }

    public CodegenExpression getExprMKSerde(CodegenMethod method, CodegenClassScope classScope) {
        return forge.codegen(method, classScope, null);
    }
}
