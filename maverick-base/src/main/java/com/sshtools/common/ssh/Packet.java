package com.sshtools.common.ssh;

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
