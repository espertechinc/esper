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

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;

public class Antlr4ErrorStrategy extends BailErrorStrategy {
    public void reportError(Parser recognizer, RecognitionException e) {
        // Antlr has an issue handling LexerNoViableAltException as then offending token can be null
        // Try: "select a.b('aa\") from A"
        if (e instanceof LexerNoViableAltException && e.getOffendingToken() == null) {
            return;
        }
        super.reportError(recognizer, e);
    }
}

