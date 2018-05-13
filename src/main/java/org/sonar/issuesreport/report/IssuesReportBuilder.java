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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.issuesreport.provider.RuleProvider;
import org.sonar.issuesreport.fs.ResourceNode;
import org.sonar.issuesreport.fs.InputFilesCollector;

import java.util.Date;

import javax.annotation.CheckForNull;

@ScannerSide
public class IssuesReportBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(IssuesReportBuilder.class);

  private final RuleProvider ruleProvider;
  private final InputFilesCollector inputFilesCollector;

  public IssuesReportBuilder(RuleProvider ruleProvider, InputFilesCollector inputFilesCollector) {
    this.ruleProvider = ruleProvider;
    this.inputFilesCollector = inputFilesCollector;
  }

  public IssuesReport buildReport(PostJobContext context) {
    IssuesReport issuesReport = new IssuesReport();
    issuesReport.setTitle(context.config().get(CoreProperties.PROJECT_NAME_PROPERTY).orElse(""));
    issuesReport.setDate(new Date());

    processIssues(issuesReport, context.issues(), false);
    processIssues(issuesReport, context.resolvedIssues(), true);

    return issuesReport;
  }

  private void processIssues(IssuesReport issuesReport, Iterable<PostJobIssue> issues, boolean resolved) {
    for (PostJobIssue issue : issues) {
      Rule rule = findRule(issue);
      ResourceNode resource = inputFilesCollector.getResource(issue.componentKey());
      if (!validate(issue, rule, resource)) {
        continue;
      }
      if (resolved) {
        issuesReport.addResolvedIssueOnResource(resource, issue, rule);
      } else {
        issuesReport.addIssueOnResource(resource, issue, rule);
      }
    }
  }

  private boolean validate(PostJobIssue issue, Rule rule, ResourceNode resource) {
    if (rule == null) {
      LOG.warn("Unknow rule for issue {}", issue);
      return false;
    }
    if (resource == null) {
      LOG.debug("Unknow resource with key {}", issue.componentKey());
      return false;
    }
    return true;
  }

  @CheckForNull
  private Rule findRule(PostJobIssue issue) {
    RuleKey ruleKey = issue.ruleKey();
    return ruleProvider.getRule(ruleKey);
  }
}
