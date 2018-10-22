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
package com.espertech.esper.common.internal.epl.resultset.order;


import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedMethods;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenTypedParam;

import java.util.List;

public interface OrderByProcessorFactoryForge {

    void instantiateCodegen(CodegenMethod method, CodegenClassScope classScope);

    void ctorCodegen(CodegenCtor ctor, List<CodegenTypedParam> members, CodegenClassScope classScope);

    void sortPlainCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods);

    void sortWGroupKeysCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods);

    void sortRollupCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods);

    void getSortKeyCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods);

    void getSortKeyRollupCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods);

    void sortWOrderKeysCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods);

    void sortTwoKeysCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods);
}
