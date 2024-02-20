package com.sshtools.common.publickey;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Hashtable;

import com.sshtools.common.util.Base64;

/**
 *
 * @author Lee David Painter
 */
public abstract class Base64EncodedFileFormat {
  /**  */
  protected String begin;

  /**  */
  protected String end;
  private Hashtable<String,String> headers = new Hashtable<String,String>();
  private int MAX_LINE_LENGTH = 70;

  protected Base64EncodedFileFormat(String begin, String end) {
    this.begin = begin;
    this.end = end;
  }

  public static boolean isFormatted(byte[] formattedKey, String begin,
                                    String end) {
    String test = new String(formattedKey);

    if ( (test.indexOf(begin) >= 0) && (test.indexOf(end) > 0)) {
      return true;
    }
	return false;
  }

  public void setHeaderValue(String headerTag, String headerValue) {
    headers.put(headerTag, headerValue);
  }

  public String getHeaderValue(String headerTag) {
    return (String) headers.get(headerTag);
  }

  protected byte[] getKeyBlob(byte[] formattedKey) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        new ByteArrayInputStream(formattedKey)));

    String line;
    String headerTag;
    String headerValue;
    StringBuffer blobBuf=new StringBuffer("");
    
    int index;

    // Read in the lines looking for the start
    do {
      line = reader.readLine();

      if (line == null) {
        throw new IOException("Incorrect file format!");
      }

    } while (!line.trim().endsWith(begin));

    // Read the headers
    while (true) {
      line = reader.readLine();

      if (line == null) {
        throw new IOException("Incorrect file format!");
      }
      
      line = line.trim();

      index = line.indexOf(": ");

      if (index > 0) {
        while (line.endsWith("\\")) {
          line = line.substring(0, line.length() - 1);

          String tmp = reader.readLine();

          if (tmp == null) {
            throw new IOException(
                "Incorrect file format!");
          }

          line += tmp.trim();
        }

        // Record the header
        headerTag = line.substring(0, index);
        headerValue = line.substring(index + 2);
        headers.put(headerTag, headerValue);
      }
      else {
        break;
      }
    }

    // This is now the public key blob Base64 encoded
    while (true) {
      blobBuf.append(line);

      line = reader.readLine();

      if (line == null) {
        throw new IOException("Invalid file format!");
      }

      line = line.trim();
      
      if (line.endsWith(end)) {
        break;
      }
    }

    // Convert the blob to some useful data
    return Base64.decode(blobBuf.toString());

  }

  protected byte[] formatKey(byte[] keyblob) throws IOException {

    ByteArrayOutputStream out = new ByteArrayOutputStream();

    String headerTag;
    String headerValue;
    String line;

    out.write(begin.getBytes());
    out.write('\n');

    int pos;

    //Set tags = headers.keySet();
    //Iterator it = tags.iterator();

    for (Enumeration<String> e = headers.keys(); e.hasMoreElements(); ) {
      headerTag = e.nextElement();
      headerValue = headers.get(headerTag);

      String header = headerTag + ": " + headerValue;
      pos = 0;

      while (pos < header.length()) {
        line = header.substring(pos,
                                ( ( (pos + MAX_LINE_LENGTH) < header.length())
                                 ? (pos + MAX_LINE_LENGTH) : header.length()))
            + ( ( (pos + MAX_LINE_LENGTH) < header.length()) ? "\\" : "");

        out.write(line.getBytes());
        out.write('\n');
        pos += MAX_LINE_LENGTH;
      }
    }

    String encoded = Base64.encodeBytes(keyblob, false);
    out.write(encoded.getBytes());
    out.write('\n');
    out.write(end.getBytes());
    out.write('\n');

    return out.toByteArray();

  }
}
