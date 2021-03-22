package org.sonar.plugins.delphi.operator;

import static org.sonar.plugins.delphi.type.DelphiType.untypedType;
import static org.sonar.plugins.delphi.type.intrinsic.IntrinsicArgumentMatcher.ANY_ORDINAL;
import static org.sonar.plugins.delphi.type.intrinsic.IntrinsicArgumentMatcher.ANY_SET;
import static org.sonar.plugins.delphi.type.intrinsic.IntrinsicArgumentMatcher.POINTER_MATH_OPERAND;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.plugins.delphi.symbol.declaration.MethodKind;
import org.sonar.plugins.delphi.symbol.resolve.Invocable;
import org.sonar.plugins.delphi.type.Type;
import org.sonar.plugins.delphi.type.Type.PointerType;
import org.sonar.plugins.delphi.type.Type.StructType;
import org.sonar.plugins.delphi.type.factory.TypeFactory;
import org.sonar.plugins.delphi.type.intrinsic.IntrinsicType;

public class OperatorInvocableCollector {
  private final TypeFactory typeFactory;

  public OperatorInvocableCollector(TypeFactory typeFactory) {
    this.typeFactory = typeFactory;
  }

  public Set<Invocable> collect(Type type, Operator operator) {
    if (operator instanceof BinaryOperator) {
      return collectBinary(type, (BinaryOperator) operator);
    }

    if (operator instanceof UnaryOperator) {
      return collectUnary(type, (UnaryOperator) operator);
    }

    throw new AssertionError("Unhandled Operator");
  }

  private Set<Invocable> collectBinary(Type type, BinaryOperator operator) {
    Set<Invocable> result = new HashSet<>();

    if (type.isStruct()) {
      result.addAll(collectOperatorOverloads((StructType) type, operator));
    } else if (type.isPointer()) {
      result.addAll(createPointerMath((PointerType) type, operator));
    } else if (type.isVariant() && operator != BinaryOperator.IN && operator != BinaryOperator.AS) {
      result.add(createVariantBinary(operator));
    }

    switch (operator) {
      case AND:
        result.addAll(createLogicalAndBitwise("And"));
        break;
      case OR:
        result.addAll(createLogicalAndBitwise("Or"));
        break;
      case XOR:
        result.addAll(createLogicalAndBitwise("Xor"));
        break;
      case EQUAL:
        result.add(createComparison("Equal"));
        break;
      case GREATER_THAN:
        result.add(createComparison("GreaterThan"));
        break;
      case LESS_THAN:
        result.add(createComparison("LessThan"));
        break;
      case GREATER_THAN_EQUAL:
        result.add(createComparison("GreaterThanEqual"));
        break;
      case LESS_THAN_EQUAL:
        result.add(createComparison("LessThanEqual"));
        break;
      case NOT_EQUAL:
        result.add(createComparison("NotEqual"));
        break;
      case IN:
        result.add(createIn());
        break;
      case ADD:
        result.addAll(createAdd());
        break;
      case SUBTRACT:
        result.addAll(createArithmeticAndSet("Subtract"));
        break;
      case MULTIPLY:
        result.addAll(createArithmeticAndSet("Multiply"));
        break;
      case DIVIDE:
        result.add(createDivide());
        break;
      case DIV:
        result.addAll(createIntegerBinary("IntDivide"));
        break;
      case MOD:
        result.addAll(createIntegerBinary("Modulus"));
        break;
      case SHL:
        result.addAll(createIntegerBinary("LeftShift"));
        break;
      case SHR:
        result.addAll(createIntegerBinary("RightShift"));
        break;
      default:
        // do nothing
    }

    return result;
  }

  private static Set<Invocable> addAll(Set<Invocable> collection, Collection<Invocable> other) {
    collection.addAll(other);
    return collection;
  }

  private static Set<Invocable> addAll(Set<Invocable> collection, Invocable... other) {
    return addAll(collection, Set.of(other));
  }

  private Set<Invocable> collectOperatorOverloads(StructType type, Operator operator) {
    return type.typeScope().getMethodDeclarations().stream()
        .filter(method -> method.getMethodKind() == MethodKind.OPERATOR)
        .filter(operator::isOverloadedByMethod)
        .collect(Collectors.toSet());
  }

  private Set<Invocable> createPointerMath(PointerType type, BinaryOperator operator) {
    if (!type.allowsPointerMath()) {
      return Collections.emptySet();
    }

    switch (operator) {
      case ADD:
        return createPointerMathAdd(type);
      case SUBTRACT:
        return createPointerMathSubtract(type);
      default:
        return Collections.emptySet();
    }
  }

  private Invocable createVariantBinary(BinaryOperator operator) {
    final String PREFIX = "Variant::";
    Type variant = typeFactory.getIntrinsic(IntrinsicType.VARIANT);
    List<Type> arguments = List.of(variant, variant);
    Type returnType;
    switch (operator) {
      case EQUAL:
      case NOT_EQUAL:
      case LESS_THAN:
      case LESS_THAN_EQUAL:
      case GREATER_THAN:
      case GREATER_THAN_EQUAL:
        returnType = typeFactory.getIntrinsic(IntrinsicType.BOOLEAN);
        break;
      default:
        returnType = variant;
    }
    return new OperatorIntrinsic(PREFIX + operator.name(), arguments, returnType);
  }

