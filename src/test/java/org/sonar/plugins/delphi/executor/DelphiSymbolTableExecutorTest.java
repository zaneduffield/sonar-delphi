package org.sonar.plugins.delphi.executor;

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.delphi.utils.DelphiUtils.uriToAbsolutePath;

import com.google.common.collect.Sets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultTextPointer;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.plugins.delphi.file.DelphiFile.DelphiInputFile;
import org.sonar.plugins.delphi.project.DelphiProjectHelper;
import org.sonar.plugins.delphi.symbol.SymbolTable;
import org.sonar.plugins.delphi.symbol.declaration.UnitNameDeclaration;
import org.sonar.plugins.delphi.utils.DelphiUtils;
import org.sonar.plugins.delphi.utils.builders.DelphiTestFileBuilder;

public class DelphiSymbolTableExecutorTest {
  private static final String ROOT_PATH = "/org/sonar/plugins/delphi/symbol/";
  private static final String STANDARD_LIBRARY = "/org/sonar/plugins/delphi/standardLibrary";

  private DelphiInputFile mainFile;
  private SymbolTable symbolTable;
  private DelphiSymbolTableExecutor executor;
  private SensorContextTester context;
  private Set<String> unitScopeNames;
  private Map<String, String> unitAliases;
  private String componentKey;

  @Before
  public void setup() {
    executor = new DelphiSymbolTableExecutor();
    context = SensorContextTester.create(DelphiUtils.getResource(ROOT_PATH));
    unitScopeNames = new HashSet<>();
    unitAliases = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  }

  @Test
  public void testSimpleFile() {
    execute("Simple.pas");
    verifyUsages(7, 2, reference(20, 10), reference(29, 10), reference(34, 10));
    verifyUsages(12, 3);
    verifyUsages(20, 2);
    verifyUsages(9, 14, reference(29, 22), reference(36, 1));
    verifyUsages(10, 14, reference(31, 1), reference(34, 22));
  }

  @Test
  public void testSimilarParameterDeclarations() {
    execute("SimilarParameterDeclarations.pas");
    verifyUsages(6, 2, reference(14, 10), reference(19, 10));
    verifyUsages(8, 14, reference(14, 15));
    verifyUsages(8, 19);
    verifyUsages(9, 14, reference(19, 15), reference(16, 2));
    verifyUsages(9, 19);
  }

  @Test
  public void testRecords() {
    execute("Records.pas");
    verifyUsages(6, 2, reference(16, 21));
    verifyUsages(7, 4, reference(31, 30));
    verifyUsages(10, 2, reference(24, 11), reference(29, 11));
    verifyUsages(12, 4, reference(26, 14));
    verifyUsages(13, 6, reference(26, 31));
    verifyUsages(16, 4, reference(31, 14));
    verifyUsages(18, 13, reference(24, 16));
    verifyUsages(19, 13, reference(29, 16));
  }

  @Test
  public void testRecordVariants() {
    execute("RecordVariants.pas");
    verifyUsages(14, 10, reference(33, 2));
    verifyUsages(19, 10, reference(34, 2));
    verifyUsages(24, 10, reference(35, 2));
  }

  @Test
  public void testInheritedInvocations() {
    execute("InheritedInvocations.pas");
    verifyUsages(7, 14, reference(38, 14), reference(50, 14));
    verifyUsages(
        12,
        13,
        reference(25, 16),
        reference(37, 4),
        reference(38, 4),
        reference(39, 4),
        reference(40, 4),
        reference(40, 14),
        reference(41, 4),
        reference(52, 14),
        reference(55, 14),
        reference(62, 19),
        reference(63, 14));
    verifyUsages(
        13,
        13,
        reference(30, 16),
        reference(39, 14),
        reference(41, 14),
        reference(42, 14),
        reference(49, 4),
        reference(50, 4),
        reference(51, 4),
        reference(51, 14),
        reference(52, 4),
        reference(53, 4),
        reference(53, 14),
        reference(54, 14),
        reference(62, 14),
        reference(63, 30));
    verifyUsages(18, 13, reference(35, 16), reference(56, 4));
    verifyUsages(19, 13, reference(44, 4), reference(47, 16));
    verifyUsages(20, 13, reference(57, 4), reference(60, 16));
  }

  @Test
  public void testNestedMethods() {
    execute("NestedMethods.pas");
    verifyUsages(6, 9);
    verifyUsages(8, 11, reference(27, 12));
    verifyUsages(8, 15, reference(10, 7));
    verifyUsages(17, 11, reference(27, 16));
    verifyUsages(17, 15, reference(19, 7));
  }

  @Test
  public void testNestedTypes() {
    execute("NestedTypes.pas");
    verifyUsages(7, 8, reference(20, 10), reference(21, 5));
    verifyUsages(8, 26, reference(23, 4), reference(24, 4));
  }

  @Test
  public void testTypeAliasParameter() {
    execute("TypeAliasParameter.pas");
    verifyUsages(6, 2, reference(9, 14), reference(13, 25));
    verifyUsages(9, 2, reference(18, 31));
    verifyUsages(11, 2, reference(18, 10));
    verifyUsages(13, 14, reference(18, 15));
  }

