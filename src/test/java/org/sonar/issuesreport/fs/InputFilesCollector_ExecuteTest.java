package org.sonar.issuesreport.fs;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

import static org.fest.assertions.Assertions.assertThat;

public class InputFilesCollector_ExecuteTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private SensorContextTester sensorContext;

  private InputFilesCollector inputFilesCollector;

  @Before
  public void setup() {
     sensorContext = SensorContextTester.create(temp.getRoot());
     inputFilesCollector = new InputFilesCollector();
  }

  @Test
  public void testValidInputFile() {
    // given
    sensorContext.fileSystem().add(new TestInputFileBuilder("a","b.c")
                                       .setModuleBaseDir(temp.getRoot().toPath()).build());

    // when
    inputFilesCollector.execute(sensorContext);

    // then
    assertThat(inputFilesCollector.getResource("a:b.c")).isNotNull();
  }

  @Test
  public void testExcludedInputFile() {
    // given
    sensorContext.fileSystem().add(new TestInputFileBuilder("1","2.3")
                                       .setModuleBaseDir(temp.getRoot().toPath()).build());
    sensorContext.settings().setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "*.3");

    // when
    inputFilesCollector.execute(sensorContext);

    // then
    assertThat(inputFilesCollector.getResource("1:2.3")).isNull();
  }

}
