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

import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.internal.compile.stage1.Compilable;
import com.espertech.esper.common.internal.compile.stage1.spec.SelectClauseStreamSelectorEnum;
import com.espertech.esper.common.internal.compile.stage1.spec.StatementSpecRaw;
import com.espertech.esper.common.internal.compile.stage1.specmapper.StatementSpecMapEnv;
import com.espertech.esper.common.internal.compile.stage1.specmapper.StatementSpecMapper;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompileException;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.util.ValidationException;
import com.espertech.esper.compiler.internal.generated.EsperEPL2GrammarParser;
import com.espertech.esper.compiler.internal.parse.*;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.tree.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompilerHelperSingleEPL {

    private final static ParseRuleSelector EPL_PARSE_RULE;
    private final static Logger log = LoggerFactory.getLogger(CompilerHelperSingleEPL.class);

    static {
        EPL_PARSE_RULE = new ParseRuleSelector() {
            public Tree invokeParseRule(EsperEPL2GrammarParser parser) throws RecognitionException {
                return parser.startEPLExpressionRule();
            }
        };
    }

    protected static StatementSpecRaw parseWalk(Compilable compilable, StatementCompileTimeServices compileTimeServices)
            throws StatementSpecCompileException {
        StatementSpecRaw specRaw;
        try {
            if (compilable instanceof CompilableEPL) {
                CompilableEPL compilableEPL = (CompilableEPL) compilable;
                specRaw = parseWalk(compilableEPL.getEpl(), compileTimeServices.getStatementSpecMapEnv());
            } else if (compilable instanceof CompilableSODA) {
                EPStatementObjectModel soda = ((CompilableSODA) compilable).getSoda();
                specRaw = StatementSpecMapper.map(soda, compileTimeServices.getStatementSpecMapEnv());
            } else {
                throw new IllegalStateException("Unrecognized compilable " + compilable);
            }
        } catch (StatementSpecCompileException ex) {
            throw ex;
        } catch (Throwable t) {
            throw new StatementSpecCompileException("Unexpected exception parsing statement: " + t.getMessage(), t, compilable.toEPL());
        }
        return specRaw;
    }

    public static StatementSpecRaw parseWalk(String epl, StatementSpecMapEnv mapEnv)
            throws StatementSpecCompileException {
        ParseResult parseResult = ParseHelper.parse(epl, epl, true, EPL_PARSE_RULE, true);
        Tree ast = parseResult.getTree();

        SelectClauseStreamSelectorEnum defaultStreamSelector = StatementSpecMapper.mapFromSODA(mapEnv.getConfiguration().getCompiler().getStreamSelection().getDefaultStreamSelector());
        EPLTreeWalkerListener walker = new EPLTreeWalkerListener(parseResult.getTokenStream(), defaultStreamSelector, parseResult.getScripts(), mapEnv);

        try {
            ParseHelper.walk(ast, walker, epl, epl);
        } catch (ASTWalkException | ValidationException ex) {
            throw new StatementSpecCompileException(ex.getMessage(), ex, epl);
        } catch (RuntimeException ex) {
            String message = "Invalid expression encountered";
            throw new StatementSpecCompileException(getNullableErrortext(message, ex.getMessage()), ex, epl);
        }

        if (log.isDebugEnabled()) {
            ASTUtil.dumpAST(ast);
        }

        return walker.getStatementSpec();
    }

    private static String getNullableErrortext(String msg, String cause) {
        if (cause == null) {
            return msg;
        } else {
            return cause;
        }
    }
}
