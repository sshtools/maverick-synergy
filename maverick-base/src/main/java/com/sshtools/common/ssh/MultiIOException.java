/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
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
    	super.printStackTrace();
    	for(var o : others) {
    		if(o != getCause()) {
    	    	s.println(String.format("Exception %d", occurence++));
    			o.printStackTrace(s);
    		}
    	}
    }
}
