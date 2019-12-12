package org.sonar.plugins.delphi.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.delphi.utils.DelphiUtils.uriToAbsolutePath;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultTextPointer;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.plugins.delphi.file.DelphiFile;
import org.sonar.plugins.delphi.file.DelphiFileConfig;
import org.sonar.plugins.delphi.project.DelphiProjectHelper;
import org.sonar.plugins.delphi.symbol.SymbolTable;
import org.sonar.plugins.delphi.utils.DelphiUtils;
import org.sonar.plugins.delphi.utils.builders.DelphiTestFileBuilder;

public class DelphiSymbolTableExecutorTest {
  private static final String ROOT_PATH = "/org/sonar/plugins/delphi/symbol/";
  private static final String STANDARD_LIBRARY = "/org/sonar/plugins/delphi/standardLibrary";

  private DelphiSymbolTableExecutor executor;
  private SensorContextTester context;
  private Set<String> unitScopeNames;
  private String componentKey;

  @Before
  public void setup() {
    executor = new DelphiSymbolTableExecutor();
    context = SensorContextTester.create(DelphiUtils.getResource(ROOT_PATH));
    unitScopeNames = new HashSet<>();
  }

  @Test
  public void testSimpleFile() {
    execute("Simple.pas");
    verifyUsages(9, 2, reference(22, 10), reference(31, 10), reference(36, 10));
    verifyUsages(14, 3);
    verifyUsages(22, 2);
    verifyUsages(11, 14, reference(31, 22), reference(38, 1));
    verifyUsages(12, 14, reference(33, 1), reference(36, 22));
  }

  @Test
  public void testSimilarParameterDeclarations() {
    execute("SimilarParameterDeclarations.pas");
    verifyUsages(8, 2, reference(16, 10), reference(21, 10));
    verifyUsages(10, 14, reference(16, 15));
    verifyUsages(10, 19);
    verifyUsages(11, 14, reference(21, 15), reference(18, 2));
    verifyUsages(11, 19);
  }

  @Test
  public void testRecords() {
    execute("Records.pas");
    verifyUsages(8, 2, reference(18, 21));
    verifyUsages(9, 4, reference(33, 30));
    verifyUsages(12, 2, reference(26, 11), reference(31, 11));
    verifyUsages(14, 4, reference(28, 14));
    verifyUsages(15, 6, reference(28, 31));
    verifyUsages(18, 4, reference(33, 14));
    verifyUsages(20, 13, reference(26, 16));
    verifyUsages(21, 13, reference(31, 16));
  }

  @Test
  public void testInheritedInvocations() {
    execute("InheritedInvocations.pas");
    verifyUsages(8, 2, reference(14, 15), reference(23, 12), reference(28, 12));
    verifyUsages(10, 14, reference(23, 17), reference(35, 4), reference(37, 14), reference(45, 14));
    verifyUsages(11, 14, reference(28, 17), reference(36, 14), reference(43, 4), reference(44, 14));
    verifyUsages(14, 2, reference(33, 12), reference(41, 12), reference(50, 12));
    verifyUsages(16, 14, reference(33, 17), reference(46, 4));
    verifyUsages(17, 14, reference(41, 17), reference(38, 4));
    verifyUsages(18, 14, reference(47, 4), reference(50, 17));
  }

  @Test
  public void testNestedMethods() {
    execute("NestedMethods.pas");
    verifyUsages(8, 9);
    verifyUsages(10, 11, reference(29, 12));
    verifyUsages(10, 15, reference(12, 7));
    verifyUsages(19, 11, reference(29, 16));
    verifyUsages(19, 15, reference(21, 7));
  }

  @Test
  public void testTypeAliasParameter() {
    execute("TypeAliasParameter.pas");
    verifyUsages(8, 2, reference(11, 14), reference(15, 25));
    verifyUsages(11, 2, reference(20, 31));
    verifyUsages(13, 2, reference(20, 10));
    verifyUsages(15, 14, reference(20, 15));
  }

