/*
 * Sonar Delphi Plugin
 * Copyright (C) 2019-2022 Integrated Application Development
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
package au.com.integradev.delphi.antlr.ast.node;

import au.com.integradev.delphi.antlr.ast.visitors.DelphiParserVisitor;
import au.com.integradev.delphi.operator.UnaryOperator;
import au.com.integradev.delphi.symbol.resolve.ExpressionTypeResolver;
import javax.annotation.Nonnull;
import org.antlr.runtime.Token;
import org.sonar.plugins.communitydelphi.api.ast.ExpressionNode;
import org.sonar.plugins.communitydelphi.api.ast.UnaryExpressionNode;
import org.sonar.plugins.communitydelphi.api.type.Type;

public final class UnaryExpressionNodeImpl extends ExpressionNodeImpl
    implements UnaryExpressionNode {
  private UnaryOperator operator;
  private String image;

  public UnaryExpressionNodeImpl(Token token) {
    super(token);
  }

  @Override
  public <T> T accept(DelphiParserVisitor<T> visitor, T data) {
    return visitor.visit(this, data);
  }

  @Override
  public UnaryOperator getOperator() {
    if (operator == null) {
      operator = UnaryOperator.from(getTokenType());
    }
    return operator;
  }

  @Override
  public ExpressionNode getOperand() {
    return (ExpressionNode) getChild(0);
  }

  @Override
  public String getImage() {
    if (image == null) {
      image = getToken().getImage() + " " + getOperand().getImage();
    }
    return image;
  }

  @Override
  @Nonnull
  protected Type createType() {
    return new ExpressionTypeResolver(getTypeFactory()).resolve(this);
  }
}
