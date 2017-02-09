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
package com.espertech.esper.client.soda;

import java.io.Serializable;
import java.io.StringWriter;

/**
 * Interface representing a permutation expression for use in match-recognize.
 */
public class MatchRecognizeRegExPermutation extends MatchRecognizeRegEx implements Serializable {
    private static final long serialVersionUID = -734937123541485671L;

    public void writeEPL(StringWriter writer) {
        String delimiter = "";
        writer.write("match_recognize_permute(");
        for (MatchRecognizeRegEx node : this.getChildren()) {
            writer.append(delimiter);
            node.writeEPL(writer);
            delimiter = ",";
        }
        writer.write(")");
    }
}