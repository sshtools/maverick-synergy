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
