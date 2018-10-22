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
package com.espertech.esper.common.client.util;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

/**
 * For use with Count-min sketch aggregation functions:
 * The agent implementation encapsulates transformation of value objects to byte-array and back (when needed),
 * and may override or provide custom behavior.
 * <p>
 * This is an extension API and may use internal classes. As such the interface may change between versions.
 * </p>
 */
public interface CountMinSketchAgentForge {
    /**
     * Returns an array of types that the agent can handle, for validation purposes.
     * For example, an agent that accepts byte-array type values should return "new Class[] {String.class}".
     * Interfaces and supertype classes can also be part of the class array.
     *
     * @return class array of acceptable type
     */
    public Class[] getAcceptableValueTypes();

    /**
     * Provides the code for the agent.
     *
     * @param parent     parent methods
     * @param classScope class scope
     * @return expression
     */
    CodegenExpression codegenMake(CodegenMethod parent, CodegenClassScope classScope);
}
