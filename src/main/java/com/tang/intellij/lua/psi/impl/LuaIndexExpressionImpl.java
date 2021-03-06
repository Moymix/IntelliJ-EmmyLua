/*
 * Copyright (c) 2017. tangzx(love.tangzx@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tang.intellij.lua.psi.impl;

import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceService;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.IElementType;
import com.tang.intellij.lua.lang.type.LuaType;
import com.tang.intellij.lua.lang.type.LuaTypeSet;
import com.tang.intellij.lua.psi.LuaClassField;
import com.tang.intellij.lua.psi.LuaExpression;
import com.tang.intellij.lua.psi.LuaIndexExpr;
import com.tang.intellij.lua.search.SearchContext;
import com.tang.intellij.lua.stubs.LuaIndexStub;
import com.tang.intellij.lua.stubs.index.LuaClassFieldIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 *
 * Created by TangZX on 2017/4/12.
 */
public class LuaIndexExpressionImpl extends StubBasedPsiElementBase<LuaIndexStub> implements LuaExpression, LuaClassField {

    LuaIndexExpressionImpl(@NotNull LuaIndexStub stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    LuaIndexExpressionImpl(@NotNull ASTNode node) {
        super(node);
    }

    LuaIndexExpressionImpl(LuaIndexStub stub, IElementType nodeType, ASTNode node) {
        super(stub, nodeType, node);
    }


    @NotNull
    @Override
    public PsiReference[] getReferences() {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this, PsiReferenceService.Hints.NO_HINTS);
    }

    @Override
    public PsiReference getReference() {
        PsiReference[] references = getReferences();

        if (references.length > 0)
            return references[0];
        return null;
    }

    @Override
    public LuaTypeSet guessType(SearchContext context) {
        LuaTypeSet result = LuaTypeSet.create();
        if (context.push(this, SearchContext.Overflow.GuessType)) {
            LuaIndexExpr indexExpr = (LuaIndexExpr) this;

            // value type
            LuaIndexStub stub = indexExpr.getStub();
            LuaTypeSet valueTypeSet;
            if (stub != null)
                valueTypeSet = stub.guessValueType();
            else
                valueTypeSet = indexExpr.guessValueType(context);

            result = result.union(valueTypeSet);

            String propName = this.getFieldName();
            if (propName != null) {
                LuaTypeSet prefixType = indexExpr.guessPrefixType(context);
                if (prefixType != null && !prefixType.isEmpty()) {
                    for (LuaType type : prefixType.getTypes()) {
                        LuaTypeSet typeSet = guessFieldType(propName, type, context);
                        result = result.union(typeSet);
                    }
                }
            }
            context.pop(this);
        }
        return result;
    }

    @Nullable
    private LuaTypeSet guessFieldType(String fieldName, LuaType type, SearchContext context) {
        LuaTypeSet set = LuaTypeSet.create();

        Collection<LuaClassField> all = LuaClassFieldIndex.findAll(type, fieldName, context);
        for (LuaClassField fieldDef : all) {
            if (fieldDef instanceof LuaIndexExpr) {
                LuaIndexExpr indexExpr = (LuaIndexExpr) fieldDef;
                LuaIndexStub stub = indexExpr.getStub();
                if (stub != null)
                    set = set.union(stub.guessValueType());
                else
                    set = set.union(indexExpr.guessValueType(context));

                if (fieldDef == this)
                    return set;
            }

            if (fieldDef != null) {
                set = set.union(fieldDef.guessType(context));
            } else {
                LuaType superType = type.getSuperClass(context);
                if (superType != null)
                    set = set.union(guessFieldType(fieldName, superType, context));
            }
        }

        return set;
    }

    @Override
    public String getFieldName() {
        LuaIndexStub stub = getStub();
        if (stub != null)
            return stub.getFieldName();
        return getName();
    }
}
