/*
 * Sonar Delphi Plugin
 * Copyright (C) 2023 Integrated Application Development
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package au.com.integradev.delphi.checks;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.communitydelphi.api.ast.ArrayAccessorNode;
import org.sonar.plugins.communitydelphi.api.ast.AssignmentStatementNode;
import org.sonar.plugins.communitydelphi.api.ast.BinaryExpressionNode;
import org.sonar.plugins.communitydelphi.api.ast.CompoundStatementNode;
import org.sonar.plugins.communitydelphi.api.ast.DelphiNode;
import org.sonar.plugins.communitydelphi.api.ast.ExpressionNode;
import org.sonar.plugins.communitydelphi.api.ast.ForToStatementNode;
import org.sonar.plugins.communitydelphi.api.ast.IntegerLiteralNode;
import org.sonar.plugins.communitydelphi.api.ast.NameReferenceNode;
import org.sonar.plugins.communitydelphi.api.ast.Node;
import org.sonar.plugins.communitydelphi.api.ast.PrimaryExpressionNode;
import org.sonar.plugins.communitydelphi.api.ast.StatementListNode;
import org.sonar.plugins.communitydelphi.api.ast.StatementNode;
import org.sonar.plugins.communitydelphi.api.ast.VarStatementNode;
import org.sonar.plugins.communitydelphi.api.ast.Visibility;
import org.sonar.plugins.communitydelphi.api.check.DelphiCheck;
import org.sonar.plugins.communitydelphi.api.check.DelphiCheckContext;
import org.sonar.plugins.communitydelphi.api.operator.BinaryOperator;
import org.sonar.plugins.communitydelphi.api.symbol.Invocable;
import org.sonar.plugins.communitydelphi.api.symbol.declaration.MethodNameDeclaration;
import org.sonar.plugins.communitydelphi.api.symbol.declaration.NameDeclaration;
import org.sonar.plugins.communitydelphi.api.symbol.declaration.PropertyNameDeclaration;
import org.sonar.plugins.communitydelphi.api.symbol.declaration.VariableNameDeclaration;
import org.sonar.plugins.communitydelphi.api.symbol.scope.MethodScope;
import org.sonar.plugins.communitydelphi.api.type.StructKind;
import org.sonar.plugins.communitydelphi.api.type.Type;
import org.sonar.plugins.communitydelphi.api.type.Type.ScopedType;
import org.sonar.plugins.communitydelphi.api.type.Type.StructType;

@Rule(key = "IndexBasedEnumeratorLoop")
public class IndexBasedEnumeratorLoopCheck extends DelphiCheck {

  public static final String MESSAGE = "Enumerate this collection using a for-in loop.";

  @Override
  public DelphiCheckContext visit(ForToStatementNode forNode, DelphiCheckContext context) {
    doVisit(forNode, context);
    return super.visit(forNode, context);
  }

  private void doVisit(ForToStatementNode forNode, DelphiCheckContext context) {
    if (isViolation(forNode)) {
      reportIssue(context, forNode.getVariable(), MESSAGE);
    }
  }

  private static boolean isViolation(ForToStatementNode forNode) {
    StatementNode loopBody = forNode.getStatement();
    NameDeclaration loopVarDecl = forNode.getVariable().getNameDeclaration();
    return loopVarDecl != null
        && isLiteralZero(forNode.getInitializerExpression())
        && isEnumerableCountMinusOne(forNode.getTargetExpression())
            .filter(
                decl -> isFirstStatementIndexedAssignmentToLocalVar(loopBody, loopVarDecl, decl))
            .isPresent()
        && countReferencesInLoop(loopBody, loopVarDecl) == 1;
  }

  private static long countReferencesInLoop(DelphiNode loopBody, NameDeclaration forVarDecl) {
    return loopBody.findDescendantsOfType(NameReferenceNode.class).stream()
        .filter(n -> n.getNameDeclaration().equals(forVarDecl))
        .count();
  }

  private static boolean isFirstStatementIndexedAssignmentToLocalVar(
      StatementNode loopBody, NameDeclaration forVarDecl, NameDeclaration enumerable) {
    return Optional.of(loopBody)
        .filter(CompoundStatementNode.class::isInstance)
        .map(node -> node.getFirstChildOfType(StatementListNode.class))
        .map(node -> node.getChild(0))
        .filter(node -> isIndexedAssignment(node, enumerable, forVarDecl))
        .isPresent();
  }

  private static Optional<NameDeclaration> isEnumerableCountMinusOne(DelphiNode endValue) {
    return Optional.of(endValue)
        .filter(BinaryExpressionNode.class::isInstance)
        .map(BinaryExpressionNode.class::cast)
        .filter(binary -> binary.getOperator() == BinaryOperator.SUBTRACT)
        .filter(binary -> isLiteralOne(binary.getRight()))
        .map(binary -> getOnlyChild(binary.getLeft()))
        .filter(NameReferenceNode.class::isInstance)
        .map(NameReferenceNode.class::cast)
        .filter(IndexBasedEnumeratorLoopCheck::isEnumerableDotCount)
        .map(NameReferenceNode::getNameDeclaration);
  }

  private static DelphiNode getOnlyChild(DelphiNode node) {
    return node.getChildren().size() == 1 ? node.getChild(0) : null;
  }

  private static boolean isLiteralZero(DelphiNode node) {
    return isLiteralIntWithValue(node, 0);
  }

  private static boolean isLiteralOne(DelphiNode right) {
    return isLiteralIntWithValue(right, 1);
  }

  private static boolean isLiteralIntWithValue(DelphiNode node, int i) {
    return Optional.of(node)
        .filter(PrimaryExpressionNode.class::isInstance)
        .map(PrimaryExpressionNode.class::cast)
        .map(ExpressionNode::extractLiteral)
        .filter(IntegerLiteralNode.class::isInstance)
        .filter(n -> n.getValueAsInt() == i)
        .isPresent();
  }

  private static boolean isEnumerableDotCount(NameReferenceNode node) {
    NameReferenceNode lastName = node.getLastName();
    return lastName.getIdentifier().getImage().equalsIgnoreCase("Count")
        && isReferenceToEnumerableType(lastName.prevName());
  }

  private static boolean isReferenceToEnumerableType(NameReferenceNode child) {
    NameDeclaration nameDeclaration = child.getNameDeclaration();
    return nameDeclaration instanceof VariableNameDeclaration
        && isEnumerableType(((VariableNameDeclaration) nameDeclaration).getType());
  }

  private static boolean isEnumerableType(Type type) {
    return type.isSubTypeOf("System.IEnumerable") || isImplicitEnumerableType(type);
  }

  private static boolean isImplicitEnumerableType(Type type) {
    if (!(type instanceof StructType)) {
      return false;
    }
    var decls = ((StructType) type).typeScope().getMethodDeclarations();
    return decls.stream()
        .filter(IndexBasedEnumeratorLoopCheck::isGetEnumeratorMethod)
        .findFirst()
        .map(Invocable::getReturnType)
        .filter(IndexBasedEnumeratorLoopCheck::isEnumeratorType)
        .isPresent();
  }

  private static boolean isEnumeratorType(Type type) {
    return type.isSubTypeOf("System.IEnumerator") || isImplicitEnumeratorType(type);
  }

  private static boolean isImplicitEnumeratorType(Type type) {
    return Optional.of(type)
        .filter(StructType.class::isInstance)
        .map(StructType.class::cast)
        .filter(
            structType ->
                StructKind.OBJECT == structType.kind()
                    || StructKind.CLASS == structType.kind()
                    || StructKind.INTERFACE == structType.kind()
                    || StructKind.RECORD == structType.kind())
        .map(ScopedType::typeScope)
        .filter(typeScope -> hasCurrentProperty(typeScope.getPropertyDeclarations()))
        .filter(typeScope -> hasMoveNextMethod(typeScope.getMethodDeclarations()))
        .isPresent();
  }

  private static boolean isVisibleForEnumeration(Visibility decl) {
    return decl.isPublished() || decl.isPublic();
  }

  private static boolean isGetEnumeratorMethod(MethodNameDeclaration decl) {
    return isVisibleForEnumeration(decl) && decl.getName().equalsIgnoreCase("GetEnumerator");
  }

  private static boolean hasMoveNextMethod(Set<MethodNameDeclaration> decls) {
    return decls.stream()
        .anyMatch(
            d ->
                isVisibleForEnumeration(d)
                    && d.getName().equalsIgnoreCase("MoveNext")
                    && d.getReturnType().isBoolean());
  }

  private static boolean hasCurrentProperty(Set<PropertyNameDeclaration> decls) {
    return decls.stream()
        .anyMatch(d -> isVisibleForEnumeration(d) && d.getName().equalsIgnoreCase("Current"));
  }

  private static boolean isIndexedAssignment(
      Node node, NameDeclaration enumerable, NameDeclaration index) {
    return isAssignmentTargetCompatible(node)
        && Optional.ofNullable(getAssignmentExpression(node))
            .filter(PrimaryExpressionNode.class::isInstance)
            .filter(expr -> expr.getChildren().size() == 2)
            .filter(expr -> isReferenceTo(expr.getChild(0), enumerable))
            .filter(expr -> isArrayAccessWithNameReference(expr, index))
            .isPresent();
  }

  private static boolean isArrayAccessWithNameReference(
      ExpressionNode assignmentExpr, NameDeclaration index) {
    return Optional.of(assignmentExpr.getChild(1))
        .filter(ArrayAccessorNode.class::isInstance)
        .map(IndexBasedEnumeratorLoopCheck::getOnlyChild)
        .filter(PrimaryExpressionNode.class::isInstance)
        .map(IndexBasedEnumeratorLoopCheck::getOnlyChild)
        .filter(NameReferenceNode.class::isInstance)
        .map(NameReferenceNode.class::cast)
        .map(NameReferenceNode::getNameDeclaration)
        .filter(decl -> decl.equals(index))
        .isPresent();
  }

  private static boolean isReferenceTo(DelphiNode enumerableReference, NameDeclaration enumerable) {
    return (enumerableReference instanceof NameReferenceNode)
        && Objects.equals(
            ((NameReferenceNode) enumerableReference).getNameDeclaration(), enumerable);
  }

  private static boolean isAssignmentTargetCompatible(Node node) {
    return (node instanceof VarStatementNode)
        || Optional.of(node)
            .filter(AssignmentStatementNode.class::isInstance)
            .map(n -> ((AssignmentStatementNode) n).getAssignee())
            .filter(PrimaryExpressionNode.class::isInstance)
            .map(IndexBasedEnumeratorLoopCheck::getOnlyChild)
            .filter(NameReferenceNode.class::isInstance)
            .map(NameReferenceNode.class::cast)
            .filter(n -> n.getNameDeclaration().getScope() instanceof MethodScope)
            .map(DelphiNode::getChildren)
            .filter(children -> children.size() == 1)
            .isPresent();
  }

  private static ExpressionNode getAssignmentExpression(Node node) {
    if (node instanceof VarStatementNode) {
      return ((VarStatementNode) node).getExpression();
    } else if (node instanceof AssignmentStatementNode) {
      return ((AssignmentStatementNode) node).getValue();
    }
    return null;
  }
}
