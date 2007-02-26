package org.drools.semantics.java.builder;

import java.util.List;

import org.drools.lang.descr.PredicateDescr;
import org.drools.rule.Declaration;
import org.drools.rule.PredicateConstraint;

public interface PredicateBuilder {
    public void build(final BuildContext context,
                      final BuildUtils utils,
                      final List[] usedIdentifiers,
                      final Declaration[] previousDeclarations,
                      final Declaration[] localDeclarations,
                      final PredicateConstraint predicateConstraint,
                      final PredicateDescr predicateDescr);
}
