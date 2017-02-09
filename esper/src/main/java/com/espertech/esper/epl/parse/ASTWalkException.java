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
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;

/**
 * This exception is thrown to indicate a problem in statement creation.
 */
public class ASTWalkException extends RuntimeException {
    private static final long serialVersionUID = -339092618059394426L;

    public static ASTWalkException from(String message, Exception ex) {
        return new ASTWalkException(message, ex);
    }

    public static ASTWalkException from(String message) {
        return new ASTWalkException(message);
    }

    public static ASTWalkException from(String message, String parseTreeTextMayHaveNoWhitespace) {
        return new ASTWalkException(message + " in text '" + parseTreeTextMayHaveNoWhitespace + "'");
    }

    public static ASTWalkException from(String message, CommonTokenStream tokenStream, RuleContext parseTree) {
        return new ASTWalkException(message + " in text '" + tokenStream.getText(parseTree) + "'");
    }

    public static ASTWalkException from(String message, Token token) {
        return new ASTWalkException(message + " in text '" + token.getText() + "'");
    }

    /**
     * Ctor.
     *
     * @param message is the error message
     */
    private ASTWalkException(String message) {
        super(message);
    }

    public ASTWalkException(String message, Throwable cause) {
        super(message, cause);
    }
}

