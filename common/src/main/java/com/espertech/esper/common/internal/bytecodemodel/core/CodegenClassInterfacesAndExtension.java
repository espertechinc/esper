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
package com.espertech.esper.common.internal.bytecodemodel.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CodegenClassInterfacesAndExtension {
    private final List<CodegenClassReference> implemented = new ArrayList<>(2);
    private CodegenClassReference extended;

    public List<CodegenClassReference> getImplemented() {
        return implemented;
    }

    public CodegenClassReference getExtended() {
        return extended;
    }

    public void addInterfaceImplemented(Class clazzImplemented) {
        implemented.add(new CodegenClassReference(clazzImplemented));
    }

    public void addInterfaceImplemented(String classNameImplemented) {
        implemented.add(new CodegenClassReference(classNameImplemented));
    }

    public void setClassExtended(String classNameExtended) {
        extended = new CodegenClassReference(classNameExtended);
    }

    public void setClassExtended(Class clazzExtended) {
        extended = new CodegenClassReference(clazzExtended);
    }

    void addReferenced(Set<Class> classes) {
        for (CodegenClassReference implement : implemented) {
            implement.addReferenced(classes);
        }
        if (extended != null) {
            extended.addReferenced(classes);
        }
    }

    public void render(StringBuilder builder, Map<Class, String> imports) {
        if (!implemented.isEmpty()) {
            builder.append(" implements ");
            boolean first = true;
            for (CodegenClassReference implement : implemented) {
                if (!first) {
                    builder.append(", ");
                }
                first = false;
                implement.render(builder, imports);
            }
        }

        if (extended != null) {
            builder.append(" extends ");
            extended.render(builder, imports);
        }
    }
}
