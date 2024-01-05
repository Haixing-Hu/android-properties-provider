////////////////////////////////////////////////////////////////////////////////
//
//    Copyright (c) 2022 - 2024.
//    Haixing Hu, Qubit Co. Ltd.
//
//    All rights reserved.
//
////////////////////////////////////////////////////////////////////////////////
package ltd.qubit.android.commons.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.os.Build.VERSION_CODES;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The class of configurations.
 *
 * @author Haixing Hu
 */
@RequiresApi(api = VERSION_CODES.GINGERBREAD)
public class Config extends ConcurrentHashMap<String, String> {

  public static final String FILE_NAME = "config.properties";

  private final File file;
  private final Charset charset;

  /**
   * Gets the configuration of an application.
   *
   * @param context
   *     The context of an application.
   * @return
   *     The singleton instance of the configuration.
   */
  public static Config get(final Context context) throws IOException {
    final File file = new File(context.getFilesDir(), FILE_NAME);
    final Config config = new Config(file, UTF_8);
    final Logger logger = LoggerFactory.getLogger(Config.class);
    if (file.exists()) {
      logger.info("Loading configuration from {}", file);
      config.load();
    } else {
      logger.info("Creating configuration at {}", file);
      config.store();
    }
    return config;
  }

  /**
   * Construct the configuration from a properties file.
   *
   * @param file
   *     The path of the properties file.
   * @param charset
   *     The charset of the properties file.
   */
  private Config(final File file, final Charset charset) {
    this.file = Objects.requireNonNull(file);
    this.charset = Objects.requireNonNull(charset);
  }

  public File getFile() {
    return file;
  }

  public Charset getCharset() {
    return charset;
  }

  /**
   * Loads the configuration from the properties file.
   */
  public synchronized void load() throws IOException {
    try (final InputStream in = new FileInputStream(file)) {
      final Reader reader = new InputStreamReader(in, charset);
      final Properties properties = new Properties();
      properties.load(reader);
      this.clear();
      for (final String key : properties.stringPropertyNames()) {
        this.put(key, properties.getProperty(key));
      }
    }
  }

  /**
   * Saves the configuration to the properties file.
   */
  public void store() throws IOException {
    this.store(null);
  }

  /**
   * Saves the configuration to the properties file.
   *
   * @param comments
   *     The comments to be written to the properties file, or {@code null} if
   *     no comments.
   */
  public synchronized void store(@Nullable final String comments) throws IOException {
    try (final OutputStream out = new FileOutputStream(file)) {
      final Writer writer = new OutputStreamWriter(out, charset);
      final Properties properties = new Properties();
      for (final Entry<String, String> entry : this.entrySet()) {
        properties.setProperty(entry.getKey(), entry.getValue());
      }
      properties.store(writer, comments);
    }
  }
}