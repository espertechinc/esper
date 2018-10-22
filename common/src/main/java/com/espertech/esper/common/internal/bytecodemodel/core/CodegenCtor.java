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
package com.espertech.esper.common.internal.bytecodemodel.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSymbolProviderEmpty;

import java.util.List;
import java.util.Set;

public class CodegenCtor extends CodegenMethod {
    private final List<CodegenTypedParam> params;

    public CodegenCtor(Class generator, boolean includeDebugSymbols, List<CodegenTypedParam> params) {
        super(null, null, generator, CodegenSymbolProviderEmpty.INSTANCE, new CodegenScope(includeDebugSymbols));
        this.params = params;
    }

    public CodegenCtor(Class generator, CodegenClassScope classScope, List<CodegenTypedParam> params) {
        super(null, null, generator, CodegenSymbolProviderEmpty.INSTANCE, new CodegenScope(classScope.isDebug()));
        this.params = params;
    }

    public List<CodegenTypedParam> getCtorParams() {
        return params;
    }

    @Override
    public void mergeClasses(Set<Class> classes) {
        super.mergeClasses(classes);
        for (CodegenTypedParam param : params) {
            param.mergeClasses(classes);
        }
    }
}
