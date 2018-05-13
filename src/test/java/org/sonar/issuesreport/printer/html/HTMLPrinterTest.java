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
package org.sonar.issuesreport.printer.html;

import com.google.common.base.Charsets;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.rule.RuleKey;
import org.sonar.issuesreport.IssuesReportFakeUtils;
import org.sonar.issuesreport.IssuesReportPlugin;
import org.sonar.issuesreport.fs.ResourceNode;
import org.sonar.issuesreport.provider.RuleNameProvider;
import org.sonar.issuesreport.provider.SourceProvider;
import org.sonar.issuesreport.report.IssuesReport;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HTMLPrinterTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private MapSettings settings;
  private HtmlPrinter htmlPrinter;
  private RuleNameProvider ruleNameProvider;
  private FileSystem fs;

  @Before
  public void prepare() {
    ruleNameProvider = mock(RuleNameProvider.class);
    SourceProvider sourceProvider = mock(SourceProvider.class);
    fs = mock(FileSystem.class);
    settings = new MapSettings();

    htmlPrinter = new HtmlPrinter(ruleNameProvider, sourceProvider, fs, settings.asConfig());
  }

  @Test
  public void shouldAllowOnlyLightReport() {
    settings.setProperty(IssuesReportPlugin.HTML_REPORT_LIGHTMODE_ONLY, "true");
    assertThat(htmlPrinter.isLightModeOnly()).isTrue();
  }

  @Test
  public void shouldGenerateEmptyReport() throws IOException {
    when(fs.encoding()).thenReturn(Charsets.UTF_8);
    IssuesReport report = new IssuesReport();
    report.setTitle("Fake report");
    report.setDate(new Date());
    File reportDir = temp.newFolder();
    File reportFile = new File(reportDir, "report.html");
    htmlPrinter.writeToFile(report, reportFile, true);
    assertThat(reportFile).exists();
  }

  @Test
  public void shouldPrintIntoDefaultReportFile() {
    settings.setProperty(IssuesReportPlugin.HTML_REPORT_NAME_KEY, IssuesReportPlugin.HTML_REPORT_NAME_DEFAULT);
    settings.setProperty(IssuesReportPlugin.HTML_REPORT_LOCATION_KEY, IssuesReportPlugin.HTML_REPORT_LOCATION_DEFAULT);
    when(fs.workDir()).thenReturn(new File("target"));
    HtmlPrinter spy = spy(htmlPrinter);
    doNothing().when(spy).writeToFile(any(IssuesReport.class), any(File.class), anyBoolean());

    spy.print(mock(IssuesReport.class));

    verify(spy).writeToFile(any(IssuesReport.class), eq(new File("target/issues-report/issues-report.html")), eq(true));
    verify(spy).writeToFile(any(IssuesReport.class), eq(new File("target/issues-report/issues-report-light.html")), eq(false));
  }

  @Test
  public void shouldConfigureReportLocation() throws IOException {
    File reportPath = new File("target/path/to/report");
    settings.setProperty(IssuesReportPlugin.HTML_REPORT_LOCATION_KEY, reportPath.getAbsolutePath());
    settings.setProperty(IssuesReportPlugin.HTML_REPORT_NAME_KEY, IssuesReportPlugin.HTML_REPORT_NAME_DEFAULT);

    HtmlPrinter spy = spy(htmlPrinter);
    doNothing().when(spy).writeToFile(any(IssuesReport.class), any(File.class), anyBoolean());

    spy.print(mock(IssuesReport.class));

    verify(spy).writeToFile(any(IssuesReport.class), eq(new File(reportPath.getAbsoluteFile(), "issues-report.html")), eq(true));
    verify(spy).writeToFile(any(IssuesReport.class), eq(new File(reportPath.getAbsoluteFile(), "issues-report-light.html")), eq(false));
  }

  @Test
  public void shouldConfigureReportLocation_deprecated() throws IOException {
    File reportPath = new File("target/path/to/report.html");
    settings.setProperty(IssuesReportPlugin.HTML_REPORT_LOCATION_KEY, reportPath.getAbsolutePath());
    settings.setProperty(IssuesReportPlugin.HTML_REPORT_NAME_KEY, IssuesReportPlugin.HTML_REPORT_NAME_DEFAULT);

    HtmlPrinter spy = spy(htmlPrinter);
    doNothing().when(spy).writeToFile(any(IssuesReport.class), any(File.class), anyBoolean());

    spy.print(mock(IssuesReport.class));

    verify(spy).writeToFile(any(IssuesReport.class), eq(new File(reportPath.getAbsoluteFile().getParent(), "issues-report.html")), eq(true));
    verify(spy).writeToFile(any(IssuesReport.class), eq(new File(reportPath.getAbsoluteFile().getParent(), "issues-report-light.html")), eq(false));
  }

  @Test
  public void shouldGenerateReportWithNewViolation() throws IOException {
    File reportDir = temp.newFolder();
    File reportFile = new File(reportDir, "issues-report.html");
    File lightReportFile = new File(reportDir, "issues-report-light.html");
    settings.setProperty(IssuesReportPlugin.HTML_REPORT_LOCATION_KEY, reportDir.getAbsolutePath());
    settings.setProperty(IssuesReportPlugin.HTML_REPORT_NAME_KEY, IssuesReportPlugin.HTML_REPORT_NAME_DEFAULT);

    when(fs.encoding()).thenReturn(Charsets.UTF_8);

    ResourceNode file = IssuesReportFakeUtils.fakeFile("com.foo.Bar");
    mockRuleNameProvider();

    IssuesReport report = IssuesReportFakeUtils.sampleReportWith2IssuesPerFile(file);

    htmlPrinter.print(report);

    assertThat(reportFile).exists();
    assertThat(lightReportFile).exists();
  }

  @Test
  public void shouldGenerateOnlyLightReport() throws IOException {
    File reportDir = temp.newFolder();
    File reportFile = new File(reportDir, "issues-report.html");
    File lightReportFile = new File(reportDir, "issues-report-light.html");
    settings.setProperty(IssuesReportPlugin.HTML_REPORT_LOCATION_KEY, reportDir.getAbsolutePath());
    settings.setProperty(IssuesReportPlugin.HTML_REPORT_NAME_KEY, IssuesReportPlugin.HTML_REPORT_NAME_DEFAULT);
    settings.setProperty(IssuesReportPlugin.HTML_REPORT_LIGHTMODE_ONLY, true);

    when(fs.encoding()).thenReturn(Charsets.UTF_8);

    ResourceNode file = IssuesReportFakeUtils.fakeFile("com.foo.Bar");
    mockRuleNameProvider();

    IssuesReport report = IssuesReportFakeUtils.sampleReportWith2IssuesPerFile(file);

    htmlPrinter.print(report);

    assertThat(reportFile).doesNotExist();
    assertThat(lightReportFile).exists();
  }

  private void mockRuleNameProvider() {
    when(ruleNameProvider.nameForHTML(eq(RuleKey.of("foo", "bar")))).thenReturn("My Rule 1");
    when(ruleNameProvider.nameForHTML(eq(RuleKey.of("foo", "bar2")))).thenReturn("My Rule 2");
    when(ruleNameProvider.nameForHTML(any(org.sonar.api.rules.Rule.class))).thenReturn("My Rule");
    when(ruleNameProvider.nameForJS("foo:bar")).thenReturn("My Rule 2");
    when(ruleNameProvider.nameForJS("foo:bar2")).thenReturn("My Rule 2");
  }

  @Test
  public void shouldGenerateReportWithSeveralResources() throws IOException {
    File reportDir = temp.newFolder();
    File reportFile = new File(reportDir, "issues-report.html");
    File lightReportFile = new File(reportDir, "issues-report-light.html");
    settings.setProperty(IssuesReportPlugin.HTML_REPORT_LOCATION_KEY, reportDir.getAbsolutePath());
    settings.setProperty(IssuesReportPlugin.HTML_REPORT_NAME_KEY, IssuesReportPlugin.HTML_REPORT_NAME_DEFAULT);

    when(fs.encoding()).thenReturn(Charsets.UTF_8);

    ResourceNode file1 = IssuesReportFakeUtils.fakeFile("com.foo.Bar");
    ResourceNode file2 = IssuesReportFakeUtils.fakeFile("com.foo.Foo");
    mockRuleNameProvider();

    IssuesReport report = IssuesReportFakeUtils.sampleReportWith2IssuesPerFile(file1, file2);

    htmlPrinter.print(report);

    assertThat(reportFile).exists();
    assertThat(lightReportFile).exists();
  }

  @Test
  public void shouldGenerateReportWithPackageLevelViolation() throws IOException {
    File reportDir = temp.newFolder();
    File reportFile = new File(reportDir, "issues-report.html");
    File lightReportFile = new File(reportDir, "issues-report-light.html");
    settings.setProperty(IssuesReportPlugin.HTML_REPORT_LOCATION_KEY, reportDir.getAbsolutePath());
    settings.setProperty(IssuesReportPlugin.HTML_REPORT_NAME_KEY, IssuesReportPlugin.HTML_REPORT_NAME_DEFAULT);

    when(fs.encoding()).thenReturn(Charsets.UTF_8);

    ResourceNode pac = IssuesReportFakeUtils.fakePackage("com.foo");
    mockRuleNameProvider();

    IssuesReport report = IssuesReportFakeUtils.sampleReportWith2IssuesPerFile(pac);

    htmlPrinter.print(report);

    assertThat(reportFile).exists();
    assertThat(lightReportFile).exists();
  }
}
