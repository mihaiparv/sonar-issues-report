/*
 * Sonar :: Issues Report :: Plugin
 * Copyright (C) 2013 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.issuesreport;

import com.google.common.collect.ImmutableList;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.issuesreport.report.RuleNames;
import org.sonar.issuesreport.report.html.HTMLPrinter;

import java.util.List;

@Properties({
  @Property(key = IssuesReportConstants.HTML_REPORT_ENABLED_KEY, name = "Enable HTML report", description = "Set this to true to generate an HTML report",
    type = PropertyType.BOOLEAN, defaultValue = "false"),
  @Property(key = IssuesReportConstants.HTML_REPORT_LOCATION_KEY, name = "HTML Report location",
    description = "Location of the generated report. Can be absolute or relative to working directory",
    type = PropertyType.STRING, defaultValue = IssuesReportConstants.HTML_REPORT_LOCATION_DEFAULT, global = false, project = false),
  @Property(key = IssuesReportConstants.CONSOLE_REPORT_ENABLED_KEY, name = "Enable console report", description = "Set this to true to generate a report in console output",
    type = PropertyType.BOOLEAN, defaultValue = "false")})
public final class IssuesReportPlugin extends SonarPlugin {

  public List getExtensions() {
    return ImmutableList.of(ReportJob.class, RuleNames.class, HTMLPrinter.class);
  }
}