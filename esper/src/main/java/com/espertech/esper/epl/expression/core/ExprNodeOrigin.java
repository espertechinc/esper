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
package com.espertech.esper.epl.expression.core;

public enum ExprNodeOrigin {
    SELECT("select-clause"),
    WHERE("where-clause"),
    GROUPBY("group-by-clause"),
    HAVING("having-clause"),
    METHODINVJOIN("from-clause method-invocation"),
    DATABASEPOLL("from-clause database-access parameter"),
    CONTEXT("context declaration"),
    CONTEXTDISTINCT("context distinct-clause"),
    CONTEXTCONDITION("context condition"),
    VARIABLEASSIGN("variable-assignment"),
    DATAFLOW("dataflow operator"),
    DATAFLOWBEACON("beacon dataflow operator"),
    DATAFLOWFILTER("filter dataflow operator"),
    UPDATEASSIGN("update assignment"),
    PLUGINSINGLEROWPARAM("single-row function parameter"),
    AGGPARAM("aggregation function parameter"),
    OUTPUTLIMIT("output limit"),
    DECLAREDEXPRPARAM("declared expression parameter"),
    DECLAREDEXPRBODY("declared expression body"),
    ALIASEXPRBODY("alias expression body"),
    ORDERBY("order-by-clause"),
    SCRIPTPARAMS("script parameter"),
    FOLLOWEDBYMAX("pattern followed-by max"),
    PATTERNMATCHUNTILBOUNDS("pattern match-until bounds"),
    PATTERNGUARD("pattern guard"),
    PATTERNEVERYDISTINCT("pattern every-distinct"),
    PATTERNOBSERVER("pattern observer"),
    DOTNODEPARAMETER("method-chain parameter"),
    DOTNODE("method-chain"),
    CONTAINEDEVENT("contained-event"),
    CREATEWINDOWFILTER("create-window filter"),
    CREATETABLECOLUMN("table-column"),
    CREATEINDEXCOLUMN("create-index index-column"),
    CREATEINDEXPARAMETER("create-index index-parameter"),
    SUBQUERYSELECT("subquery select-clause"),
    FILTER("filter"),
    FORCLAUSE("for-clause"),
    VIEWPARAMETER("view parameter"),
    MATCHRECOGDEFINE("match-recognize define"),
    MATCHRECOGMEASURE("match-recognize measure"),
    MATCHRECOGPARTITION("match-recognize partition"),
    MATCHRECOGINTERVAL("match-recognize interval"),
    MATCHRECOGPATTERN("match-recognize pattern"),
    JOINON("on-clause join"),
    MERGEMATCHCOND("match condition"),
    MERGEMATCHWHERE("match where-clause"),
    HINT("hint");

    private final String clauseName;

    private ExprNodeOrigin(String clauseName) {
        this.clauseName = clauseName;
    }

    public String getClauseName() {
        return clauseName;
    }
}