  @Test
  public void testArrays() {
    execute("Arrays.pas");
    verifyUsages(
        8,
        14,
        reference(30, 15),
        reference(40, 17),
        reference(41, 30),
        reference(42, 30),
        reference(43, 23),
        reference(44, 25),
        reference(45, 9),
        reference(46, 46),
        reference(47, 14));
    verifyUsages(15, 13, reference(46, 27));
    verifyUsages(24, 13, reference(44, 6));
    verifyUsages(25, 13, reference(46, 6));
    verifyUsages(37, 2, reference(40, 2));
    verifyUsages(38, 2, reference(41, 2), reference(42, 2));
  }

  @Test
  public void testArrayArgument() {
    execute("ArrayArgument.pas");
    verifyUsages(7, 10, reference(14, 2));
    verifyUsages(12, 14, reference(14, 6));
  }

  @Test
  public void testArrayConstructor() {
    execute("ArrayConstructor.pas");
    verifyUsages(10, 10, reference(17, 2), reference(27, 2));
    verifyUsages(20, 9, reference(28, 4), reference(29, 4), reference(30, 4));
  }

  @Test
  public void testArrayOfConst() {
    execute("ArrayOfConst.pas");
    verifyUsages(7, 10, reference(14, 2), reference(15, 2), reference(16, 2));
    verifyUsages(12, 14, reference(15, 7), reference(16, 7));
  }

  @Test
  public void testAnonymousMethods() {
    execute("AnonymousMethods.pas");
    verifyUsages(6, 2, reference(11, 20), reference(26, 20));
    verifyUsages(7, 2, reference(16, 20), reference(26, 47));
    verifyUsages(11, 10, reference(30, 2), reference(31, 2));
    verifyUsages(16, 10, reference(36, 2), reference(37, 2));
    verifyUsages(21, 10, reference(39, 4));
    verifyUsages(28, 2, reference(33, 13));
    verifyUsages(31, 16, reference(33, 4));
  }

  @Test
  public void testUsesDeclarations() {
    execute("UsesDeclarations.pas");
    verifyUsages(1, 5, reference(23, 7));
    verifyUsages(15, 11, reference(22, 2), reference(23, 2));
    verifyUsages(6, 2, reference(24, 2));
    verifyUsages(13, 2, reference(25, 2));
  }

  @Test
  public void testResultTypes() {
    execute("ResultTypes.pas");
    verifyUsages(
        6,
        2,
        reference(15, 10),
        reference(20, 19),
        reference(25, 54),
        reference(27, 12),
        reference(34, 12),
        reference(40, 44),
        reference(42, 12));
    verifyUsages(8, 14, reference(15, 15), reference(28, 9), reference(35, 9), reference(43, 9));
    verifyUsages(20, 10, reference(29, 2), reference(36, 2), reference(44, 2));
  }

  @Test
  public void testPascalResultAssignments() {
    execute("PascalResultAssignments.pas");
    verifyUsages(5, 9, reference(11, 9), reference(13, 2));
    verifyUsages(6, 9, reference(16, 9), reference(21, 4), reference(25, 2));
    verifyUsages(7, 9, reference(28, 9));
    verifyUsages(30, 2, reference(32, 2));
  }

  @Test
  public void testSelfTypes() {
    execute("SelfTypes.pas");
    verifyUsages(17, 10, reference(34, 2));
    verifyUsages(22, 10, reference(39, 2));
    verifyUsages(27, 10, reference(46, 2));
  }

  @Test
  public void testInitializationFinalization() {
    execute("InitializationFinalization.pas");
    verifyUsages(6, 2, reference(14, 7), reference(17, 9));
    verifyUsages(8, 14, reference(18, 6));
    verifyUsages(14, 2, reference(20, 2));
  }

  @Test
  public void testRecordExpressionItems() {
    execute("RecordExpressionItems.pas");
    verifyUsages(14, 10, reference(21, 11));
  }

  @Test
  public void testHardTypeCast() {
    execute("HardTypeCast.pas");
    verifyUsages(8, 4, reference(17, 12));
    verifyUsages(15, 18, reference(17, 44));
  }

  @Test
  public void testHandlerProperty() {
    execute("HandlerProperty.pas");
    verifyUsages(10, 4, reference(21, 2));
    verifyUsages(19, 19, reference(21, 22));
  }

  @Test
  public void testWithStatement() {
    execute("WithStatement.pas");
    verifyUsages(7, 4, reference(42, 4));
    verifyUsages(12, 4, reference(24, 4));
    verifyUsages(8, 4, reference(25, 4));
    verifyUsages(21, 2, reference(26, 4));
  }

  @Test
  public void testForStatement() {
    execute("ForStatement.pas");
    verifyUsages(9, 2, reference(11, 6), reference(15, 6), reference(19, 6));
  }

  @Test
  public void testBareInterfaceMethodReference() {
    execute("BareInterfaceMethodReference.pas");
    verifyUsages(5, 9, reference(15, 9));
    verifyUsages(6, 9, reference(20, 9));
    verifyUsages(10, 10, reference(17, 2));
  }

  @Test
  public void testClassReferenceMethodResolution() {
    execute("classReferences/MethodResolution.pas");
    verifyUsages(9, 14, reference(18, 6));
  }

  @Test
  public void testClassReferenceArgumentResolution() {
    execute("classReferences/ArgumentResolution.pas");
    verifyUsages(18, 10, reference(25, 2), reference(26, 2));
  }

