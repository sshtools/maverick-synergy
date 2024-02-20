package com.sshtools.common.ssh;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;

import com.sshtools.common.util.ByteArrayWriter;

/**
 * A utility class that provides the SSH layers with the ability
 * to dynamically write an SSH packet.
 *
 * @author Lee David Painter
 */
public class Packet extends ByteArrayWriter {

    int markedPosition = -1;
    public Packet() throws IOException {
      this(35000);
    }

    public Packet(int size) throws IOException {
        super(size+4);

        // Save some space for the length field
        writeInt(0);
    }

    public int setPosition(int pos) {
        int count = this.count;
        this.count = pos;
        return count;
    }

    public int position() {
        return count;
    }

    public void finish() {

      buf[0] = (byte)(count-4 >> 24);
      buf[1] = (byte)(count-4 >> 16);
      buf[2] = (byte)(count-4 >> 8);
      buf[3] = (byte)(count-4);

    }

    public void reset() {

      super.reset();
      try {
        writeInt(0);
      }
      catch(IOException ex) {
      }
    }
  }