  @Test
  public void testArrays() {
    execute("Arrays.pas");
    verifyUsages(
        10,
        14,
        reference(32, 15),
        reference(42, 17),
        reference(43, 30),
        reference(44, 30),
        reference(45, 23),
        reference(46, 25),
        reference(47, 9),
        reference(48, 46),
        reference(49, 14));
    verifyUsages(17, 13, reference(48, 27));
    verifyUsages(26, 13, reference(46, 6));
    verifyUsages(27, 13, reference(48, 6));
    verifyUsages(39, 2, reference(42, 2));
    verifyUsages(40, 2, reference(43, 2), reference(44, 2));
  }

  @Test
  public void testArrayArgument() {
    execute("ArrayArgument.pas");
    verifyUsages(9, 10, reference(16, 2));
    verifyUsages(14, 14, reference(16, 6));
  }

  @Test
  public void testArrayOfConst() {
    execute("ArrayOfConst.pas");
    verifyUsages(9, 10, reference(16, 2), reference(17, 2), reference(18, 2));
    verifyUsages(14, 14, reference(17, 7), reference(18, 7));
  }

  @Test
  public void testAnonymousMethods() {
    execute("AnonymousMethods.pas");
    verifyUsages(8, 2, reference(12, 20), reference(17, 20));
    verifyUsages(12, 10, reference(21, 2), reference(22, 2));
    verifyUsages(22, 16, reference(24, 4));
    verifyUsages(19, 2, reference(24, 13));
  }

  @Test
  public void testUsesDeclarations() {
    execute("UsesDeclarations.pas");
    verifyUsages(1, 5, reference(25, 7));
    verifyUsages(17, 11, reference(24, 2), reference(25, 2));
    verifyUsages(8, 2, reference(26, 2));
    verifyUsages(15, 2, reference(27, 2));
  }

  @Test
  public void testResults() {
    execute("Results.pas");
    verifyUsages(8, 2, reference(15, 10), reference(20, 19), reference(25, 15), reference(27, 12));
    verifyUsages(10, 14, reference(15, 15), reference(28, 9));
    verifyUsages(20, 10, reference(29, 2));
  }

  @Test
  public void testInitializationFinalization() {
    execute("InitializationFinalization.pas");
    verifyUsages(8, 2, reference(16, 7), reference(19, 9));
    verifyUsages(10, 14, reference(20, 6));
    verifyUsages(16, 2, reference(22, 2));
  }

  @Test
  public void testRecordExpressionItems() {
    execute("RecordExpressionItems.pas");
    verifyUsages(16, 10, reference(23, 11));
  }

  @Test
  public void testClassReference() {
    execute("ClassReference.pas");
    verifyUsages(11, 14, reference(20, 6));
  }

  @Test
  public void testHardTypeCast() {
    execute("HardTypeCast.pas");
    verifyUsages(10, 4, reference(19, 12));
    verifyUsages(17, 18, reference(19, 44));
  }

  @Test
  public void testHandlerProperty() {
    execute("HandlerProperty.pas");
    verifyUsages(12, 4, reference(23, 2));
    verifyUsages(21, 19, reference(23, 22));
  }

  @Test
  public void testClassReferenceConstructor() {
    execute("ClassReferenceConstructor.pas");
    verifyUsages(17, 10, reference(24, 2));
    verifyUsages(10, 16, reference(24, 11));
  }

  @Test
  public void testSimpleForwardDeclarations() {
    execute("forwardDeclarations/Simple.pas");
    verifyUsages(24, 26, reference(26, 14));
  }

  @Test
  public void testInheritanceForwardDeclarations() {
    execute("forwardDeclarations/Inheritance.pas");
    verifyUsages(29, 10, reference(36, 2), reference(37, 2));
    verifyUsages(16, 15, reference(36, 11));
    verifyUsages(34, 26, reference(37, 7));
  }

  @Test
  public void testSimpleTypeResolution() {
    execute("typeResolution/Simple.pas");
    verifyUsages(8, 2, reference(16, 10), reference(18, 21), reference(27, 2), reference(31, 22));
    verifyUsages(10, 16, reference(27, 7), reference(28, 9));
    verifyUsages(11, 14, reference(25, 7), reference(26, 9), reference(27, 14), reference(28, 16));
    verifyUsages(14, 2, reference(23, 10), reference(31, 9));
    verifyUsages(16, 4, reference(25, 2), reference(33, 12));
    verifyUsages(17, 14, reference(23, 15));
    verifyUsages(18, 13, reference(26, 2), reference(28, 2), reference(31, 14));
  }

