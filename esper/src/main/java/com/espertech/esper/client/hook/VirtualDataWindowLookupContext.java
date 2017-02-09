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
package com.espertech.esper.client.hook;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Context passed to {@link VirtualDataWindow} upon obtaining a lookup strategy for use by an EPL statement
 * that queries the virtual data window.
 * <p>
 * Represents an analysis of correlation information provided in the where-clause of the querying EPL statement (join, subquery etc.).
 * Hash-fields are always operator-equals semantics. Btree fields require sorted access as the operator is always
 * a range or relational(&gt;, &lt;, &gt;=, &lt;=) operator.
 * <p>
 * For example, the query "select * from MyVirtualDataWindow, MyTrigger where prop = trigger and prop2 between trigger1 and trigger2"
 * indicates a single hash-field "prop" and a single btree field "prop2" with a range operator.
 */
public class VirtualDataWindowLookupContext {
    private String statementName;
    private int statementId;
    private Annotation[] statementAnnotations;
    private boolean isFireAndForget;
    private String namedWindowName;
    private List<VirtualDataWindowLookupFieldDesc> hashFields;
    private List<VirtualDataWindowLookupFieldDesc> btreeFields;

    /**
     * Ctor.
     *
     * @param hashFields           operator-equals semantics fields
     * @param btreeFields          sorted-access fields, check the {@link VirtualDataWindowLookupOp} operator for what range or relational-operator applies
     * @param namedWindowName      named window name
     * @param statementName        the statement name of the statement performing the lookup; Null for fire-and-forget queries
     * @param statementAnnotations the statement annotations of the statement performing the lookup; Null for fire-and-forget queries
     * @param fireAndForget        true for fire-and-forget queries
     * @param statementId          statement id
     */
    public VirtualDataWindowLookupContext(String statementName, int statementId, Annotation[] statementAnnotations, boolean fireAndForget, String namedWindowName, List<VirtualDataWindowLookupFieldDesc> hashFields, List<VirtualDataWindowLookupFieldDesc> btreeFields) {
        this.statementName = statementName;
        this.statementId = statementId;
        this.statementAnnotations = statementAnnotations;
        isFireAndForget = fireAndForget;
        this.namedWindowName = namedWindowName;
        this.hashFields = hashFields;
        this.btreeFields = btreeFields;
    }

    /**
     * Returns the named window name.
     *
     * @return named window name
     */
    public String getNamedWindowName() {
        return namedWindowName;
    }

    /**
     * Returns the list of hash field descriptors.
     *
     * @return hash fields
     */
    public List<VirtualDataWindowLookupFieldDesc> getHashFields() {
        return hashFields;
    }

    /**
     * Returns the list of btree field descriptors.
     *
     * @return btree fields
     */
    public List<VirtualDataWindowLookupFieldDesc> getBtreeFields() {
        return btreeFields;
    }

    /**
     * Returns the statement name of the statement to be performing the lookup, or null for fire-and-forget statements.
     *
     * @return statement name
     */
    public String getStatementName() {
        return statementName;
    }

    /**
     * Returns the statement id of the statement to be performing the lookup, or -1 for fire-and-forget statements.
     *
     * @return statement name
     */
    public int getStatementId() {
        return statementId;
    }

    /**
     * Returns the statement annotations of the statement to be performing the lookup, or null for fire-and-forget statements.
     *
     * @return statement name
     */
    public Annotation[] getStatementAnnotations() {
        return statementAnnotations;
    }

    /**
     * Returns true for fire-and-forget queries.
     *
     * @return indicator whether fire-and-forget query
     */
    public boolean isFireAndForget() {
        return isFireAndForget;
    }
}
