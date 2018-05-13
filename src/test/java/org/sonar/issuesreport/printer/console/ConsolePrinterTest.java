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
package org.sonar.issuesreport.printer.console;

import org.junit.Test;
import org.sonar.issuesreport.IssuesReportFakeUtils;
import org.sonar.issuesreport.fs.ResourceNode;
import org.sonar.issuesreport.report.IssuesReport;

import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ConsolePrinterTest {

  @Test
  public void shouldGenerateReportWhenNoViolation() {
    ConsolePrinter.ConsoleLogger logger = mock(ConsolePrinter.ConsoleLogger.class);
    ConsolePrinter printer = new ConsolePrinter(logger);

    IssuesReport report = new IssuesReport();
    printer.print(report);

    verify(logger).log(contains("No new issue"));
  }

  @Test
  public void shouldGenerateReportWhenNewViolation() {
    ConsolePrinter.ConsoleLogger logger = mock(ConsolePrinter.ConsoleLogger.class);
    ConsolePrinter printer = new ConsolePrinter(logger);

    ResourceNode file = IssuesReportFakeUtils.fakeFile("com.foo.Bar");

    IssuesReport report = IssuesReportFakeUtils.sampleReportWith2IssuesPerFile(file);

    printer.print(report);

    verify(logger).log(contains("+1 issue"));
    verify(logger).log(contains("+1 blocking"));
  }
}
