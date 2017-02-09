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

import com.espertech.esper.util.MetaDefItem;

import java.io.Serializable;

/**
 * Specification for creating a named window index column.
 */
public class CreateIndexItem implements MetaDefItem, Serializable {
    private static final long serialVersionUID = -3552356958442063252L;

    private final String name;
    private final CreateIndexType type;

    public CreateIndexItem(String name, CreateIndexType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public CreateIndexType getType() {
        return type;
    }
}