  @Test
  public void testClassReferenceConstructorTypeResolution() {
    execute("classReferences/ConstructorTypeResolution.pas");
    verifyUsages(15, 10, reference(22, 2));
    verifyUsages(8, 16, reference(22, 11));
  }

  @Test
  public void testSimpleForwardDeclarations() {
    execute("forwardDeclarations/Simple.pas");
    verifyUsages(22, 26, reference(24, 14));
  }

  @Test
  public void testInheritanceForwardDeclarations() {
    execute("forwardDeclarations/Inheritance.pas");
    verifyUsages(27, 10, reference(34, 2), reference(35, 2));
    verifyUsages(14, 15, reference(34, 11));
    verifyUsages(32, 26, reference(35, 7));
  }

  @Test
  public void testImplicitForwardDeclarations() {
    execute("forwardDeclarations/ImplicitForwarding.pas");
    verifyUsages(9, 2, reference(6, 17), reference(7, 22));
  }

  @Test
  public void testTypeSignaturesOfForwardDeclaration() {
    execute("forwardDeclarations/TypeSignature.pas");
    verifyUsages(19, 12, reference(24, 10), reference(37, 2));
    verifyUsages(20, 12, reference(29, 10), reference(40, 2));
  }

  @Test
  public void testSimpleTypeResolution() {
    execute("typeResolution/Simple.pas");
    verifyUsages(6, 2, reference(14, 10), reference(16, 21), reference(25, 2), reference(29, 22));
    verifyUsages(8, 16, reference(25, 7), reference(26, 9));
    verifyUsages(9, 14, reference(23, 7), reference(24, 9), reference(25, 14), reference(26, 16));
    verifyUsages(12, 2, reference(21, 10), reference(29, 9));
    verifyUsages(14, 4, reference(23, 2), reference(31, 12));
    verifyUsages(15, 14, reference(21, 15));
    verifyUsages(16, 13, reference(24, 2), reference(26, 2), reference(29, 14));
  }

  @Test
  public void testCharTypeResolution() {
    execute("typeResolution/Chars.pas");
    verifyUsages(7, 9, reference(22, 2));
    verifyUsages(12, 9, reference(23, 2));
  }

  @Test
  public void testCastTypeResolution() {
    execute("typeResolution/Casts.pas");
    verifyUsages(8, 14, reference(15, 12), reference(16, 16));
  }

  @Test
  public void testConstructorTypeResolution() {
    execute("typeResolution/Constructors.pas");
    verifyUsages(8, 14, reference(23, 14));
    verifyUsages(13, 14, reference(24, 14));
  }

  @Test
  public void testEnumsTypeResolution() {
    execute("typeResolution/Enums.pas");
    verifyUsages(18, 9, reference(28, 2), reference(29, 2), reference(30, 2));
  }

  @Test
  public void testPointersTypeResolution() {
    execute("typeResolution/Pointers.pas");
    verifyUsages(7, 10, reference(16, 2), reference(17, 2), reference(18, 2), reference(19, 2));
  }

  @Test
  public void testSubRangeHostTypeResolution() {
    execute("typeResolution/SubRangeHostType.pas");
    verifyUsages(12, 10, reference(19, 2), reference(20, 2), reference(21, 2));
  }

  @Test
  public void testLowHighIntrinsics() {
    execute("intrinsics/LowHighIntrinsics.pas");
    verifyUsages(
        12,
        10,
        reference(29, 2),
        reference(30, 2),
        reference(31, 2),
        reference(32, 2),
        reference(33, 2),
        reference(34, 2),
        reference(35, 2),
        reference(36, 2));
    verifyUsages(17, 10, reference(37, 2), reference(38, 2));
    verifyUsages(22, 10, reference(39, 2), reference(40, 2));
  }

  @Test
  public void testDefaultIntrinsic() {
    execute("intrinsics/DefaultIntrinsic.pas");
    verifyUsages(7, 10, reference(29, 2));
    verifyUsages(12, 10, reference(30, 2));
    verifyUsages(17, 10, reference(31, 2));
    verifyUsages(22, 10, reference(32, 2));
  }

  @Test
  public void testBinaryOperatorIntrinsics() {
    execute("operators/BinaryOperatorIntrinsics.pas");
    verifyUsages(
        10,
        10,
        reference(46, 2),
        reference(47, 2),
        reference(48, 2),
        reference(51, 2),
        reference(52, 2),
        reference(53, 2),
        reference(54, 2),
        reference(55, 2),
        reference(56, 2),
        reference(57, 2),
        reference(115, 2));
    verifyUsages(
        15,
        10,
        reference(60, 2),
        reference(61, 2),
        reference(62, 2),
        reference(63, 2),
        reference(64, 2),
        reference(77, 2),
        reference(78, 2),
        reference(79, 2),
        reference(80, 2),
        reference(81, 2));
    verifyUsages(
        20,
        10,
        reference(65, 2),
        reference(66, 2),
        reference(67, 2),
        reference(68, 2),
        reference(69, 2),
        reference(70, 2),
        reference(71, 2),
        reference(72, 2),
        reference(73, 2),
        reference(74, 2),
        reference(82, 2),
        reference(83, 2),
        reference(84, 2),
        reference(85, 2),
        reference(86, 2),
        reference(87, 2),
        reference(88, 2),
        reference(89, 2),
        reference(90, 2),
        reference(91, 2));
    verifyUsages(
        25,
        10,
        reference(92, 2),
        reference(93, 2),
        reference(94, 2),
        reference(95, 2),
        reference(96, 2),
        reference(97, 2),
        reference(98, 2),
        reference(99, 2),
        reference(100, 2),
        reference(101, 2),
        reference(102, 2),
        reference(103, 2),
        reference(104, 2),
        reference(105, 2),
        reference(106, 2),
        reference(107, 2),
        reference(108, 2));
    verifyUsages(30, 10, reference(111, 2), reference(112, 2));
    verifyUsages(35, 10, reference(116, 2), reference(117, 2), reference(118, 2));
  }

