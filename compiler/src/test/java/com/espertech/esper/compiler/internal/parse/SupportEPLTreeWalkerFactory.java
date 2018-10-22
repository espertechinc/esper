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
package com.espertech.esper.compiler.internal.parse;

import com.espertech.esper.common.internal.compile.stage1.spec.SelectClauseStreamSelectorEnum;
import com.espertech.esper.common.internal.compile.stage1.specmapper.StatementSpecMapEnv;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.support.SupportClasspathImport;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.Collections;

public class SupportEPLTreeWalkerFactory {
    public static EPLTreeWalkerListener makeWalker(CommonTokenStream tokenStream, ClasspathImportServiceCompileTime engineImportService) {
        StatementSpecMapEnv mapEnv = SupportStatementSpecMapEnv.make(engineImportService);
        return new EPLTreeWalkerListener(tokenStream, SelectClauseStreamSelectorEnum.ISTREAM_ONLY, Collections.emptyList(), mapEnv);
    }

    public static EPLTreeWalkerListener makeWalker(CommonTokenStream tokenStream) {
        return makeWalker(tokenStream, SupportClasspathImport.INSTANCE);
    }
}
