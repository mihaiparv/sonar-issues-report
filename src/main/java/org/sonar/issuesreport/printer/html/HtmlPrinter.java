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

import com.google.common.collect.Maps;

import freemarker.log.Logger;
import freemarker.template.Template;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Loggers;
import org.sonar.issuesreport.IssuesReportPlugin;
import org.sonar.issuesreport.printer.ReportPrinter;
import org.sonar.issuesreport.provider.RuleNameProvider;
import org.sonar.issuesreport.provider.SourceProvider;
import org.sonar.issuesreport.report.IssuesReport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.Map;

public class HtmlPrinter implements ReportPrinter {

  private static final org.sonar.api.utils.log.Logger LOG = Loggers.get(HtmlPrinter.class);

  private final RuleNameProvider ruleNameProvider;
  private final FileSystem fs;
  private Configuration settings;

  private SourceProvider sourceProvider;

  public HtmlPrinter(RuleNameProvider ruleNameProvider, SourceProvider sourceProvider, FileSystem fs, Configuration settings) {
    this.ruleNameProvider = ruleNameProvider;
    this.sourceProvider = sourceProvider;
    this.fs = fs;
    this.settings = settings;
  }

  public String getRequiredProperty() {
    return IssuesReportPlugin.HTML_REPORT_ENABLED_KEY;
  }

  public boolean isLightModeOnly() {
    return settings.getBoolean(IssuesReportPlugin.HTML_REPORT_LIGHTMODE_ONLY).orElse(Boolean.FALSE);
  }

  public void print(IssuesReport report) {
    File reportFileDir = getReportFileDir();
    String reportName = settings.get(IssuesReportPlugin.HTML_REPORT_NAME_KEY).orElse(null);
    if (!isLightModeOnly()) {
      File reportFile = new File(reportFileDir, reportName + ".html");
      if (LOG.isDebugEnabled()) {
        LOG.debug(String.format("Generating HTML Report to: %s", reportFile.getAbsolutePath()));
      }
      writeToFile(report, reportFile, true);
      LOG.info(String.format("HTML Issues Report generated: %s", reportFile.getAbsolutePath()));
    }
    File lightReportFile = new File(reportFileDir, reportName + "-light.html");
    if (LOG.isDebugEnabled()) {
      LOG.debug(String.format("Generating Light HTML Report to: %s", lightReportFile.getAbsolutePath()));
    }
    writeToFile(report, lightReportFile, false);
    LOG.info(String.format("Light HTML Issues Report generated: %s", lightReportFile.getAbsolutePath()));
    try {
      copyDependencies(reportFileDir);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to copy HTML report resources to: " + reportFileDir, e);
    }
  }

  private File getReportFileDir() {
    String reportFileDirStr = settings.get(IssuesReportPlugin.HTML_REPORT_LOCATION_KEY).orElse("");
    File reportFileDir = new File(reportFileDirStr);
    if (!reportFileDir.isAbsolute()) {
      reportFileDir = new File(fs.workDir(), reportFileDirStr);
    }
    if (reportFileDirStr.endsWith(".html")) {
      LOG.warn(String.format("%s should indicate a directory. Using parent folder.",
                             IssuesReportPlugin.HTML_REPORT_LOCATION_KEY));
      reportFileDir = reportFileDir.getParentFile();
    }
    try {
      FileUtils.forceMkdir(reportFileDir);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create the directory " + reportFileDirStr, e);
    }
    return reportFileDir;
  }

  public void writeToFile(IssuesReport report, File toFile, boolean complete) {
    Writer writer = null;
    FileOutputStream fos = null;
    try {
      Logger.selectLoggerLibrary(Logger.LIBRARY_NONE);
      freemarker.template.Configuration cfg = new freemarker.template.Configuration();
      cfg.setClassForTemplateLoading(HtmlPrinter.class, "");

      Map<String, Object> root = Maps.newHashMap();
      root.put("report", report);
      root.put("ruleNameProvider", ruleNameProvider);
      root.put("sourceProvider", sourceProvider);
      root.put("complete", complete);

      Template template = cfg.getTemplate("issuesreport.ftl");
      fos = new FileOutputStream(toFile);
      writer = new OutputStreamWriter(fos, fs.encoding());
      template.process(root, writer);
      writer.flush();

    } catch (Exception e) {
      throw new IllegalStateException("Fail to generate HTML Issues Report to: " + toFile, e);

    } finally {
      IOUtils.closeQuietly(writer);
      IOUtils.closeQuietly(fos);
    }
  }

  void copyDependencies(File toDir) throws URISyntaxException, IOException {
    File target = new File(toDir, "issuesreport_files");
    FileUtils.forceMkdir(target);

    // I don't know how to extract a directory from classpath, that's why an exhaustive list of files
    // is provided here :
    copyDependency(target, "sonar.eot");
    copyDependency(target, "sonar.svg");
    copyDependency(target, "sonar.ttf");
    copyDependency(target, "sonar.woff");
    copyDependency(target, "favicon.ico");
    copyDependency(target, "PRJ.png");
    copyDependency(target, "DIR.png");
    copyDependency(target, "FIL.png");
    copyDependency(target, "jquery.min.js");
    copyDependency(target, "sep12.png");
    copyDependency(target, "sonar.css");
    copyDependency(target, "sonarqube-24x100.png");
  }

  private void copyDependency(File target, String filename) {
    InputStream input = null;
    OutputStream output = null;
    try {
      input = getClass().getResourceAsStream("/org/sonar/issuesreport/printer/html/issuesreport_files/" + filename);
      output = new FileOutputStream(new File(target, filename));
      IOUtils.copy(input, output);

    } catch (IOException e) {
      throw new IllegalStateException("Fail to copy file " + filename + " to " + target, e);
    } finally {
      IOUtils.closeQuietly(input);
      IOUtils.closeQuietly(output);
    }
  }
}
