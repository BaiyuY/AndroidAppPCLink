/*
 * Copyright (C) 2012 TaiDoc Technology Corporation. All rights reserved.
 */
package com.taidoc.pclinklibrary.demo.util;

import android.content.Context;

/**
 * SharedPreferences utility
 * 
 * @author Cloud Tu
 * @author Jay Lee
 */
public class SharePreferencesUtils {
    /**
     * SharedPreferences name
     */
    public static final String SHARED_PREFERENCES_NAME = "com.taidoc.pclinklibrary.demo_preferences";

    /**
     * 檢查某一個對應的SharedPreference key是否存在
     * 
     * @param context
     *            Context
     * @param key
     *            Preferences key
     * @return true, contain, false, not exist
     */
    public static boolean checkSharedPreferencesKey(Context context, String key) {
        return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .contains(key);
    }

    /**
     * Clear all data in sharedPreferences
     * 
     * @param context
     *            Context
     * 
     * @return true:執行成功,false:執行失敗
     */
    public static boolean clearAllDataInSharedPreferences(Context context) {
        return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                .clear().commit();
    }

    /**
     * 取得SharedPreference的總數
     * 
     * @param context
     *            Context
     * @return The size of SharedPreference
     */
    public static int getSharedPreferencesSize(Context context) {
        return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).getAll()
                .size();
    }

    /**
     * Get the preference value from SharedPreferences
     * 
     * @param context
     *            Context
     * @param configName
     *            Config name
     * @param configDefaultValue
     *            Default config value,when no value found
     * 
     * @return Config value in SharedPreferences
     * 
     * @throws RuntimeException
     *             執行失敗時 ，丟出RuntimeException
     */
    public static Object getValueFromSharedPreferences(Context context, String configName,
            Object configDefaultValue) {
    	try {
	        if (configDefaultValue.getClass() == Boolean.class) {
	            return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
	                    .getBoolean(configName, (Boolean) configDefaultValue);
	        } else if (configDefaultValue.getClass() == Float.class) {
	            return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
	                    .getFloat(configName, (Float) configDefaultValue);
	        } else if (configDefaultValue.getClass() == Integer.class) {
	            return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
	                    .getInt(configName, (Integer) configDefaultValue);
	        } else if (configDefaultValue.getClass() == Long.class) {
	            return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
	                    .getLong(configName, (Long) configDefaultValue);
	        } else if (configDefaultValue.getClass() == String.class) {
	            return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
	                    .getString(configName, (String) configDefaultValue);
	        } /* end of if */
    	}
    	catch (ClassCastException cce) {
    		return configDefaultValue;
    	}
    	catch (Exception e) {
    		return configDefaultValue;
    	}

    	throw new RuntimeException("Restore fail");
    }

    /**
     * Save the value to SharedPreferences
     * 
     * @param context
     *            Context
     * @param configName
     *            Config name
     * @param configValue
     *            Config value
     * 
     * @throws RuntimeException
     *             執行失敗時 ，丟出RuntimeException
     */
    public static void setValueToSharedPreferences(Context context, String configName,
            Object configValue) {
        if (configValue instanceof Boolean) {
            context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                    .putBoolean(configName, (Boolean) configValue).commit();
        } else if (configValue instanceof Float) {
            context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                    .putFloat(configName, (Float) configValue).commit();
        } else if (configValue instanceof Integer) {
            context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                    .putInt(configName, (Integer) configValue).commit();
        } else if (configValue instanceof Long) {
            context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                    .putLong(configName, (Long) configValue).commit();
        } else if (configValue instanceof String) {
            context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                    .putString(configName, (String) configValue).commit();
        } else {
            throw new RuntimeException("Save fail");
        } /* end of if */
    }
}
