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
import org.sonar.plugins.communitydelphi.api.ast.BinaryExpressionNode;
import org.sonar.plugins.communitydelphi.api.ast.DelphiNode;
import org.sonar.plugins.communitydelphi.api.ast.ExpressionNode;
import org.sonar.plugins.communitydelphi.api.ast.ForToStatementNode;
import org.sonar.plugins.communitydelphi.api.ast.IntegerLiteralNode;
import org.sonar.plugins.communitydelphi.api.ast.NameReferenceNode;
import org.sonar.plugins.communitydelphi.api.ast.PrimaryExpressionNode;
import org.sonar.plugins.communitydelphi.api.ast.StatementNode;
import org.sonar.plugins.communitydelphi.api.ast.Visibility;
import org.sonar.plugins.communitydelphi.api.check.DelphiCheck;
import org.sonar.plugins.communitydelphi.api.check.DelphiCheckContext;
import org.sonar.plugins.communitydelphi.api.operator.BinaryOperator;
import org.sonar.plugins.communitydelphi.api.symbol.Invocable;
import org.sonar.plugins.communitydelphi.api.symbol.declaration.MethodNameDeclaration;
import org.sonar.plugins.communitydelphi.api.symbol.declaration.NameDeclaration;
import org.sonar.plugins.communitydelphi.api.symbol.declaration.PropertyNameDeclaration;
import org.sonar.plugins.communitydelphi.api.symbol.declaration.VariableNameDeclaration;
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
        && extractLeftSideOfSubByOne(forNode.getTargetExpression())
            .flatMap(IndexBasedEnumeratorLoopCheck::extractEnumerableFromDotCount)
            .map(NameReferenceNode::getNameDeclaration)
            .filter(
                enumerableDecl ->
                    allReferencesToLoopVarCompatibleWithEnumeration(
                        loopBody, loopVarDecl, enumerableDecl))
            .isPresent();
  }

  private static boolean allReferencesToLoopVarCompatibleWithEnumeration(
      StatementNode loopBody, NameDeclaration forVarDecl, NameDeclaration enumerable) {
    return loopBody.findDescendantsOfType(NameReferenceNode.class).stream()
        .filter(n -> Objects.equals(n.getNameDeclaration(), forVarDecl))
        .allMatch(n -> isSimpleIndexIntoEnumerable(n, enumerable, forVarDecl));
  }

  private static Optional<ExpressionNode> extractLeftSideOfSubByOne(DelphiNode endValue) {
    return Optional.ofNullable(endValue)
        .filter(BinaryExpressionNode.class::isInstance)
        .map(BinaryExpressionNode.class::cast)
        .filter(binary -> binary.getOperator() == BinaryOperator.SUBTRACT)
        .filter(binary -> isLiteralOne(binary.getRight()))
        .map(BinaryExpressionNode::getLeft);
  }

  private static Optional<NameReferenceNode> extractEnumerableFromDotCount(ExpressionNode node) {
    return Optional.ofNullable(node)
        .map(IndexBasedEnumeratorLoopCheck::getOnlyChild)
        .filter(NameReferenceNode.class::isInstance)
        .map(NameReferenceNode.class::cast)
        .filter(IndexBasedEnumeratorLoopCheck::isEnumerableDotCount);
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
    return Optional.ofNullable(node)
        .filter(ExpressionNode.class::isInstance)
        .map(ExpressionNode.class::cast)
        .map(ExpressionNode::skipParentheses)
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
    return Optional.ofNullable(type)
        .filter(StructType.class::isInstance)
        .map(StructType.class::cast)
        .filter(
            structType ->
                StructKind.CLASS == structType.kind()
                    || StructKind.INTERFACE == structType.kind()
                    || StructKind.RECORD == structType.kind()
                    || StructKind.OBJECT == structType.kind())
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

  private static boolean isSimpleIndexIntoEnumerable(
      DelphiNode node, NameDeclaration enumerableDecl, NameDeclaration indexDecl) {
    return Optional.ofNullable(node)
        .map(n -> n.getFirstParentOfType(ArrayAccessorNode.class))
        .map(DelphiNode::getParent)
        .filter(PrimaryExpressionNode.class::isInstance)
        .filter(e -> e.getChildren().size() == 2)
        .filter(e -> isReferenceTo(e.getChild(0), enumerableDecl))
        .filter(e -> isArrayAccessorWithReferenceToDecl(e.getChild(1), indexDecl))
        .isPresent();
  }

  private static boolean isArrayAccessorWithReferenceToDecl(
      DelphiNode expr, NameDeclaration indexDecl) {
    return Optional.ofNullable(expr)
        .filter(ArrayAccessorNode.class::isInstance)
        .map(IndexBasedEnumeratorLoopCheck::getOnlyChild)
        .filter(ExpressionNode.class::isInstance)
        .map(ExpressionNode.class::cast)
        .map(ExpressionNode::skipParentheses)
        .map(IndexBasedEnumeratorLoopCheck::getOnlyChild)
        .filter(NameReferenceNode.class::isInstance)
        .map(NameReferenceNode.class::cast)
        .map(NameReferenceNode::getNameDeclaration)
        .filter(decl -> decl.equals(indexDecl))
        .isPresent();
  }

  private static boolean isReferenceTo(DelphiNode reference, NameDeclaration decl) {
    return (reference instanceof NameReferenceNode)
        && Objects.equals(((NameReferenceNode) reference).getNameDeclaration(), decl);
  }
}
