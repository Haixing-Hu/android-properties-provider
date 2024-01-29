////////////////////////////////////////////////////////////////////////////////
//
//    Copyright (c) 2017 - 2024.
//    Nanjing Smart Medical Investment Operation Service Co. Ltd.
//
//    All rights reserved.
//
////////////////////////////////////////////////////////////////////////////////
package cn.njzhyl.android.commons.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

@RequiresApi(api = VERSION_CODES.GINGERBREAD)
public class ConfigProvider extends ContentProvider {

  public static final String PATH_CONTAINS = "contains";

  public static final String PATH_GET = "get";

  public static final String PATH_GET_ALL = "getAll";

  public static final String PATH_PUT = "put";

  public static final String PATH_PUT_ALL = "putAll";

  public static final String PATH_REMOVE = "remove";

  public static final String PATH_CLEAR = "clear";

  public static final String PATH_SAVE = "save";

  private static final int CODE_CONTAINS = 0;

  private static final int CODE_GET = 1;

  private static final int CODE_GET_ALL = 2;

  private static final int CODE_PUT = 3;

  private static final int CODE_PUT_ALL = 4;

  private static final int CODE_REMOVE = 5;

  private static final int CODE_CLEAR = 6;

  private static final int CODE_SAVE = 7;

  private final String authority;
  private final Logger logger;
  private final UriMatcher uriMatcher;
  private final HashMap<Integer, String> types;
  private Config config = null;

  public ConfigProvider(final String authority) {
    this.authority = Objects.requireNonNull(authority);
    logger = LoggerFactory.getLogger(this.getClass());
    uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    uriMatcher.addURI(authority, PATH_CONTAINS, CODE_CONTAINS);
    uriMatcher.addURI(authority, PATH_GET, CODE_GET);
    uriMatcher.addURI(authority, PATH_GET_ALL, CODE_GET_ALL);
    uriMatcher.addURI(authority, PATH_PUT, CODE_PUT);
    uriMatcher.addURI(authority, PATH_PUT_ALL, CODE_PUT_ALL);
    uriMatcher.addURI(authority, PATH_REMOVE, CODE_REMOVE);
    uriMatcher.addURI(authority, PATH_CLEAR, CODE_CLEAR);
    uriMatcher.addURI(authority, PATH_SAVE, CODE_SAVE);
    types = new HashMap<>();
    types.put(CODE_CONTAINS, buildType(PATH_CONTAINS));
    types.put(CODE_GET, buildType(PATH_GET));
    types.put(CODE_GET_ALL, buildType(PATH_GET_ALL));
    types.put(CODE_PUT, buildType(PATH_PUT));
    types.put(CODE_PUT_ALL, buildType(PATH_PUT_ALL));
    types.put(CODE_REMOVE, buildType(PATH_REMOVE));
    types.put(CODE_CLEAR, buildType(PATH_CLEAR));
    types.put(CODE_SAVE, buildType(PATH_SAVE));
  }

  private String buildType(final String path) {
    return "vnd.android.cursor.item/vnd." + authority + "." + path;
  }

  public String getAuthority() {
    return authority;
  }

  @Override
  public boolean onCreate() {
    logger.debug("Creating the configuration provider.");
    if (config == null) {
      final Context context = this.getContext();
      assert context != null;
      try {
        config = Config.get(context);
      } catch (final Exception e) {
        logger.error("Failed to get the configuration.", e);
        config = null;
        return false;
      }
    }
    return true;
  }

  @Nullable
  @Override
  public Cursor query(@NonNull final Uri uri,
      @Nullable final String[] projection, @Nullable final String selection,
      @Nullable final String[] selectionArgs,
      @Nullable final String sortOrder) {
    if (config == null) {
      return null;
    }
    switch (uriMatcher.match(uri)) {
      case CODE_CONTAINS:
        return contains(selectionArgs);
      case CODE_GET:
        return get(selectionArgs);
      case CODE_GET_ALL:
        return getAll();
      default:
        return null;
    }
  }

