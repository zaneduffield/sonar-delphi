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

import java.util.Optional;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.communitydelphi.api.ast.ArrayAccessorNode;
import org.sonar.plugins.communitydelphi.api.ast.AssignmentStatementNode;
import org.sonar.plugins.communitydelphi.api.ast.BinaryExpressionNode;
import org.sonar.plugins.communitydelphi.api.ast.CommonDelphiNode;
import org.sonar.plugins.communitydelphi.api.ast.CompoundStatementNode;
import org.sonar.plugins.communitydelphi.api.ast.DelphiNode;
import org.sonar.plugins.communitydelphi.api.ast.ExpressionNode;
import org.sonar.plugins.communitydelphi.api.ast.ForLoopVarDeclarationNode;
import org.sonar.plugins.communitydelphi.api.ast.ForLoopVarReferenceNode;
import org.sonar.plugins.communitydelphi.api.ast.ForToStatementNode;
import org.sonar.plugins.communitydelphi.api.ast.IntegerLiteralNode;
import org.sonar.plugins.communitydelphi.api.ast.NameReferenceNode;
import org.sonar.plugins.communitydelphi.api.ast.Node;
import org.sonar.plugins.communitydelphi.api.ast.PrimaryExpressionNode;
import org.sonar.plugins.communitydelphi.api.ast.StatementListNode;
import org.sonar.plugins.communitydelphi.api.ast.VarStatementNode;
import org.sonar.plugins.communitydelphi.api.check.DelphiCheck;
import org.sonar.plugins.communitydelphi.api.check.DelphiCheckContext;
import org.sonar.plugins.communitydelphi.api.operator.BinaryOperator;
import org.sonar.plugins.communitydelphi.api.symbol.Invocable;
import org.sonar.plugins.communitydelphi.api.symbol.declaration.MethodNameDeclaration;
import org.sonar.plugins.communitydelphi.api.symbol.declaration.NameDeclaration;
import org.sonar.plugins.communitydelphi.api.symbol.declaration.PropertyNameDeclaration;
import org.sonar.plugins.communitydelphi.api.symbol.declaration.VariableNameDeclaration;
import org.sonar.plugins.communitydelphi.api.symbol.scope.DelphiScope;
import org.sonar.plugins.communitydelphi.api.symbol.scope.MethodScope;
import org.sonar.plugins.communitydelphi.api.token.DelphiTokenType;
import org.sonar.plugins.communitydelphi.api.type.Type;
import org.sonar.plugins.communitydelphi.api.type.Type.StructType;

@Rule(key = "IndexBasedEnumeratorLoop")
public class IndexBasedEnumeratorLoopCheck extends DelphiCheck {
  @Override
  public DelphiCheckContext visit(ForToStatementNode forNode, DelphiCheckContext context) {
    doVisit(forNode, context);
    return super.visit(forNode, context);
  }

  private void doVisit(ForToStatementNode forNode, DelphiCheckContext context) {
    if (forNode.getChildren().size() < 7) {
      return;
    }

    var forVar = forNode.getChild(0);
    var assign = forNode.getChild(1);
    var startValue = forNode.getChild(2);
    var to = forNode.getChild(3);
    var endValue = forNode.getChild(4);
    var loopBody = forNode.getChild(6);

    if (!isAssignOp(assign) || !isLiteralZero(startValue) || !isTo(to)) {
      return;
    }

    Optional<NameDeclaration> loopVarDecl = getLoopVariableDecl(forVar);
    Optional<NameDeclaration> enumerableDecl = isEnumerableVarCountMinusOne(endValue);
    if (loopVarDecl.isPresent()
        && enumerableDecl.isPresent()
        && isFirstStatementIndexedAssignment(loopBody, loopVarDecl.get(), enumerableDecl.get())
        && countReferencesInLoop(loopBody, loopVarDecl.get()) == 1) {
      reportIssue(context, forVar, "Enumerate this collection using a for-in loop.");
    }
  }

