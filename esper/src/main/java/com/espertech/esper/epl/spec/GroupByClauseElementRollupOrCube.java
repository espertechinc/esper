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

public class GroupByClauseElementRollupOrCube implements GroupByClauseElement {
    private static final long serialVersionUID = 4518704111521658749L;
    private final boolean cube;
    private final List<GroupByClauseElement> rollupExpressions;

    public GroupByClauseElementRollupOrCube(boolean cube, List<GroupByClauseElement> rollupExpressions) {
        this.cube = cube;
        this.rollupExpressions = rollupExpressions;
    }

    public List<GroupByClauseElement> getRollupExpressions() {
        return rollupExpressions;
    }

    public boolean isCube() {
        return cube;
    }
}
