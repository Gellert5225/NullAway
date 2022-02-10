package com.uber.nullaway;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NullAwayJspecifyTests {
  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  protected NullAwayJspecifyCompilationHelper defaultCompilationHelper;

  @SuppressWarnings("CheckReturnValue")
  @Before
  public void setup() {
    defaultCompilationHelper =
        makeTestHelperWithArgs(
            Arrays.asList(
                "-d",
                temporaryFolder.getRoot().getAbsolutePath(),
                "-XepOpt:NullAway:KnownInitializers="
                    + "com.uber.nullaway.testdata.CheckFieldInitNegativeCases.Super.doInit,"
                    + "com.uber.nullaway.testdata.CheckFieldInitNegativeCases"
                    + ".SuperInterface.doInit2",
                "-XepOpt:NullAway:AnnotatedPackages=com.uber,com.ubercab,io.reactivex"));
  }

  protected NullAwayJspecifyCompilationHelper makeTestHelperWithArgs(List<String> args) {
    return NullAwayJspecifyCompilationHelper.newInstance(NullAway.class, getClass()).setArgs(args);
  }

  @Test
  public void sampleTest() {
    defaultCompilationHelper.addSourceFile("sample-jspecify/NullCheck.java").doTest();
  }
}
