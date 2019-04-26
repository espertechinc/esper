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
package com.espertech.esper.common.internal.compile.stage3;

import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClass;

public interface StmtClassForgeable {
    String getClassName();

    StmtClassForgeableType getForgeableType();

    CodegenClass forge(boolean includeDebugSymbols, boolean fireAndForget);
}
