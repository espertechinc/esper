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
package com.espertech.esper.supportregression.util;

public class IndexAssertion {
    private String hint;
    private String whereClause;
    private String expectedIndexName;
    private Class expectedStrategy;
    private String indexBackingClass;
    private Boolean unique;
    private IndexAssertionEventSend eventSendAssertion;
    private IndexAssertionFAF fafAssertion;

    public IndexAssertion(String hint, String whereClause) {
        this.hint = hint;
        this.whereClause = whereClause;
    }

    public IndexAssertion(String whereClause, String expectedIndexName, Class expectedStrategy, IndexAssertionEventSend eventSendAssertion) {
        this.whereClause = whereClause;
        this.expectedIndexName = expectedIndexName;
        this.eventSendAssertion = eventSendAssertion;
        this.expectedStrategy = expectedStrategy;
    }

    public IndexAssertion(String hint, String whereClause, String expectedIndexName, String indexBackingClass, IndexAssertionEventSend eventSendAssertion) {
        this.hint = hint;
        this.whereClause = whereClause;
        this.expectedIndexName = expectedIndexName;
        this.indexBackingClass = indexBackingClass;
        this.eventSendAssertion = eventSendAssertion;
    }

    public IndexAssertion(String hint, String whereClause, String expectedIndexName, String indexBackingClass, IndexAssertionFAF fafAssertion) {
        this.hint = hint;
        this.whereClause = whereClause;
        this.expectedIndexName = expectedIndexName;
        this.indexBackingClass = indexBackingClass;
        this.fafAssertion = fafAssertion;
    }

    public IndexAssertion(String hint, String whereClause, boolean unique, IndexAssertionEventSend eventSendAssertion) {
        this.hint = hint;
        this.whereClause = whereClause;
        this.unique = unique;
        this.eventSendAssertion = eventSendAssertion;
    }

    public IndexAssertion(String hint, String whereClause, boolean unique, IndexAssertionFAF fafAssertion) {
        this.hint = hint;
        this.whereClause = whereClause;
        this.unique = unique;
        this.fafAssertion = fafAssertion;
    }

    public String getHint() {
        return hint;
    }

    public String getWhereClause() {
        return whereClause;
    }

    public IndexAssertionEventSend getEventSendAssertion() {
        return eventSendAssertion;
    }

    public String getExpectedIndexName() {
        return expectedIndexName;
    }

    public String getIndexBackingClass() {
        return indexBackingClass;
    }

    public IndexAssertionFAF getFafAssertion() {
        return fafAssertion;
    }

    public Boolean getUnique() {
        return unique;
    }

    public Class getExpectedStrategy() {
        return expectedStrategy;
    }
}
