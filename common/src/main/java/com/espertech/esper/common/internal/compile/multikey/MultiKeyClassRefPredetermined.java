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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

public class MultiKeyClassRefPredetermined implements MultiKeyClassRef {
    private final EPTypeClass clazzMK;
    private final EPType[] mkTypes;
    private final DataInputOutputSerdeForge serdeForge;

    public MultiKeyClassRefPredetermined(EPTypeClass clazzMK, EPType[] mkTypes, DataInputOutputSerdeForge serdeForge) {
        this.clazzMK = clazzMK;
        this.mkTypes = mkTypes;
        this.serdeForge = serdeForge;
    }

    public String getClassNameMK() {
        return clazzMK.getTypeName();
    }

    public CodegenExpression getExprMKSerde(CodegenMethod method, CodegenClassScope classScope) {
        return serdeForge.codegen(method, classScope, null);
    }

    public EPType[] getMKTypes() {
        return mkTypes;
    }
}