  @Test
  public void testCharTypeResolution() {
    execute("typeResolution/Chars.pas");
    verifyUsages(9, 9, reference(24, 2));
    verifyUsages(14, 9, reference(25, 2));
  }

  @Test
  public void testCastTypeResolution() {
    execute("typeResolution/Casts.pas");
    verifyUsages(10, 14, reference(17, 12), reference(18, 16));
  }

  @Test
  public void testConstructorTypeResolution() {
    execute("typeResolution/Constructors.pas");
    verifyUsages(10, 14, reference(25, 14));
    verifyUsages(15, 14, reference(26, 14));
  }

  @Test
  public void testSimpleProperties() {
    execute("properties/Simple.pas");
    verifyUsages(
        8,
        2,
        reference(16, 10),
        reference(18, 26),
        reference(19, 21),
        reference(20, 24),
        reference(21, 23),
        reference(22, 26),
        reference(27, 27),
        reference(32, 22),
        reference(39, 7),
        reference(41, 19),
        reference(44, 18),
        reference(47, 21));
    verifyUsages(10, 16, reference(41, 24), reference(44, 23), reference(47, 26));
    verifyUsages(11, 14, reference(42, 16), reference(45, 15), reference(48, 18));
    verifyUsages(14, 2, reference(27, 10), reference(32, 9), reference(37, 20));
    verifyUsages(
        16,
        4,
        reference(21, 33),
        reference(21, 44),
        reference(22, 36),
        reference(22, 47),
        reference(29, 2),
        reference(34, 12));
    verifyUsages(18, 14, reference(20, 47), reference(27, 15));
    verifyUsages(19, 13, reference(20, 34), reference(32, 14));
    verifyUsages(20, 13, reference(41, 6), reference(42, 6));
    verifyUsages(21, 13, reference(44, 6), reference(45, 6));
    verifyUsages(22, 13, reference(47, 6), reference(48, 6));
  }

  @Test
  public void testOverrideProperties() {
    execute("properties/OverrideProperties.pas");
    verifyUsages(
        10, 14, reference(31, 10), reference(32, 10), reference(33, 13), reference(34, 13));
    verifyUsages(18, 13, reference(31, 6), reference(33, 6));
    verifyUsages(23, 13, reference(32, 6), reference(34, 6));
  }

  @Test
  public void testProceduralProperties() {
    execute("properties/ProceduralProperties.pas");
    verifyUsages(14, 13, reference(21, 6));
    verifyUsages(19, 26, reference(21, 14));
  }

  @Test
  public void testHiddenDefaultProperties() {
    execute("properties/HiddenDefaultProperties.pas");
    verifyUsages(13, 14, reference(29, 25));
  }

  @Test
  public void testSimpleOverloads() {
    execute("overloads/Simple.pas");
    verifyUsages(10, 10, reference(16, 10), reference(37, 2));
    verifyUsages(11, 10, reference(21, 10), reference(38, 2));
    verifyUsages(12, 10, reference(26, 10), reference(39, 2), reference(40, 2));
  }

  @Test
  public void testTypeTypeOverloads() {
    execute("overloads/TypeType.pas");
    verifyUsages(8, 2, reference(17, 19), reference(25, 10));
    verifyUsages(12, 10, reference(27, 2));
    verifyUsages(17, 10, reference(28, 2));
  }

  @Test
  public void testNestedExpressions() {
    execute("overloads/NestedExpressions.pas");
    verifyUsages(9, 2, reference(23, 9), reference(40, 12), reference(43, 14));
    verifyUsages(10, 13, reference(23, 25), reference(44, 15));
    verifyUsages(13, 10, reference(28, 10), reference(44, 2));
    verifyUsages(14, 10, reference(33, 10), reference(45, 2), reference(46, 2));
    verifyUsages(18, 9, reference(45, 6), reference(46, 9));
  }

  @Test
  public void testAmbiguousMethodReferences() {
    execute("overloads/AmbiguousMethodReferences.pas");
    verifyUsages(8, 2, reference(10, 19), reference(20, 19), reference(32, 11));
    verifyUsages(10, 10, reference(20, 10), reference(35, 2), reference(36, 2));
    verifyUsages(11, 10, reference(25, 10), reference(37, 2), reference(38, 2));
    verifyUsages(15, 9, reference(36, 6));
  }

