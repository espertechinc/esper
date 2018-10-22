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
package com.espertech.esper.common.internal.epl.join.querygraph;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprIdentNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCompare;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Property lists stored as a value for each stream-to-stream relationship, for use by {@link QueryGraphForge}.
 */
public class QueryGraphValueForge {
    private List<QueryGraphValueDescForge> items;

    /**
     * Ctor.
     */
    public QueryGraphValueForge() {
        items = new ArrayList<>();
    }

    public boolean isEmptyNotNavigable() {
        return items.isEmpty();
    }

    public List<QueryGraphValueDescForge> getItems() {
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
        QueryGraphValueDescForge value = findIdentEntry(indexPropertyIdent);
        if (value != null && value.getEntry() instanceof QueryGraphValueEntryHashKeyedForgeExpr) {
            // if this index property exists and is compared to a constant, ignore the index prop
            QueryGraphValueEntryHashKeyedForgeExpr expr = (QueryGraphValueEntryHashKeyedForgeExpr) value.getEntry();
            if (expr.isConstant()) {
                return false;
            }
        }
        if (value != null && value.getEntry() instanceof QueryGraphValueEntryHashKeyedForgeProp) {
            return false;   // second comparison, ignore
        }

        items.add(new QueryGraphValueDescForge(new ExprNode[]{indexPropertyIdent}, new QueryGraphValueEntryHashKeyedForgeProp(keyPropNode, keyProperty, keyPropNode.getExprEvaluatorIdent().getGetter())));
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

        items.add(new QueryGraphValueDescForge(new ExprNode[]{propertyValueIdent}, new QueryGraphValueEntryRangeInForge(rangeType, propertyStart, propertyEnd, true)));
    }

    public void addRelOp(ExprNode propertyKey, QueryGraphRangeEnum op, ExprIdentNode propertyValueIdent, boolean isBetweenOrIn) {

        // Note: Read as follows:
        // System.out.println("If I have an index on '" + propertyValue + "' I'm evaluating " + propertyKey + " and finding all values of " + propertyValue + " " + op + " then " + propertyKey);

        // Check if there is an opportunity to convert this to a range or remove an earlier specification
        QueryGraphValueDescForge existing = findIdentEntry(propertyValueIdent);
        if (existing == null) {
            items.add(new QueryGraphValueDescForge(new ExprNode[]{propertyValueIdent}, new QueryGraphValueEntryRangeRelOpForge(op, propertyKey, isBetweenOrIn)));
            return;
        }

        if (!(existing.getEntry() instanceof QueryGraphValueEntryRangeRelOpForge)) {
            return; // another comparison exists already, don't add range
        }

        QueryGraphValueEntryRangeRelOpForge relOp = (QueryGraphValueEntryRangeRelOpForge) existing.getEntry();
        QueryGraphRangeConsolidateDesc opsDesc = QueryGraphRangeUtil.getCanConsolidate(op, relOp.getType());
        if (opsDesc != null) {
            ExprNode start = !opsDesc.isReverse() ? relOp.getExpression() : propertyKey;
            ExprNode end = !opsDesc.isReverse() ? propertyKey : relOp.getExpression();
            items.remove(existing);
            addRange(opsDesc.getType(), start, end, propertyValueIdent);
        }
    }

    public void addUnkeyedExpr(ExprIdentNode indexedPropIdent, ExprNode exprNodeNoIdent) {
        items.add(new QueryGraphValueDescForge(new ExprNode[]{indexedPropIdent}, new QueryGraphValueEntryHashKeyedForgeExpr(exprNodeNoIdent, false)));
    }

    public void addKeyedExpr(ExprIdentNode indexedPropIdent, ExprNode exprNodeNoIdent) {
        items.add(new QueryGraphValueDescForge(new ExprNode[]{indexedPropIdent}, new QueryGraphValueEntryHashKeyedForgeExpr(exprNodeNoIdent, true)));
    }

    public QueryGraphValuePairHashKeyIndexForge getHashKeyProps() {
        List<QueryGraphValueEntryHashKeyedForge> keys = new ArrayList<QueryGraphValueEntryHashKeyedForge>();
        Deque<String> indexed = new ArrayDeque<String>();
        for (QueryGraphValueDescForge desc : items) {
            if (desc.getEntry() instanceof QueryGraphValueEntryHashKeyedForge) {
                QueryGraphValueEntryHashKeyedForge keyprop = (QueryGraphValueEntryHashKeyedForge) desc.getEntry();
                keys.add(keyprop);
                indexed.add(getSingleIdentNodeProp(desc.getIndexExprs()));
            }
        }

        String[] strictKeys = new String[indexed.size()];
        int count = 0;
        for (QueryGraphValueDescForge desc : items) {
            if (desc.getEntry() instanceof QueryGraphValueEntryHashKeyedForge) {
                if (desc.getEntry() instanceof QueryGraphValueEntryHashKeyedForgeProp) {
                    QueryGraphValueEntryHashKeyedForgeProp keyprop = (QueryGraphValueEntryHashKeyedForgeProp) desc.getEntry();
                    strictKeys[count] = keyprop.getKeyProperty();
                }
                count++;
            }
        }

        return new QueryGraphValuePairHashKeyIndexForge(indexed.toArray(new String[indexed.size()]), keys, strictKeys);
    }