  @Test
  public void testBinaryOperatorOverloads() {
    execute("operators/BinaryOperatorOverloads.pas");
    verifyUsages(64, 10, reference(77, 2), reference(80, 2), reference(83, 2), reference(86, 2));
    verifyUsages(69, 10, reference(89, 2), reference(92, 2));
  }

  @Test
  public void testUnaryOperatorIntrinsics() {
    execute("operators/UnaryOperatorIntrinsics.pas");
    verifyUsages(10, 10, reference(40, 2));
    verifyUsages(15, 10, reference(43, 2), reference(47, 2), reference(48, 2));
    verifyUsages(20, 10, reference(44, 2), reference(49, 2), reference(50, 2));
    verifyUsages(25, 10, reference(51, 2), reference(52, 2));
  }

  @Test
  public void testUnaryOperatorOverloads() {
    execute("operators/UnaryOperatorOverloads.pas");
    verifyUsages(32, 10, reference(49, 2));
    verifyUsages(37, 10, reference(50, 2));
    verifyUsages(42, 10, reference(51, 2));
  }

  @Test
  public void testImplicitOperator() {
    execute("operators/ImplicitOperator.pas");
    verifyUsages(23, 10, reference(35, 2));
  }

  @Test
  public void testImplicitOperatorShouldHaveLowestPriority() {
    execute("operators/ImplicitOperatorLowestPriority.pas");
    verifyUsages(33, 10, reference(40, 2));
  }

  @Test
  public void testOperatorsAreNotCallable() {
    execute("operators/NotCallable.pas");
    verifyUsages(7, 20, reference(15, 7));
  }

  @Test
  public void testPointerMathOperators() {
    execute("operators/PointerMath.pas");
    verifyUsages(
        15,
        10,
        reference(49, 2),
        reference(60, 2),
        reference(61, 2),
        reference(72, 2),
        reference(73, 2),
        reference(50, 2),
        reference(51, 2),
        reference(62, 2),
        reference(63, 2),
        reference(74, 2),
        reference(75, 2));
    verifyUsages(
        20,
        10,
        reference(42, 2),
        reference(43, 2),
        reference(44, 2),
        reference(45, 2),
        reference(46, 2),
        reference(47, 2),
        reference(48, 2));
    verifyUsages(
        25,
        10,
        reference(53, 2),
        reference(54, 2),
        reference(55, 2),
        reference(56, 2),
        reference(57, 2),
        reference(58, 2),
        reference(59, 2));
    verifyUsages(
        30,
        10,
        reference(65, 2),
        reference(66, 2),
        reference(67, 2),
        reference(68, 2),
        reference(69, 2),
        reference(70, 2),
        reference(71, 2));
  }

  @Test
  public void testVariantOperators() {
    execute("operators/VariantOperators.pas");
    verifyUsages(
        8,
        10,
        reference(41, 2),
        reference(42, 2),
        reference(43, 2),
        reference(44, 2),
        reference(45, 2),
        reference(46, 2),
        reference(47, 2),
        reference(48, 2),
        reference(49, 2),
        reference(50, 2),
        reference(51, 2),
        reference(52, 2),
        reference(53, 2),
        reference(54, 2),
        reference(55, 2),
        reference(56, 2),
        reference(57, 2),
        reference(58, 2),
        reference(59, 2),
        reference(60, 2),
        reference(61, 2),
        reference(62, 2),
        reference(63, 2),
        reference(64, 2),
        reference(65, 2),
        reference(66, 2),
        reference(67, 2),
        reference(68, 2));
    verifyUsages(23, 10, reference(70, 2), reference(71, 2));
    verifyUsages(
        28,
        10,
        reference(73, 2),
        reference(74, 2),
        reference(75, 2),
        reference(76, 2),
        reference(77, 2),
        reference(78, 2),
        reference(79, 2),
        reference(80, 2),
        reference(81, 2),
        reference(82, 2),
        reference(83, 2),
        reference(84, 2));
  }

  @Test
  public void testSimpleProperties() {
    execute("properties/Simple.pas");
    verifyUsages(
        6,
        2,
        reference(14, 10),
        reference(16, 26),
        reference(17, 21),
        reference(18, 24),
        reference(19, 23),
        reference(20, 26),
        reference(25, 27),
        reference(30, 22),
        reference(37, 7),
        reference(39, 19),
        reference(42, 18),
        reference(45, 21));
    verifyUsages(8, 16, reference(39, 24), reference(42, 23), reference(45, 26));
    verifyUsages(9, 14, reference(40, 16), reference(43, 15), reference(46, 18));
    verifyUsages(12, 2, reference(25, 10), reference(30, 9), reference(35, 20));
    verifyUsages(
        14,
        4,
        reference(19, 33),
        reference(19, 44),
        reference(20, 36),
        reference(20, 47),
        reference(27, 2),
        reference(32, 12));
    verifyUsages(16, 14, reference(18, 47), reference(25, 15));
    verifyUsages(17, 13, reference(18, 34), reference(30, 14));
    verifyUsages(18, 13, reference(39, 6), reference(40, 6));
    verifyUsages(19, 13, reference(42, 6), reference(43, 6));
    verifyUsages(20, 13, reference(45, 6), reference(46, 6));
  }

