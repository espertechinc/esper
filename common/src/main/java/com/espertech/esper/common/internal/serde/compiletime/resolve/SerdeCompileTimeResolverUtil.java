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
package com.espertech.esper.common.internal.serde.compiletime.resolve;

import com.espertech.esper.common.client.serde.*;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.serde.serdeset.builtin.DIONullableObjectArraySerde;
import com.espertech.esper.common.internal.serde.serdeset.builtin.DIOSerializableObjectSerde;
import com.espertech.esper.common.internal.util.ClassHelperPrint;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.Externalizable;
import java.io.Serializable;
import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;

public class SerdeCompileTimeResolverUtil {

    // Order of resolution:
    // (1) any serde providers are asked first, first one taking it counts
    // (2) class implements Externalizable (when allowed)
    // (3) class implements Serializable (when allowed)
    protected static SerdeProvision determineSerde(EPTypeClass type, Collection<SerdeProvider> serdeProviders, boolean allowSerializable, boolean allowExternalizable, boolean allowSerializationFallback, SerdeProviderAdditionalInfo additionalInfo) {

        SerdeProvision serde;
        if (!serdeProviders.isEmpty()) {
            serde = determineSerdeFromProviders(type, serdeProviders, additionalInfo);
            if (serde != null) {
                return serde;
            }

            if (type.getType().isArray()) {
                EPTypeClass componentType = JavaClassHelper.getArrayComponentType(type);
                SerdeProvision componentSerde = determineSerdeFromProviders(componentType, serdeProviders, additionalInfo);
                if (componentSerde != null) {
                    return new SerdeProvisionParameterized(DIONullableObjectArraySerde.class,
                        vars -> constant(componentType.getType()),
                        vars -> componentSerde.toForge().codegen(vars.getMethod(), vars.getScope(), vars.getOptionalEventTypeResolver()));
                }
            }
        }

        serde = determineSerializable(type, allowExternalizable, allowSerializable, allowSerializationFallback);
        if (serde != null) {
            return serde;
        }

        throw makeFailedToFindException(type, allowExternalizable, allowSerializable, serdeProviders.size(), additionalInfo);
    }

    private static SerdeProvision determineSerdeFromProviders(EPTypeClass type, Collection<SerdeProvider> serdeProviders, SerdeProviderAdditionalInfo additionalInfo) {
        if (serdeProviders.isEmpty()) {
            return null;
        }
        SerdeProviderContextClass context = new SerdeProviderContextClass(type, additionalInfo);
        for (SerdeProvider provider : serdeProviders) {
            try {
                SerdeProvision serde = provider.resolveSerdeForClass(context);
                if (serde != null) {
                    return serde;
                }
            } catch (DataInputOutputSerdeException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                throw handleProviderRuntimeException(provider, type, ex);
            }
        }
        return null;
    }

    private static SerdeProvision determineSerializable(EPTypeClass type, boolean allowExternalizable, boolean allowSerializable, boolean allowSerializationFallback) {
        if (allowSerializationFallback) {
            return new SerdeProvisionByClass(DIOSerializableObjectSerde.class);
        }

        if (JavaClassHelper.isImplementsInterface(type, Externalizable.class) && allowExternalizable) {
            return new SerdeProvisionByClass(DIOSerializableObjectSerde.class);
        }
        if (type.getType().isArray() && JavaClassHelper.isImplementsInterface(type.getType().getComponentType(), Externalizable.class) && allowExternalizable) {
            return new SerdeProvisionByClass(DIOSerializableObjectSerde.class);
        }

        if (JavaClassHelper.isImplementsInterface(type, Serializable.class) && allowSerializable) {
            return new SerdeProvisionByClass(DIOSerializableObjectSerde.class);
        }
        if (type.getType().isArray() && JavaClassHelper.isImplementsInterface(type.getType().getComponentType(), Serializable.class) && allowSerializable) {
            return new SerdeProvisionByClass(DIOSerializableObjectSerde.class);
        }

        return null;
    }

    private static DataInputOutputSerdeException handleProviderRuntimeException(SerdeProvider provider, EPTypeClass type, RuntimeException ex) {
        return new DataInputOutputSerdeException("Unexpected exception invoking serde provider '" + provider.getClass().getName() + "' passing '" + type + "': " + ex.getMessage(), ex);
    }

    private static DataInputOutputSerdeException makeFailedToFindException(EPTypeClass clazz, boolean allowExternalizable, boolean allowSerializable, int numSerdeProviders, SerdeProviderAdditionalInfo additionalInfo) {
        return new DataInputOutputSerdeException("Failed to find serde for class '" + ClassHelperPrint.getClassNameFullyQualPretty(clazz) +
            "' for use with " + additionalInfo + " (" +
            "allowExternalizable=" + allowExternalizable + "," +
            "allowSerializable=" + allowSerializable + "," +
            "serdeProvider-count=" + numSerdeProviders
            + ")");
    }
}