  @Test
  public void testProceduralVariables() {
    execute("overloads/ProceduralVariables.pas");
    verifyUsages(7, 10, reference(12, 10), reference(27, 20));
    verifyUsages(8, 10, reference(17, 10), reference(28, 19));
    verifyUsages(24, 2, reference(27, 2), reference(30, 2));
    verifyUsages(25, 2, reference(28, 2), reference(31, 2));
  }

  @Test
  public void testCharInSet() {
    execute("overloads/CharInSet.pas");
    verifyUsages(15, 13, reference(27, 22));
    verifyUsages(20, 10, reference(27, 12));
    verifyUsages(25, 19, reference(27, 36));
  }

  @Test
  public void testSimpleMethodResolutionClause() {
    execute("methodResolutionClause/Simple.pas");
    verifyUsages(9, 14, reference(14, 26));
    verifyUsages(13, 14, reference(14, 42));
  }

  @Test
  public void testMethodResolutionClauseWithOverloadedImplementation() {
    execute("methodResolutionClause/OverloadedImplementation.pas");
    verifyUsages(9, 14, reference(15, 26));
    verifyUsages(13, 14, reference(15, 42));
    verifyUsages(14, 14);
  }

  @Test
  public void testMethodResolutionClauseWithOverloadedInterfaceAndImplementation() {
    execute("methodResolutionClause/OverloadedInterfaceAndImplementation.pas");
    verifyUsages(9, 14, reference(16, 26));
    verifyUsages(14, 14, reference(16, 42));
    verifyUsages(15, 14);
  }

  @Test
  public void testImports() {
    execute("imports/Unit1.pas", "imports/Unit2.pas", "imports/Unit3.pas");
    verifyUsages(1, 5, reference(25, 2), reference(28, 18));
    verifyUsages(8, 2, reference(28, 2));
    verifyUsages(11, 2, reference(28, 24), reference(29, 12));
    verifyUsages(16, 2, reference(25, 18));
    verifyUsages(18, 10, reference(25, 8), reference(26, 2));
  }

  @Test
  public void testNamespaces() {
    execute(
        "namespaces/Namespaced.Unit1.pas",
        "namespaces/Namespaced.Unit2.pas",
        "namespaces/Unit3.pas",
        "namespaces/UnitScopeName.Unit2.pas",
        "namespaces/UnitScopeName.ScopedUnit3.pas");

    verifyUsages(1, 5, reference(25, 2), reference(28, 18));
    verifyUsages(8, 2, reference(28, 2));
    verifyUsages(11, 2, reference(28, 35), reference(29, 12));
    verifyUsages(16, 2, reference(25, 29));
    verifyUsages(18, 10, reference(25, 19), reference(26, 2));
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

    verifyUsages(1, 5, reference(25, 2), reference(28, 30));
    verifyUsages(8, 2, reference(28, 2));
    verifyUsages(11, 2, reference(28, 48), reference(29, 18));
    verifyUsages(16, 2, reference(25, 30));
    verifyUsages(18, 10, reference(25, 20), reference(26, 2));
  }

  @Test
  public void testUnscopedEnums() {
    execute("enums/UnscopedEnum.pas");
    verifyUsages(8, 2, reference(12, 19));
    verifyUsages(8, 9, reference(14, 11));
  }

  @Test
  public void testScopedEnums() {
    execute("enums/ScopedEnum.pas");
    verifyUsages(10, 2);
    verifyUsages(10, 9);
  }

  private void execute(String filename, String... include) {
    var mainFile = DelphiTestFileBuilder.fromResource(ROOT_PATH + filename).delphiFile();
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

    DelphiFileConfig fileConfig =
        DelphiFile.createConfig(
            delphiProjectHelper.encoding(), Collections.emptyList(), Collections.emptySet());

    SymbolTable symbolTable =
        SymbolTable.builder()
            .sourceFiles(
                inputFiles.values().stream()
                    .map(InputFile::uri)
                    .map(Path::of)
                    .collect(Collectors.toList()))
            .standardLibraryPath(DelphiUtils.getResource(STANDARD_LIBRARY).toPath())
            .unitScopeNames(unitScopeNames)
            .fileConfig(fileConfig)
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
}