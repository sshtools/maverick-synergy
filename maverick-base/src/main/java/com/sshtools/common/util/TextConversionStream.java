/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
/* HEADER */
package com.sshtools.common.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * <p>This class processes text data and corrects line endings according to
 * the settings provided. Specifically you can set the input style to process
 * so that lone \r and \n characters within a \r\n encoded file are written
 * through without amendments (the SFTP protocol states that this must be
 * the case when processing text files).</p>
 *
 * @author Lee David Painter
 */
public class TextConversionStream extends FilterOutputStream {

        /**
         * This output style specifies that the text will have line endings set to
         * the current system setting.
         */
        public final static int TEXT_SYSTEM = 0;
        /**
         * This indicates a CRLF line ending combinarion and can be used
         * for both input and output style parameters.
         */
        public final static int TEXT_WINDOWS = 1;
        /**
         * This indicates a CRLF line ending combinarion and can be used
         * for both input and output style parameters.
         */
        public final static int TEXT_DOS = 1;
        /*
         * This indicates a CRLF line ending combinarion and can be used
         * for both input and output style parameters.
         */
        public final static int TEXT_CRLF = 1;
        /**
         * This indicates a LF line ending style and can be
         * used for either input or output style parameters
         */
        public final static int TEXT_UNIX = 2;
        /**
         * This indicates a single LF line ending style and can be used for
         * either input or output style parameters
         */
        public final static int TEXT_LF = 2;
        /**
         * This indicates a MAC line ending and can be used in either the
         * input or output style parameters
         */
        public final static int TEXT_MAC = 3;
        /**
         * This indicates a CR line ending style and can be used for either
         * input or output style parameters
         */
        public final static int TEXT_CR = 3;

        /**
         * This input style instructs the conversion to strip
         * all line ending type characters and replace with the output style
         * line ending
         */
        public final static int TEXT_ALL = 4;

        byte[] lineEnding;
        String systemNL = System.getProperty("line.separator");
        boolean stripCR;
        boolean stripLF;
        boolean stripCRLF;
        boolean encounteredBinary = false;

        boolean lastCharacterWasCR = false;

        public TextConversionStream(int inputStyle, int outputStyle, OutputStream out) {

            super(out);

            switch (inputStyle) {
                    case TEXT_CRLF: {
                             stripCR = false;
                             stripLF = false;
                             stripCRLF = true;
                    } break;
                    case TEXT_CR: {
                             stripCR = true;
                             stripLF = false;
                             stripCRLF = false;
                    } break;
                    case TEXT_LF: {
                             stripCR = false;
                             stripLF = true;
                             stripCRLF = false;
                    } break;
                    case TEXT_ALL: {
                        stripCR = true;
                        stripLF = true;
                        stripCRLF = true;
                    } break;
                    default: {
                            throw new IllegalArgumentException("Unknown text style: " + outputStyle);
                    }
            }


            switch (outputStyle) {
                    case TEXT_SYSTEM: {
                             lineEnding = systemNL.getBytes();
                    } break;
                    case TEXT_CRLF: {
                             lineEnding = new byte[]{(byte)'\r',(byte)'\n'};
                    } break;
                    case TEXT_CR: {
                             lineEnding = new byte[]{(byte)'\r'};
                    } break;
                    case TEXT_LF: {
                             lineEnding = new byte[]{(byte)'\n'};
                    } break;
                    case TEXT_ALL: {
                        throw new IllegalArgumentException("TEXT_ALL cannot be used for an output style");
                    }
                    default: {
                            throw new IllegalArgumentException("Unknown text style: " + outputStyle);
                    }
            }
        }

        /**
         * Check to see if binary data was encountered during the encoding
         * process
         * @return boolean
         */
        public boolean hasBinary() {
            return encounteredBinary;
        }

        public void write(int b) throws IOException {
            write(new byte[] { (byte)b });
        }

        public void close() throws IOException {

            if(lastCharacterWasCR && !stripCR)
                out.write('\r');

            super.close();
        }

        /**
         *
         * @param buf byte[]
         * @param off int
         * @param len int
         * @param out OutputStream
         * @throws IOException
         */
        public void write(byte[] buf,
                                    int off,
                                    int len)
                throws IOException {


                BufferedInputStream bin = new BufferedInputStream(
                         new ByteArrayInputStream(buf, off, len), 32768);

                int b;
                while((b = bin.read()) != -1){

                    if (b == '\r') {

                        if(stripCRLF) {
                            bin.mark(1);
                            int ch = bin.read();
                            if(ch==-1) {
                                lastCharacterWasCR = true;
                                break;
                            }
                            if(ch == '\n') {
                                // This is STYLE_RN do we output as is or replace with NL?
                                out.write(lineEnding);

                            } else {
                                // move the stream back and process a single CR
                                bin.reset();
                                if(stripCR) {
                                    out.write(lineEnding);
                                } else
                                    out.write(b);
                            }
                        } else {
                            // This is STYLE_R do we output as is or replace with NL?
                            if (stripCR)
                                out.write(lineEnding);
                            else
                                out.write(b);

                        }
                    } else if(b == '\n') {

                        // Are we processing between blocks and was the last character a CR
                        if(lastCharacterWasCR) {
                            out.write(lineEnding);
                            lastCharacterWasCR = false;
                        } else {
                                // This is STYLE_N do we output as is or replace with NL?
                            if (stripLF)
                                out.write(lineEnding);
                            else
                                out.write(b);
                        }
                    } else {

                        // Check for previous CR in stream
                        if(lastCharacterWasCR) {
                            if(stripCR) {
                               out.write(lineEnding);
                            } else
                                out.write(b);
                        }

                        // Not an EOL
                        if(b != 't'
                           && b != '\f'
                           && (b & 0xff) < 32) {
                            encounteredBinary = true;
                        }

                        out.write(b);
                    }
                }

        }


        public static void main(String[] args) {

            try {
                TextConversionStream t = new TextConversionStream(
                              TEXT_CRLF,
                              TEXT_CR,
                              new FileOutputStream("C:\\TEXT.txt"));


                t.write("1234567890\r".getBytes());
                t.write("\n01234567890\r\n".getBytes());
                t.write("\r\n12323445546657".getBytes());
                t.write("21344356545656\r".getBytes());

                t.close();

            } catch(Exception e) {
                System.out.println("RECIEVED IOException IN Ssh1Protocol.close:"+e.getMessage());

            }

        }



}
