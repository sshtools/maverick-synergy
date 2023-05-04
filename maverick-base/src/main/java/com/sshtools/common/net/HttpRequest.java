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
