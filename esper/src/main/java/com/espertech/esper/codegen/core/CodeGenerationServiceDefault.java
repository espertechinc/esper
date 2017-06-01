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

import com.espertech.esper.epl.expression.core.ExprValidationException;

public class CodeGenerationServiceDefault<T> implements CodeGenerationService<T>  {
    public static final CodeGenerationService INSTANCE = new CodeGenerationServiceDefault();

    public T generate(CodegenClass clazz, Class<T> result) throws ExprValidationException {
        throw new ExprValidationException("Code generation is not enabled");
    }
}
