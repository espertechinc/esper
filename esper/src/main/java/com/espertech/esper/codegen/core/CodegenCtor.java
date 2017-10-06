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
package com.espertech.esper.codegen.core;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.base.CodegenSymbolProviderEmpty;

import java.util.List;

public class CodegenCtor extends CodegenMethodNode {
    private final List<CodegenTypedParam> params;

    public CodegenCtor(Class generator, CodegenClassScope classScope, List<CodegenTypedParam> params) {
        super(null, generator, CodegenSymbolProviderEmpty.INSTANCE, classScope);
        this.params = params;
    }

    public List<CodegenTypedParam> getCtorParams() {
        return params;
    }
}
