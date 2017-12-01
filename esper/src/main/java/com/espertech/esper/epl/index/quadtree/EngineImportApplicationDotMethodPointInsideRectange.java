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

public class EngineImportApplicationDotMethodPointInsideRectange extends EngineImportApplicationDotMethodBase {
    protected final static String LOOKUP_OPERATION_NAME = "point.inside(rectangle)";
    public final static String INDEXTYPE_NAME = "pointregionquadtree";

    public EngineImportApplicationDotMethodPointInsideRectange(ExprDotNodeImpl parent, String lhsName, ExprNode[] lhs, String dotMethodName, String rhsName, ExprNode[] rhs, ExprNode[] indexNamedParameter) {
        super(parent, lhsName, lhs, dotMethodName, rhsName, rhs, indexNamedParameter);
    }

    protected ExprForge validateAll(String lhsName, ExprNode[] lhs, String rhsName, ExprNode[] rhs, ExprValidationContext validationContext) throws ExprValidationException {
        EPLValidationUtil.validateParameterNumber(lhsName, LHS_VALIDATION_NAME, false, 2, lhs.length);
        EPLValidationUtil.validateParametersTypePredefined(lhs, lhsName, LHS_VALIDATION_NAME, EPLExpressionParamType.NUMERIC);

        EPLValidationUtil.validateParameterNumber(rhsName, RHS_VALIDATION_NAME, true, 4, rhs.length);
        EPLValidationUtil.validateParametersTypePredefined(rhs, rhsName, RHS_VALIDATION_NAME, EPLExpressionParamType.NUMERIC);

        ExprForge pxEval = lhs[0].getForge();
        ExprForge pyEval = lhs[1].getForge();
        ExprForge xEval = rhs[0].getForge();
        ExprForge yEval = rhs[1].getForge();
        ExprForge widthEval = rhs[2].getForge();
        ExprForge heightEval = rhs[3].getForge();
        return new PointIntersectsRectangleForge(parent, pxEval, pyEval, xEval, yEval, widthEval, heightEval);
    }

    protected String operationName() {
        return LOOKUP_OPERATION_NAME;
    }

    protected String indexTypeName() {
        return INDEXTYPE_NAME;
    }

    public final static class PointIntersectsRectangleForge implements ExprForge {

        private final ExprDotNodeImpl parent;
        protected final ExprForge pxEval;
        protected final ExprForge pyEval;
        protected final ExprForge xEval;
        protected final ExprForge yEval;
        protected final ExprForge widthEval;
        protected final ExprForge heightEval;

        public PointIntersectsRectangleForge(ExprDotNodeImpl parent, ExprForge pxEval, ExprForge pyEval, ExprForge xEval, ExprForge yEval, ExprForge widthEval, ExprForge heightEval) {
            this.parent = parent;
            this.pxEval = pxEval;
            this.pyEval = pyEval;
            this.xEval = xEval;
            this.yEval = yEval;
            this.widthEval = widthEval;
            this.heightEval = heightEval;
        }

        public ExprEvaluator getExprEvaluator() {
            return new PointIntersectsRectangleEvaluator(pxEval.getExprEvaluator(), pyEval.getExprEvaluator(), xEval.getExprEvaluator(), yEval.getExprEvaluator(), widthEval.getExprEvaluator(), heightEval.getExprEvaluator());
        }

        public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return PointIntersectsRectangleEvaluator.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
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

    public final static class PointIntersectsRectangleEvaluator implements ExprEvaluator {
        private final ExprEvaluator pxEval;
        private final ExprEvaluator pyEval;
        private final ExprEvaluator xEval;
        private final ExprEvaluator yEval;
        private final ExprEvaluator widthEval;
        private final ExprEvaluator heightEval;

        PointIntersectsRectangleEvaluator(ExprEvaluator pxEval, ExprEvaluator pyEval, ExprEvaluator xEval, ExprEvaluator yEval, ExprEvaluator widthEval, ExprEvaluator heightEval) {
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

        public static CodegenExpression codegen(PointIntersectsRectangleForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, EngineImportApplicationDotMethodRectangeIntersectsRectangle.RectangleIntersectsRectangleEvaluator.class, codegenClassScope);

            CodegenBlock block = methodNode.getBlock();
            CodegenLegoCast.asDoubleNullReturnNull(block, "px", forge.pxEval, methodNode, exprSymbol, codegenClassScope);
            CodegenLegoCast.asDoubleNullReturnNull(block, "py", forge.pyEval, methodNode, exprSymbol, codegenClassScope);
            CodegenLegoCast.asDoubleNullReturnNull(block, "x", forge.xEval, methodNode, exprSymbol, codegenClassScope);
            CodegenLegoCast.asDoubleNullReturnNull(block, "y", forge.yEval, methodNode, exprSymbol, codegenClassScope);
            CodegenLegoCast.asDoubleNullReturnNull(block, "width", forge.widthEval, methodNode, exprSymbol, codegenClassScope);
            CodegenLegoCast.asDoubleNullReturnNull(block, "height", forge.heightEval, methodNode, exprSymbol, codegenClassScope);
            block.methodReturn(staticMethod(BoundingBox.class, "containsPoint", ref("x"), ref("y"), ref("width"), ref("height"), ref("px"), ref("py")));
            return localMethod(methodNode);
        }

    }
}
