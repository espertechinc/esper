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

import com.espertech.esper.codegen.model.expression.CodegenExpression;

/**
 * Parser of a String input to an object.
 */
public interface SimpleTypeParser {
    /**
     * Parses the text and returns an object value.
     *
     * @param text to parse
     * @return object value
     */
    public Object parse(String text);

    CodegenExpression codegen(CodegenExpression input);
}
