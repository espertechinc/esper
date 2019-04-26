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
package com.espertech.esper.common.internal.epl.expression.funcs;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInSingleRowFunction;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotStaticMethodWrap;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotStaticMethodWrapFactory;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotForge;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNodeFilterAnalyzerInputStatic;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNodeForgeStaticMethod;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNodeUtility;
import com.espertech.esper.common.internal.epl.expression.visitor.*;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.settings.ClasspathImportSingleRowDesc;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an invocation of a plug-in single-row function  in the expression tree.
 */
public class ExprPlugInSingleRowNode extends ExprNodeBase implements ExprFilterOptimizableNode, ExprNodeInnerNodeProvider {

    private final String functionName;
    private final Class clazz;
    private final List<ExprChainedSpec> chainSpec;
    private final ClasspathImportSingleRowDesc config;

    private ExprPlugInSingleRowNodeForge forge;
    private transient StatementCompileTimeServices compileTimeServices;
    private transient StatementRawInfo statementRawInfo;

    public ExprPlugInSingleRowNode(String functionName, Class clazz, List<ExprChainedSpec> chainSpec, ClasspathImportSingleRowDesc config) {
        this.functionName = functionName;
        this.clazz = clazz;
        this.chainSpec = chainSpec;
        this.config = config;
    }

    public ExprEvaluator getExprEvaluator() {
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    public ExprForge getForge() {
        checkValidated(forge);
        return forge;
    }

    public List<ExprChainedSpec> getChainSpec() {
        return chainSpec;
    }

    public String getFunctionName() {
        return functionName;
    }

    public boolean getFilterLookupEligible() {
        boolean eligible = !forge.isReturnsConstantResult();
        if (eligible) {
            eligible = chainSpec.size() == 1;
        }
        if (eligible) {
            eligible = config.getFilterOptimizable() == ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable.ENABLED;
        }
        if (eligible) {
            // We disallow context properties in a filter-optimizable expression if they are passed in since
            // the evaluation is context-free and shared.
            ExprNodeContextPropertiesVisitor visitor = new ExprNodeContextPropertiesVisitor();
            ExprNodeUtilityQuery.acceptChain(visitor, chainSpec);
            eligible = !visitor.isFound();
        }
        if (eligible) {
            ExprNodeStreamRequiredVisitor visitor = new ExprNodeStreamRequiredVisitor();
            ExprNodeUtilityQuery.acceptChain(visitor, chainSpec);
            for (int stream : visitor.getStreamsRequired()) {
                if (stream != 0) {
                    eligible = false;
                }
            }
        }
        if (eligible) {
            ExprNodeSubselectDeclaredDotVisitor visitor = new ExprNodeSubselectDeclaredDotVisitor();
            ExprNodeUtilityQuery.acceptChain(visitor, chainSpec);
            if (!visitor.getSubselects().isEmpty()) {
                eligible = false;
            }
        }
        if (eligible) {
            if (forge.isHasMethodInvocationContextParam()) {
                eligible = false;
            }
        }
        return eligible;
    }

    public ExprFilterSpecLookupableForge getFilterLookupable() {
        checkValidated(forge);
        DataInputOutputSerdeForge filterSerde = compileTimeServices.getSerdeResolver().serdeForFilter(forge.getEvaluationType(), statementRawInfo);
        return new ExprFilterSpecLookupableForge(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(this), forge, forge.getEvaluationType(), true, filterSerde);
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        ExprNodeUtilityPrint.toExpressionString(chainSpec, writer, false, functionName);
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprPlugInSingleRowNode)) {
            return false;
        }

        ExprPlugInSingleRowNode other = (ExprPlugInSingleRowNode) node;
        if (other.chainSpec.size() != this.chainSpec.size()) {
            return false;
        }
        for (int i = 0; i < chainSpec.size(); i++) {
            if (!(this.chainSpec.get(i).equals(other.chainSpec.get(i)))) {
                return false;
            }
        }
        return other.clazz == this.clazz && other.functionName.endsWith(this.functionName);
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        this.compileTimeServices = validationContext.getStatementCompileTimeService();
        this.statementRawInfo = validationContext.getStatementRawInfo();

        ExprNodeUtilityValidate.validate(ExprNodeOrigin.PLUGINSINGLEROWPARAM, chainSpec, validationContext);

        // get first chain item
        List<ExprChainedSpec> chainList = new ArrayList<ExprChainedSpec>(chainSpec);
        ExprChainedSpec firstItem = chainList.remove(0);

