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

import com.espertech.esper.util.LikeUtil;

/**
 *
 */
public class StringPatternSetLike implements StringPatternSet {
    private final String likeString;
    private final LikeUtil likeUtil;
    private static final long serialVersionUID = -707941336445095011L;

    /**
     * Ctor.
     *
     * @param likeString pattern to match
     */
    public StringPatternSetLike(String likeString) {
        this.likeString = likeString;
        likeUtil = new LikeUtil(likeString, '\\', false);
    }

    /**
     * Match the string returning true for a match, using SQL-like semantics.
     *
     * @param stringToMatch string to match
     * @return true for match
     */
    public boolean match(String stringToMatch) {
        if (stringToMatch == null) {
            return false;
        }
        return likeUtil.compare(stringToMatch);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StringPatternSetLike that = (StringPatternSetLike) o;

        if (!likeString.equals(that.likeString)) return false;

        return true;
    }

    public int hashCode() {
        return likeString.hashCode();
    }
}
