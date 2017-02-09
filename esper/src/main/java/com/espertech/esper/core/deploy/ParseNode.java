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

public abstract class ParseNode {
    private EPLModuleParseItem item;

    protected ParseNode(EPLModuleParseItem item) {
        this.item = item;
    }

    public EPLModuleParseItem getItem() {
        return item;
    }
}