  @Test
  public void testOverrideProperties() {
    execute("properties/OverrideProperties.pas");
    verifyUsages(8, 14, reference(29, 10), reference(30, 10), reference(31, 13), reference(32, 13));
    verifyUsages(16, 13, reference(29, 6), reference(31, 6));
    verifyUsages(21, 13, reference(30, 6), reference(32, 6));
  }

  @Test
  public void testProceduralProperties() {
    execute("properties/ProceduralProperties.pas");
    verifyUsages(12, 13, reference(19, 6));
    verifyUsages(17, 26, reference(19, 14));
  }

  @Test
  public void testHiddenDefaultProperties() {
    execute("properties/HiddenDefaultProperties.pas");
    verifyUsages(11, 14, reference(27, 25));
  }

  @Test
  public void testSimpleOverloads() {
    execute("overloads/Simple.pas");
    verifyUsages(8, 10, reference(14, 10), reference(35, 2));
    verifyUsages(9, 10, reference(19, 10), reference(36, 2));
    verifyUsages(10, 10, reference(24, 10), reference(37, 2), reference(38, 2));
  }

  @Test
  public void testTypeTypeOverloads() {
    execute("overloads/TypeType.pas");
    verifyUsages(6, 2, reference(15, 19), reference(23, 10));
    verifyUsages(10, 10, reference(25, 2));
    verifyUsages(15, 10, reference(26, 2));
  }

  @Test
  public void testNestedExpressions() {
    execute("overloads/NestedExpressions.pas");
    verifyUsages(7, 2, reference(21, 9), reference(38, 12), reference(41, 14));
    verifyUsages(8, 13, reference(21, 25), reference(42, 15));
    verifyUsages(11, 10, reference(26, 10), reference(42, 2));
    verifyUsages(12, 10, reference(31, 10), reference(43, 2), reference(44, 2));
    verifyUsages(16, 9, reference(43, 6), reference(44, 9));
  }

  @Test
  public void testAmbiguousMethodReferences() {
    execute("overloads/AmbiguousMethodReferences.pas");
    verifyUsages(6, 2, reference(8, 19), reference(18, 19), reference(30, 11));
    verifyUsages(8, 10, reference(18, 10), reference(33, 2), reference(34, 2));
    verifyUsages(9, 10, reference(23, 10), reference(35, 2), reference(36, 2));
    verifyUsages(13, 9, reference(34, 6));
  }

  @Test
  public void testProceduralVariables() {
    execute("overloads/ProceduralVariables.pas");
    verifyUsages(5, 10, reference(10, 10), reference(25, 20));
    verifyUsages(6, 10, reference(15, 10), reference(26, 19));
    verifyUsages(22, 2, reference(25, 2), reference(28, 2));
    verifyUsages(23, 2, reference(26, 2), reference(29, 2));
  }

  @Test
  public void testCharInSet() {
    execute("overloads/CharInSet.pas");
    verifyUsages(13, 13, reference(25, 22));
    verifyUsages(18, 10, reference(25, 12));
    verifyUsages(23, 19, reference(25, 36));
  }

  @Test
  public void testImportedOverloads() {
    execute(
        "overloads/Imports.pas",
        "overloads/imports/IntegerFoo.pas",
        "overloads/imports/StringFoo.pas");
    verifyUsages(13, 10, reference(30, 2), reference(31, 2));
    verifyUsages(24, 2, reference(28, 6));
    verifyUsages(25, 2, reference(29, 6));
    verifyUsages(26, 2, reference(30, 6));
  }

  @Test
  public void testDisambiguationOfOverloadsByDistanceFromCallSite() {
    execute("overloads/Distance.pas", "overloads/imports/DistantFoo.pas");
    verifyUsages(8, 10, reference(53, 2));
    verifyUsages(12, 16, reference(56, 14));
    verifyUsages(13, 14, reference(58, 6), reference(64, 2));
    verifyUsages(17, 16, reference(55, 14));
    verifyUsages(18, 14, reference(57, 6), reference(63, 2));
    verifyUsages(49, 15, reference(54, 6), reference(56, 21), reference(58, 10));
  }

  @Test
  public void testOverriddenOverloads() {
    execute("overloads/Overrides.pas", "overloads/imports/BaseFoo.pas");
    verifyUsages(31, 2, reference(35, 10));
    verifyUsages(32, 2, reference(36, 10));
    verifyUsages(33, 2, reference(37, 10));
  }

  @Test
  public void testRegularMethodPreferredOverImplicitSpecializations() {
    execute("generics/RegularMethodPreferredOverImplicitSpecialization.pas");
    verifyUsages(10, 20, reference(10, 26));
    verifyUsages(10, 15, reference(20, 2));
    verifyUsages(11, 15, reference(19, 2));
  }

  @Test
  public void testGenericArrayAssignmentCompatibility() {
    execute("generics/ArrayAssignmentCompatibility.pas");
    verifyUsages(12, 15, reference(25, 2));
    verifyUsages(13, 15, reference(26, 2));
    verifyUsages(14, 15, reference(27, 2));
    verifyUsages(15, 15, reference(28, 2));
  }

