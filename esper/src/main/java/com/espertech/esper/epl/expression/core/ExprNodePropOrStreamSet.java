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

import java.util.*;

public class ExprNodePropOrStreamSet {

    private Set<ExprNodePropOrStreamPropDesc> properties;
    private List<ExprNodePropOrStreamExprDesc> expressions;

    public ExprNodePropOrStreamSet() {
    }

    public void add(ExprNodePropOrStreamDesc desc) {
        if (desc instanceof ExprNodePropOrStreamPropDesc) {
            allocateProperties();
            properties.add((ExprNodePropOrStreamPropDesc) desc);
        } else if (desc instanceof ExprNodePropOrStreamExprDesc) {
            allocateExpressions();
            expressions.add((ExprNodePropOrStreamExprDesc) desc);
        }
    }

    public void addAll(List<ExprNodePropOrStreamDesc> propertiesNode) {
        for (ExprNodePropOrStreamDesc desc : propertiesNode) {
            add(desc);
        }
    }

    public void addAll(ExprNodePropOrStreamSet other) {
        if (other.properties != null) {
            allocateProperties();
            properties.addAll(other.properties);
        }
        if (other.expressions != null) {
            allocateExpressions();
            expressions.addAll(other.expressions);
        }
    }

    public boolean isEmpty() {
        return (properties == null || properties.isEmpty()) &&
                (expressions == null || expressions.isEmpty());
    }

    /**
     * Remove from the provided list those that are matching any of the contained-herein
     *
     * @param items target list
     */
    public void removeFromList(List<ExprNodePropOrStreamDesc> items) {
        Iterator<ExprNodePropOrStreamDesc> item = items.iterator();
        for (; item.hasNext(); ) {
            if (findItem(item.next())) {
                item.remove();
            }
        }
    }

    public String notContainsAll(ExprNodePropOrStreamSet other) {
        if (other.properties != null) {
            for (ExprNodePropOrStreamPropDesc otherProp : other.properties) {
                boolean found = findItem(otherProp);
                if (!found) {
                    return otherProp.getTextual();
                }
            }
        }
        if (other.expressions != null) {
            for (ExprNodePropOrStreamExprDesc otherExpr : other.expressions) {
                boolean found = findItem(otherExpr);
                if (!found) {
                    return otherExpr.getTextual();
                }
            }
        }
        return null;
    }

    public Collection<ExprNodePropOrStreamPropDesc> getProperties() {
        if (properties == null) {
            return Collections.emptyList();
        }
        return properties;
    }

    public ExprNodePropOrStreamExprDesc getFirstExpression() {
        if (expressions == null || expressions.isEmpty()) {
            return null;
        }
        return expressions.get(0);
    }

    public ExprNodePropOrStreamDesc getFirstWithStreamNumNotZero() {
        if (properties != null) {
            for (ExprNodePropOrStreamPropDesc prop : properties) {
                if (prop.getStreamNum() != 0) {
                    return prop;
                }
            }
        }
        if (expressions != null) {
            for (ExprNodePropOrStreamExprDesc expr : expressions) {
                if (expr.getStreamNum() != 0) {
                    return expr;
                }
            }
        }
        return null;
    }

    private void allocateProperties() {
        if (properties == null) {
            properties = new HashSet<ExprNodePropOrStreamPropDesc>();
        }
    }

    private void allocateExpressions() {
        if (expressions == null) {
            expressions = new ArrayList<ExprNodePropOrStreamExprDesc>(4);
        }
    }

    private boolean findItem(ExprNodePropOrStreamDesc item) {
        if (item instanceof ExprNodePropOrStreamPropDesc) {
            return properties != null && properties.contains(item);
        }
        if (expressions == null) {
            return false;
        }
        ExprNodePropOrStreamExprDesc exprItem = (ExprNodePropOrStreamExprDesc) item;
        for (ExprNodePropOrStreamExprDesc expression : expressions) {
            if (expression.getStreamNum() != exprItem.getStreamNum()) {
                continue;
            }
            if (ExprNodeUtilityCore.deepEquals(expression.getOriginator(), exprItem.getOriginator(), false)) {
                return true;
            }
        }
        return false;
    }
}
