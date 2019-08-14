/*
 * Sonar Delphi Plugin
 * Copyright (C) 2011 Sabre Airline Solutions and Fabricio Colombo
 * Author(s):
 * Przemyslaw Kociolek (przemyslaw.kociolek@sabre.com)
 * Michal Wojcik (michal.wojcik@sabre.com)
 * Fabricio Colombo (fabricio.colombo.mva@gmail.com)
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
package org.sonar.plugins.delphi.pmd;

/** Constants for Delphi pmd */
public final class DelphiPmdConstants {

  public static final String REPOSITORY_KEY = "delph";
  public static final String REPOSITORY_NAME = "Delphi PMD";

  public static final String RULES_XML = "/org/sonar/plugins/delphi/pmd/rules.xml";

  public static final String TEMPLATE_XPATH_CLASS =
      "org.sonar.plugins.delphi.pmd.rules.XPathTemplateRule";
  public static final String BUILTIN_XPATH_CLASS =
      "org.sonar.plugins.delphi.pmd.rules.XPathBuiltinRule";

  public static final String TEMPLATE_XPATH_EXPRESSION_PARAM = "xPath";
  public static final String BUILTIN_XPATH_EXPRESSION_PARAM = "builtinXPath";

  public static final String BASE_EFFORT = "baseEffort";
  public static final String SCOPE = "scope";
  public static final String TEMPLATE = "template";
  public static final String TYPE = "type";

  private DelphiPmdConstants() {}
}
