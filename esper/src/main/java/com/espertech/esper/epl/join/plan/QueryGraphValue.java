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
package com.espertech.esper.epl.join.plan;

import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprIdentNode;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.util.CollectionUtil;

import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Property lists stored as a value for each stream-to-stream relationship, for use by {@link QueryGraph}.
 */
public class QueryGraphValue {
    private List<QueryGraphValueDesc> items;

    /**
     * Ctor.
     */
    public QueryGraphValue() {
        items = new ArrayList<>();
    }

    public boolean isEmptyNotNavigable() {
        return items.isEmpty();
    }

    public List<QueryGraphValueDesc> getItems() {
        return items;
    }

    /**
     * Add key and index property.
     *
     * @param keyProperty        - key property
     * @param indexPropertyIdent - index property
     * @param keyPropNode        key node
     * @return true if added and either property did not exist, false if either already existed
     */
    public boolean addStrictCompare(String keyProperty, ExprIdentNode keyPropNode, ExprIdentNode indexPropertyIdent) {
        QueryGraphValueDesc value = findIdentEntry(indexPropertyIdent);
        if (value != null && value.getEntry() instanceof QueryGraphValueEntryHashKeyedExpr) {
            // if this index property exists and is compared to a constant, ignore the index prop
            QueryGraphValueEntryHashKeyedExpr expr = (QueryGraphValueEntryHashKeyedExpr) value.getEntry();
            if (expr.isConstant()) {
                return false;
            }
        }
        if (value != null && value.getEntry() instanceof QueryGraphValueEntryHashKeyedProp) {
            return false;   // second comparison, ignore
        }

        items.add(new QueryGraphValueDesc(new ExprNode[]{indexPropertyIdent}, new QueryGraphValueEntryHashKeyedProp(keyPropNode, keyProperty)));
        return true;
    }

    public void addRange(QueryGraphRangeEnum rangeType, ExprNode propertyStart, ExprNode propertyEnd, ExprIdentNode propertyValueIdent) {
        if (!rangeType.isRange()) {
            throw new IllegalArgumentException("Expected range type, received " + rangeType);
        }

        // duplicate can be removed right away
        if (findIdentEntry(propertyValueIdent) != null) {
            return;
        }

        items.add(new QueryGraphValueDesc(new ExprNode[]{propertyValueIdent}, new QueryGraphValueEntryRangeIn(rangeType, propertyStart, propertyEnd, true)));
    }

    public void addRelOp(ExprNode propertyKey, QueryGraphRangeEnum op, ExprIdentNode propertyValueIdent, boolean isBetweenOrIn) {

        // Note: Read as follows:
        // System.out.println("If I have an index on '" + propertyValue + "' I'm evaluating " + propertyKey + " and finding all values of " + propertyValue + " " + op + " then " + propertyKey);

        // Check if there is an opportunity to convert this to a range or remove an earlier specification
        QueryGraphValueDesc existing = findIdentEntry(propertyValueIdent);
        if (existing == null) {
            items.add(new QueryGraphValueDesc(new ExprNode[]{propertyValueIdent}, new QueryGraphValueEntryRangeRelOp(op, propertyKey, isBetweenOrIn)));
            return;
        }

        if (!(existing.getEntry() instanceof QueryGraphValueEntryRangeRelOp)) {
            return; // another comparison exists already, don't add range
        }

        QueryGraphValueEntryRangeRelOp relOp = (QueryGraphValueEntryRangeRelOp) existing.getEntry();
        QueryGraphRangeConsolidateDesc opsDesc = QueryGraphRangeUtil.getCanConsolidate(op, relOp.getType());
        if (opsDesc != null) {
            ExprNode start = !opsDesc.isReverse() ? relOp.getExpression() : propertyKey;
            ExprNode end = !opsDesc.isReverse() ? propertyKey : relOp.getExpression();
            items.remove(existing);
            addRange(opsDesc.getType(), start, end, propertyValueIdent);
        }
    }

