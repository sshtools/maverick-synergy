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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * Utility class to process HTTP responses. 
 * @author Lee David Painter
 *
 */
public class HttpResponse extends HttpHeader {
    private String version;
    private int status;
    private String reason;

    public HttpResponse() throws IOException {
    
    }
    
    public void process(ByteBuffer input) throws IOException {
    	begin = readLine(input);

        while (begin.trim().length() == 0) {
            begin = readLine(input);
        }

        processResponse();
        processHeaderFields(input);
    }

    public String getVersion() {
        return version;
    }

    public int getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    private void processResponse() throws IOException {
        StringTokenizer tokens = new StringTokenizer(begin, white_SPACE, false);

        try {
            version = tokens.nextToken();
            status = Integer.parseInt(tokens.nextToken());
            reason = tokens.nextToken();
        } catch (NoSuchElementException e) {
            throw new IOException("Failed to read HTTP repsonse header");
        } catch (NumberFormatException e) {
            throw new IOException("Failed to read HTTP resposne header");
        }
    }

    public String getAuthenticationMethod() {
        String auth = getHeaderField("Proxy-Authenticate");
        String method = null;

        if (auth != null) {
            int n = auth.indexOf(' ');
            method = auth.substring(0, n);
        }

        return method;
    }

    public String getAuthenticationRealm() {
        String auth = getHeaderField("Proxy-Authenticate");
        String realm = "";

        if (auth != null) {
            int l;
            int r = auth.indexOf('=');

            while (r >= 0) {
                l = auth.lastIndexOf(' ', r);
                if (l > -1) {
                    String val = auth.substring(l + 1, r);

                    if (val.equalsIgnoreCase("realm")) {
                        l = r + 2;
                        r = auth.indexOf('"', l);
                        realm = auth.substring(l, r);

                        break;
                    }

                    r = auth.indexOf('=', r + 1);
                }
            }
        }

        return realm;
    }

}
