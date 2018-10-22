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
package com.espertech.esper.common.internal.context.aifactory.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSymbolProvider;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.module.EPModuleScriptInitServices;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public class ModuleScriptInitializeSymbol implements CodegenSymbolProvider {
    public final static CodegenExpressionRef REF_INITSVC = ref("moduleScriptInitSvc");

    private CodegenExpressionRef optionalInitServicesRef;

    public CodegenExpressionRef getAddInitSvc(CodegenMethodScope scope) {
        if (optionalInitServicesRef == null) {
            optionalInitServicesRef = REF_INITSVC;
        }
        scope.addSymbol(optionalInitServicesRef);
        return optionalInitServicesRef;
    }

    public void provide(Map<String, Class> symbols) {
        if (optionalInitServicesRef != null) {
            symbols.put(optionalInitServicesRef.getRef(), EPModuleScriptInitServices.class);
        }
    }
}
