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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.core.EngineImportApplicationDotMethod;
import com.espertech.esper.epl.core.EngineImportException;
import com.espertech.esper.epl.declexpr.ExprDeclaredNode;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.dot.ExprDotNode;
import com.espertech.esper.epl.expression.visitor.ExprNodeIdentifierAndStreamRefVisitor;
import com.espertech.esper.epl.index.service.AdvancedIndexFactoryProvider;
import com.espertech.esper.epl.index.service.FilterExprAnalyzerAffectorIndexProvision;
import com.espertech.esper.epl.join.plan.FilterExprAnalyzerAffector;
import com.espertech.esper.epl.lookup.AdvancedIndexConfigContextPartition;
import com.espertech.esper.epl.util.EPLExpressionParamType;
import com.espertech.esper.epl.util.EPLValidationUtil;
import com.espertech.esper.filter.FilterSpecCompilerAdvIndexDesc;
import com.espertech.esper.spatial.quadtree.core.BoundingBox;
import com.espertech.esper.util.CollectionUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EngineImportApplicationDotMethodPointInsideRectange implements EngineImportApplicationDotMethod {
    public final static String LOOKUP_OPERATION_NAME = "point.inside(rectangle)";

    private final String lhsName;
    private final ExprNode[] lhs;
    private final String dotMethodName;
    private final String rhsName;
    private final ExprNode[] rhs;
    private final ExprNode[] indexNamedParameter;
    private String optionalIndexName;
    private AdvancedIndexConfigContextPartition optionalIndexConfig;

    private ExprEvaluator evaluator;

    public EngineImportApplicationDotMethodPointInsideRectange(String lhsName, ExprNode[] lhs, String dotMethodName, String rhsName, ExprNode[] rhs, ExprNode[] indexNamedParameter) {
        this.lhsName = lhsName;
        this.lhs = lhs;
        this.dotMethodName = dotMethodName;
        this.rhsName = rhsName;
        this.rhs = rhs;
        this.indexNamedParameter = indexNamedParameter;
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
        ExprNodeUtility.getValidatedSubtree(ExprNodeOrigin.DOTNODEPARAMETER, lhs, validationContext);
        ExprNodeUtility.getValidatedSubtree(ExprNodeOrigin.DOTNODEPARAMETER, rhs, validationContext);

        EPLValidationUtil.validateParameterNumber(lhsName, "left-hand-side", false, 2, lhs.length);
        for (int i = 0; i < lhs.length; i++) {
            ExprNode node = lhs[i];
            EPLValidationUtil.validateParameterType(lhsName, "left-hand-side", true, EPLExpressionParamType.NUMERIC, null, node.getExprEvaluator().getType(), i, node);
        }

        EPLValidationUtil.validateParameterNumber(rhsName, "right-hand-side", true, 4, rhs.length);
        for (int i = 0; i < rhs.length; i++) {
            ExprNode node = rhs[i];
            EPLValidationUtil.validateParameterType(rhsName, "right-hand-side", true, EPLExpressionParamType.NUMERIC, null, node.getExprEvaluator().getType(), i, node);
        }

        ExprEvaluator pxEval = lhs[0].getExprEvaluator();
        ExprEvaluator pyEval = lhs[1].getExprEvaluator();
        ExprEvaluator xEval = rhs[0].getExprEvaluator();
        ExprEvaluator yEval = rhs[1].getExprEvaluator();
        ExprEvaluator widthEval = rhs[2].getExprEvaluator();
        ExprEvaluator heightEval = rhs[3].getExprEvaluator();
        evaluator = new IntersectsEvaluator(pxEval, pyEval, xEval, yEval, widthEval, heightEval);

        if (indexNamedParameter != null) {
            validateIndexNamedParameter(validationContext);
        }
        return null;
    }

    public ExprEvaluator getExprEvaluator() {
        return evaluator;
    }

    public FilterExprAnalyzerAffector getFilterExprAnalyzerAffector() {
        ExprNodeIdentifierAndStreamRefVisitor visitor = new ExprNodeIdentifierAndStreamRefVisitor(false);
        lhs[0].accept(visitor);
        lhs[1].accept(visitor);
        Set<Integer> indexedPropertyStreams = new HashSet<>();
        for (ExprNodePropOrStreamDesc ref : visitor.getRefs()) {
            indexedPropertyStreams.add(ref.getStreamNum());
        }

        if (indexedPropertyStreams.size() == 0 || indexedPropertyStreams.size() > 1) {
            return null; // there are no properties from any streams that could be used for building an index, or the properties come from different disjoint streams
        }
        int streamNumIndex = indexedPropertyStreams.iterator().next();

        ExprNode[] indexExpressions = new ExprNode[] {lhs[0], lhs[1]};

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

        return new FilterExprAnalyzerAffectorIndexProvision(EngineImportApplicationDotMethodPointInsideRectange.LOOKUP_OPERATION_NAME, indexExpressions, keyExpressions, streamNumIndex);
    }

    public FilterSpecCompilerAdvIndexDesc getFilterSpecCompilerAdvIndexDesc() {
        if (indexNamedParameter == null) {
            return null;
        }
        return new FilterSpecCompilerAdvIndexDesc(lhs, rhs, optionalIndexConfig, optionalIndexName);
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

        optionalIndexConfig = provider.validateConfigureFilterIndex(optionalIndexName, indexTypeName, ExprNodeUtility.toArray(params), validationContext);
    }

    private ExprValidationException getIndexNameMessage(String message) {
        return new ExprValidationException("Named parameter '" + ExprDotNode.FILTERINDEX_NAMED_PARAMETER + "' " + message);
    }

    public final static class IntersectsEvaluator implements ExprEvaluator {
        private final ExprEvaluator pxEval;
        private final ExprEvaluator pyEval;
        private final ExprEvaluator xEval;
        private final ExprEvaluator yEval;
        private final ExprEvaluator widthEval;
        private final ExprEvaluator heightEval;

        IntersectsEvaluator(ExprEvaluator pxEval, ExprEvaluator pyEval, ExprEvaluator xEval, ExprEvaluator yEval, ExprEvaluator widthEval, ExprEvaluator heightEval) {
            this.pxEval = pxEval;
            this.pyEval = pyEval;
            this.xEval = xEval;
            this.yEval = yEval;
            this.widthEval = widthEval;
            this.heightEval = heightEval;
        }

        public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            Number px = (Number) pxEval.evaluate(eventsPerStream, isNewData, context);
            if (px == null) {
                return null;
            }
            Number py = (Number) pyEval.evaluate(eventsPerStream, isNewData, context);
            if (py == null) {
                return null;
            }
            Number x = (Number) xEval.evaluate(eventsPerStream, isNewData, context);
            if (x == null) {
                return null;
            }
            Number y = (Number) yEval.evaluate(eventsPerStream, isNewData, context);
            if (y == null) {
                return null;
            }
            Number width = (Number) widthEval.evaluate(eventsPerStream, isNewData, context);
            if (width == null) {
                return null;
            }
            Number height = (Number) heightEval.evaluate(eventsPerStream, isNewData, context);
            if (height == null) {
                return null;
            }
            return BoundingBox.containsPoint(x.doubleValue(), y.doubleValue(), width.doubleValue(), height.doubleValue(), px.doubleValue(), py.doubleValue());
        }

        public Class getType() {
            return Boolean.class;
        }
    }
}
