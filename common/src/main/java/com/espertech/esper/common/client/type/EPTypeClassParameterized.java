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
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Provides type parameters.
 * <p>
 * For example, type <code>List&lt;String&gt;</code> is equivalent to <code>new EPTypeClassParameterized(List.class, new EPTypeClass[] {EPTypePremade.STRING.getEPType()})</code>
 * For example, type <code>Map&lt;String, Integer&gt;</code> is equivalent to <code>new EPTypeClassParameterized(List.class, new EPTypeClass[] {EPTypePremade.STRING.getEPType(), EPTypePremade.INTEGERBOXED.getEPType()})</code>
 * </p>
 * <p>
 * The array information is part of the {@link EPTypeClass#getType()}.
 * </p>
 * <p>
 * Use this class only when the type is parameterized. It requires at least a single type parameter.
 * Use {@link EPTypeClass} instead if the type is not parameterized.
 * </p>
 */
public class EPTypeClassParameterized extends EPTypeClass {
    /**
     * Type information
     */
    public final static EPTypeClass EPTYPE = new EPTypeClass(EPTypeClassParameterized.class);

    private final EPTypeClass[] parameters;

    /**
     * Ctor.
     *
     * @param typeClass  the type
     * @param parameters type parameters
     */
    public EPTypeClassParameterized(Class<?> typeClass, EPTypeClass[] parameters) {
        super(typeClass);
        this.parameters = parameters;
        if (parameters == null || parameters.length == 0) {
            throw new IllegalStateException("No type parameters provided");
        }
        for (EPTypeClass parameter : parameters) {
            if (parameter == null) {
                throw new IllegalStateException("One of the type parameters is null");
            }
        }
    }

    /**
     * Build an instance using the type and the single non-null type parameter provided.
     * <p>
     * For example, use <code>from(List.class, String.class)</code> to build <code>List&lt;String&gt;</code>.
     * </p>
     *
     * @param typeClass type (non-null)
     * @param parameter single type parameter (non-null)
     * @return instance
     */
    public static EPTypeClassParameterized from(Class<?> typeClass, Class<?> parameter) {
        if (typeClass == null) {
            throw new IllegalStateException("Type is null");
        }
        if (parameter == null) {
            throw new IllegalStateException("Type parameter is null");
        }
        return new EPTypeClassParameterized(typeClass, new EPTypeClass[]{EPTypePremade.getOrCreate(parameter)});
    }

    /**
     * Build an instance using the type and the single non-null type parameter provided.
     * <p>
     * For example, use <code>from(List.class, EPTypePremade.STRING.getEPType())</code> to build <code>List&lt;String&gt;</code>.
     * </p>
     *
     * @param typeClass type (non-null)
     * @param parameter single type parameter (non-null)
     * @return instance
     */
    public static EPTypeClassParameterized from(Class<?> typeClass, EPTypeClass parameter) {
        if (typeClass == null) {
            throw new IllegalStateException("Type is null");
        }
        if (parameter == null) {
            throw new IllegalStateException("Type parameter is null");
        }
        return new EPTypeClassParameterized(typeClass, new EPTypeClass[]{parameter});
    }

    /**
     * Build an instance using the type and two non-null type parameters provided.
     * <p>
     * For example, use <code>from(List.class, String.class, Integer.class)</code> to build <code>Map&lt;String, Integer&gt;</code>.
     * </p>
     *
     * @param typeClass type
     * @param first     first type parameter
     * @param second    second type parameter
     * @return instance
     */
    public static EPTypeClassParameterized from(Class<?> typeClass, Class<?> first, Class<?> second) {
        if (typeClass == null) {
            throw new IllegalStateException("Type is null");
        }
        if (first == null) {
            throw new IllegalStateException("First type parameter is null");
        }
        if (second == null) {
            throw new IllegalStateException("Second type parameter is null");
        }
        return new EPTypeClassParameterized(typeClass, new EPTypeClass[]{EPTypePremade.getOrCreate(first), EPTypePremade.getOrCreate(second)});
    }

    /**
     * Returns the type parameters
     *
     * @return type parameters
     */
    public EPTypeClass[] getParameters() {
        return parameters;
    }

    @Override
    public void traverseClasses(Consumer<Class<?>> classConsumer) {
        classConsumer.accept(typeClass);
        for (EPTypeClass parameter : parameters) {
            parameter.traverseClasses(classConsumer);
        }
    }

    @Override
    public String toFullName() {
        StringWriter writer = new StringWriter();
        appendFullName(writer);
        return writer.toString();
    }

    @Override
    public String toSimpleName() {
        StringWriter writer = new StringWriter();
        appendSimpleName(writer);
        return writer.toString();
    }

    @Override
    public void appendFullName(StringWriter writer) {
        append(writer, param -> param.appendFullName(writer));
    }

    @Override
    public void appendSimpleName(StringWriter writer) {
        append(writer, param -> param.appendSimpleName(writer));
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        EPTypeClassParameterized that = (EPTypeClassParameterized) o;

        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(parameters, that.parameters);
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(parameters);
        return result;
    }

    private void append(StringWriter writer, Consumer<EPTypeClass> paramWriter) {
        Class<?> innerMost = JavaClassHelper.getArrayComponentTypeInnermost(typeClass);
        writer.append(ClassHelperPrint.getClassNameFullyQualPretty(innerMost));
        writer.append("<");
        String delimiter = "";
        for (EPTypeClass param : parameters) {
            writer.append(delimiter);
            paramWriter.accept(param);
            delimiter = ",";
        }
        writer.append(">");
        ClassHelperPrint.appendDimensions(writer, typeClass);
    }
}
