package com.sshtools.common.util;

/*-
 * #%L
 * Utils
 * %%
 * Copyright (C) 2002 - 2023 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

public class Arrays {
	
    /**
     * Returns the index of the median of the three indexed integers.
     */
    private static int med3(int x[], int a, int b, int c) {
		return (x[a] < x[b] ?
			(x[b] < x[c] ? b : x[a] < x[c] ? c : a) :
			(x[b] > x[c] ? b : x[a] > x[c] ? c : a));
    }
	
    /**
     * Swaps x[a] with x[b].
     */
    private static void swap(int x[], int a, int b) {
	int t = x[a];
	x[a] = x[b];
	x[b] = t;
    }
	
    /**
     * Sorts the specified array of ints into ascending numerical order.
     * The sorting algorithm is a tuned quicksort, adapted from Jon
     * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
     * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
     * 1993).  This algorithm offers n*log(n) performance on many data sets
     * that cause other quicksorts to degrade to quadratic performance.
     *
     * @param a the array to be sorted
     */
    public static void sort(int[] a) {
	sort1(a, 0, a.length);
    }

    private static void sort1(int x[], int off, int len) {
    	// Insertion sort on smallest arrays
    	if (len < 7) {
    	    for (int i=off; i<len+off; i++)
    		for (int j=i; j>off && x[j-1]>x[j]; j--)
    		    swap(x, j, j-1);
    	    return;
    	}

    	// Choose a partition element, v
    	int m = off + (len >> 1);       // Small arrays, middle element
    	if (len > 7) {
    	    int l = off;
    	    int n = off + len - 1;
    	    if (len > 40) {        // Big arrays, pseudomedian of 9
    		int s = len/8;
    		l = med3(x, l,     l+s, l+2*s);
    		m = med3(x, m-s,   m,   m+s);
    		n = med3(x, n-2*s, n-s, n);
    	    }
    	    m = med3(x, l, m, n); // Mid-size, med of 3
    	}
    	int v = x[m];

    	// Establish Invariant: v* (<v)* (>v)* v*
    	int a = off, b = a, c = off + len - 1, d = c;
    	while(true) {
    	    while (b <= c && x[b] <= v) {
    		if (x[b] == v)
    		    swap(x, a++, b);
    		b++;
    	    }
    	    while (c >= b && x[c] >= v) {
    		if (x[c] == v)
    		    swap(x, c, d--);
    		c--;
    	    }
    	    if (b > c)
    		break;
    	    swap(x, b++, c--);
    	}
    }

	public static boolean areEqual(byte[] a, byte[] b) {
		if(a.length!=b.length) {
			return false;
		}
		for(int i=0; i<a.length;i++) {
			if(a[i]!=b[i]) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] add(T obj, T... array) {
		
		Object[] res = new Object[array.length + 1];
		System.arraycopy(array, 0, res, 0, array.length);
		res[array.length] = obj;
		
		return (T[])res;
	}

	public static boolean areEqual(char[] a, char[] b) {
		if(a.length!=b.length) {
			return false;
		}
		for(int i=0; i<a.length;i++) {
			if(a[i]!=b[i]) {
				return false;
			}
		}
		return true;
	}
	
	public static byte[] copy(byte[] array, int len) {
		return copy(array, 0, len);
	}
	
	public static byte[] copy(byte[] array, int offset, int len) {
		byte[] tmp = new byte[len];
		System.arraycopy(array, offset, tmp, 0, len);
		return tmp;
	}

	public static byte[] cat(byte[] a, byte[] b) {
		byte[] tmp = new byte[a.length + b.length];
		System.arraycopy(a, 0, tmp, 0, a.length);
		System.arraycopy(b, 0, tmp, a.length, b.length);
		return tmp;
	}
}
