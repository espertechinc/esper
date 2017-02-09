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
package com.espertech.esper.type;

import java.io.Serializable;

/**
 * Implementation match a string against a pattern.
 */
public interface StringPatternSet extends Serializable {
    /**
     * Returns true for a match, false for no-match.
     *
     * @param stringToMatch value to match
     * @return match result
     */
    public boolean match(String stringToMatch);
}
