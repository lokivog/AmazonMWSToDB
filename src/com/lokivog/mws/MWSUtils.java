package com.lokivog.mws;

import java.util.ArrayList;
import java.util.List;

public class MWSUtils {

	/**
	 * Splits a list into multiple sub lists.
	 * 
	 * @param list the list
	 * @param i the i
	 * @return the list
	 */
	public static List split(final List list, int i) {
		int size = list.size();
		int number = size / i;
		int remain = size % i;
		if (remain != 0) {
			number++;
		}
		List out = new ArrayList<List>(number);
		for (int j = 0; j < number; j++) {
			int start = j * i;
			int end = start + i;
			if (end > list.size()) {
				end = list.size();
			}
			out.add(list.subList(start, end));
		}
		return out;
	}

}
