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
package com.espertech.esper.common.internal.context.aifactory.createindex;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItemForge;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowDeployTimeResolver;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableDeployTimeResolver;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class StatementAgentInstanceFactoryCreateIndexForge {

    private final EventType eventType;
    private final String indexName;
    private final String indexModuleName;
    private final QueryPlanIndexItemForge explicitIndexDesc;
    private final IndexMultiKey imk;
    private final NamedWindowMetaData namedWindow;
    private final TableMetaData table;

    public StatementAgentInstanceFactoryCreateIndexForge(EventType eventType, String indexName, String indexModuleName, QueryPlanIndexItemForge explicitIndexDesc, IndexMultiKey imk, NamedWindowMetaData namedWindow, TableMetaData table) {
        this.eventType = eventType;
        this.indexName = indexName;
        this.indexModuleName = indexModuleName;
        this.explicitIndexDesc = explicitIndexDesc;
        this.imk = imk;
        this.namedWindow = namedWindow;
        this.table = table;
    }

    public CodegenMethod initializeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(StatementAgentInstanceFactoryCreateIndex.class, this.getClass(), classScope);
        CodegenExpressionRef saiff = ref("saiff");
        method.getBlock()
                .declareVar(StatementAgentInstanceFactoryCreateIndex.class, saiff.getRef(), newInstance(StatementAgentInstanceFactoryCreateIndex.class))
                .exprDotMethod(saiff, "setEventType", EventTypeUtility.resolveTypeCodegen(eventType, symbols.getAddInitSvc(method)))
                .exprDotMethod(saiff, "setIndexName", constant(indexName))
                .exprDotMethod(saiff, "setIndexModuleName", constant(indexModuleName))
                .exprDotMethod(saiff, "setIndexMultiKey", imk.make(method, classScope))
                .exprDotMethod(saiff, "setExplicitIndexDesc", explicitIndexDesc.make(method, classScope));
        if (namedWindow != null) {
            method.getBlock().exprDotMethod(saiff, "setNamedWindow", NamedWindowDeployTimeResolver.makeResolveNamedWindow(namedWindow, symbols.getAddInitSvc(method)));
        } else {
            method.getBlock().exprDotMethod(saiff, "setTable", TableDeployTimeResolver.makeResolveTable(table, symbols.getAddInitSvc(method)));
        }
        method.getBlock().methodReturn(saiff);
        return method;
    }
}
