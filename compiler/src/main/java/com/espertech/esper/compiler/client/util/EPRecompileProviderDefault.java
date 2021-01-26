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
package com.espertech.esper.compiler.client.util;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.hook.recompile.EPRecompileProvider;
import com.espertech.esper.common.client.hook.recompile.EPRecompileProviderContext;
import com.espertech.esper.common.client.hook.recompile.EPRecompileProviderException;
import com.espertech.esper.common.client.module.ModuleProperty;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EPRecompileProviderDefault implements EPRecompileProvider {
    private static final Logger log = LoggerFactory.getLogger(EPRecompileProviderDefault.class);

    public EPCompiled provide(EPRecompileProviderContext env) throws EPRecompileProviderException {
        String epl = (String) env.getModuleProperties().get(ModuleProperty.MODULETEXT);
        if (epl == null) {
            throw new EPRecompileProviderException("EPL not part of module properties");
        }
        log.info("Recompiling EPL: " + epl);

        CompilerArguments args = new CompilerArguments(env.getConfiguration());
        args.getPath().addAll(env.getPath());

        try {
            return EPCompilerProvider.getCompiler().compile(epl, args);
        } catch (EPCompileException ex) {
            throw new EPRecompileProviderException("Failed to recompile epl '" + epl + "': " + ex.getMessage(), ex);
        }
    }
}