    public void addUnkeyedExpr(ExprIdentNode indexedPropIdent, ExprNode exprNodeNoIdent) {
        items.add(new QueryGraphValueDesc(new ExprNode[]{indexedPropIdent}, new QueryGraphValueEntryHashKeyedExpr(exprNodeNoIdent, false)));
    }

    public void addKeyedExpr(ExprIdentNode indexedPropIdent, ExprNode exprNodeNoIdent) {
        items.add(new QueryGraphValueDesc(new ExprNode[]{indexedPropIdent}, new QueryGraphValueEntryHashKeyedExpr(exprNodeNoIdent, true)));
    }

    public QueryGraphValuePairHashKeyIndex getHashKeyProps() {
        List<QueryGraphValueEntryHashKeyed> keys = new ArrayList<QueryGraphValueEntryHashKeyed>();
        Deque<String> indexed = new ArrayDeque<String>();
        for (QueryGraphValueDesc desc : items) {
            if (desc.getEntry() instanceof QueryGraphValueEntryHashKeyed) {
                QueryGraphValueEntryHashKeyed keyprop = (QueryGraphValueEntryHashKeyed) desc.getEntry();
                keys.add(keyprop);
                indexed.add(getSingleIdentNodeProp(desc.getIndexExprs()));
            }
        }

        String[] strictKeys = new String[indexed.size()];
        int count = 0;
        for (QueryGraphValueDesc desc : items) {
            if (desc.getEntry() instanceof QueryGraphValueEntryHashKeyed) {
                if (desc.getEntry() instanceof QueryGraphValueEntryHashKeyedProp) {
                    QueryGraphValueEntryHashKeyedProp keyprop = (QueryGraphValueEntryHashKeyedProp) desc.getEntry();
                    strictKeys[count] = keyprop.getKeyProperty();
                }
                count++;
            }
        }

        return new QueryGraphValuePairHashKeyIndex(indexed.toArray(new String[indexed.size()]), keys, strictKeys);
    }

    public QueryGraphValuePairRangeIndex getRangeProps() {
        Deque<String> indexed = new ArrayDeque<String>();
        List<QueryGraphValueEntryRange> keys = new ArrayList<QueryGraphValueEntryRange>();
        for (QueryGraphValueDesc desc : items) {
            if (desc.getEntry() instanceof QueryGraphValueEntryRange) {
                QueryGraphValueEntryRange keyprop = (QueryGraphValueEntryRange) desc.getEntry();
                keys.add(keyprop);
                indexed.add(getSingleIdentNodeProp(desc.getIndexExprs()));
            }
        }
        return new QueryGraphValuePairRangeIndex(indexed.toArray(new String[indexed.size()]), keys);
    }

    public String toString() {
        StringWriter writer = new StringWriter();
        writer.append("QueryGraphValue ");
        String delimiter = "";
        for (QueryGraphValueDesc desc : items) {
            writer.append(delimiter);
            writer.append(ExprNodeUtilityCore.toExpressionStringMinPrecedenceAsList(desc.getIndexExprs()));
            writer.append(": ");
            writer.append(desc.getEntry().toString());
            delimiter = ", ";
        }
        return writer.toString();
    }

    public void addInKeywordMultiIdx(ExprNode testPropExpr, ExprNode[] setProps) {
        items.add(new QueryGraphValueDesc(setProps, new QueryGraphValueEntryInKeywordMultiIdx(testPropExpr)));
    }

    public void addInKeywordSingleIdx(ExprNode testPropIdent, ExprNode[] setPropExpr) {
        ExprNode[] indexExpressions = new ExprNode[]{testPropIdent};
        QueryGraphValueDesc found = findEntry(indexExpressions);

        ExprNode[] setExpressions = setPropExpr;
        if (found != null && found.getEntry() instanceof QueryGraphValueEntryInKeywordSingleIdx) {
            QueryGraphValueEntryInKeywordSingleIdx existing = (QueryGraphValueEntryInKeywordSingleIdx) found.getEntry();
            setExpressions = (ExprNode[]) CollectionUtil.addArrays(existing.getKeyExprs(), setPropExpr);
            items.remove(found);
        }
        items.add(new QueryGraphValueDesc(new ExprNode[]{testPropIdent}, new QueryGraphValueEntryInKeywordSingleIdx(setExpressions)));
    }

