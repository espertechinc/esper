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
package com.espertech.esper.core.deploy;

public class ParseNodeUses extends ParseNode {
    private String uses;

    public ParseNodeUses(EPLModuleParseItem item, String uses) {
        super(item);
        this.uses = uses;
    }

    public String getUses() {
        return uses;
    }
}
