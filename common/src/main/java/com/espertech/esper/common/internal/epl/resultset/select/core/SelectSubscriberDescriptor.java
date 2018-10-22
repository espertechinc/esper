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
package com.espertech.esper.common.internal.epl.resultset.select.core;

import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

public class SelectSubscriberDescriptor {
    private final Class[] selectClauseTypes;
    private final String[] selectClauseColumnNames;
    private final boolean forClauseDelivery;
    private final ExprNode[] groupDelivery;

    public SelectSubscriberDescriptor() {
        this.selectClauseTypes = null;
        this.selectClauseColumnNames = null;
        this.forClauseDelivery = false;
        this.groupDelivery = null;
    }

    public SelectSubscriberDescriptor(Class[] selectClauseTypes, String[] selectClauseColumnNames, boolean forClauseDelivery, ExprNode[] groupDelivery) {
        this.selectClauseTypes = selectClauseTypes;
        this.selectClauseColumnNames = selectClauseColumnNames;
        this.forClauseDelivery = forClauseDelivery;
        this.groupDelivery = groupDelivery;
    }

    public Class[] getSelectClauseTypes() {
        return selectClauseTypes;
    }

    public String[] getSelectClauseColumnNames() {
        return selectClauseColumnNames;
    }

    public boolean isForClauseDelivery() {
        return forClauseDelivery;
    }

    public ExprNode[] getGroupDelivery() {
        return groupDelivery;
    }
}
