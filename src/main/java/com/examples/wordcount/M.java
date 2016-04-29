package com.examples.wordcount;

import com.main.Context;
import com.map.Mapper;

/**
 * Example mapper class
 */
public class M extends Mapper {
	
	/**
	 * Custom map method
	 */
	@Override
	public void map(String key, String value, Context context) {
		context.write(value, String.valueOf(1));		
	}
}
