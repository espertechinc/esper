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

/**
 * Specification for creating a named window index column type.
 */
public enum CreateIndexType {
    HASH("hash"),
    BTREE("btree");

    private final String nameLower;

    CreateIndexType(String nameLower) {
        this.nameLower = nameLower;
    }

    public String getNameLower() {
        return nameLower;
    }
}
