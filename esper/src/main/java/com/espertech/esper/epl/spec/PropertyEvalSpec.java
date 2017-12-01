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
package com.espertech.esper.epl.spec;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Specification for property evaluation.
 */
public class PropertyEvalSpec implements Serializable {
    private List<PropertyEvalAtom> atoms;
    private static final long serialVersionUID = -8843638696605082278L;

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
