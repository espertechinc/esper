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
package com.espertech.esper.epl.index.quadtree;

import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.core.engineimport.EngineImportApplicationDotMethod;
import com.espertech.esper.epl.core.engineimport.EngineImportException;
import com.espertech.esper.epl.declexpr.ExprDeclaredNode;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.dot.ExprDotNode;
import com.espertech.esper.epl.expression.dot.ExprDotNodeImpl;
import com.espertech.esper.epl.expression.visitor.ExprNodeIdentifierAndStreamRefVisitor;
import com.espertech.esper.epl.index.service.AdvancedIndexFactoryProvider;
import com.espertech.esper.epl.index.service.FilterExprAnalyzerAffectorIndexProvision;
import com.espertech.esper.epl.join.plan.FilterExprAnalyzerAffector;
import com.espertech.esper.epl.lookup.AdvancedIndexConfigContextPartition;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.filter.FilterSpecCompilerAdvIndexDesc;
import com.espertech.esper.util.CollectionUtil;

import java.util.*;

public abstract class EngineImportApplicationDotMethodBase implements EngineImportApplicationDotMethod {
    protected final static String LHS_VALIDATION_NAME = "left-hand-side";
    protected final static String RHS_VALIDATION_NAME = "right-hand-side";

    protected final ExprDotNodeImpl parent;
    private final String lhsName;
    private final ExprNode[] lhs;
    private final String dotMethodName;
    private final String rhsName;
    private final ExprNode[] rhs;
    private final ExprNode[] indexNamedParameter;
    private String optionalIndexName;
    private AdvancedIndexConfigContextPartition optionalIndexConfig;

    private transient ExprForge forge;

    protected abstract ExprForge validateAll(String lhsName, ExprNode[] lhs, String rhsName, ExprNode[] rhs, ExprValidationContext validationContext) throws ExprValidationException;
    protected abstract String indexTypeName();
    protected abstract String operationName();

    public EngineImportApplicationDotMethodBase(ExprDotNodeImpl parent, String lhsName, ExprNode[] lhs, String dotMethodName, String rhsName, ExprNode[] rhs, ExprNode[] indexNamedParameter) {
        this.parent = parent;
        this.lhsName = lhsName;
        this.lhs = lhs;
        this.dotMethodName = dotMethodName;
        this.rhsName = rhsName;
        this.rhs = rhs;
        this.indexNamedParameter = indexNamedParameter;
    }

    public ExprForge getForge() {
        return forge;
    }

    public String getLhsName() {
        return lhsName;
    }

    public ExprNode[] getLhs() {
        return lhs;
    }

    public String getDotMethodName() {
        return dotMethodName;
    }

    public String getRhsName() {
        return rhsName;
    }

    public ExprNode[] getRhs() {
        return rhs;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.DOTNODEPARAMETER, lhs, validationContext);
        ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.DOTNODEPARAMETER, rhs, validationContext);

        forge = validateAll(lhsName, lhs, rhsName, rhs, validationContext);

        if (indexNamedParameter != null) {
            validateIndexNamedParameter(validationContext);
        }
        return null;
    }


    public FilterExprAnalyzerAffector getFilterExprAnalyzerAffector() {
        ExprNodeIdentifierAndStreamRefVisitor visitor = new ExprNodeIdentifierAndStreamRefVisitor(false);
        for (ExprNode lhsNode : lhs) {
            lhsNode.accept(visitor);
        }
        Set<Integer> indexedPropertyStreams = new HashSet<>();
        for (ExprNodePropOrStreamDesc ref : visitor.getRefs()) {
            indexedPropertyStreams.add(ref.getStreamNum());
        }

        if (indexedPropertyStreams.size() == 0 || indexedPropertyStreams.size() > 1) {
            return null; // there are no properties from any streams that could be used for building an index, or the properties come from different disjoint streams
        }
        int streamNumIndex = indexedPropertyStreams.iterator().next();

        List<Pair<ExprNode, int[]>> keyExpressions = new ArrayList<>();
        Set<Integer> dependencies = new HashSet<>();
        for (ExprNode node : rhs) {
            visitor.reset();
            dependencies.clear();
            node.accept(visitor);
            for (ExprNodePropOrStreamDesc ref : visitor.getRefs()) {
                dependencies.add(ref.getStreamNum());
            }
            if (dependencies.contains(streamNumIndex)) {
                return null;
            }
            Pair<ExprNode, int[]> pair = new Pair<>(node, CollectionUtil.intArray(dependencies));
            keyExpressions.add(pair);
        }

        return new FilterExprAnalyzerAffectorIndexProvision(operationName(), lhs, keyExpressions, streamNumIndex);
    }

    public FilterSpecCompilerAdvIndexDesc getFilterSpecCompilerAdvIndexDesc() {
        if (indexNamedParameter == null) {
            return null;
        }
        return new FilterSpecCompilerAdvIndexDesc(lhs, rhs, optionalIndexConfig, indexTypeName(), optionalIndexName);
    }

    private void validateIndexNamedParameter(ExprValidationContext validationContext) throws ExprValidationException {
        if (indexNamedParameter.length != 1 || !(indexNamedParameter[0] instanceof ExprDeclaredNode)) {
            throw getIndexNameMessage("requires an expression name");
        }
        ExprDeclaredNode node = (ExprDeclaredNode) indexNamedParameter[0];
        if (!(node.getBody() instanceof ExprDotNode)) {
            throw getIndexNameMessage("requires an index expression");
        }
        ExprDotNode dotNode = (ExprDotNode) node.getBody();
        if (dotNode.getChainSpec().size() > 1) {
            throw getIndexNameMessage("invalid chained index expression");
        }
        List<ExprNode> params = dotNode.getChainSpec().get(0).getParameters();
        String indexTypeName = dotNode.getChainSpec().get(0).getName();
        optionalIndexName = node.getPrototype().getName();

        AdvancedIndexFactoryProvider provider;
        try {
            provider = validationContext.getEngineImportService().resolveAdvancedIndexProvider(indexTypeName);
        } catch (EngineImportException e) {
            throw new ExprValidationException(e.getMessage(), e);
        }

        if (!indexTypeName.toLowerCase(Locale.ENGLISH).equals(indexTypeName())) {
            throw new ExprValidationException("Invalid index type '" + indexTypeName + "', expected '" + indexTypeName() + "'");
        }

        optionalIndexConfig = provider.validateConfigureFilterIndex(optionalIndexName, indexTypeName, ExprNodeUtilityCore.toArray(params), validationContext);
    }

    private ExprValidationException getIndexNameMessage(String message) {
        return new ExprValidationException("Named parameter '" + ExprDotNode.FILTERINDEX_NAMED_PARAMETER + "' " + message);
    }
}
