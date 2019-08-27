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
package com.espertech.esper.common.internal.epl.enummethod.dot;

import com.espertech.esper.common.client.hook.enummethod.EnumMethodDescriptor;
import com.espertech.esper.common.client.hook.enummethod.EnumMethodForgeFactory;
import com.espertech.esper.common.client.hook.enummethod.EnumMethodInitializeContext;
import com.espertech.esper.common.internal.epl.enummethod.plugin.ExprDotForgeEnumMethodFactoryPlugin;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.settings.ClasspathImportException;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Locale;

public class EnumMethodResolver {
    public static boolean isEnumerationMethod(String name, ClasspathImportServiceCompileTime classpathImportService) throws ExprValidationException {
        for (EnumMethodBuiltin e : EnumMethodBuiltin.values()) {
            if (e.getNameCamel().toLowerCase(Locale.ENGLISH).equals(name.toLowerCase(Locale.ENGLISH))) {
                return true;
            }
        }
        try {
            return classpathImportService.resolveEnumMethod(name) != null;
        } catch (ClasspathImportException e) {
            throw new ExprValidationException("Failed to resolve enum-method '" + name + "': " + e.getMessage(), e);
        }
    }

    public static EnumMethodDesc fromName(String name, ClasspathImportServiceCompileTime classpathImportService) throws ExprValidationException {
        for (EnumMethodBuiltin e : EnumMethodBuiltin.values()) {
            if (e.getNameCamel().toLowerCase(Locale.ENGLISH).equals(name.toLowerCase(Locale.ENGLISH))) {
                return e.getDescriptor();
            }
        }

        try {
            Class factory = classpathImportService.resolveEnumMethod(name);
            if (factory != null) {
                EnumMethodForgeFactory forgeFactory = (EnumMethodForgeFactory) JavaClassHelper.instantiate(EnumMethodForgeFactory.class, factory);
                EnumMethodDescriptor descriptor = forgeFactory.initialize(new EnumMethodInitializeContext());
                ExprDotForgeEnumMethodFactoryPlugin plugin = new ExprDotForgeEnumMethodFactoryPlugin(name, forgeFactory);
                return new EnumMethodDesc(name, EnumMethodEnum.PLUGIN, plugin, descriptor.getFootprints());
            }
        } catch (Exception ex) {
            throw new ExprValidationException("Failed to resolve date-time-method '" + name + "' :" + ex.getMessage(), ex);
        }
        return null;
    }
}
