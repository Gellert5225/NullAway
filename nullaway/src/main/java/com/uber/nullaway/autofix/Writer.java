package com.uber.nullaway.autofix;

import com.google.errorprone.VisitorState;
import com.uber.nullaway.ErrorMessage;
import com.uber.nullaway.autofix.out.ErrorInfo;
import com.uber.nullaway.autofix.out.Fix;
import com.uber.nullaway.autofix.out.SeperatedValueDisplay;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Writer {
  public final Path ERROR;
  public final Path SUGGEST_FIX;
  public final String DELIMITER = "$*$";

  public Writer(AutoFixConfig config) {
    String outputDirectory = config.OUTPUT_DIRECTORY;
    this.ERROR = Paths.get(outputDirectory, "errors.csv");
    this.SUGGEST_FIX = Paths.get(outputDirectory, "fixes.csv");
    reset(config);
  }

  public void saveFix(Fix fix) {
    appendToFile(fix, SUGGEST_FIX);
  }

  public void saveErrorNode(ErrorMessage errorMessage, VisitorState state, boolean deep) {
    ErrorInfo error = new ErrorInfo(errorMessage);
    if (deep) {
      error.findEnclosing(state);
    }
    appendToFile(error, ERROR);
  }

  private void resetFile(Path path, String header) {
    try {
      Files.deleteIfExists(path);
      OutputStream os = new FileOutputStream(path.toFile());
      header += "\n";
      os.write(header.getBytes(Charset.defaultCharset()), 0, header.length());
      os.flush();
      os.close();
    } catch (IOException e) {
      throw new RuntimeException(
          "Could not finish resetting File at Path: " + path + ", Exception: " + e);
    }
  }

  private void reset(AutoFixConfig config) {
    try {
      Files.createDirectories(Paths.get(config.OUTPUT_DIRECTORY));
      if (config.SUGGEST_ENABLED) {
        resetFile(SUGGEST_FIX, Fix.header(DELIMITER));
      }
      if (config.LOG_ERROR_ENABLED) {
        resetFile(ERROR, ErrorInfo.header(DELIMITER));
      }
    } catch (IOException e) {
      throw new RuntimeException("Could not finish resetting writer: " + e);
    }
  }

  private void appendToFile(SeperatedValueDisplay value, Path path) {
    OutputStream os;
    String display = value.display(DELIMITER);
    if (display == null || display.equals("")) {
      return;
    }
    display = display.replaceAll("\\R+", " ").replaceAll("\t", "") + "\n";
    try {
      os = new FileOutputStream(path.toFile(), true);
      os.write(display.getBytes(Charset.defaultCharset()), 0, display.length());
      os.flush();
      os.close();
    } catch (Exception e) {
      System.err.println("Error happened for writing at file: " + path);
    }
  }
}