  private static Optional<NameDeclaration> getLoopVariableDecl(DelphiNode forVar) {
    if (forVar instanceof ForLoopVarReferenceNode) {
      return Optional.of(
          ((ForLoopVarReferenceNode) forVar).getNameReference().getNameDeclaration());
    } else if (forVar instanceof ForLoopVarDeclarationNode) {
      return Optional.of(
          ((ForLoopVarDeclarationNode) forVar).getNameDeclarationNode().getNameDeclaration());
    } else {
      return Optional.empty();
    }
  }

  private static long countReferencesInLoop(DelphiNode loopBody, NameDeclaration forVarDecl) {
    return loopBody.findDescendantsOfType(NameReferenceNode.class).stream()
        .filter(n -> n.getNameDeclaration().equals(forVarDecl))
        .count();
  }

  private static boolean isFirstStatementIndexedAssignment(
      DelphiNode loopBody, NameDeclaration forVarDecl, NameDeclaration enumerable) {
    return Optional.of(loopBody)
        .filter(CompoundStatementNode.class::isInstance)
        .map(node -> node.getFirstChildOfType(StatementListNode.class))
        .map(node -> node.getChild(0))
        .filter(node -> isIndexedAssignment(node, enumerable, forVarDecl))
        .isPresent();
  }

  private static Optional<NameDeclaration> isEnumerableVarCountMinusOne(DelphiNode endValue) {
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

  private static <T> T castOrNull(Object obj, Class<T> clazz) {
    return clazz.isInstance(obj) ? clazz.cast(obj) : null;
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
    return node instanceof PrimaryExpressionNode
        && Optional.ofNullable(getOnlyChild(node))
            .filter(IntegerLiteralNode.class::isInstance)
            .filter(n -> ((IntegerLiteralNode) n).getValueAsInt() == i)
            .isPresent();
  }

  private static boolean isTo(DelphiNode to) {
    return to instanceof CommonDelphiNode && to.getToken().getType() == DelphiTokenType.TO;
  }

  private static boolean isAssignOp(DelphiNode assign) {
    return assign instanceof CommonDelphiNode
        && assign.getToken().getType() == DelphiTokenType.ASSIGN;
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
        .filter(d -> d.getName().equalsIgnoreCase("GetEnumerator"))
        .findFirst()
        .map(Invocable::getReturnType)
        .filter(IndexBasedEnumeratorLoopCheck::isEnumeratorType)
        .isPresent();
  }

  private static boolean isEnumeratorType(Type type) {
    if (type.isSubTypeOf("System.IEnumerator")) {
      return true;
    }

    if (!(type instanceof StructType)) {
      return false;
    }
    DelphiScope delphiScope = ((StructType) type).typeScope();

    return hasCurrentProperty(delphiScope.getPropertyDeclarations())
        && hasMoveNextMethod(delphiScope.getMethodDeclarations())
        && hasResetMethod(delphiScope.getMethodDeclarations());
  }

  private static boolean hasResetMethod(Set<MethodNameDeclaration> decls) {
    return decls.stream()
        .anyMatch(d -> d.getName().equalsIgnoreCase("Reset") && d.getReturnType().isVoid());
  }

  private static boolean hasMoveNextMethod(Set<MethodNameDeclaration> decls) {
    return decls.stream()
        .anyMatch(d -> d.getName().equalsIgnoreCase("MoveNext") && d.getReturnType().isBoolean());
  }

  private static boolean hasCurrentProperty(Set<PropertyNameDeclaration> decls) {
    return decls.stream()
        .anyMatch(d -> d.getName().equalsIgnoreCase("Current") && !d.getReturnType().isVoid());
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
        .map(indexExpr -> ((PrimaryExpressionNode) indexExpr).extractSimpleNameReference())
        .filter(indexReference -> indexReference.getNameDeclaration().equals(index))
        .isPresent();
  }

  private static boolean isReferenceTo(DelphiNode enumerableReference, NameDeclaration enumerable) {
    return (enumerableReference instanceof NameReferenceNode)
        && ((NameReferenceNode) enumerableReference).getNameDeclaration().equals(enumerable);
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
