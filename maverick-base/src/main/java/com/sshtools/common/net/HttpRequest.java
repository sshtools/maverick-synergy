/* HEADER */
package com.sshtools.common.net;

import com.sshtools.common.util.Base64;

/**
 * Utility class to process HTTP requests.
 * @author Lee David Painter
 *
 */
public class HttpRequest extends HttpHeader {


    public HttpRequest() {
        super();
    }

    public void setHeaderBegin(String begin) {
        this.begin = begin;
    }

    public void setBasicAuthentication(String username, String password) {
        String str = username + ":" + password;
        setHeaderField("Proxy-Authorization",
            "Basic " + Base64.encodeBytes(str.getBytes(), true));
    }
}
