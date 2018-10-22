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
package com.espertech.esper.common.internal.epl.ontrigger;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorForge;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorUtil;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableDeployTimeResolver;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class InfraOnMergeActionInsForge extends InfraOnMergeActionForge {
    private final SelectExprProcessorForge insertHelper;
    private final TableMetaData insertIntoTable;
    private final boolean audit;
    private final boolean route;

    public InfraOnMergeActionInsForge(ExprNode optionalFilter, SelectExprProcessorForge insertHelper, TableMetaData insertIntoTable, boolean audit, boolean route) {
        super(optionalFilter);
        this.insertHelper = insertHelper;
        this.insertIntoTable = insertIntoTable;
        this.audit = audit;
        this.route = route;
    }

    public TableMetaData getInsertIntoTable() {
        return insertIntoTable;
    }

    protected CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(InfraOnMergeActionIns.class, this.getClass(), classScope);
        CodegenExpressionNewAnonymousClass anonymousSelect = SelectExprProcessorUtil.makeAnonymous(insertHelper, method, symbols.getAddInitSvc(method), classScope);
        method.getBlock().methodReturn(newInstance(InfraOnMergeActionIns.class,
                makeFilter(method, classScope), anonymousSelect,
                insertIntoTable == null ? constantNull() : TableDeployTimeResolver.makeResolveTable(insertIntoTable, symbols.getAddInitSvc(method)), constant(audit), constant(route)));
        return localMethod(method);
    }
}