        // Get the types of the parameters for the first invocation
        boolean allowWildcard = validationContext.getStreamTypeService().getEventTypes().length == 1;
        EventType streamZeroType = null;
        if (validationContext.getStreamTypeService().getEventTypes().length > 0) {
            streamZeroType = validationContext.getStreamTypeService().getEventTypes()[0];
        }
        final ExprNodeUtilMethodDesc staticMethodDesc = ExprNodeUtilityResolve.resolveMethodAllowWildcardAndStream(clazz.getName(), null, firstItem.getName(), firstItem.getParameters(), allowWildcard, streamZeroType, new ExprNodeUtilResolveExceptionHandlerDefault(firstItem.getName(), true), functionName, validationContext.getStatementRawInfo(), validationContext.getStatementCompileTimeService());

        boolean allowValueCache = true;
        boolean isReturnsConstantResult;
        if (config.getValueCache() == ConfigurationCompilerPlugInSingleRowFunction.ValueCache.DISABLED) {
            isReturnsConstantResult = false;
            allowValueCache = false;
        } else if (config.getValueCache() == ConfigurationCompilerPlugInSingleRowFunction.ValueCache.CONFIGURED) {
            boolean isUDFCache = validationContext.getStatementCompileTimeService().getConfiguration().getCompiler().getExpression().isUdfCache();
            isReturnsConstantResult = isUDFCache && staticMethodDesc.isAllConstants() && chainList.isEmpty();
            allowValueCache = isUDFCache;
        } else if (config.getValueCache() == ConfigurationCompilerPlugInSingleRowFunction.ValueCache.ENABLED) {
            isReturnsConstantResult = staticMethodDesc.isAllConstants() && chainList.isEmpty();
        } else {
            throw new IllegalStateException("Invalid value cache code " + config.getValueCache());
        }

        // this may return a pair of null if there is no lambda or the result cannot be wrapped for lambda-function use
        ExprDotStaticMethodWrap optionalLambdaWrap = ExprDotStaticMethodWrapFactory.make(staticMethodDesc.getReflectionMethod(), chainList, config.getOptionalEventTypeName(), validationContext);
        EPType typeInfo = optionalLambdaWrap != null ? optionalLambdaWrap.getTypeInfo() : EPTypeHelper.singleValue(staticMethodDesc.getReflectionMethod().getReturnType());

        ExprDotForge[] eval = ExprDotNodeUtility.getChainEvaluators(-1, typeInfo, chainList, validationContext, false, new ExprDotNodeFilterAnalyzerInputStatic()).getChainWithUnpack();
        ExprDotNodeForgeStaticMethod staticMethodForge = new ExprDotNodeForgeStaticMethod(this, isReturnsConstantResult, clazz.getName(), staticMethodDesc.getReflectionMethod(), staticMethodDesc.getChildForges(), allowValueCache && staticMethodDesc.isAllConstants(), eval, optionalLambdaWrap, config.isRethrowExceptions(), null, validationContext.getStatementName());

        // If caching the result, evaluate now and return the result.
        if (isReturnsConstantResult) {
            forge = new ExprPlugInSingleRowNodeForgeConst(this, staticMethodForge);
        } else {
            forge = new ExprPlugInSingleRowNodeForgeNC(this, staticMethodForge);
        }

        return null;
    }

    @Override
    public void accept(ExprNodeVisitor visitor) {
        super.accept(visitor);
        ExprNodeUtilityQuery.acceptChain(visitor, chainSpec);
    }

    @Override
    public void accept(ExprNodeVisitorWithParent visitor) {
        super.accept(visitor);
        ExprNodeUtilityQuery.acceptChain(visitor, chainSpec, this);
    }

    @Override
    public void acceptChildnodes(ExprNodeVisitorWithParent visitor, ExprNode parent) {
        super.acceptChildnodes(visitor, parent);
        ExprNodeUtilityQuery.acceptChain(visitor, chainSpec, this);
    }

    @Override
    public void replaceUnlistedChildNode(ExprNode nodeToReplace, ExprNode newNode) {
        ExprNodeUtilityModify.replaceChainChildNode(nodeToReplace, newNode, chainSpec);
    }

    public List<ExprNode> getAdditionalNodes() {
        return ExprNodeUtilityQuery.collectChainParameters(chainSpec);
    }
}