  @Test
  public void testStructAssignmentCompatibility() {
    execute("generics/StructAssignmentCompatibility.pas");
    verifyUsages(14, 15, reference(27, 2));
    verifyUsages(15, 15, reference(28, 2));
    verifyUsages(16, 15, reference(29, 2));
    verifyUsages(17, 15, reference(30, 2));
  }

  @Test
  public void testGenericMethodInterfaceNameResolution() {
    execute("generics/MethodInterfaceNameResolution.pas");
    verifyUsages(6, 2, reference(16, 10));
    verifyUsages(7, 15, reference(16, 18));
    verifyUsages(10, 2, reference(21, 10));
    verifyUsages(11, 15, reference(21, 15));
  }

  @Test
  public void testGenericSameNameTypes() {
    execute("generics/SameNameType.pas");
    verifyUsages(6, 2, reference(18, 15));
    verifyUsages(7, 2, reference(19, 16));
    verifyUsages(8, 2, reference(20, 18));
  }

  @Test
  public void testGenericParameterizedMethods() {
    execute("generics/ParameterizedMethod.pas");
    verifyUsages(
        11,
        14,
        reference(16, 15),
        reference(29, 2),
        reference(30, 2),
        reference(31, 2),
        reference(32, 2));
  }

  @Test
  public void testGenericImplicitSpecializations() {
    execute("generics/ImplicitSpecialization.pas");
    verifyUsages(14, 14, reference(19, 20), reference(26, 2), reference(27, 2), reference(28, 2));
  }

  @Test
  public void testGenericConstraints() {
    execute("generics/Constraint.pas");
    verifyUsages(11, 14, reference(20, 25), reference(53, 8));
  }

  @Test
  public void testGenericTypeParameterNameDeclarations() {
    execute("generics/TypeParameterNameDeclaration.pas");
    verifyUsages(
        6,
        7,
        reference(8, 22),
        reference(9, 19),
        reference(10, 18),
        reference(13, 49),
        reference(13, 53),
        reference(15, 45),
        reference(17, 31),
        reference(22, 14),
        reference(22, 35),
        reference(22, 39));
    verifyUsages(12, 13, reference(12, 27));
    verifyUsages(13, 22, reference(13, 41));
    verifyUsages(14, 11, reference(15, 31), reference(15, 54), reference(15, 58));
  }

  @Test
  public void testGenericTypeParameterConflicts() {
    execute("generics/TypeParameterNameConflict.pas");
    verifyUsages(7, 14, reference(25, 8));
    verifyUsages(11, 14, reference(26, 6), reference(27, 9));
    verifyUsages(14, 11, reference(15, 11), reference(21, 19));
    verifyUsages(
        16, 19, reference(16, 33), reference(21, 27), reference(21, 35), reference(23, 10));
  }

  @Test
  public void testPropertySpecialization() {
    execute("generics/PropertySpecialization.pas");
    verifyUsages(13, 10, reference(20, 2));
  }

  @Test
  public void testSimpleMethodResolutionClause() {
    execute("methodResolutionClauses/Simple.pas");
    verifyUsages(7, 14, reference(12, 26));
    verifyUsages(11, 14, reference(12, 42));
  }

  @Test
  public void testMethodResolutionClauseWithOverloadedImplementation() {
    execute("methodResolutionClauses/OverloadedImplementation.pas");
    verifyUsages(7, 14, reference(13, 26));
    verifyUsages(11, 14, reference(13, 42));
    verifyUsages(12, 14);
  }

  @Test
  public void testMethodResolutionClauseWithOverloadedInterfaceAndImplementation() {
    execute("methodResolutionClauses/OverloadedInterfaceAndImplementation.pas");
    verifyUsages(7, 14, reference(14, 26));
    verifyUsages(12, 14, reference(14, 42));
    verifyUsages(13, 14);
  }

  @Test
  public void testImports() {
    execute(
        "imports/source/Unit1.pas",
        "imports/Unit2.pas",
        "imports/source/Unit3.pas",
        "imports/ignored/Unit2.pas",
        "imports/ignored/Unit3.pas");
    verifyUsages(1, 5, reference(23, 2), reference(26, 18));
    verifyUsages(6, 2, reference(26, 2));
    verifyUsages(9, 2, reference(26, 24), reference(27, 12));
    verifyUsages(14, 2, reference(23, 18));
    verifyUsages(16, 10, reference(23, 8), reference(24, 2));
  }

  @Test
  public void testNamespaces() {
    execute(
        "namespaces/Namespaced.Unit1.pas",
        "namespaces/Unit1.pas",
        "namespaces/Namespaced.Unit2.pas",
        "namespaces/Unit3.pas",
        "namespaces/UnitScopeName.Unit2.pas",
        "namespaces/UnitScopeName.ScopedUnit3.pas");

    verifyUsages(1, 5, reference(23, 2), reference(26, 18));
    verifyUsages(6, 2, reference(30, 2));
    verifyUsages(6, 9, reference(26, 2));
    verifyUsages(9, 2, reference(26, 35), reference(27, 12), reference(29, 23), reference(30, 29));
    verifyUsages(14, 2, reference(23, 29));
    verifyUsages(16, 10, reference(23, 19), reference(24, 2));
  }

