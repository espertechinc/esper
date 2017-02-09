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
package com.espertech.esper.util;

import java.io.PrintWriter;

/**
 * Writer that uses an underlying PrintWriter to indent output text for easy reading.
 */
public class IndentWriter {
    private final PrintWriter writer;
    private final int deltaIndent;
    private int currentIndent;

    /**
     * Ctor.
     *
     * @param writer      to output to
     * @param startIndent is the depth of indent to start
     * @param deltaIndent is the number of characters to indent for every incrIndent() call
     */
    public IndentWriter(PrintWriter writer, int startIndent, int deltaIndent) {
        if (startIndent < 0) {
            throw new IllegalArgumentException("Invalid start indent");
        }
        if (deltaIndent < 0) {
            throw new IllegalArgumentException("Invalid delta indent");
        }

        this.writer = writer;
        this.deltaIndent = deltaIndent;
        this.currentIndent = startIndent;
    }

    /**
     * Increase the indentation one level.
     */
    public void incrIndent() {
        currentIndent += deltaIndent;
    }

    /**
     * Decrease the indentation one level.
     */
    public void decrIndent() {
        currentIndent -= deltaIndent;
    }

    /**
     * Print text to the underlying writer.
     *
     * @param text to print
     */
    public void println(String text) {
        int indent = currentIndent;
        if (indent < 0) {
            indent = 0;
        }
        writer.println(Indent.indent(indent) + text);
    }
}
