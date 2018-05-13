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
package org.sonar.issuesreport.fs;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Configuration;
import org.sonar.issuesreport.IssuesReportPlugin;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class InputFilesCollector_ShouldExecuteOnProjectTest {

  private Predicate<Configuration> predicate;

  private String analysisMode;
  private Boolean htmlReportEnabled;
  private Boolean consoleReportEnabled;
  private Boolean shouldExecuteOnProject;

  public InputFilesCollector_ShouldExecuteOnProjectTest(String analysisMode, Boolean htmlReportEnabled,
                                                        Boolean consoleReportEnabled,
                                                        Boolean shouldExecuteOnProject) {
    this.analysisMode = analysisMode;
    this.htmlReportEnabled = htmlReportEnabled;
    this.consoleReportEnabled = consoleReportEnabled;
    this.shouldExecuteOnProject = shouldExecuteOnProject;

    predicate = InputFilesCollector.InputFilesCollectorPredicate.shouldExecuteOnProject();
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        { CoreProperties.ANALYSIS_MODE_ISSUES, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE },
        { CoreProperties.ANALYSIS_MODE_PREVIEW, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE },
        { CoreProperties.ANALYSIS_MODE_ISSUES, Boolean.TRUE, Boolean.FALSE, Boolean.TRUE },
        { CoreProperties.ANALYSIS_MODE_ISSUES, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE },
        { CoreProperties.ANALYSIS_MODE_PUBLISH, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE },
        { null, null, null, Boolean.FALSE },
    });
  }


  @Test
  public void should_execute_on_project() {
    // given
    Configuration configuration = mock(Configuration.class);

    // when
    when(configuration.get(eq(CoreProperties.ANALYSIS_MODE))).thenReturn(Optional.ofNullable(analysisMode));
    when(configuration.getBoolean(eq(IssuesReportPlugin.HTML_REPORT_ENABLED_KEY))).thenReturn(Optional.ofNullable(htmlReportEnabled));
    when(configuration.getBoolean(eq(IssuesReportPlugin.CONSOLE_REPORT_ENABLED_KEY))).thenReturn(Optional.ofNullable(
        consoleReportEnabled));

    // then
    assertThat(predicate.test(configuration)).isEqualTo(shouldExecuteOnProject);
  }
}
