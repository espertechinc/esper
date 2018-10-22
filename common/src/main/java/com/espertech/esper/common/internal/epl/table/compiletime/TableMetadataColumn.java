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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleTableInitializeSymbol;

import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public abstract class TableMetadataColumn {

    private String columnName;
    private boolean key;

    public TableMetadataColumn() {
    }

    protected TableMetadataColumn(String columnName, boolean key) {
        this.columnName = columnName;
        this.key = key;
    }

    protected abstract CodegenExpression make(CodegenMethodScope parent, ModuleTableInitializeSymbol symbols, CodegenClassScope classScope);

    void makeSettersInline(CodegenExpressionRef col, CodegenBlock block) {
        block.exprDotMethod(col, "setKey", constant(key))
                .exprDotMethod(col, "setColumnName", constant(columnName));
    }

    public static CodegenExpression makeColumns(Map<String, TableMetadataColumn> columns, CodegenMethodScope parent, ModuleTableInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(Map.class, TableMetadataColumn.class, classScope);
        method.getBlock().declareVar(Map.class, "cols", newInstance(HashMap.class));
        for (Map.Entry<String, TableMetadataColumn> entry : columns.entrySet()) {
            method.getBlock().exprDotMethod(ref("cols"), "put", constant(entry.getKey()), entry.getValue().make(method, symbols, classScope));
        }
        method.getBlock().methodReturn(ref("cols"));
        return localMethod(method);
    }

    public boolean isKey() {
        return key;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public void setKey(boolean key) {
        this.key = key;
    }
}
