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
package com.espertech.esper.common.internal.compile.stage1.spec;

import java.util.ArrayList;
import java.util.List;

/**
 * Specification for property evaluation.
 */
public class PropertyEvalSpec {
    private List<PropertyEvalAtom> atoms;

    /**
     * Ctor.
     */
    public PropertyEvalSpec() {
        this.atoms = new ArrayList<PropertyEvalAtom>();
    }

    /**
     * Return a list of atoms.
     *
     * @return atoms
     */
    public List<PropertyEvalAtom> getAtoms() {
        return atoms;
    }

    /**
     * Add an atom.
     *
     * @param atom to add
     */
    public void add(PropertyEvalAtom atom) {
        atoms.add(atom);
    }
}
