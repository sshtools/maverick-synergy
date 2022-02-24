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


package com.sshtools.common.net;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Utility class to process HTTP headers.
 * 
 * @author Lee David Painter
 *
 */
public abstract class HttpHeader {

    protected final static String white_SPACE = " \t\r";
    Hashtable<String,String> fields;


    protected String begin;

    protected HttpHeader() {
        fields = new Hashtable<String,String>();
    }

    protected String readLine(ByteBuffer in) throws IOException {
        StringBuffer lineBuf = new StringBuffer();
        int c;

        while (true) {
            
        	if(!in.hasRemaining()) {
        		throw new EOFException("Unexpected EOF during HTTP header read");
        	}
        	
        	c = (int) in.get() & 0xFF;

            if (c == '\r') {
                continue;
            }

            if (c != '\n') {
                lineBuf.append((char) c);
            } else {
                break;
            }
        }

        return new String(lineBuf);
    }


    public String getStartLine() {
        return begin;
    }

    public Hashtable<String,String> getHeaderFields() {
        return fields;
    }

    public Enumeration<String> getHeaderFieldNames() {
        return fields.keys();
    }

    public String getHeaderField(String headerName) {
    	for(Enumeration<String> e = fields.keys();e.hasMoreElements();) {
    		String f = (String)e.nextElement();
    		if(f.equalsIgnoreCase(headerName)) {
    			return (String) fields.get(f);
    		}
    	}
        return null;
    }

    public void setHeaderField(String headerName, String value) {
        fields.put(headerName, value);
    }

    public String toString() {
        String str = begin + "\r\n";
        Enumeration<String> it = getHeaderFieldNames();

        while (it.hasMoreElements()) {
            String fieldName = (String) it.nextElement();
            str += (fieldName + ": " + getHeaderField(fieldName) + "\r\n");
        }

        str += "\r\n";

        return str;
    }

    protected void processHeaderFields(ByteBuffer in)
        throws IOException {
        fields = new Hashtable<String,String>();

        StringBuffer lineBuf = new StringBuffer();
        String lastHeaderName = null;
        int c;

        while (true) {
        	
        	if(!in.hasRemaining()) {
        		throw new EOFException("Unexpected EOF whilst reading HTTP Headers");
        	}
        	
            c = (int) in.get() & 0xFF;

            if (c == '\r') {
                continue;
            }

            if (c != '\n') {
                lineBuf.append((char) c);
            } else {
                if (lineBuf.length() != 0) {
                    String line = lineBuf.toString();
                    lastHeaderName = processNextLine(line, lastHeaderName);
                    lineBuf.setLength(0);
                } else {
                    break;
                }
            }
        }
//        LDP: Not quite sure why this is here.
//        c = (int) in.get() & 0xFF;
    }

    private String processNextLine(String line, String lastHeaderName)
        throws IOException {
        String name;
        String value;
        char c = line.charAt(0);

        if ((c == ' ') || (c == '\t')) {
            name = lastHeaderName;
            value = getHeaderField(lastHeaderName) + " " + line.trim();
        } else {
            int n = line.indexOf(':');

            if (n == -1) {
                throw new IOException(
                    "HTTP Header encoutered a corrupt field: '" + line + "'");
            }

            name = line.substring(0, n).toLowerCase();
            value = line.substring(n + 1).trim();
        }

        setHeaderField(name, value);

        return name;
    }
}
