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
package com.espertech.esper.compiler.client.option;

import com.espertech.esper.common.internal.compile.compiler.CompilerAbstraction;

/**
 * Implement this interface to provide a compiler to use
 */
public interface CompilerHookOption {
    /**
     * Returns the compiler to use, or null for the default compiler
     *
     * @param env the compiler tool context
     * @return compiler or null for the default compiler.
     */
    CompilerAbstraction getValue(CompilerHookContext env);
}

