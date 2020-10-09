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
package com.espertech.esper.common.internal.epl.agg.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class AggregationClassAssignment {
    private final int offset;
    private final CodegenMemberCol members;
    private final CodegenCtor ctor;
    private final List<ExprForge[]> methodForges;
    private final List<AggregationForgeFactory> methodFactories;
    private final List<AggregationStateFactoryForge> accessStateFactories;
    private final List<AggregationVColMethod> vcolMethods = new ArrayList<>(8);
    private final List<AggregationVColAccess> vcolAccess = new ArrayList<>(8);
    private String className;
    private String memberName;

    public AggregationClassAssignment(int offset, Class forgeClass, CodegenClassScope classScope) {
        this.offset = offset;
        this.ctor = new CodegenCtor(forgeClass, classScope, Collections.emptyList());
        this.members = new CodegenMemberCol();
        this.methodForges = new ArrayList<>();
        this.methodFactories = new ArrayList<>();
        this.accessStateFactories = new ArrayList<>();
    }

    public void addMethod(AggregationVColMethod vcol) {
        vcolMethods.add(vcol);
    }

    public void addAccess(AggregationVColAccess vcol) {
        vcolAccess.add(vcol);
    }

    public void add(AggregationForgeFactory methodFactory, ExprForge[] forges) {
        methodFactories.add(methodFactory);
        methodForges.add(forges);
    }

    public void add(AggregationStateFactoryForge factory) {
        accessStateFactories.add(factory);
    }

    public int size() {
        return members.getMembersPerColumn().size();
    }

    public CodegenMemberCol getMembers() {
        return members;
    }

    public CodegenCtor getCtor() {
        return ctor;
    }

    public AggregationForgeFactory[] getMethodFactories() {
        return methodFactories.toArray(new AggregationForgeFactory[0]);
    }

    public AggregationStateFactoryForge[] getAccessStateFactories() {
        return accessStateFactories.toArray(new AggregationStateFactoryForge[0]);
    }

    public ExprForge[][] getMethodForges() {
        return methodForges.toArray(new ExprForge[0][]);
    }

    public int getMemberSize() {
        return members.getMembers().size();
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public int getOffset() {
        return offset;
    }

    public List<AggregationVColMethod> getVcolMethods() {
        return vcolMethods;
    }

    public List<AggregationVColAccess> getVcolAccess() {
        return vcolAccess;
    }
}
