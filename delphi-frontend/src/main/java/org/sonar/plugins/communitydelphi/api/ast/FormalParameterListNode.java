package org.sonar.plugins.communitydelphi.api.ast;

import java.util.List;
import org.sonar.plugins.communitydelphi.api.ast.FormalParameterNode.FormalParameterData;
import org.sonar.plugins.communitydelphi.api.type.Type;

public interface FormalParameterListNode extends DelphiNode {
  List<FormalParameterData> getParameters();

  List<Type> getParameterTypes();
}