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
import java.util.ArrayList;
import java.util.List;

public class ForClauseSpec implements Serializable {
    private static final long serialVersionUID = -8529660985454535028L;

    private List<ForClauseItemSpec> clauses;

    public ForClauseSpec() {
        clauses = new ArrayList<ForClauseItemSpec>();
    }

    public List<ForClauseItemSpec> getClauses() {
        return clauses;
    }

    public void setClauses(List<ForClauseItemSpec> clauses) {
        this.clauses = clauses;
    }
}