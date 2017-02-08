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

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;

import java.io.IOException;
import java.io.Reader;

/**
 * For use with ANTLR to create a case-insensitive token stream.
 */
public class NoCaseSensitiveStream extends ANTLRInputStream {
    /**
     * Ctor.
     *
     * @param reader is the reader providing the characters to inspect
     * @throws IOException to indicate IO errors
     */
    public NoCaseSensitiveStream(Reader reader)
            throws IOException {
        super(reader);
    }

    public int LA(int i) {
        if (i == 0) {
            return 0; // undefined
        }
        if (i < 0) {
            i++; // e.g., translate LA(-1) to use offset 0
        }
        if ((p + i - 1) >= n) {
            return CharStream.EOF;
        }
        return Character.toLowerCase(data[p + i - 1]);
    }
}
