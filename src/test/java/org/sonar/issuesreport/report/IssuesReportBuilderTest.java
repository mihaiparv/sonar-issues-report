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
package org.sonar.issuesreport.report;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.issuesreport.IssuesReportFakeUtils;
import org.sonar.issuesreport.fs.InputFilesCollector;
import org.sonar.issuesreport.fs.ResourceNode;
import org.sonar.issuesreport.provider.RuleProvider;

import java.util.Collections;
import java.util.Optional;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IssuesReportBuilderTest {

  private PostJobContext context;
  private RuleProvider ruleFinder;
  private IssuesReportBuilder builder;
  private InputFilesCollector inputFilesCollector;

  @Before
  public void prepare() {
    Configuration config = mock(Configuration.class);
    when(config.get(CoreProperties.PROJECT_NAME_PROPERTY)).thenReturn(Optional.of("Project Name"));

    context = mock(PostJobContext.class);
    when(context.config()).thenReturn(config);

    ruleFinder = mock(RuleProvider.class);
    inputFilesCollector = mock(InputFilesCollector.class);
    builder = new IssuesReportBuilder(ruleFinder, inputFilesCollector);
  }

  @Test
  public void shouldNotFailWhenIssueOnUnknowResource() {
    PostJobIssue fakeIssue = IssuesReportFakeUtils.fakeIssue(false, RuleKey.of("foo", "bar"), "com.foo.Bar", null);
    when(context.issues()).thenReturn(Collections.singletonList(fakeIssue));
    when(context.resolvedIssues()).thenReturn(Collections.emptyList());

    IssuesReport report = builder.buildReport(context);
    assertThat(report.getResourceReports()).isEmpty();
  }

  @Test
  public void shouldNotFailWhenRuleNotFoundOnIssue() {
    ResourceNode fakeFile = IssuesReportFakeUtils.fakeFile("com.foo.Bar");
    when(inputFilesCollector.getResource("com.foo.Bar")).thenReturn(fakeFile);

    PostJobIssue fakeIssue = IssuesReportFakeUtils.fakeIssue(false, RuleKey.of("foo", "bar"), "com.foo.Bar", null);

    when(context.issues()).thenReturn(Collections.singletonList(fakeIssue));
    when(context.resolvedIssues()).thenReturn(Collections.emptyList());

    IssuesReport report = builder.buildReport(context);
    assertThat(report.getResourceReports()).isEmpty();
  }

  @Test
  public void shouldGenerateReportWithOneViolation() {
    ResourceNode fakeFile = IssuesReportFakeUtils.fakeFile("project:com.foo.Bar");
    when(inputFilesCollector.getResource("project:com.foo.Bar")).thenReturn(fakeFile);

    RuleKey ruleKey = RuleKey.of("foo", "bar");
    PostJobIssue fakeIssue = IssuesReportFakeUtils.fakeIssue(false, ruleKey, "project:com.foo.Bar", 4);

    when(context.issues()).thenReturn(Collections.singletonList(fakeIssue));
    when(context.resolvedIssues()).thenReturn(Collections.emptyList());

    Rule fakeRule = IssuesReportFakeUtils.fakeRule(ruleKey);
    when(ruleFinder.getRule(eq(ruleKey))).thenReturn(fakeRule);

    IssuesReport report = builder.buildReport(context);
    assertThat(report.getSummary().getTotal().getCountInCurrentAnalysis()).isEqualTo(1);
    assertThat(report.getSummary().getTotal().getNewIssuesCount()).isEqualTo(0);
    assertThat(report.getSummary().getTotal().getResolvedIssuesCount()).isEqualTo(0);
    assertThat(report.getResourceReports()).hasSize(1);
    ResourceReport resourceReport = report.getResourceReports().get(0);
    assertThat(resourceReport.getName()).isEqualTo("foo.bar.Foo");
    assertThat(resourceReport.getTotal().getCountInCurrentAnalysis()).isEqualTo(1);
    assertThat(resourceReport.getTotal().getNewIssuesCount()).isEqualTo(0);
    assertThat(resourceReport.getTotal().getResolvedIssuesCount()).isEqualTo(0);

    assertThat(resourceReport.isDisplayableLine(1, false)).isEqualTo(false);
    assertThat(resourceReport.isDisplayableLine(2, false)).isEqualTo(false);
    assertThat(resourceReport.isDisplayableLine(2, true)).isEqualTo(true);
    assertThat(resourceReport.isDisplayableLine(3, false)).isEqualTo(false);
    assertThat(resourceReport.isDisplayableLine(3, true)).isEqualTo(true);
    assertThat(resourceReport.isDisplayableLine(4, false)).isEqualTo(false);
    assertThat(resourceReport.isDisplayableLine(4, true)).isEqualTo(true);
    assertThat(resourceReport.isDisplayableLine(5, false)).isEqualTo(false);
    assertThat(resourceReport.isDisplayableLine(5, true)).isEqualTo(true);
    assertThat(resourceReport.isDisplayableLine(6, false)).isEqualTo(false);
    assertThat(resourceReport.isDisplayableLine(6, true)).isEqualTo(true);
    assertThat(resourceReport.isDisplayableLine(7, false)).isEqualTo(false);
  }

  @Test
  public void shouldGenerateReportWithOneNewViolation() {
    ResourceNode fakeFile = IssuesReportFakeUtils.fakeFile("project:com.foo.Bar");
    when(inputFilesCollector.getResource("project:com.foo.Bar")).thenReturn(fakeFile);

    RuleKey ruleKey = RuleKey.of("foo", "bar");
    PostJobIssue fakeIssue = IssuesReportFakeUtils.fakeIssue(true, ruleKey, "project:com.foo.Bar", 4);

    when(context.issues()).thenReturn(Collections.singletonList(fakeIssue));
    when(context.resolvedIssues()).thenReturn(Collections.emptyList());

    Rule fakeRule = IssuesReportFakeUtils.fakeRule(ruleKey);
    when(ruleFinder.getRule(eq(ruleKey))).thenReturn(fakeRule);

    IssuesReport report = builder.buildReport(context);
    assertThat(report.getSummary().getTotal().getCountInCurrentAnalysis()).isEqualTo(1);
    assertThat(report.getSummary().getTotal().getNewIssuesCount()).isEqualTo(1);
    assertThat(report.getSummary().getTotal().getResolvedIssuesCount()).isEqualTo(0);
    assertThat(report.getResourceReports()).hasSize(1);
    ResourceReport resourceReport = report.getResourceReports().get(0);
    assertThat(resourceReport.getName()).isEqualTo("foo.bar.Foo");
    assertThat(resourceReport.getTotal().getCountInCurrentAnalysis()).isEqualTo(1);
    assertThat(resourceReport.getTotal().getNewIssuesCount()).isEqualTo(1);
    assertThat(resourceReport.getTotal().getResolvedIssuesCount()).isEqualTo(0);

    assertThat(resourceReport.isDisplayableLine(null, false)).isEqualTo(false);
    assertThat(resourceReport.isDisplayableLine(0, false)).isEqualTo(false);
    assertThat(resourceReport.isDisplayableLine(1, false)).isEqualTo(false);
    assertThat(resourceReport.isDisplayableLine(2, false)).isEqualTo(true);
    assertThat(resourceReport.isDisplayableLine(3, false)).isEqualTo(true);
    assertThat(resourceReport.isDisplayableLine(4, false)).isEqualTo(true);
    assertThat(resourceReport.isDisplayableLine(5, false)).isEqualTo(true);
    assertThat(resourceReport.isDisplayableLine(6, false)).isEqualTo(true);
    assertThat(resourceReport.isDisplayableLine(7, false)).isEqualTo(false);
  }

  @Test
  public void shouldGenerateReportWithOneNewViolationAndOneResolved() {
    ResourceNode fakeFile = IssuesReportFakeUtils.fakeFile("project:com.foo.Bar");
    when(inputFilesCollector.getResource("project:com.foo.Bar")).thenReturn(fakeFile);

    RuleKey ruleKey = RuleKey.of("foo", "bar");
    PostJobIssue fakeNewIssue = IssuesReportFakeUtils.fakeIssue(true, ruleKey, "project:com.foo.Bar", null);
    PostJobIssue fakeResolvedIssue = IssuesReportFakeUtils.fakeIssue(false, ruleKey, "project:com.foo.Bar", null);

    when(context.issues()).thenReturn(Collections.singletonList(fakeNewIssue));
    when(context.resolvedIssues()).thenReturn(Collections.singletonList(fakeResolvedIssue));

    Rule fakeRule = IssuesReportFakeUtils.fakeRule(ruleKey);
    when(ruleFinder.getRule(eq(ruleKey))).thenReturn(fakeRule);

    IssuesReport report = builder.buildReport(context);
    assertThat(report.getSummary().getTotal().getCountInCurrentAnalysis()).isEqualTo(1);
    assertThat(report.getSummary().getTotal().getNewIssuesCount()).isEqualTo(1);
    assertThat(report.getSummary().getTotal().getResolvedIssuesCount()).isEqualTo(1);
    assertThat(report.getResourceReports()).hasSize(1);
    assertThat(report.getResourceReports().get(0).getName()).isEqualTo("foo.bar.Foo");
    assertThat(report.getResourceReports().get(0).getTotal().getCountInCurrentAnalysis()).isEqualTo(1);
    assertThat(report.getResourceReports().get(0).getTotal().getNewIssuesCount()).isEqualTo(1);
    assertThat(report.getResourceReports().get(0).getTotal().getResolvedIssuesCount()).isEqualTo(1);
  }

}
