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
package com.espertech.esper.epl.core.select;

import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;

import java.util.Map;

public class SelectExprProcessorCodegenSymbol {

    public final static String NAME_ISSYNTHESIZE = "isSynthesize";
    private final static CodegenExpressionRef REF_ISSYNTHESIZE = new CodegenExpressionRef(NAME_ISSYNTHESIZE);

    private CodegenExpressionRef optionalSynthesizeRef;

    public CodegenExpressionRef getAddSynthesize(CodegenMethodNode processMethod) {
        if (optionalSynthesizeRef == null) {
            optionalSynthesizeRef = REF_ISSYNTHESIZE;
        }
        processMethod.addSymbol(optionalSynthesizeRef);
        return optionalSynthesizeRef;
    }

    public void provide(Map<String, Class> symbols) {
        if (optionalSynthesizeRef != null) {
            symbols.put(optionalSynthesizeRef.getRef(), boolean.class);
        }
    }
}
