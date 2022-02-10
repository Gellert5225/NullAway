package com.uber.nullaway;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.fail;

import com.google.common.base.Predicate;
import com.google.common.io.CharSource;
import com.google.errorprone.DiagnosticTestHelper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.hamcrest.Matcher;

public class NullAwayJspecifyDiagnosticHelper extends DiagnosticTestHelper {
  private final String checkName;

  private static final String JSPECIFY_MARKER_COMMENT_INLINE = "// jspecify_nullness_mismatch:";

  public NullAwayJspecifyDiagnosticHelper(String checkName) {
    this.checkName = checkName;
  }

  public void assertHasDiagnosticOnAllMatchingLines(JavaFileObject source) throws IOException {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = getDiagnostics();
    LineNumberReader reader =
        new LineNumberReader(CharSource.wrap(source.getCharContent(false)).openStream());
    do {
      String line = reader.readLine();
      if (line == null) {
        break;
      }

      List<Predicate<? super String>> predicates = null;
      if (line.contains(JSPECIFY_MARKER_COMMENT_INLINE)) {
        // Diagnostic must contain all patterns from the bug marker comment.
        List<String> patterns = extractPatterns(line, reader, JSPECIFY_MARKER_COMMENT_INLINE);
        predicates = new ArrayList<>(patterns.size());
        for (String pattern : patterns) {
          predicates.add(new SimpleStringContains(pattern));
        }
      }
      //            else if (line.contains(BUG_MARKER_COMMENT_LOOKUP)) {
      //                int markerLineNumber = reader.getLineNumber();
      //                List<String> lookupKeys = extractPatterns(line, reader,
      // BUG_MARKER_COMMENT_LOOKUP);
      //                predicates = new ArrayList<>(lookupKeys.size());
      //                for (String lookupKey : lookupKeys) {
      //                    assertWithMessage(
      //                        "No expected error message with key [%s] as expected from line [%s]
      // "
      //                            + "with diagnostic [%s]",
      //                        lookupKey, markerLineNumber, line.trim())
      //                        .that(expectedErrorMsgs.containsKey(lookupKey))
      //                        .isTrue();
      //                    predicates.add(expectedErrorMsgs.get(lookupKey));
      //                    usedLookupKeys.add(lookupKey);
      //                }
      //            }

      if (predicates != null) {
        int lineNumber = reader.getLineNumber();
        for (Predicate<? super String> predicate : predicates) {
          Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> patternMatcher =
              hasItem(diagnosticOnLine(source.toUri(), lineNumber, predicate));
          assertWithMessage(
                  "Did not see an error on line %s matching %s. %s",
                  lineNumber, predicate, allErrors(diagnostics))
              .that(patternMatcher.matches(diagnostics))
              .isTrue();
        }

        if (checkName != null) {
          // Diagnostic must contain check name.
          Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> checkNameMatcher =
              hasItem(
                  diagnosticOnLine(
                      source.toUri(), lineNumber, new SimpleStringContains("[" + checkName + "]")));
          assertWithMessage(
                  "Did not see an error on line %s containing [%s]. %s",
                  lineNumber, checkName, allErrors(diagnostics))
              .that(checkNameMatcher.matches(diagnostics))
              .isTrue();
        }

      } else {
        int lineNumber = reader.getLineNumber();
        Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher =
            hasItem(diagnosticOnLine(source.toUri(), lineNumber));
        if (matcher.matches(diagnostics)) {
          fail("Saw unexpected error on line " + lineNumber + ". " + allErrors(diagnostics));
        }
      }
    } while (true);
    reader.close();
  }

  private static String allErrors(List<Diagnostic<? extends JavaFileObject>> diagnostics) {
    if (diagnostics.isEmpty()) {
      return "There were no errors.";
    }
    return "All errors:\n"
        + diagnostics.stream().map(Object::toString).collect(Collectors.joining("\n\n"));
  }

  private static List<String> extractPatterns(
      String line, BufferedReader reader, String matchString) throws IOException {
    int bugMarkerIndex = line.indexOf(matchString);
    if (bugMarkerIndex < 0) {
      throw new IllegalArgumentException("Line must contain bug marker prefix");
    }
    List<String> result = new ArrayList<>();
    String restOfLine = line.substring(bugMarkerIndex + matchString.length()).trim();
    result.add(restOfLine);
    line = reader.readLine().trim();
    while (line.startsWith("//")) {
      restOfLine = line.substring(2).trim();
      result.add(restOfLine);
      line = reader.readLine().trim();
    }

    return result;
  }

  private static class SimpleStringContains implements Predicate<String> {
    private final String pattern;

    SimpleStringContains(String pattern) {
      this.pattern = pattern;
    }

    @Override
    public boolean apply(String input) {
      return input.contains(pattern);
    }

    @Override
    public String toString() {
      return pattern;
    }
  }
}
