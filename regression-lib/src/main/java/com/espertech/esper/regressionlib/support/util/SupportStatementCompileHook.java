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
package com.espertech.esper.regressionlib.support.util;

import com.espertech.esper.common.internal.compile.stage2.StatementCompileHook;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;

import java.util.ArrayList;
import java.util.List;

public class SupportStatementCompileHook implements StatementCompileHook {

    private static final List<StatementSpecCompiled> SPECS = new ArrayList<>();

    public static String resetGetClassName() {
        reset();
        return SupportStatementCompileHook.class.getName();
    }

    public static void reset() {
        SPECS.clear();
    }

    public void compiled(StatementSpecCompiled compiled) {
        SPECS.add(compiled);
    }

    public static List<StatementSpecCompiled> getSpecs() {
        List<StatementSpecCompiled> copy = new ArrayList<>(SPECS);
        reset();
        return copy;
    }
}
