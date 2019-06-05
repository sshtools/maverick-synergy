package com.sshtools.server.vshell.terminal;

import java.util.Comparator;

/**
 * <p>
 * Title:
 * </p>
 * 
 * <p>
 * Description:
 * </p>
 * 
 * <p>
 * Copyright: Copyright (c) 2005
 * </p>
 * 
 * <p>
 * Company:
 * </p>
 * 
 * @author not attributable
 */
public class EqualsComparator implements Comparator<String> {
	private int length_;

	public EqualsComparator(int length) {
		length_ = length;
	}

	public int compare(String left, String right) {
		String leftS = left;
		String rightS = (String) right;

		if ((leftS.length() >= length_) && (rightS.length() >= length_)) {
			return leftS.substring(0, length_).compareTo(
					rightS.substring(0, length_));
		}
		return leftS.compareTo(rightS);
	}
}