  private Cursor contains(@Nullable final String[] selectionArgs) {
    if ((selectionArgs == null) || (selectionArgs.length == 0)) {
      return null;
    }
    final String key = selectionArgs[0];
    final boolean contains = config.containsKey(key);
    logger.debug("Checking the existence of the configuration item: contains(\"{}\") = {}", key, contains);
    final MatrixCursor cursor = new MatrixCursor(new String[]{"key", "contains"});
    cursor.addRow(new Object[]{key, contains ? 1 : 0});
    return cursor;
  }

  private Cursor get(@Nullable final String[] selectionArgs) {
    if ((selectionArgs == null) || (selectionArgs.length == 0)) {
      return null;
    }
    final String key = selectionArgs[0];
    final String value = config.get(key);
    logger.debug("Getting the configuration item: {} = {}", key, value);
    final MatrixCursor cursor = new MatrixCursor(new String[]{"value"});
    cursor.addRow(new Object[]{value});
    return cursor;
  }

  private MatrixCursor getAll() {
    final MatrixCursor cursor = new MatrixCursor(new String[]{"key", "value"});
    for (final Map.Entry<String, String> entry : config.entrySet()) {
      cursor.addRow(new Object[]{entry.getKey(), entry.getValue()});
    }
    return cursor;
  }

  @Nullable
  @Override
  public Uri insert(@NonNull final Uri uri,
      @Nullable final ContentValues values) {
    if (config == null) {
      return null;
    }
    switch (uriMatcher.match(uri)) {
      case CODE_PUT:
        return put(uri, values);
      case CODE_PUT_ALL:
        return putAll(uri, values);
      default:
        return null;
    }
  }

  private Uri put(@NonNull final Uri uri, @Nullable final ContentValues values) {
    if (values == null) {
      return null;
    }
    final String key = values.getAsString("key");
    final String value = values.getAsString("value");
    logger.debug("Setting the configuration item: {} = {}", key, value);
    config.put(key, value);
    return Uri.withAppendedPath(uri, key);
  }

  private Uri putAll(@NonNull final Uri uri, @Nullable final ContentValues values) {
    if (values == null) {
      return null;
    }
    logger.debug("Setting the configuration items.");
    for (final Entry<String, Object> entry : values.valueSet()) {
      final String key = entry.getKey();
      final String value = entry.getValue().toString();
      logger.debug("Setting the configuration item: {} = {}", key, value);
      config.put(key, value);
    }
    return uri;
  }

  @Override
  public int delete(@NonNull final Uri uri, @Nullable final String selection,
      @Nullable final String[] selectionArgs) {
    if (config == null) {
      return 0;
    }
    switch (uriMatcher.match(uri)) {
      case CODE_REMOVE:
        return remove(selectionArgs);
      case CODE_CLEAR:
        return clear();
      default:
        return 0;
    }
  }

  private int remove(@Nullable final String[] selectionArgs) {
    if ((selectionArgs == null) || (selectionArgs.length == 0)) {
      return 0;
    }
    final String key = selectionArgs[0];
    logger.debug("Removing the configuration item: {}", key);
    return (config.remove(key) == null ? 0 : 1);
  }

  private int clear() {
    final int size = config.size();
    logger.debug("Clearing all the configuration items.");
    config.clear();
    return size;
  }

  @Override
  public int update(@NonNull final Uri uri,
      @Nullable final ContentValues values, @Nullable final String selection,
      @Nullable final String[] selectionArgs) {
    if (config == null) {
      return 0;
    }
    switch (uriMatcher.match(uri)) {
      case CODE_SAVE:
        return save();
      default:
        return 0;
    }
  }

  private int save() {
    logger.info("Storing the configuration.");
    final int size = config.size();
    try {
      config.store();
    } catch (final IOException e) {
      logger.error("Failed to store the configuration.", e);
      return 0;
    }
    return size;
  }

  @Nullable
  @Override
  public String getType(@NonNull final Uri uri) {
    final int code = uriMatcher.match(uri);
    return types.get(code);
  }
}
