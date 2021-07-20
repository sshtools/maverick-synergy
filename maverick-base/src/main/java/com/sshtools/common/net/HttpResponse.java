

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