    public QueryGraphValuePairInKWSingleIdx getInKeywordSingles() {

        List<String> indexedProps = new ArrayList<String>();
        List<QueryGraphValueEntryInKeywordSingleIdx> single = new ArrayList<QueryGraphValueEntryInKeywordSingleIdx>();
        for (QueryGraphValueDesc desc : items) {
            if (desc.getEntry() instanceof QueryGraphValueEntryInKeywordSingleIdx) {
                QueryGraphValueEntryInKeywordSingleIdx keyprop = (QueryGraphValueEntryInKeywordSingleIdx) desc.getEntry();
                single.add(keyprop);
                indexedProps.add(getSingleIdentNodeProp(desc.getIndexExprs()));
            }
        }
        return new QueryGraphValuePairInKWSingleIdx(indexedProps.toArray(new String[indexedProps.size()]), single);
    }

    public List<QueryGraphValuePairInKWMultiIdx> getInKeywordMulti() {
        List<QueryGraphValuePairInKWMultiIdx> multi = new ArrayList<QueryGraphValuePairInKWMultiIdx>();
        for (QueryGraphValueDesc desc : items) {
            if (desc.getEntry() instanceof QueryGraphValueEntryInKeywordMultiIdx) {
                QueryGraphValueEntryInKeywordMultiIdx keyprop = (QueryGraphValueEntryInKeywordMultiIdx) desc.getEntry();
                multi.add(new QueryGraphValuePairInKWMultiIdx(desc.getIndexExprs(), keyprop));
            }
        }
        return multi;
    }

    public void addCustom(ExprNode[] indexExpressions, String operationName, int expressionPosition, ExprNode expression) {

        // find existing custom-entry for same index expressions
        QueryGraphValueEntryCustom found = null;
        for (QueryGraphValueDesc desc : items) {
            if (desc.getEntry() instanceof QueryGraphValueEntryCustom) {
                if (ExprNodeUtilityCore.deepEquals(desc.getIndexExprs(), indexExpressions, true)) {
                    found = (QueryGraphValueEntryCustom) desc.getEntry();
                    break;
                }
            }
        }
        if (found == null) {
            found = new QueryGraphValueEntryCustom();
            items.add(new QueryGraphValueDesc(indexExpressions, found));
        }

        // find/create operation against the indexed fields
        QueryGraphValueEntryCustomKey key = new QueryGraphValueEntryCustomKey(operationName, indexExpressions);
        QueryGraphValueEntryCustomOperation op = found.getOperations().get(key);
        if (op == null) {
            op = new QueryGraphValueEntryCustomOperation();
            found.getOperations().put(key, op);
        }
        op.getPositionalExpressions().put(expressionPosition, expression);
    }

    private QueryGraphValueDesc findIdentEntry(ExprIdentNode search) {
        for (QueryGraphValueDesc desc : items) {
            if (desc.getIndexExprs().length > 1 || !(desc.getIndexExprs()[0] instanceof ExprIdentNode)) {
                continue;
            }
            ExprIdentNode other = (ExprIdentNode) desc.getIndexExprs()[0];
            if (search.getResolvedPropertyName().equals(other.getResolvedPropertyName())) {
                return desc;
            }
        }
        return null;
    }

    private QueryGraphValueDesc findEntry(ExprNode[] search) {
        for (QueryGraphValueDesc desc : items) {
            if (ExprNodeUtilityCore.deepEquals(search, desc.getIndexExprs(), true)) {
                return desc;
            }
        }
        return null;
    }

    private String getSingleIdentNodeProp(ExprNode[] indexExprs) {
        if (indexExprs.length != 1) {
            throw new IllegalStateException("Incorrect number of index expressions");
        }
        ExprIdentNode identNode = (ExprIdentNode) indexExprs[0];
        return identNode.getResolvedPropertyName();
    }
}

