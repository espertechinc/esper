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
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexMetadataUtil;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowDeployTimeResolver;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class FireAndForgetProcessorNamedWindowForge implements FireAndForgetProcessorForge {
    private final NamedWindowMetaData namedWindow;

    public FireAndForgetProcessorNamedWindowForge(NamedWindowMetaData namedWindow) {
        this.namedWindow = namedWindow;
    }

    public String getNamedWindowOrTableName() {
        return namedWindow.getEventType().getName();
    }

    public EventType getEventTypeRSPInputEvents() {
        return namedWindow.getEventType();
    }

    public EventType getEventTypePublic() {
        return namedWindow.getEventType();
    }

    public String getContextName() {
        return namedWindow.getContextName();
    }

    public String[][] getUniqueIndexes() {
        return EventTableIndexMetadataUtil.getUniqueness(namedWindow.getIndexMetadata(), namedWindow.getUniqueness());
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(FireAndForgetProcessorNamedWindow.class, this.getClass(), classScope);
        CodegenExpressionRef nw = ref("nw");
        method.getBlock()
                .declareVar(FireAndForgetProcessorNamedWindow.class, nw.getRef(), newInstance(FireAndForgetProcessorNamedWindow.class))
                .exprDotMethod(nw, "setNamedWindow", NamedWindowDeployTimeResolver.makeResolveNamedWindow(namedWindow, symbols.getAddInitSvc(method)))
                .methodReturn(nw);
        return localMethod(method);
    }
}
