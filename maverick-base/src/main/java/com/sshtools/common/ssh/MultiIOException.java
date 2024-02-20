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
