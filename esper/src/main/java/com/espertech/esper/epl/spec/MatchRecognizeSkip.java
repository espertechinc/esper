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
package com.espertech.esper.epl.spec;

import java.io.Serializable;

/**
 * Specification for the skip-part of match_recognize.
 */
public class MatchRecognizeSkip implements Serializable {
    private MatchRecognizeSkipEnum skip;
    private static final long serialVersionUID = 579228626022249216L;

    /**
     * Ctor.
     *
     * @param skip enum
     */
    public MatchRecognizeSkip(MatchRecognizeSkipEnum skip) {
        this.skip = skip;
    }

    /**
     * Skip enum.
     *
     * @return skip value
     */
    public MatchRecognizeSkipEnum getSkip() {
        return skip;
    }

    /**
     * Sets the skip value.
     *
     * @param skip to set
     */
    public void setSkip(MatchRecognizeSkipEnum skip) {
        this.skip = skip;
    }
}
