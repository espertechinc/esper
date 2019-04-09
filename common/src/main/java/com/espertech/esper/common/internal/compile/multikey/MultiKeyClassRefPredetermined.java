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

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

public class MultiKeyClassRefPredetermined implements MultiKeyClassRef {
    private final Class clazzMK;
    private final Class[] mkTypes;
    private final CodegenExpression clazzMKSerde;

    public MultiKeyClassRefPredetermined(Class clazzMK, Class[] mkTypes, CodegenExpression clazzMKSerde) {
        this.clazzMK = clazzMK;
        this.mkTypes = mkTypes;
        this.clazzMKSerde = clazzMKSerde;
    }

    public String getClassNameMK() {
        return clazzMK.getName();
    }

    public CodegenExpression getExprMKSerde() {
        return clazzMKSerde;
    }

    public Class[] getMKTypes() {
        return mkTypes;
    }
}
