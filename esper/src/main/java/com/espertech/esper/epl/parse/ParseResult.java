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
package com.espertech.esper.epl.parse;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.Tree;

import java.util.List;

/**
 * Result of a parse action.
 */
public class ParseResult {
    private Tree tree;
    private String expressionWithoutAnnotations;
    private CommonTokenStream tokenStream;
    private List<String> scripts;

    /**
     * Ctor.
     *
     * @param tree                         parse tree
     * @param expressionWithoutAnnotations expression text no annotations, or null if same
     * @param scripts                      script list
     * @param tokenStream                  tokens
     */
    public ParseResult(Tree tree, String expressionWithoutAnnotations, CommonTokenStream tokenStream, List<String> scripts) {
        this.tree = tree;
        this.expressionWithoutAnnotations = expressionWithoutAnnotations;
        this.tokenStream = tokenStream;
        this.scripts = scripts;
    }

    /**
     * AST.
     *
     * @return ast
     */
    public Tree getTree() {
        return tree;
    }

    /**
     * Returns the expression text no annotations.
     *
     * @return expression text no annotations.
     */
    public String getExpressionWithoutAnnotations() {
        return expressionWithoutAnnotations;
    }

    public CommonTokenStream getTokenStream() {
        return tokenStream;
    }

    public List<String> getScripts() {
        return scripts;
    }
}