  @Test
  public void testUnitScopeNames() {
    unitScopeNames = Set.of("NonexistentUnitScope", "UnitScopeName", "ABCUnitScopeXYZ");

    execute(
        "namespaces/UnitScopeNameTest.pas",
        "namespaces/UnitScopeName.Unit2.pas",
        "namespaces/UnitScopeName.ScopedUnit3.pas",
        "namespaces/Namespaced.Unit1.pas",
        "namespaces/Namespaced.Unit2.pas",
        "namespaces/Unit3.pas");

    verifyUsages(1, 5, reference(23, 2), reference(26, 30));
    verifyUsages(6, 2, reference(26, 2));
    verifyUsages(9, 2, reference(26, 48), reference(27, 18));
    verifyUsages(14, 2, reference(23, 30));
    verifyUsages(16, 10, reference(23, 20), reference(24, 2));
  }

  @Test
  public void testUnitAliases() {
    unitAliases.put("UnitX", "Unit2");
    unitAliases.put("UnitY", "Unit3");

    execute("unitAliases/Unit1.pas", "unitAliases/Unit2.pas", "unitAliases/Unit3.pas");

    verifyUsages(1, 5, reference(23, 2), reference(26, 18));
    verifyUsages(6, 2, reference(26, 2));
    verifyUsages(9, 2, reference(26, 24), reference(27, 12));
    verifyUsages(14, 2, reference(23, 18));
    verifyUsages(16, 10, reference(23, 8), reference(24, 2));
  }

  @Test
  public void testUnscopedEnums() {
    execute("enums/UnscopedEnum.pas");
    verifyUsages(6, 2, reference(10, 19));
    verifyUsages(6, 9, reference(12, 11));
  }

  @Test
  public void testNestedUnscopedEnums() {
    execute("enums/NestedUnscopedEnum.pas");
    verifyUsages(8, 12, reference(15, 9));
    verifyUsages(20, 16, reference(24, 9));
    verifyUsages(29, 8, reference(31, 9));
  }

  @Test
  public void testScopedEnums() {
    execute("enums/ScopedEnum.pas");
    verifyUsages(8, 2);
    verifyUsages(8, 9);
  }

  @Test
  public void testSimpleHelpers() {
    execute("helpers/Simple.pas");
    verifyUsages(10, 14, reference(15, 21), reference(22, 6));
  }

  @Test
  public void testClassHelperOverrides() {
    execute("helpers/ClassHelperOverride.pas");
    verifyUsages(7, 14, reference(16, 15));
    verifyUsages(11, 14, reference(21, 21), reference(28, 6));
  }

  @Test
  public void testClassHelperOverloads() {
    execute("helpers/ClassHelperOverload.pas");
    verifyUsages(7, 14, reference(20, 15), reference(37, 6));
    verifyUsages(11, 14, reference(25, 25));
    verifyUsages(15, 14, reference(30, 21), reference(38, 6));
  }

  @Test
  public void testClassHelperSelfValues() {
    execute("helpers/ClassHelperSelfValue.pas");
    verifyUsages(15, 10, reference(22, 2));
  }

  @Test
  public void testClassHelperInheritedStatements() {
    execute("helpers/ClassHelperInheritedStatement.pas");
    verifyUsages(7, 14, reference(29, 19), reference(66, 2));
    verifyUsages(8, 14, reference(34, 19), reference(74, 2));
    verifyUsages(13, 14, reference(44, 15), reference(67, 12), reference(75, 12));
    verifyUsages(14, 14, reference(49, 15), reference(68, 12), reference(76, 12));
    verifyUsages(9, 14, reference(39, 19), reference(69, 12), reference(77, 12));
  }

  @Test
  public void testClassHelperAccessingExtendedTypes() {
    execute("helpers/ClassHelperAccessingExtendedType.pas");
    verifyUsages(8, 14, reference(26, 19));
    verifyUsages(10, 14, reference(31, 19), reference(52, 2));
    verifyUsages(15, 14, reference(37, 15));
    verifyUsages(17, 14, reference(42, 15), reference(53, 2));
  }

  @Test
  public void testRecordHelperLiterals() {
    execute("helpers/RecordHelperLiteral.pas");
    verifyUsages(7, 14, reference(24, 21), reference(46, 10));
    verifyUsages(11, 14, reference(29, 24), reference(47, 9));
    verifyUsages(15, 14, reference(34, 22), reference(48, 6));
    verifyUsages(19, 14, reference(39, 26), reference(49, 29));
  }

  @Test
  public void testRecordHelperTypeReference() {
    execute("helpers/RecordHelperTypeReference.pas");
    verifyUsages(7, 14, reference(19, 9), reference(20, 16));
  }

  @Test
  public void testRecordHelperConstants() {
    execute("helpers/RecordHelperConstant.pas");
    verifyUsages(9, 6, reference(16, 11), reference(17, 16));
    verifyUsages(14, 10, reference(16, 2), reference(17, 2));
  }

  @Test
  public void testRecordHelperSelfValues() {
    execute("helpers/RecordHelperSelfValue.pas");
    verifyUsages(12, 10, reference(19, 2));
  }

  @Test
  public void testHelperImports() {
    execute(
        "helpers/imports/Unit1.pas",
        "helpers/imports/Unit2.pas",
        "helpers/imports/Unit3.pas",
        "helpers/imports/Unit4.pas");
    verifyUsages(17, 10, reference(24, 2));
  }

