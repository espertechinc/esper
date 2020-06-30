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
package com.espertech.esper.common.internal.util;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenFieldSharable;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newAnonymousClass;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public class SimpleTypeParserCodegenFieldSharable implements CodegenFieldSharable {
    private final SimpleTypeParserSPI parser;
    private final CodegenClassScope classScope;

    public SimpleTypeParserCodegenFieldSharable(SimpleTypeParserSPI parser, CodegenClassScope classScope) {
        this.parser = parser;
        this.classScope = classScope;
    }

    public EPTypeClass type() {
        return SimpleTypeParser.EPTYPE;
    }

    public CodegenExpression initCtorScoped() {
        CodegenExpressionNewAnonymousClass anonymousClass = newAnonymousClass(classScope.getPackageScope().getInitMethod().getBlock(), SimpleTypeParser.EPTYPE);
        CodegenMethod parse = CodegenMethod.makeParentNode(EPTypePremade.OBJECT.getEPType(), this.getClass(), classScope).addParam(EPTypePremade.STRING.getEPType(), "text");
        anonymousClass.addMethod("parse", parse);
        parse.getBlock().methodReturn(parser.codegen(ref("text")));
        return anonymousClass;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleTypeParserCodegenFieldSharable that = (SimpleTypeParserCodegenFieldSharable) o;

        return parser.equals(that.parser);
    }

    public int hashCode() {
        return parser.hashCode();
    }
}
