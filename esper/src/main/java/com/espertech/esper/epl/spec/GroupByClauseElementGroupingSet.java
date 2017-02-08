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

import java.util.List;

public class GroupByClauseElementGroupingSet implements GroupByClauseElement {
    private static final long serialVersionUID = -1370009169493362021L;
    private final List<GroupByClauseElement> elements;

    public GroupByClauseElementGroupingSet(List<GroupByClauseElement> elements) {
        this.elements = elements;
    }

    public List<GroupByClauseElement> getElements() {
        return elements;
    }
}