  @Test
  public void testDependencyReferencedImplicitly() {
    execute("dependencies/Implicit.pas");
    verifyDependencies("System.SysUtils");
  }

  @Test
  public void testDependencyReferencedExplicitly() {
    execute("dependencies/Explicit.pas");
    verifyDependencies("System.SysUtils");
  }

  @Test
  public void testDependencyForHelperReference() {
    execute("dependencies/Helper.pas");
    verifyDependencies("System.SysUtils");
  }

  @Test
  public void testDependencyForComponentAncestor() {
    execute("dependencies/ComponentAncestor.pas");
    verifyDependencies("Vcl.Controls", "System.Classes");
  }

  @Test
  public void testDependencyForComponentAncestorDeclaredInImplementation() {
    execute("dependencies/ComponentAncestorDeclaredInImplementation.pas");
    verifyDependencies("Vcl.Controls");
  }

  @Test
  public void testDependencyForComponentAncestorWithPublishedFieldInNonNonComponentType() {
    execute("dependencies/ComponentAncestorWithPublishedFieldInNonComponentType.pas");
    verifyDependencies("Vcl.Controls");
  }

  @Test
  public void testDependencyForComponentAncestorDependencyWithNonPublishedField() {
    execute("dependencies/ComponentAncestorWithNonPublishedField.pas");
    verifyDependencies("Vcl.Controls");
  }

  @Test
  public void testDependencyRequiredForInlineMethodExpansion() {
    execute("dependencies/InlineMethodExpansion.pas");
    verifyDependencies("System.UITypes", "Vcl.Dialogs");
  }

  @Test
  public void testDependencyRequiredForInlineMethodExpansionViaDefaultArrayProperties() {
    execute(
        "dependencies/InlineMethodExpansionViaDefaultArrayProperty.pas",
        "dependencies/imports/UnitWithDefaultArrayPropertyBackedByInlineMethod.pas");
    verifyDependencies("UnitWithDefaultArrayPropertyBackedByInlineMethod", "System.SysUtils");
  }

  @Test
  public void testDependencyShouldNotBeIntroducedForImplementationMethods() {
    execute(
        "dependencies/ImplementationVisibility.pas",
        "dependencies/imports/UnitWithImplementationMethod.pas");
    verifyDependencies("System.SysUtils");
  }

  @Test
  public void testDependencyRequiredForImplicitInvocationOfGetEnumerator() {
    execute(
        "dependencies/Enumerator.pas", "dependencies/imports/UnitWithGetEnumeratorForTObject.pas");
    verifyDependencies("UnitWithGetEnumeratorForTObject");
  }

  private void execute(String filename, String... include) {
    mainFile = DelphiTestFileBuilder.fromResource(ROOT_PATH + filename).delphiFile();
    Map<String, InputFile> inputFiles = new HashMap<>();

    inputFiles.put(uriToAbsolutePath(mainFile.getInputFile().uri()), mainFile.getInputFile());

    for (String name : include) {
      String path = ROOT_PATH + name;
      InputFile inputFile = DelphiTestFileBuilder.fromResource(path).delphiFile().getInputFile();
      inputFiles.put(uriToAbsolutePath(inputFile.uri()), inputFile);
    }

    DelphiProjectHelper delphiProjectHelper = mock(DelphiProjectHelper.class);
    when(delphiProjectHelper.getFile(anyString()))
        .thenAnswer(
            invocation -> {
              String path = invocation.getArgument(0);
              return inputFiles.get(path);
            });

    symbolTable =
        SymbolTable.builder()
            .sourceFiles(
                inputFiles.values().stream()
                    .map(InputFile::uri)
                    .map(Path::of)
                    .collect(Collectors.toList()))
            .standardLibraryPath(DelphiUtils.getResource(STANDARD_LIBRARY).toPath())
            .unitScopeNames(unitScopeNames)
            .unitAliases(unitAliases)
            .build();

    ExecutorContext executorContext = new ExecutorContext(context, symbolTable);

    componentKey = mainFile.getInputFile().key();
    executor.execute(executorContext, mainFile);
  }

  private void verifyUsages(int line, int offset, TextPointer... pointers) {
    Collection<TextRange> textRanges = context.referencesForSymbolAt(componentKey, line, offset);

    assertThat(textRanges).as("Expected symbol to be created").isNotNull();

    if (pointers.length == 0) {
      assertThat(textRanges).as("Expected no symbol references to exist").isEmpty();
    } else {
      var usages = textRanges.stream().map(TextRange::start).collect(Collectors.toList());
      assertThat(usages).as("Expected symbol references to exist").isNotEmpty().contains(pointers);
    }
  }

  private static TextPointer reference(int line, int column) {
    return new DefaultTextPointer(line, column);
  }

  private void verifyDependencies(String... dependency) {
    String path = mainFile.getSourceCodeFile().getAbsolutePath();
    UnitNameDeclaration unit = symbolTable.getUnitByPath(path);

    Set<String> dependencies =
        Sets.union(unit.getInterfaceDependencies(), unit.getImplementationDependencies()).stream()
            .map(UnitNameDeclaration::getName)
            .filter(not("System"::equals))
            .filter(not(unit.getName()::equals))
            .collect(Collectors.toUnmodifiableSet());

    assertThat(dependencies).containsExactlyInAnyOrder(dependency);
  }
}
