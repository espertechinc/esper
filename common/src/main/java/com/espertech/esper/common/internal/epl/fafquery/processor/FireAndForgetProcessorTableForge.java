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
package com.espertech.esper.common.internal.epl.fafquery.processor;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableDeployTimeResolver;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class FireAndForgetProcessorTableForge implements FireAndForgetProcessorForge {
    private final TableMetaData table;

    public FireAndForgetProcessorTableForge(TableMetaData table) {
        this.table = table;
    }

    public String getNamedWindowOrTableName() {
        return table.getTableName();
    }

    public String getContextName() {
        return table.getOptionalContextName();
    }

    public EventType getEventTypeRSPInputEvents() {
        return table.getInternalEventType();
    }

    public EventType getEventTypePublic() {
        return table.getPublicEventType();
    }

    public String[][] getUniqueIndexes() {
        return table.getIndexMetadata().getUniqueIndexProps();
    }

    public TableMetaData getTable() {
        return table;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(FireAndForgetProcessorTable.class, this.getClass(), classScope);
        CodegenExpressionRef nw = ref("tbl");
        method.getBlock()
                .declareVar(FireAndForgetProcessorTable.class, nw.getRef(), newInstance(FireAndForgetProcessorTable.class))
                .exprDotMethod(nw, "setTable", TableDeployTimeResolver.makeResolveTable(table, symbols.getAddInitSvc(method)))
                .methodReturn(nw);
        return localMethod(method);
    }
}
