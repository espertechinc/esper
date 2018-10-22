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
package com.espertech.esper.common.client.hook.forgeinject;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

/**
 * Interface for providing the compiler with code that allocates and initializes an instance of some class
 */
public interface InjectionStrategy {
    /**
     * Returns the initialization expression
     *
     * @param classScope the class scope
     * @return class scope
     */
    CodegenExpression getInitializationExpression(CodegenClassScope classScope);
}
