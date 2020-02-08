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
package com.espertech.esper.common.internal.epl.classprovided.core;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.util.ByteArrayProvidingClassLoader;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ClassProvided {

    private Map<String, byte[]> bytes;
    private String className;
    private String moduleName;
    private NameAccessModifier visibility = NameAccessModifier.TRANSIENT;
    private List<Class> classesMayNull;

    public ClassProvided() {
    }

    public ClassProvided(Map<String, byte[]> bytes, String className) {
        this.bytes = bytes;
        this.className = className;
    }

    public void loadClasses(ClassLoader parentClassLoader) {
        classesMayNull = new ArrayList<>(2);
        ByteArrayProvidingClassLoader cl = new ByteArrayProvidingClassLoader(bytes, parentClassLoader);
        for (Map.Entry<String, byte[]> entry : getBytes().entrySet()) {
            try {
                Class<?> clazz = Class.forName(entry.getKey(), false, cl);
                classesMayNull.add(clazz);
            } catch (ClassNotFoundException e) {
                throw new EPException("Unexpected exception loading class " + entry.getKey() + ": " + e.getMessage(), e);
            }
        }
    }

    public CodegenExpression make(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ClassProvided.class, this.getClass(), classScope);
        if (bytes.isEmpty()) {
            method.getBlock().declareVar(Map.class, "bytes", staticMethod(Collections.class, "emptyMap"));
        } else {
            method.getBlock().declareVar(Map.class, "bytes", newInstance(HashMap.class, constant(CollectionUtil.capacityHashMap(bytes.size()))));
            for (Map.Entry<String, byte[]> entry : bytes.entrySet()) {
                method.getBlock().exprDotMethod(ref("bytes"), "put",
                    constant(entry.getKey()), constant(entry.getValue()));
            }

        }
        method.getBlock()
            .declareVar(ClassProvided.class, "cp", newInstance(ClassProvided.class))
            .exprDotMethod(ref("cp"), "setBytes", ref("bytes"))
            .exprDotMethod(ref("cp"), "setClassName", constant(className))
            .exprDotMethod(ref("cp"), "setModuleName", constant(moduleName))
            .exprDotMethod(ref("cp"), "setVisibility", constant(visibility))
            .methodReturn(ref("cp"));
        return localMethod(method);
    }

    public Map<String, byte[]> getBytes() {
        return bytes;
    }

    public void setBytes(Map<String, byte[]> bytes) {
        this.bytes = bytes;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public NameAccessModifier getVisibility() {
        return visibility;
    }

    public void setVisibility(NameAccessModifier visibility) {
        this.visibility = visibility;
    }

    public List<Class> getClassesMayNull() {
        return classesMayNull;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
