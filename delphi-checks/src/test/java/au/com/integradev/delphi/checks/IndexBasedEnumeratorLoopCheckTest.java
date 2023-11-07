/*
 * Sonar Delphi Plugin
 * Copyright (C) 2015 Fabricio Colombo
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

import au.com.integradev.delphi.builders.DelphiTestUnitBuilder;
import au.com.integradev.delphi.checks.verifier.CheckVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IndexBasedEnumeratorLoopCheckTest {
  private DelphiTestUnitBuilder enumeratorUnit() {
    return new DelphiTestUnitBuilder()
        .unitName("MyEnumerable")
        .appendDecl("type")
        .appendDecl("  TEnumerator = class(TInterfacedObject, System.IEnumerator)")
        .appendDecl("    function GetCurrent: TObject;")
        .appendDecl("    function MoveNext: Boolean;")
        .appendDecl("    procedure Reset;")
        .appendDecl("    property Current: TObject read GetCurrent;")
        .appendDecl("  end;")
        .appendDecl("  TEnumerable = class(TInterfacedObject, System.IEnumerable)")
        .appendDecl("    FCount: Integer;")
        .appendDecl("    property Count: Integer read FCount;")
        .appendDecl("    function GetEnumerator: TEnumerator;")
        .appendDecl("  end;");
  }

  @Test
  void testWithoutUseOfIndexShouldNotAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new IndexBasedEnumeratorLoopCheck())
        .withSearchPathUnit(enumeratorUnit())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("uses")
                .appendImpl("  MyEnumerable;")
                .appendImpl("procedure Test;")
                .appendImpl("var")
                .appendImpl("  I: Integer;")
                .appendImpl("  E: TEnumerable;")
                .appendImpl("begin")
                .appendImpl("  for I := 0 to E.Count - 1 do begin")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyNoIssues();
  }

  @Test
  void testWithHardCastShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new IndexBasedEnumeratorLoopCheck())
        .withSearchPathUnit(enumeratorUnit())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("uses")
                .appendImpl("  MyEnumerable;")
                .appendImpl("procedure Test;")
                .appendImpl("var")
                .appendImpl("  I: Integer;")
                .appendImpl("  E: TEnumerable;")
                .appendImpl("  O: TObject;")
                .appendImpl("begin")
                .appendImpl("  for I := 0 to E.Count - 1 do begin // Noncompliant")
                .appendImpl("    O := TObject(E[I]);")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyIssues();
  }

  @Test
  void testWithSoftCastShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new IndexBasedEnumeratorLoopCheck())
        .withSearchPathUnit(enumeratorUnit())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("uses")
                .appendImpl("  MyEnumerable;")
                .appendImpl("procedure Test;")
                .appendImpl("var")
                .appendImpl("  I: Integer;")
                .appendImpl("  E: TEnumerable;")
                .appendImpl("  O: TObject;")
                .appendImpl("begin")
                .appendImpl("  for I := 0 to E.Count - 1 do begin // Noncompliant")
                .appendImpl("    O := E[I] as TObject;")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyIssues();
  }

  @Test
  void testExplicitEnumeratorShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new IndexBasedEnumeratorLoopCheck())
        .withSearchPathUnit(enumeratorUnit())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("uses")
                .appendImpl("  MyEnumerable;")
                .appendImpl("procedure Test;")
                .appendImpl("var")
                .appendImpl("  I: Integer;")
                .appendImpl("  E: TEnumerable;")
                .appendImpl("  O: TObject;")
                .appendImpl("begin")
                .appendImpl("  for I := 0 to E.Count - 1 do begin // Noncompliant")
                .appendImpl("    O := E[I];")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyIssues();
  }

  @Test
  void testVisibleImplicitEnumeratorShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new IndexBasedEnumeratorLoopCheck())
        .withSearchPathUnit(getImplicitEnumeratorUnit("Private", "private"))
        .withSearchPathUnit(getImplicitEnumeratorUnit("Protected", "protected"))
        .withSearchPathUnit(getImplicitEnumeratorUnit("Default", ""))
        .withSearchPathUnit(getImplicitEnumeratorUnit("Published", "published"))
        .withSearchPathUnit(getImplicitEnumeratorUnit("Public", "public"))
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("uses")
                .appendImpl("  PrivateEnumerable,")
                .appendImpl("  ProtectedEnumerable,")
                .appendImpl("  DefaultEnumerable,")
                .appendImpl("  PublishedEnumerable,")
                .appendImpl("  PublicEnumerable;")
                .appendImpl("")
                .appendImpl("procedure Test;")
                .appendImpl("var")
                .appendImpl("  I: Integer;")
                .appendImpl("  O: TObject;")
                .appendImpl("  Private: TPrivateEnumerable;")
                .appendImpl("  Protected: TProtectedEnumerable;")
                .appendImpl("  Default: TDefaultEnumerable;")
                .appendImpl("  Published: TPublishedEnumerable;")
                .appendImpl("  Public: TPublicEnumerable;")
                .appendImpl("begin")
                .appendImpl("  for I := 0 to Private.Count - 1 do begin")
                .appendImpl("    O := Private[I];")
                .appendImpl("  end;")
                .appendImpl("")
                .appendImpl("  for I := 0 to Protected.Count - 1 do begin")
                .appendImpl("    O := Protected[I];")
                .appendImpl("  end;")
                .appendImpl("")
                .appendImpl("  for I := 0 to Default.Count - 1 do begin // Noncompliant")
                .appendImpl("    O := Default[I];")
                .appendImpl("  end;")
                .appendImpl("")
                .appendImpl("  for I := 0 to Published.Count - 1 do begin // Noncompliant")
                .appendImpl("    O := Published[I];")
                .appendImpl("  end;")
                .appendImpl("")
                .appendImpl("  for I := 0 to Public.Count - 1 do begin // Noncompliant")
                .appendImpl("    O := Public[I];")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyIssues();
  }

  private static DelphiTestUnitBuilder getImplicitEnumeratorUnit(String name, String visibility) {
    return getImplicitEnumeratorUnit(name, visibility, "class");
  }

  private static DelphiTestUnitBuilder getImplicitEnumeratorUnit(
      String name, String visibility, String type) {
    visibility = visibility.toLowerCase();
    String className = "T" + name + "Enumerator";
    return new DelphiTestUnitBuilder()
        .unitName(name + "Enumerable")
        .appendDecl("type")
        .appendDecl("  " + className + " = " + type)
        .appendDecl("    " + visibility)
        .appendDecl("      function MyGetCurrent: TObject;")
        .appendDecl("      function MoveNext: Boolean;")
        .appendDecl("      property Current: TObject read MyGetCurrent;")
        .appendDecl("  end;")
        .appendDecl("  T" + name + "Enumerable = class")
        .appendDecl("    " + visibility)
        .appendDecl("      function GetEnumerator: " + className + ";")
        .appendDecl("    private")
        .appendDecl("      FCount: Integer;")
        .appendDecl("    public")
        .appendDecl("      property Count: Integer read FCount;")
        .appendDecl("  end;");
  }

  @Test
  void testEnumerableWithBadEnumeratorsShouldNotAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new IndexBasedEnumeratorLoopCheck())
        .withSearchPathUnit(
            new DelphiTestUnitBuilder()
                .unitName("BadEnumerators")
                .appendDecl("type")
                .appendDecl("  TBaseEnumerable = class")
                .appendDecl("    private")
                .appendDecl("      FCount: Integer;")
                .appendDecl("    public")
                .appendDecl("      property Count: Integer read FCount;")
                .appendDecl("  end;")
                .appendDecl("")
                .appendDecl("  TEnumeratorWithoutCurrent = class")
                .appendDecl("    public")
                .appendDecl("      function MoveNext: Boolean;")
                .appendDecl("  end;")
                .appendDecl("  TEnumerableWithoutCurrent = class(TBaseEnumerable)")
                .appendDecl("    public")
                .appendDecl("      function GetEnumerator: TEnumeratorWithoutCurrent;")
                .appendDecl("  end;")
                .appendDecl("")
                .appendDecl("  TEnumeratorWithoutMoveNext = class")
                .appendDecl("    public")
                .appendDecl("      function MyGetCurrent: TObject;")
                .appendDecl("      property Current: TObject read MyGetCurrent;")
                .appendDecl("  end;")
                .appendDecl("  TEnumerableWithoutCurrent = class(TBaseEnumerable)")
                .appendDecl("    public")
                .appendDecl("      function GetEnumerator: TEnumeratorWithoutMoveNext;")
                .appendDecl("  end;")
                .appendDecl("")
                .appendDecl("  TEnumerableWithVoidEnumerator = class(TBaseEnumerable)")
                .appendDecl("    public")
                .appendDecl("      procedure GetEnumerator;")
                .appendDecl("  end;")
                .appendDecl("")
                .appendDecl("  TEnumerableWithNonStructTypeEnumerator = class(TBaseEnumerable)")
                .appendDecl("    public")
                .appendDecl("      function GetEnumerator: Integer;")
                .appendDecl("  end;")
                .appendDecl(""))
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("uses")
                .appendImpl("  BadEnumerators;")
                .appendImpl("")
                .appendImpl("procedure Test;")
                .appendImpl("var")
                .appendImpl("  I: Integer;")
                .appendImpl("  O: TObject;")
                .appendImpl("  EnumerableWithoutCurrent: TEnumerableWithoutCurrent;")
                .appendImpl("  EnumerableWithoutMoveNext: TEnumerableWithoutMoveNext;")
                .appendImpl("  EnumerableWithVoidEnumerator: TEnumerableWithVoidEnumerator;")
                .appendImpl(
                    "  EnumerableWithNonStructTypeEnumerator:"
                        + " TEnumerableWithNonStructTypeEnumerator;")
                .appendImpl("begin")
                .appendImpl("  for I := 0 to EnumerableWithoutCurrent.Count - 1 do begin")
                .appendImpl("    O := EnumerableWithoutCurrent[I];")
                .appendImpl("  end;")
                .appendImpl("")
                .appendImpl("  for I := 0 to EnumerableWithoutMoveNext.Count - 1 do begin")
                .appendImpl("    O := EnumerableWithoutMoveNext[I];")
                .appendImpl("  end;")
                .appendImpl("")
                .appendImpl("  for I := 0 to Default.Count - 1 do begin")
                .appendImpl("    O := Default[I];")
                .appendImpl("  end;")
                .appendImpl("")
                .appendImpl("  for I := 0 to EnumerableWithVoidEnumerator.Count - 1 do begin")
                .appendImpl("    O := EnumerableWithVoidEnumerator[I];")
                .appendImpl("  end;")
                .appendImpl("")
                .appendImpl(
                    "  for I := 0 to EnumerableWithNonStructTypeEnumerator.Count - 1 do begin")
                .appendImpl("    O := EnumerableWithNonStructTypeEnumerator[I];")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyNoIssues();
  }

  @ParameterizedTest
  @ValueSource(strings = {"record", "object", "interface", "class"})
  void testEnumerableWithRecordEnumeratorShouldAddIssue(String type) {
    CheckVerifier.newVerifier()
        .withCheck(new IndexBasedEnumeratorLoopCheck())
        .onFile(
            getImplicitEnumeratorUnit(type, "", type)
                .appendImpl("procedure Test;")
                .appendImpl("var")
                .appendImpl("  I: Integer;")
                .appendImpl("  O: TObject;")
                .appendImpl("  E: T" + type + "Enumerable;")
                .appendImpl("begin")
                .appendImpl("  for I := 0 to E.Count - 1 do begin // Noncompliant")
                .appendImpl("    O := E[I];")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyIssues();
  }

  @Test
  void testIndexIsUsedShouldNotAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new IndexBasedEnumeratorLoopCheck())
        .withSearchPathUnit(enumeratorUnit())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("uses")
                .appendImpl("  MyEnumerable;")
                .appendImpl("procedure Test;")
                .appendImpl("var")
                .appendImpl("  I: Integer;")
                .appendImpl("  E: TEnumerable;")
                .appendImpl("  O: TObject;")
                .appendImpl("begin")
                .appendImpl("  for I := 0 to E.Count - 1 do begin")
                .appendImpl("    O := E[I];")
                .appendImpl("    WriteLn(I);")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyNoIssues();
  }

  @Test
  void testWithAssignmentToGlobalShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new IndexBasedEnumeratorLoopCheck())
        .withSearchPathUnit(enumeratorUnit())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("uses")
                .appendImpl("  MyEnumerable;")
                .appendImpl("var")
                .appendImpl("  Global: TObject;")
                .appendImpl("procedure Test;")
                .appendImpl("var")
                .appendImpl("  I: Integer;")
                .appendImpl("  E: TEnumerable;")
                .appendImpl("begin")
                .appendImpl("  for I := 0 to E.Count - 1 do begin // Noncompliant")
                .appendImpl("    Global := E[I];")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyIssues();
  }

  @Test
  void testWithAssignmentToComplexVarShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new IndexBasedEnumeratorLoopCheck())
        .withSearchPathUnit(enumeratorUnit())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("uses")
                .appendImpl("  MyEnumerable;")
                .appendImpl("type")
                .appendImpl("  TObjWrapper = class")
                .appendImpl("    O: TObject;")
                .appendImpl("  end;")
                .appendImpl("procedure Test;")
                .appendImpl("var")
                .appendImpl("  W: TObjWrapper;")
                .appendImpl("  I: Integer;")
                .appendImpl("  E: TEnumerable;")
                .appendImpl("begin")
                .appendImpl("  for I := 0 to E.Count - 1 do begin // Noncompliant")
                .appendImpl("    W.O := E[I];")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyIssues();
  }

  @Test
  void testInlineVarShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new IndexBasedEnumeratorLoopCheck())
        .withSearchPathUnit(enumeratorUnit())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("uses")
                .appendImpl("  MyEnumerable;")
                .appendImpl("procedure Test;")
                .appendImpl("var")
                .appendImpl("  E: TEnumerable;")
                .appendImpl("  O: TObject;")
                .appendImpl("begin")
                .appendImpl("  for var I := 0 to E.Count - 1 do begin // Noncompliant")
                .appendImpl("    O := E[I];")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyIssues();
  }

  @Test
  void testInlineVarObjectShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new IndexBasedEnumeratorLoopCheck())
        .withSearchPathUnit(enumeratorUnit())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("uses")
                .appendImpl("  MyEnumerable;")
                .appendImpl("procedure Test;")
                .appendImpl("var")
                .appendImpl("  I: Integer;")
                .appendImpl("  E: TEnumerable;")
                .appendImpl("begin")
                .appendImpl("  for I := 0 to E.Count - 1 do begin // Noncompliant")
                .appendImpl("    var O := E[I];")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyIssues();
  }

  @Test
  void testInlineVarObjectOutsideLoopShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new IndexBasedEnumeratorLoopCheck())
        .withSearchPathUnit(enumeratorUnit())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("uses")
                .appendImpl("  MyEnumerable;")
                .appendImpl("procedure Test;")
                .appendImpl("var")
                .appendImpl("  I: Integer;")
                .appendImpl("  E: TEnumerable;")
                .appendImpl("begin")
                .appendImpl("  var O := nil;")
                .appendImpl("  for I := 0 to E.Count - 1 do begin // Noncompliant")
                .appendImpl("    O := E[I];")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyIssues();
  }

  @Test
  void testCountAndEnumeratorAtDifferentLevelsOfHierarchyShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new IndexBasedEnumeratorLoopCheck())
        .withSearchPathUnit(
            new DelphiTestUnitBuilder()
                .unitName("MyEnumerable")
                .appendDecl("type")
                .appendDecl("  TEnumerator = class(TInterfacedObject, System.IEnumerator)")
                .appendDecl("    public")
                .appendDecl("      function GetCurrent: TObject;")
                .appendDecl("      function MoveNext: Boolean;")
                .appendDecl("      procedure Reset;")
                .appendDecl("      property Current: TObject read GetCurrent;")
                .appendDecl("  end;")
                .appendDecl("  TIntermediateEnumerable = class")
                .appendDecl("    private")
                .appendDecl("      FCount: Integer;")
                .appendDecl("    public")
                .appendDecl("      property Count: Integer read FCount;")
                .appendDecl("  end;")
                .appendDecl("  TEnumerable = class(TInterfacedObject, System.IEnumerable)")
                .appendDecl("    public")
                .appendDecl("      function GetEnumerator: TEnumerator;")
                .appendDecl("  end;"))
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("uses")
                .appendImpl("  MyEnumerable;")
                .appendImpl("procedure Test;")
                .appendImpl("var")
                .appendImpl("  I: Integer;")
                .appendImpl("  E: TEnumerable;")
                .appendImpl("begin")
                .appendImpl("  var O := nil;")
                .appendImpl("  for I := 0 to E.Count - 1 do begin // Noncompliant")
                .appendImpl("    O := E[I];")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyIssues();
  }

  @Test
  void testEnumeratorAccessedIndirectlyShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new IndexBasedEnumeratorLoopCheck())
        .withSearchPathUnit(enumeratorUnit())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("uses")
                .appendImpl("  MyEnumerable;")
                .appendImpl("type")
                .appendImpl("  TWrapper = class")
                .appendImpl("    E: TEnumerable;")
                .appendImpl("  end;")
                .appendImpl("procedure Test;")
                .appendImpl("var")
                .appendImpl("  O: TObject;")
                .appendImpl("  I: Integer;")
                .appendImpl("  W: TWrapper;")
                .appendImpl("begin")
                .appendImpl("  for I := 0 to W.E.Count - 1 do begin // Noncompliant")
                .appendImpl("    O := W.E[I];")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyIssues();
  }

  @Test
  void testUnMatchingTypesShouldNotAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new IndexBasedEnumeratorLoopCheck())
        .withSearchPathUnit(enumeratorUnit())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("uses")
                .appendImpl("  MyEnumerable;")
                .appendImpl("type")
                .appendImpl("  TWrapper = class")
                .appendImpl("    E: TEnumerable;")
                .appendImpl("    function Count: Integer;")
                .appendImpl("  end;")
                .appendImpl("procedure Test;")
                .appendImpl("var")
                .appendImpl("  E: TEnumerable;")
                .appendImpl("  O: TObject;")
                .appendImpl("  I: Integer;")
                .appendImpl("  W: TWrapper;")
                .appendImpl("begin")
                .appendImpl("  for I := 0 to E.Count - 1 do begin")
                .appendImpl("    O := W.E[I];")
                .appendImpl("  end;")
                .appendImpl("")
                .appendImpl("  for I := 0 to W.E.Count - 1 do begin")
                .appendImpl("    O := E[I];")
                .appendImpl("  end;")
                .appendImpl("")
                .appendImpl("  for I := 0 to W.Count - 1 do begin")
                .appendImpl("    O := E[I];")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyNoIssues();
  }

  @Test
  void testAssignmentNotFirstLoopBodyStatementShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new IndexBasedEnumeratorLoopCheck())
        .withSearchPathUnit(enumeratorUnit())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("uses")
                .appendImpl("  MyEnumerable;")
                .appendImpl("procedure Test;")
                .appendImpl("var")
                .appendImpl("  E: TEnumerable;")
                .appendImpl("  O: TObject;")
                .appendImpl("  I: Integer;")
                .appendImpl("begin")
                .appendImpl("  for I := 0 to E.Count - 1 do begin // Noncompliant")
                .appendImpl("    Assert(1 < 2);")
                .appendImpl("    O := E[I];")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyIssues();
  }
}
