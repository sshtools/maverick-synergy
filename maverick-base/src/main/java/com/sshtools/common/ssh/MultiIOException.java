/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.ssh;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;

@SuppressWarnings("serial")
public class MultiIOException extends IOException {

	private Throwable[] others;

	public MultiIOException(Throwable... others) {
		super(others.length == 0 ? null : others[0]);
		this.others = others;
	}
	
	public MultiIOException(Collection<Throwable> others) {
		this(others.toArray(new Throwable[0]));
	}

	public MultiIOException(String message, Collection<Throwable> others) {
		this(message, others.toArray(new Throwable[0]));
	}

	public MultiIOException(String message, Throwable... others) {
		super(message, others.length == 0 ? null : others[0]);
		this.others = others;
	}
	
	public Throwable[] getOthers() {
		return others;
	}

    public void printStackTrace(PrintStream s) {
    	int occurence = 1;
    	s.println(String.format("Exception %d", occurence++));
    	super.printStackTrace(s);
    	for(var o : others) {
    		if(o != getCause()) {
    	    	s.println(String.format("Exception %d", occurence++));
    			o.printStackTrace(s);
    		}
    	}
    }
}