  private Set<Invocable> createPointerMathAdd(PointerType type) {
    final String NAME = "Add";
    Type integer = typeFactory.getIntrinsic(IntrinsicType.INTEGER);

    return Sets.newHashSet(
        new OperatorIntrinsic(NAME, List.of(type, integer), type),
        new OperatorIntrinsic(NAME, List.of(integer, type), type),
        new OperatorIntrinsic(NAME, List.of(type, POINTER_MATH_OPERAND), type));
  }

  private Set<Invocable> createPointerMathSubtract(PointerType type) {
    final String NAME = "Subtract";
    Type integer = typeFactory.getIntrinsic(IntrinsicType.INTEGER);

    return Sets.newHashSet(
        new OperatorIntrinsic(NAME, List.of(type, integer), type),
        new OperatorIntrinsic(NAME, List.of(type, POINTER_MATH_OPERAND), integer));
  }

  private Set<Invocable> createArithmeticBinary(String name) {
    Type integer = typeFactory.getIntrinsic(IntrinsicType.INTEGER);
    Type extended = typeFactory.getIntrinsic(IntrinsicType.EXTENDED);
    return addAll(
        createIntegerBinary(name),
        new OperatorIntrinsic(name, List.of(extended, extended), extended),
        new OperatorIntrinsic(name, List.of(integer, extended), extended),
        new OperatorIntrinsic(name, List.of(extended, integer), extended));
  }

  private Set<Invocable> createArithmeticAndSet(String name) {
    return addAll(createArithmeticBinary(name), createSet(name));
  }

  private Set<Invocable> createAdd() {
    final String NAME = "Add";
    Type string = typeFactory.getIntrinsic(IntrinsicType.STRING);
    return addAll(
        createArithmeticAndSet(NAME), new OperatorIntrinsic(NAME, List.of(string, string), string));
  }

  private Invocable createDivide() {
    Type extended = typeFactory.getIntrinsic(IntrinsicType.EXTENDED);
    return new OperatorIntrinsic("Divide", List.of(extended, extended), extended);
  }

  private Set<Invocable> createLogicalAndBitwise(String suffix) {
    return addAll(createIntegerBinary("Bitwise" + suffix), createLogical("Logical" + suffix));
  }

  private Invocable createLogical(String name) {
    Type bool = typeFactory.getIntrinsic(IntrinsicType.BOOLEAN);
    return new OperatorIntrinsic(name, List.of(bool, bool), bool);
  }

  private Invocable createComparison(String name) {
    Type bool = typeFactory.getIntrinsic(IntrinsicType.BOOLEAN);
    return new OperatorIntrinsic(name, List.of(untypedType(), untypedType()), bool);
  }

  private Invocable createSet(String name) {
    return new OperatorIntrinsic(name, List.of(ANY_SET, ANY_SET), typeFactory.emptySet());
  }

  private Invocable createIn() {
    Type bool = typeFactory.getIntrinsic(IntrinsicType.BOOLEAN);
    return new OperatorIntrinsic("In", List.of(ANY_ORDINAL, ANY_SET), bool);
  }

  private Set<Invocable> createIntegerBinary(String name) {
    Type integer = typeFactory.getIntrinsic(IntrinsicType.INTEGER);
    Type int64 = typeFactory.getIntrinsic(IntrinsicType.INT64);
    return Sets.newHashSet(
        new OperatorIntrinsic(name, List.of(integer, integer), integer),
        new OperatorIntrinsic(name, List.of(integer, int64), int64),
        new OperatorIntrinsic(name, List.of(int64, integer), int64),
        new OperatorIntrinsic(name, List.of(int64, int64), int64));
  }

  private Set<Invocable> collectUnary(Type type, UnaryOperator operator) {
    Set<Invocable> result = new HashSet<>();

    if (type.isStruct()) {
      result.addAll(collectOperatorOverloads((StructType) type, operator));
    } else if (type.isVariant()) {
      result.add(createVariantUnary(operator));
    }

    switch (operator) {
      case NOT:
        result.addAll(createNot());
        break;
      case PLUS:
        result.addAll(createArithmeticUnary("Positive"));
        break;
      case NEGATE:
        result.addAll(createArithmeticUnary("Negative"));
        break;
      default:
        // do nothing
    }

    return result;
  }

  private Set<Invocable> createNot() {
    Type bool = typeFactory.getIntrinsic(IntrinsicType.BOOLEAN);
    return addAll(
        createIntegerUnary("BitwiseNot"), new OperatorIntrinsic("LogicalNot", List.of(bool), bool));
  }

  private Set<Invocable> createArithmeticUnary(String name) {
    Type extended = typeFactory.getIntrinsic(IntrinsicType.EXTENDED);
    return addAll(
        createIntegerUnary(name), new OperatorIntrinsic(name, List.of(extended), extended));
  }

  private Set<Invocable> createIntegerUnary(String name) {
    Type integer = typeFactory.getIntrinsic(IntrinsicType.INTEGER);
    Type int64 = typeFactory.getIntrinsic(IntrinsicType.INT64);
    return Sets.newHashSet(
        new OperatorIntrinsic(name, List.of(integer), integer),
        new OperatorIntrinsic(name, List.of(int64), int64));
  }

  private Invocable createVariantUnary(UnaryOperator operator) {
    final String PREFIX = "Variant::";
    Type variant = typeFactory.getIntrinsic(IntrinsicType.VARIANT);
    return new OperatorIntrinsic(PREFIX + operator.name(), List.of(variant, variant), variant);
  }
}