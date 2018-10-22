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
package com.espertech.esper.common.internal.epl.table.compiletime;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleTableInitializeSymbol;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class TableMetadataColumnPlain extends TableMetadataColumn {

    private int indexPlain;

    public TableMetadataColumnPlain() {
    }

    public TableMetadataColumnPlain(String columnName, boolean key, int indexPlain) {
        super(columnName, key);
        this.indexPlain = indexPlain;
    }

    protected CodegenExpression make(CodegenMethodScope parent, ModuleTableInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(TableMetadataColumnPlain.class, this.getClass(), classScope);
        method.getBlock().declareVar(TableMetadataColumnPlain.class, "col", newInstance(TableMetadataColumnPlain.class));
        super.makeSettersInline(ref("col"), method.getBlock());
        method.getBlock()
                .exprDotMethod(ref("col"), "setIndexPlain", constant(indexPlain))
                .methodReturn(ref("col"));
        return localMethod(method);
    }

    public int getIndexPlain() {
        return indexPlain;
    }

    public void setIndexPlain(int indexPlain) {
        this.indexPlain = indexPlain;
    }
}
