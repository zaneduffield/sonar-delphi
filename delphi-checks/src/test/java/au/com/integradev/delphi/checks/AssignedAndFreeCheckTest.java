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
package au.com.integradev.delphi.checks;

import au.com.integradev.delphi.builders.DelphiTestUnitBuilder;
import au.com.integradev.delphi.checks.verifier.CheckVerifier;
import org.junit.jupiter.api.Test;

class AssignedAndFreeCheckTest {
  @Test
  void testNilComparisonFollowedByFreeShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new AssignedAndFreeCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("procedure MyProcedure;")
                .appendImpl("begin")
                .appendImpl("  if MyVar <> nil then begin")
                .appendImpl("    MyVar.Free;")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyIssueOnLine(10);
  }

  @Test
  void testQualifiedNilComparisonFollowedByFreeShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new AssignedAndFreeCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("procedure MyProcedure;")
                .appendImpl("begin")
                .appendImpl("  if MyClass.MyVar <> nil then begin")
                .appendImpl("    MyClass.MyVar.Free;")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyIssueOnLine(10);
  }

  @Test
  void testBackwardsNilComparisonFollowedByFreeShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new AssignedAndFreeCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("procedure MyProcedure;")
                .appendImpl("begin")
                .appendImpl("  if nil <> MyVar then begin")
                .appendImpl("    MyVar.Free;")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyIssueOnLine(10);
  }

  @Test
  void testQualifiedBackwardsNilComparisonFollowedByFreeShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new AssignedAndFreeCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("procedure MyProcedure;")
                .appendImpl("begin")
                .appendImpl("  if nil <> MyClass.MyVar then begin")
                .appendImpl("    MyClass.MyVar.Free;")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyIssueOnLine(10);
  }

  @Test
  void testAssignedFollowedByFreeShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new AssignedAndFreeCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("procedure MyProcedure;")
                .appendImpl("begin")
                .appendImpl("  if Assigned(MyVar) then begin")
                .appendImpl("    MyVar.Free;")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyIssueOnLine(10);
  }

  @Test
  void testQualifiedAssignedFollowedByFreeShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new AssignedAndFreeCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("procedure MyProcedure;")
                .appendImpl("begin")
                .appendImpl("  if Assigned(MyClass.MyVar) then begin")
                .appendImpl("    MyClass.MyVar.Free;")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyIssueOnLine(10);
  }

  @Test
  void testStandaloneFreeShouldNotAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new AssignedAndFreeCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("procedure MyProcedure;")
                .appendImpl("begin")
                .appendImpl("  MyVar.Free;")
                .appendImpl("end;"))
        .verifyNoIssues();
  }

  @Test
  void testNilComparisonFollowedByFreeWithoutBeginShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new AssignedAndFreeCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("procedure MyProcedure;")
                .appendImpl("begin")
                .appendImpl("  if MyVar <> nil then MyVar.Free")
                .appendImpl("end;"))
        .verifyIssueOnLine(9);
  }

  @Test
  void testBackwardsNilComparisonFollowedByFreeWithoutBeginShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new AssignedAndFreeCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("procedure MyProcedure;")
                .appendImpl("begin")
                .appendImpl("  if nil <> MyVar then MyVar.Free")
                .appendImpl("end;"))
        .verifyIssueOnLine(9);
  }

  @Test
  void testAssignedFollowedByFreeWithoutBeginShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new AssignedAndFreeCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("procedure MyProcedure;")
                .appendImpl("begin")
                .appendImpl("  if Assigned(MyVar) then MyVar.Free")
                .appendImpl("end;"))
        .verifyIssueOnLine(9);
  }

  @Test
  void testNilComparisonFollowedByFreeAndNilShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new AssignedAndFreeCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("procedure MyProcedure;")
                .appendImpl("begin")
                .appendImpl("  if MyVar <> nil then begin")
                .appendImpl("    FreeAndNil(MyVar);")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyIssueOnLine(10);
  }

  @Test
  void testQualifiedNilComparisonFollowedByFreeAndNilShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new AssignedAndFreeCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("procedure MyProcedure;")
                .appendImpl("begin")
                .appendImpl("  if MyClass.MyVar <> nil then begin")
                .appendImpl("    FreeAndNil(MyClass.MyVar);")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyIssueOnLine(10);
  }

  @Test
  void testBackwardsNilComparisonFollowedByFreeAndNilShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new AssignedAndFreeCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("procedure MyProcedure;")
                .appendImpl("begin")
                .appendImpl("  if nil <> MyVar then begin")
                .appendImpl("    FreeAndNil(MyVar);")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyIssueOnLine(10);
  }

  @Test
  void testAssignedFollowedByFreeAndNilShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new AssignedAndFreeCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("procedure MyProcedure;")
                .appendImpl("begin")
                .appendImpl("  if Assigned(MyVar) then begin")
                .appendImpl("    FreeAndNil(MyVar);")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyIssueOnLine(10);
  }

  @Test
  void testQualifiedAssignedFollowedByFreeAndNilShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new AssignedAndFreeCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("procedure MyProcedure;")
                .appendImpl("begin")
                .appendImpl("  if Assigned(MyClass.MyVar) then begin")
                .appendImpl("    FreeAndNil(MyClass.MyVar);")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyIssueOnLine(10);
  }

  @Test
  void testStandaloneFreeAndNilShouldNotAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new AssignedAndFreeCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("procedure MyProcedure;")
                .appendImpl("begin")
                .appendImpl("  FreeAndNil(MyVar);")
                .appendImpl("end;"))
        .verifyNoIssues();
  }

  @Test
  void testNilComparisonFollowedByFreeAndNilWithoutBeginShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new AssignedAndFreeCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("procedure MyProcedure;")
                .appendImpl("begin")
                .appendImpl("  if MyVar <> nil then FreeAndNil(MyVar)")
                .appendImpl("end;"))
        .verifyIssueOnLine(9);
  }

  @Test
  void testBackwardsNilComparisonFollowedByFreeAndNilWithoutBeginShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new AssignedAndFreeCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("procedure MyProcedure;")
                .appendImpl("begin")
                .appendImpl("  if nil <> MyVar then FreeAndNil(MyVar)")
                .appendImpl("end;"))
        .verifyIssueOnLine(9);
  }

  @Test
  void testAssignedFollowedByFreeAndNilWithoutBeginShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new AssignedAndFreeCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("procedure MyProcedure;")
                .appendImpl("begin")
                .appendImpl("  if Assigned(MyVar) then FreeAndNil(MyVar)")
                .appendImpl("end;"))
        .verifyIssueOnLine(9);
  }

  @Test
  void testAssignCheckFollowedByAdditionalConditionsShouldNotAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new AssignedAndFreeCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("procedure AndProcedure;")
                .appendImpl("begin")
                .appendImpl("  if Assigned(MyVar) and MyVar.ShouldBeFreed then begin")
                .appendImpl("    FreeAndNil(MyVar);")
                .appendImpl("  end;")
                .appendImpl("end;")
                .appendImpl("procedure OrProcedure;")
                .appendImpl("begin")
                .appendImpl("  if Assigned(MyVar) or ShouldFreeSomethingElse then begin")
                .appendImpl("    FreeAndNil(MyVar);")
                .appendImpl("    FreeAndNil(SomethingElse);")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyNoIssues();
  }

  @Test
  void testUnrelatedGuardConditionShouldNotAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new AssignedAndFreeCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("procedure AndProcedure;")
                .appendImpl("begin")
                .appendImpl("  if 2 + 2 = 4 then begin")
                .appendImpl("    FreeAndNil(MyVar);")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyNoIssues();
  }

  @Test
  void testEdgeCasesShouldNotAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new AssignedAndFreeCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("procedure MyProcedure;")
                .appendImpl("var")
                .appendImpl("  MyBool: Boolean;")
                .appendImpl("begin")
                .appendImpl("  MyBool := Assigned(MyVar);")
                .appendImpl("  if Assigned(MyVar) then begin")
                .appendImpl("    MyBool := False;")
                .appendImpl("  end;")
                .appendImpl("  if Assigned(MyVar) then begin")
                .appendImpl("    MyClass.DoSomeProcedure")
                .appendImpl("  end;")
                .appendImpl("  if Assigned(MyVar) then begin")
                .appendImpl("    // Do nothing")
                .appendImpl("  end;")
                .appendImpl("  if Assigned(MyVar) then begin")
                .appendImpl("    MyVar := nil;")
                .appendImpl("  end;")
                .appendImpl("  if Assigned.NotAnArgumentList then begin")
                .appendImpl("    FMyField.Free;")
                .appendImpl("  end;")
                .appendImpl("  if True then begin")
                .appendImpl("    FMyField.Free;")
                .appendImpl("  end;")
                .appendImpl("  if not True then begin")
                .appendImpl("    FMyField.Free;")
                .appendImpl("  end;")
                .appendImpl("  if Assigned() then begin")
                .appendImpl("    FMyField.Free;")
                .appendImpl("  end;")
                .appendImpl("end;"))
        .verifyNoIssues();
  }
}