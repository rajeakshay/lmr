package com.map;

import com.main.Context;
/**
 * Provides an abstraction of Mapper
 */
public abstract class Mapper {
	public abstract void map(String key, String value, Context context);
}