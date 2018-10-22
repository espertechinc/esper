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
package com.espertech.esper.common.internal.context.activator;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalEventViewableForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ViewableActivatorHistoricalForge implements ViewableActivatorForge {
    private final HistoricalEventViewableForge viewableForge;

    public ViewableActivatorHistoricalForge(HistoricalEventViewableForge viewableForge) {
        this.viewableForge = viewableForge;
    }

    public CodegenExpression makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ViewableActivatorHistorical.class, this.getClass(), classScope);
        method.getBlock().declareVar(ViewableActivatorHistorical.class, "hist", newInstance(ViewableActivatorHistorical.class))
                .exprDotMethod(ref("hist"), "setFactory", viewableForge.make(method, symbols, classScope))
                .methodReturn(ref("hist"));
        return localMethod(method);
    }
}
