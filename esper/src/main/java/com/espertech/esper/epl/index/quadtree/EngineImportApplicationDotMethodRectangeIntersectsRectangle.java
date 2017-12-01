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
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.dot.ExprDotNodeImpl;
import com.espertech.esper.epl.util.EPLExpressionParamType;
import com.espertech.esper.epl.util.EPLValidationUtil;
import com.espertech.esper.spatial.quadtree.core.BoundingBox;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class EngineImportApplicationDotMethodRectangeIntersectsRectangle extends EngineImportApplicationDotMethodBase {
    protected final static String LOOKUP_OPERATION_NAME = "rectangle.intersects(rectangle)";
    public final static String INDEXTYPE_NAME = "mxcifquadtree";

    public EngineImportApplicationDotMethodRectangeIntersectsRectangle(ExprDotNodeImpl parent, String lhsName, ExprNode[] lhs, String dotMethodName, String rhsName, ExprNode[] rhs, ExprNode[] indexNamedParameter) {
        super(parent, lhsName, lhs, dotMethodName, rhsName, rhs, indexNamedParameter);
    }

    protected ExprForge validateAll(String lhsName, ExprNode[] lhs, String rhsName, ExprNode[] rhs, ExprValidationContext validationContext) throws ExprValidationException {
        EPLValidationUtil.validateParameterNumber(lhsName, LHS_VALIDATION_NAME, false, 4, lhs.length);
        EPLValidationUtil.validateParametersTypePredefined(lhs, lhsName, LHS_VALIDATION_NAME, EPLExpressionParamType.NUMERIC);

        EPLValidationUtil.validateParameterNumber(rhsName, RHS_VALIDATION_NAME, true, 4, rhs.length);
        EPLValidationUtil.validateParametersTypePredefined(rhs, rhsName, RHS_VALIDATION_NAME, EPLExpressionParamType.NUMERIC);

        ExprForge meXEval = lhs[0].getForge();
        ExprForge meYEval = lhs[1].getForge();
        ExprForge meWidthEval = lhs[2].getForge();
        ExprForge meHeightEval = lhs[3].getForge();

        ExprForge otherXEval = rhs[0].getForge();
        ExprForge otherYEval = rhs[1].getForge();
        ExprForge otherWidthEval = rhs[2].getForge();
        ExprForge otherHeightEval = rhs[3].getForge();
        return new RectangleIntersectsRectangleForge(parent, meXEval, meYEval, meWidthEval, meHeightEval, otherXEval, otherYEval, otherWidthEval, otherHeightEval);
    }

    protected String operationName() {
        return LOOKUP_OPERATION_NAME;
    }

    protected String indexTypeName() {
        return INDEXTYPE_NAME;
    }

    public final static class RectangleIntersectsRectangleForge implements ExprForge {

        private final ExprDotNodeImpl parent;
        protected final ExprForge meXEval;
        protected final ExprForge meYEval;
        protected final ExprForge meWidthEval;
        protected final ExprForge meHeightEval;
        protected final ExprForge otherXEval;
        protected final ExprForge otherYEval;
        protected final ExprForge otherWidthEval;
        protected final ExprForge otherHeightEval;

        public RectangleIntersectsRectangleForge(ExprDotNodeImpl parent, ExprForge meXEval, ExprForge meYEval, ExprForge meWidthEval, ExprForge meHeightEval, ExprForge otherXEval, ExprForge otherYEval, ExprForge otherWidthEval, ExprForge otherHeightEval) {
            this.parent = parent;
            this.meXEval = meXEval;
            this.meYEval = meYEval;
            this.meWidthEval = meWidthEval;
            this.meHeightEval = meHeightEval;
            this.otherXEval = otherXEval;
            this.otherYEval = otherYEval;
            this.otherWidthEval = otherWidthEval;
            this.otherHeightEval = otherHeightEval;
        }

        public ExprEvaluator getExprEvaluator() {
            return new RectangleIntersectsRectangleEvaluator(meXEval.getExprEvaluator(), meYEval.getExprEvaluator(), meWidthEval.getExprEvaluator(), meHeightEval.getExprEvaluator(),
                    otherXEval.getExprEvaluator(), otherYEval.getExprEvaluator(), otherWidthEval.getExprEvaluator(), otherHeightEval.getExprEvaluator());
        }

        public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return RectangleIntersectsRectangleEvaluator.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
        }

        public ExprForgeComplexityEnum getComplexity() {
            return ExprForgeComplexityEnum.INTER;
        }

        public Class getEvaluationType() {
            return Boolean.class;
        }

        public ExprNodeRenderable getForgeRenderable() {
            return parent;
        }
    }

    public final static class RectangleIntersectsRectangleEvaluator implements ExprEvaluator {

        private final ExprEvaluator meXEval;
        private final ExprEvaluator meYEval;
        private final ExprEvaluator meWidthEval;
        private final ExprEvaluator meHeightEval;
        private final ExprEvaluator otherXEval;
        private final ExprEvaluator otherYEval;
        private final ExprEvaluator otherWidthEval;
        private final ExprEvaluator otherHeightEval;

        public RectangleIntersectsRectangleEvaluator(ExprEvaluator meXEval, ExprEvaluator meYEval, ExprEvaluator meWidthEval, ExprEvaluator meHeightEval, ExprEvaluator otherXEval, ExprEvaluator otherYEval, ExprEvaluator otherWidthEval, ExprEvaluator otherHeightEval) {
            this.meXEval = meXEval;
            this.meYEval = meYEval;
            this.meWidthEval = meWidthEval;
            this.meHeightEval = meHeightEval;
            this.otherXEval = otherXEval;
            this.otherYEval = otherYEval;
            this.otherWidthEval = otherWidthEval;
            this.otherHeightEval = otherHeightEval;
        }

        public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            Number meX = (Number) meXEval.evaluate(eventsPerStream, isNewData, context);
            if (meX == null) {
                return null;
            }
            Number meY = (Number) meYEval.evaluate(eventsPerStream, isNewData, context);
            if (meY == null) {
                return null;
            }
            Number meWidth = (Number) meWidthEval.evaluate(eventsPerStream, isNewData, context);
            if (meWidth == null) {
                return null;
            }
            Number meHeight = (Number) meHeightEval.evaluate(eventsPerStream, isNewData, context);
            if (meHeight == null) {
                return null;
            }
            Number otherX = (Number) otherXEval.evaluate(eventsPerStream, isNewData, context);
            if (otherX == null) {
                return null;
            }
            Number otherY = (Number) otherYEval.evaluate(eventsPerStream, isNewData, context);
            if (otherY == null) {
                return null;
            }
            Number otherWidth = (Number) otherWidthEval.evaluate(eventsPerStream, isNewData, context);
            if (otherWidth == null) {
                return null;
            }
            Number otherHeight = (Number) otherHeightEval.evaluate(eventsPerStream, isNewData, context);
            if (otherHeight == null) {
                return null;
            }

            double x = meX.doubleValue();
            double y = meY.doubleValue();
            double width = meWidth.doubleValue();
            double height = meHeight.doubleValue();
            return BoundingBox.intersectsBoxIncludingEnd(x, y, x + width, y + height, otherX.doubleValue(), otherY.doubleValue(), otherWidth.doubleValue(), otherHeight.doubleValue());
        }

        public static CodegenExpression codegen(RectangleIntersectsRectangleForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, RectangleIntersectsRectangleEvaluator.class, codegenClassScope);

            CodegenBlock block = methodNode.getBlock();
            CodegenLegoCast.asDoubleNullReturnNull(block, "meX", forge.meXEval, methodNode, exprSymbol, codegenClassScope);
            CodegenLegoCast.asDoubleNullReturnNull(block, "meY", forge.meYEval, methodNode, exprSymbol, codegenClassScope);
            CodegenLegoCast.asDoubleNullReturnNull(block, "meWidth", forge.meWidthEval, methodNode, exprSymbol, codegenClassScope);
            CodegenLegoCast.asDoubleNullReturnNull(block, "meHeight", forge.meHeightEval, methodNode, exprSymbol, codegenClassScope);
            CodegenLegoCast.asDoubleNullReturnNull(block, "otherX", forge.otherXEval, methodNode, exprSymbol, codegenClassScope);
            CodegenLegoCast.asDoubleNullReturnNull(block, "otherY", forge.otherYEval, methodNode, exprSymbol, codegenClassScope);
            CodegenLegoCast.asDoubleNullReturnNull(block, "otherWidth", forge.otherWidthEval, methodNode, exprSymbol, codegenClassScope);
            CodegenLegoCast.asDoubleNullReturnNull(block, "otherHeight", forge.otherHeightEval, methodNode, exprSymbol, codegenClassScope);
            block.methodReturn(staticMethod(BoundingBox.class, "intersectsBoxIncludingEnd", ref("meX"), ref("meY"), op(ref("meX"), "+", ref("meWidth")), op(ref("meY"), "+", ref("meHeight")),
                    ref("otherX"), ref("otherY"), ref("otherWidth"), ref("otherHeight")));
            return localMethod(methodNode);
        }

    }
}
