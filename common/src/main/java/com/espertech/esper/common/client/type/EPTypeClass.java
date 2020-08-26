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
package com.espertech.esper.common.client.type;

import com.espertech.esper.common.internal.util.ClassHelperPrint;

import java.io.StringWriter;
import java.util.function.Consumer;

/**
 * {@link EPTypeClass} represents the value class and does not have type parameters. Use {@link EPTypeClassParameterized} for a class with type parameters.
 * <p>
 *     Use {@link EPTypePremade#getOrCreate(Class)} to obtain an {@link EPTypeClass} instance for many commonly-used types.
 *     You may also use {@link EPTypePremade#STRING} (for example for the string type).
 * </p>
 * <p>
 *     The EPL compiler and runtime do not only use {@link Class} as Java type erasure means that a class instance
 *     does not provide information about its type parameters. For instance the type <code>List&lt;String&gt;</code>
 *     has <code>String</code> as the type parameter. Looking at <code>List.class</code> alone
 *     does not provide such type information.
 * </p>
 */
public class EPTypeClass implements EPType {
    /**
     * Type information
     */
    public final static EPTypeClass EPTYPE = new EPTypeClass(EPTypeClass.class);
    private static final long serialVersionUID = 1801760751392971863L;

    protected final Class<?> typeClass;

    /**
     * Ctor.
     * @param typeClass clazz
     */
    public EPTypeClass(Class<?> typeClass) {
        if (typeClass == null) {
            throw new IllegalArgumentException("Invalid null argument");
        }
        this.typeClass = typeClass;
    }

    /**
     * Returns the type.
     * @return type
     */
    public Class<?> getType() {
        return typeClass;
    }

    public String toString() {
        return toFullName();
    }

    /**
     * Traverses classes that are referenced by the class and type parameters, if any.
     * @param classConsumer consumer
     */
    public void traverseClasses(Consumer<Class<?>> classConsumer) {
        classConsumer.accept(typeClass);
    }

    /**
     * Appends the class full name.
     * @param writer to append to
     */
    public void appendFullName(StringWriter writer) {
        writer.append(ClassHelperPrint.getClassNameFullyQualPretty(typeClass));
    }

    /**
     * Appends the class simple name.
     * @param writer to append to
     */
    public void appendSimpleName(StringWriter writer) {
        writer.append(typeClass.getSimpleName());
    }

    /**
     * Returns the class full name, including type parameters if any.
     * @return full name
     */
    public String toFullName() {
        return ClassHelperPrint.getClassNameFullyQualPretty(typeClass);
    }

    /**
     * Returns the class simple name, including type parameters if any.
     * @return simple name
     */
    public String toSimpleName() {
        return typeClass.getSimpleName();
    }

    public String getTypeName() {
        return toFullName();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EPTypeClass that = (EPTypeClass) o;

        return typeClass.equals(that.typeClass);
    }

    public int hashCode() {
        return typeClass.hashCode();
    }
}