    public QueryGraphValuePairRangeIndexForge getRangeProps() {
        Deque<String> indexed = new ArrayDeque<String>();
        List<QueryGraphValueEntryRangeForge> keys = new ArrayList<QueryGraphValueEntryRangeForge>();
        for (QueryGraphValueDescForge desc : items) {
            if (desc.getEntry() instanceof QueryGraphValueEntryRangeForge) {
                QueryGraphValueEntryRangeForge keyprop = (QueryGraphValueEntryRangeForge) desc.getEntry();
                keys.add(keyprop);
                indexed.add(getSingleIdentNodeProp(desc.getIndexExprs()));
            }
        }
        return new QueryGraphValuePairRangeIndexForge(indexed.toArray(new String[indexed.size()]), keys);
    }

    public String toString() {
        StringWriter writer = new StringWriter();
        writer.append("QueryGraphValue ");
        String delimiter = "";
        for (QueryGraphValueDescForge desc : items) {
            writer.append(delimiter);
            writer.append(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceAsList(desc.getIndexExprs()));
            writer.append(": ");
            writer.append(desc.getEntry().toString());
            delimiter = ", ";
        }
        return writer.toString();
    }

    public void addInKeywordMultiIdx(ExprNode testPropExpr, ExprNode[] setProps) {
        items.add(new QueryGraphValueDescForge(setProps, new QueryGraphValueEntryInKeywordMultiIdxForge(testPropExpr)));
    }

    public void addInKeywordSingleIdx(ExprNode testPropIdent, ExprNode[] setPropExpr) {
        ExprNode[] indexExpressions = new ExprNode[]{testPropIdent};
        QueryGraphValueDescForge found = findEntry(indexExpressions);

        ExprNode[] setExpressions = setPropExpr;
        if (found != null && found.getEntry() instanceof QueryGraphValueEntryInKeywordSingleIdxForge) {
            QueryGraphValueEntryInKeywordSingleIdxForge existing = (QueryGraphValueEntryInKeywordSingleIdxForge) found.getEntry();
            setExpressions = (ExprNode[]) CollectionUtil.addArrays(existing.getKeyExprs(), setPropExpr);
            items.remove(found);
        }
        items.add(new QueryGraphValueDescForge(new ExprNode[]{testPropIdent}, new QueryGraphValueEntryInKeywordSingleIdxForge(setExpressions)));
    }

    public QueryGraphValuePairInKWSingleIdxForge getInKeywordSingles() {
        List<String> indexedProps = new ArrayList<String>();
        List<QueryGraphValueEntryInKeywordSingleIdxForge> single = new ArrayList<QueryGraphValueEntryInKeywordSingleIdxForge>();
        for (QueryGraphValueDescForge desc : items) {
            if (desc.getEntry() instanceof QueryGraphValueEntryInKeywordSingleIdxForge) {
                QueryGraphValueEntryInKeywordSingleIdxForge keyprop = (QueryGraphValueEntryInKeywordSingleIdxForge) desc.getEntry();
                single.add(keyprop);
                indexedProps.add(getSingleIdentNodeProp(desc.getIndexExprs()));
            }
        }
        return new QueryGraphValuePairInKWSingleIdxForge(indexedProps.toArray(new String[indexedProps.size()]), single);
    }

    public List<QueryGraphValuePairInKWMultiIdx> getInKeywordMulti() {
        List<QueryGraphValuePairInKWMultiIdx> multi = new ArrayList<QueryGraphValuePairInKWMultiIdx>();
        for (QueryGraphValueDescForge desc : items) {
            if (desc.getEntry() instanceof QueryGraphValueEntryInKeywordMultiIdxForge) {
                QueryGraphValueEntryInKeywordMultiIdxForge keyprop = (QueryGraphValueEntryInKeywordMultiIdxForge) desc.getEntry();
                multi.add(new QueryGraphValuePairInKWMultiIdx(desc.getIndexExprs(), keyprop));
            }
        }
        return multi;
    }

    public void addCustom(ExprNode[] indexExpressions, String operationName, int expressionPosition, ExprNode expression) {

        // find existing custom-entry for same index expressions
        QueryGraphValueEntryCustomForge found = null;
        for (QueryGraphValueDescForge desc : items) {
            if (desc.getEntry() instanceof QueryGraphValueEntryCustomForge) {
                if (ExprNodeUtilityCompare.deepEquals(desc.getIndexExprs(), indexExpressions, true)) {
                    found = (QueryGraphValueEntryCustomForge) desc.getEntry();
                    break;
                }
            }
        }
        if (found == null) {
            found = new QueryGraphValueEntryCustomForge();
            items.add(new QueryGraphValueDescForge(indexExpressions, found));
        }

        // find/create operation against the indexed fields
        QueryGraphValueEntryCustomKeyForge key = new QueryGraphValueEntryCustomKeyForge(operationName, indexExpressions);
        QueryGraphValueEntryCustomOperationForge op = found.getOperations().get(key);
        if (op == null) {
            op = new QueryGraphValueEntryCustomOperationForge();
            found.getOperations().put(key, op);
        }
        op.getPositionalExpressions().put(expressionPosition, expression);
    }

    private QueryGraphValueDescForge findIdentEntry(ExprIdentNode search) {
        for (QueryGraphValueDescForge desc : items) {
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

    private QueryGraphValueDescForge findEntry(ExprNode[] search) {
        for (QueryGraphValueDescForge desc : items) {
            if (ExprNodeUtilityCompare.deepEquals(search, desc.getIndexExprs(), true)) {
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

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(QueryGraphValue.class, this.getClass(), classScope);
        method.getBlock().declareVar(List.class, "items", newInstance(ArrayList.class, constant(items.size())));
        for (int i = 0; i < items.size(); i++) {
            method.getBlock().exprDotMethod(ref("items"), "add", items.get(i).make(method, symbols, classScope));
        }
        method.getBlock().methodReturn(newInstance(QueryGraphValue.class, ref("items")));
        return localMethod(method);
    }
}

