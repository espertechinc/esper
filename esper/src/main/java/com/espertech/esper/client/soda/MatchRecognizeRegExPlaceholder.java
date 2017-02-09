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
 * For use in match recognize pattern expression as a placeholder to represent its child nodes.
 */
public class MatchRecognizeRegExPlaceholder extends MatchRecognizeRegEx implements Serializable {
    private static final long serialVersionUID = -3755313376510761985L;

    public void writeEPL(StringWriter writer) {
        if ((this.getChildren() == null) || (this.getChildren().size() == 0)) {
            return;
        }
        this.getChildren().get(0).writeEPL(writer);
    }
}