package com.reduce;

import com.main.Context;
/**
 * Provides an abstraction of Reducer
 */
public abstract class Reducer {
	public abstract void reduce(String key, Iterable<String> value, Context context);
}