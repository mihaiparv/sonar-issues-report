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

import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.issuesreport.IssuesReportPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.CheckForNull;

import static org.sonar.issuesreport.fs.InputFilesCollector.InputFilesCollectorPredicate.shouldExecuteOnProject;

@ScannerSide
public class InputFilesCollector implements Sensor {

  private static final Logger LOGGER = Loggers.get(InputFilesCollector.class);

  private static final Map<String, ResourceNode> RESOURCE_BY_KEY = new HashMap<>();

  @CheckForNull
  public ResourceNode getResource(String componentKey) {
    return RESOURCE_BY_KEY.get(componentKey);
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("Input Files Collector for Issue Report")
        .onlyWhenConfiguration(shouldExecuteOnProject());
  }

  private Iterable<InputFile> getInputFiles(SensorContext context) {
    final FileSystem fileSystem = context.fileSystem();
    final FilePredicates p = fileSystem.predicates();

    return fileSystem.inputFiles(
        p.doesNotMatchPathPatterns(context.config().getStringArray(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY)));
  }

  @Override
  public void execute(SensorContext context) {
    getInputFiles(context).forEach(file -> {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(String.format("Path: %s", file.uri()));
      }
      ResourceNode resourceNode = new ResourceNode(file);
      RESOURCE_BY_KEY.put(file.key(), resourceNode);
    });
  }

  static class InputFilesCollectorPredicate {

    private InputFilesCollectorPredicate() {
    }

    static Predicate<Configuration> shouldExecuteOnProject() {
      return conf ->
          isPreviewAnalysis(conf.get(CoreProperties.ANALYSIS_MODE).orElse(""))
          && (conf.getBoolean(IssuesReportPlugin.HTML_REPORT_ENABLED_KEY).orElse(false)
              || conf.getBoolean(IssuesReportPlugin.CONSOLE_REPORT_ENABLED_KEY)
                  .orElse(false));
    }

    private static boolean isPreviewAnalysis(String mode) {
      return StringUtils.isNotEmpty(mode) && (CoreProperties.ANALYSIS_MODE_ISSUES.equals(mode)
                                              || CoreProperties.ANALYSIS_MODE_PREVIEW.equals(mode));
    }
  }
}
