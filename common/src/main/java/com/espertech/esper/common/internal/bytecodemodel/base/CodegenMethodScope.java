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
package com.espertech.esper.common.internal.bytecodemodel.base;

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;

public interface CodegenMethodScope {
    CodegenMethod makeChild(String returnType, Class generator, CodegenScope codegenClassScope);

    CodegenMethod makeChild(Class returnType, Class generator, CodegenScope codegenClassScope);

    CodegenMethod makeChildWithScope(Class returnType, Class generator, CodegenSymbolProvider symbolProvider, CodegenScope codegenClassScope);

    CodegenMethodScope addSymbol(CodegenExpressionRef symbol);
}
