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
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableDeployTimeResolver;
import com.espertech.esper.common.internal.epl.updatehelper.EventBeanUpdateHelperForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class InfraOnMergeActionUpdForge extends InfraOnMergeActionForge {
    private final EventBeanUpdateHelperForge updateHelper;
    private final TableMetaData table;

    public InfraOnMergeActionUpdForge(ExprNode optionalFilter, EventBeanUpdateHelperForge updateHelper, TableMetaData table) {
        super(optionalFilter);
        this.updateHelper = updateHelper;
        this.table = table;
    }

    protected CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(InfraOnMergeActionUpd.class, this.getClass(), classScope);
        if (table == null) {
            method.getBlock().methodReturn(newInstance(InfraOnMergeActionUpd.class, makeFilter(method, classScope), updateHelper.makeWCopy(method, classScope)));
        } else {
            method.getBlock()
                    .declareVar(InfraOnMergeActionUpd.class, "upd", newInstance(InfraOnMergeActionUpd.class, makeFilter(method, classScope), updateHelper.makeNoCopy(method, classScope),
                            TableDeployTimeResolver.makeResolveTable(table, symbols.getAddInitSvc(method))))
                    .exprDotMethod(symbols.getAddInitSvc(method), "addReadyCallback", ref("upd"))
                    .methodReturn(ref("upd"));
        }

        return localMethod(method);
    }
}
