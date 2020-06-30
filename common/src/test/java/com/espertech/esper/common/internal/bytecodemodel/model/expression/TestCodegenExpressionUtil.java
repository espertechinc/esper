package com.espertech.esper.common.internal.bytecodemodel.model.expression;

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportEnum;
import junit.framework.TestCase;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionUtil.canRenderConstant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionUtil.renderConstant;

public class TestCodegenExpressionUtil extends TestCase {
    public void testRenderConstant() {
        tryRender("a", "\"a\"");
        tryRender("a\"a", "\"a\\\"a\"");
        StringBuilder builder = new StringBuilder();
        builder.append("\"a\"");
        tryRender(builder, "\\\"a\\\"");
        tryRender('x', "'x'");
        tryRender('\'', "'\\''");
        tryRender('\\', "'\\\\'");
        tryRender(1f, "1.0F");
        tryRender((short) 1, "(short) 1");
        tryRender((byte) 1, "(byte)1");
        tryRender(new int[0], "new int[]{}");
        tryRender(new Integer[0], "new java.lang.Integer[]{}");
        tryRender(new int[] {1}, "new int[] {1}");
        tryRender(new Integer[] {1, null}, "new java.lang.Integer[] {1,null}");
        tryRender(SupportEnum.ENUM_VALUE_1, SupportEnum.class.getName() + ".ENUM_VALUE_1");
        tryRender(SupportBean.class, SupportBean.class.getName() + ".class");
        tryRender(new BigInteger("10"), "new java.math.BigInteger(new byte[] {(byte)10})");
        tryRender(new BigDecimal("10.5"), "new BigDecimal(new java.math.BigInteger(new byte[] {(byte)105}),1)");
        
        assertFalse(canRenderConstant(new Object()));
    }

    private void tryRender(Object constant, String result) {
        StringBuilder builder = new StringBuilder();
        renderConstant(builder, constant, Collections.emptyMap(), false);
        assertEquals(result, builder.toString());
        assertTrue(canRenderConstant(constant));
    }
}
