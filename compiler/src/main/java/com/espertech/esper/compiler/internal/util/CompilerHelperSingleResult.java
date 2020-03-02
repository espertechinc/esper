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
package com.espertech.esper.compiler.internal.util;

import com.espertech.esper.common.internal.compile.stage1.spec.StatementSpecRaw;
import com.espertech.esper.common.internal.epl.classprovided.compiletime.ClassProvidedPrecompileResult;

public class CompilerHelperSingleResult {
    private final StatementSpecRaw statementSpecRaw;
    private ClassProvidedPrecompileResult classesInlined;

    public CompilerHelperSingleResult(StatementSpecRaw statementSpecRaw, ClassProvidedPrecompileResult classesInlined) {
        this.statementSpecRaw = statementSpecRaw;
        this.classesInlined = classesInlined;
    }

    public StatementSpecRaw getStatementSpecRaw() {
        return statementSpecRaw;
    }

    public ClassProvidedPrecompileResult getClassesInlined() {
        return classesInlined;
    }
}
