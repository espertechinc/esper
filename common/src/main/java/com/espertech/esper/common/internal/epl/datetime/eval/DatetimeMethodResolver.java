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
package com.espertech.esper.common.internal.epl.datetime.eval;

import com.espertech.esper.common.client.hook.datetimemethod.DateTimeMethodDescriptor;
import com.espertech.esper.common.client.hook.datetimemethod.DateTimeMethodForgeFactory;
import com.espertech.esper.common.client.hook.datetimemethod.DateTimeMethodInitializeContext;
import com.espertech.esper.common.internal.epl.datetime.plugin.DTMPluginForgeFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.settings.ClasspathImportException;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Locale;

public class DatetimeMethodResolver {
    public static boolean isDateTimeMethod(String name, ClasspathImportServiceCompileTime classpathImportService) throws ExprValidationException {
        for (DatetimeMethodBuiltin e : DatetimeMethodBuiltin.values()) {
            if (e.getNameCamel().toLowerCase(Locale.ENGLISH).equals(name.toLowerCase(Locale.ENGLISH))) {
                return true;
            }
        }
        try {
            return classpathImportService.resolveDateTimeMethod(name) != null;
        } catch (ClasspathImportException e) {
            throw new ExprValidationException("Failed to resolve date-time-method '" + name + "': " + e.getMessage(), e);
        }
    }

    public static DatetimeMethodDesc fromName(String name, ClasspathImportServiceCompileTime classpathImportService) throws ExprValidationException {
        for (DatetimeMethodBuiltin e : DatetimeMethodBuiltin.values()) {
            if (e.getNameCamel().toLowerCase(Locale.ENGLISH).equals(name.toLowerCase(Locale.ENGLISH))) {
                return e.getDescriptor();
            }
        }

        try {
            Class factory = classpathImportService.resolveDateTimeMethod(name);
            if (factory != null) {
                DateTimeMethodForgeFactory forgeFactory = (DateTimeMethodForgeFactory) JavaClassHelper.instantiate(DateTimeMethodForgeFactory.class, factory);
                DateTimeMethodDescriptor descriptor = forgeFactory.initialize(new DateTimeMethodInitializeContext());
                DTMPluginForgeFactory plugin = new DTMPluginForgeFactory(forgeFactory);
                return new DatetimeMethodDesc(DatetimeMethodEnum.PLUGIN, plugin, descriptor.getFootprints());
            }
        } catch (Exception ex) {
            throw new ExprValidationException("Failed to resolve date-time-method '" + name + "' :" + ex.getMessage(), ex);
        }
        return null;
    }
}
