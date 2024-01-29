////////////////////////////////////////////////////////////////////////////////
//
//    Copyright (c) 2017 - 2024.
//    Nanjing Smart Medical Investment Operation Service Co. Ltd.
//
//    All rights reserved.
//
////////////////////////////////////////////////////////////////////////////////
package cn.njzhyl.android.commons.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import static cn.njzhyl.android.commons.config.ConfigProvider.PATH_CLEAR;
import static cn.njzhyl.android.commons.config.ConfigProvider.PATH_CONTAINS;
import static cn.njzhyl.android.commons.config.ConfigProvider.PATH_GET;
import static cn.njzhyl.android.commons.config.ConfigProvider.PATH_GET_ALL;
import static cn.njzhyl.android.commons.config.ConfigProvider.PATH_PUT;
import static cn.njzhyl.android.commons.config.ConfigProvider.PATH_PUT_ALL;
import static cn.njzhyl.android.commons.config.ConfigProvider.PATH_REMOVE;
import static cn.njzhyl.android.commons.config.ConfigProvider.PATH_SAVE;

/**
 * The class of objects used to consume the shared configurations.
 *
 * @author Haixing Hu
 */
@RequiresApi(api = VERSION_CODES.GINGERBREAD)
public class ConfigConsumer {

  private final String authority;
  private final ContentResolver resolver;

  /**
   * Constructs a configuration consumer to access the shared configurations
   * with the specified authority.
   *
   * @param authority
   *     The authority of the configuration provider.
   * @param context
   *     The context of the current application.
   */
  public ConfigConsumer(@NonNull final String authority,
      @NonNull final Context context) {
    this.authority = Objects.requireNonNull(authority);
    this.resolver = context.getContentResolver();
  }

  private Uri buildUri(final String path) {
    return Uri.parse("content://" + authority + "/" + path);
  }

  /**
   * Tests whether the shared configurations contain a key.
   *
   * @param key
   *     the key to be tested.
   * @return
   *     {@code true} if the shared configurations contain the specified key;
   *     {@code false} otherwise.
   */
  public boolean contains(final String key) {
    final Uri uri = buildUri(PATH_CONTAINS);
    final String[] args = new String[]{ key };
    try (final Cursor cursor = resolver.query(uri, null, null, args, null)) {
      if ((cursor != null) && cursor.moveToFirst()) {
        final int index = cursor.getColumnIndex("contains");
        return (cursor.getInt(index) == 1);
      }
      return false;
    }
  }

  /**
   * Gets the value of a key in the shared configurations.
   *
   * @param key
   *     the key whose value is to be retrieved.
   * @return
   *     the value of the specified key; or {@code null} if the key is not
   *     found.
   */
  public String get(final String key) {
    final Uri uri = buildUri(PATH_GET);
    final String[] args = new String[]{ key };
    try (final Cursor cursor = resolver.query(uri, null, null, args, null)) {
      if ((cursor != null) && cursor.moveToFirst()) {
        final int index = cursor.getColumnIndex("value");
        return cursor.getString(index);
      }
      return null;
    }
  }

  /**
   * Gets all key-value pairs in the shared configurations.
   *
   * @return
   *     a map containing all key-value pairs in the shared configurations.
   */
  public Map<String, String> getAll() {
    final Map<String, String> result = new HashMap<>();
    final Uri uri = buildUri(PATH_GET_ALL);
    try (final Cursor cursor = resolver.query(uri, null, null, null, null)) {
      if (cursor != null) {
        final int keyIndex = cursor.getColumnIndex("key");
        final int valueIndex = cursor.getColumnIndex("value");
        while (cursor.moveToNext()) {
          final String key = cursor.getString(keyIndex);
          final String value = cursor.getString(valueIndex);
          result.put(key, value);
        }
      }
    }
    return result;
  }

  /**
   * Puts a key-value pair into the shared configurations.
   *
   * @param key
   *     the key of the pair.
   * @param value
   *     the value of the pair.
   */
  public void put(final String key, final String value) {
    final Uri uri = buildUri(PATH_PUT);
    final ContentValues values = new ContentValues();
    values.put("key", key);
    values.put("value", value);
    resolver.insert(uri, values);
  }

  /**
   * Puts all key-value pairs into the shared configurations.
   *
   * @param map
   *     the map containing the key-value pairs to be put.
   * @return
   *     the number of key-value pairs put.
   */
  public int putAll(final Map<String, String> map) {
    final Uri uri = buildUri(PATH_PUT_ALL);
    final ContentValues values = new ContentValues();
    for (final Entry<String, String> entry : map.entrySet()) {
      values.put(entry.getKey(), entry.getValue());
    }
    resolver.insert(uri, values);
    return map.size();
  }

  /**
   * Removes a key-value pair from the shared configurations.
   *
   * @param key
   *     the key of the pair to be removed.
   * @return
   *     {@code true} if the key-value pair is removed; {@code false} otherwise.
   */
  public boolean remove(final String key) {
    final Uri uri = buildUri(PATH_REMOVE);
    final String[] args = new String[]{ key };
    return (resolver.delete(uri, null, args) > 0);
  }

  /**
   * Removes all key-value pairs from the shared configurations.
   *
   * @return
   *     the number of key-value pairs removed.
   */
  public int clear() {
    final Uri uri = buildUri(PATH_CLEAR);
    return resolver.delete(uri, null, null);
  }

  /**
   * Saves all the changes of the shared configurations.
   * <p>
   * The changes will be saved to the internal storage of the App sharing the
   * configuration.
   *
   * @return
   *     the number of key-value pairs saved.
   */
  public int save() {
    final Uri uri = buildUri(PATH_SAVE);
    return resolver.update(uri, null, null, null);
  }

  /**
   * Backups all the key-value pairs of the shared configurations to another
   * configuration.
   *
   * @param config
   *     the configuration to be backed up to.
   * @return
   *     the number of key-value pairs backed up.
   */
  public int backupTo(final Config config) {
    final Map<String, String> data = this.getAll();
    config.clear();
    config.putAll(data);
    return config.size();
  }

  /**
   * Restores all the key-value pairs of the shared configurations from another
   * configuration.
   *
   * @param config
   *     the configuration to be restored from.
   * @return
   *     the number of key-value pairs restored.
   */
  public int restoreFrom(final Config config) {
    this.clear();
    this.putAll(config);
    return config.size();
  }
}
