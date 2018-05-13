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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.PostJobDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.issuesreport.printer.ReportPrinter;
import org.sonar.issuesreport.report.IssuesReport;
import org.sonar.issuesreport.report.IssuesReportBuilder;

import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReportJobTest {

  private static final String PROP_1 = "prop#1";
  private static final String PROP_2 = "prop#2";
  private IssuesReportBuilder issuesReportBuilder;
  private ReportPrinter printer1;
  private ReportPrinter printer2;
  private ReportJob job;

  @Before
  public void prepare() {
    issuesReportBuilder = mock(IssuesReportBuilder.class);
    when(issuesReportBuilder.buildReport(any(PostJobContext.class))).thenReturn(new IssuesReport());
    printer1 = mock(ReportPrinter.class);
    when(printer1.getRequiredProperty()).thenReturn(PROP_1);
    printer2 = mock(ReportPrinter.class);
    when(printer2.getRequiredProperty()).thenReturn(PROP_2);

    job = new ReportJob(issuesReportBuilder, new ReportPrinter[] {printer1, printer2});
  }

  @Test
  public void shouldNotBuildReportWhenNoPrinterEnabled() {
    final Configuration configuration = mock(Configuration.class);
    when(configuration.getBoolean(any())).thenReturn(Optional.empty());

    final PostJobContext jobContext = mock(PostJobContext.class);
    when(jobContext.config()).thenReturn(configuration);
    job.execute(jobContext);

    verify(issuesReportBuilder, never()).buildReport(any(PostJobContext.class));
    verify(printer1, never()).print(any(IssuesReport.class));
    verify(printer2, never()).print(any(IssuesReport.class));
  }

  @Test
  public void shouldPrintOnlyOnEnabledPrinter() {
    final Configuration configuration = mock(Configuration.class);
    when(configuration.getBoolean(eq(PROP_1))).thenReturn(Optional.of(Boolean.TRUE));
    when(configuration.getBoolean(eq(PROP_2))).thenReturn(Optional.of(Boolean.FALSE));

    final PostJobContext jobContext = mock(PostJobContext.class);
    when(jobContext.config()).thenReturn(configuration);
    job.execute(jobContext);

    verify(issuesReportBuilder, only()).buildReport(jobContext);
    verify(printer1, times(1)).print(any(IssuesReport.class));
    verify(printer2, never()).print(any(IssuesReport.class));
  }

  @Test
  public void shouldBuildReportOnlyOnceWhenTwoPrintersEnabled() {
    final Configuration configuration = mock(Configuration.class);
    when(configuration.getBoolean(eq(PROP_1))).thenReturn(Optional.of(Boolean.TRUE));
    when(configuration.getBoolean(eq(PROP_2))).thenReturn(Optional.of(Boolean.TRUE));

    final PostJobContext jobContext = mock(PostJobContext.class);
    when(jobContext.config()).thenReturn(configuration);
    job.execute(jobContext);

    verify(issuesReportBuilder, only()).buildReport(jobContext);
    verify(printer1, times(1)).print(any(IssuesReport.class));
    verify(printer2, times(1)).print(any(IssuesReport.class));
  }

  @Test
  public void testRequiredProperties() {
    PostJobDescriptor descriptor = mock(PostJobDescriptor.class);
    when(descriptor.name(any())).thenReturn(descriptor);

    job.describe(descriptor);

    verify(descriptor).requireProperties(PROP_1, PROP_2);
  }
}
