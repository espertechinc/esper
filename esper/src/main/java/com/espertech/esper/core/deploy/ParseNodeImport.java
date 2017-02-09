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

public class ParseNodeImport extends ParseNode {
    private String imported;

    public ParseNodeImport(EPLModuleParseItem item, String imported) {
        super(item);
        this.imported = imported;
    }

    public String getImported() {
        return imported;
    }
}
