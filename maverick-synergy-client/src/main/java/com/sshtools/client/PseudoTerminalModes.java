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

package com.sshtools.client;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayWriter;

/**
 *
 * <p>When a client requests a pseudo terminal it informs the server of
 * any terminal modes that it knows of. This is typically used  in
 * situations where advance terminal configuration is required but
 * it can also be used to perform simple configuration such as turning
 * off character echo.</p>
 *
 * <p><em>NOTE: the server may ignore some of the modes set if it does not
 * support them.</em></p>
 *
 * <blockquote><pre>
 * var session = ssh.openSessionChannel();
 * session.requestPseudoTerminal("vt100", 80, 24, 0, 0, PseudoTerminalModesBuilder.create().
 * 			// Turning off echo
 * 			withMode(PseudoTerminalModes.ECHO, false).
 * 			// Setting the Input/Output baud rate
 * 			withMode(PseudoTerminalModes.TTY_OP_ISPEED, 38400).
 * 			withMode(PseudoTerminalModes.TTY_OP_OSPEED, 38400).
 * 			build());
 * </pre></blockquote>
 *
 *
 * @author Lee David Painter
 */
public class PseudoTerminalModes {
	
	/**
	 * Builds {@link PseudoTerminalModes}.
	 * 
	 * <p>
	 * You can reuse an instance of this class providing that you do not want to
	 * change any of the modes. If you do want to change modes you can call the
	 * reset method to clear out old modes.
	 * </p>
	 */
	public final static class PseudoTerminalModesBuilder {
		private final Map<Integer, Integer> codes = new LinkedHashMap<>();
		
		/**
		 * Clear all modes set in this builder.
		 * 
		 * @return this for chaining
		 */
		public PseudoTerminalModesBuilder reset() {
			codes.clear();
			return this;
		}
		
		/**
		 * Set a <code>boolean</code> mode.
		 *  
		 * @param mode mode
		 * @param value value to set
		 * @return this for chaining
		 */
		public PseudoTerminalModesBuilder withMode(int mode, boolean value) {
			return withMode(mode, value ? 1 : 0);
		}
		
		/**
		 * Set an <code>integer</code> mode.
		 *  
		 * @param mode mode
		 * @param value value to set
		 * @return this for chaining
		 */
		public PseudoTerminalModesBuilder withMode(int mode, int value) {
			codes.put(mode, value);
			return this;
		}
		
		/**
		 * Set a <code>boolean>code> mode to <code>true</code>.
		 * 
		 * @param mode mode
		 * @return this for chaining
		 */
		public PseudoTerminalModesBuilder withMode(int mode) {
			return withMode(mode, true);
		}

		/**
		 * Set a <code>boolean>code> mode to <code>false</code>.
		 * 
		 * @param mode mode
		 * @return this for chaining
		 */
		public PseudoTerminalModesBuilder withoutMode(int mode) {
			return withMode(mode, false);
		}
		
		/**
		 * Create a new {@link PseudoTerminalModesBuilder}
		 *
		 * @return builder
		 */
		public static PseudoTerminalModesBuilder create() {
			return new PseudoTerminalModesBuilder();
		}
		
		/**
		 * Build a new {@link PseudoTerminalModes}.
		 * 
		 * @return modes
		 */
		public PseudoTerminalModes build() {
			return new PseudoTerminalModes(this);
		}
	}

    /**
     * Interrupt character; 255 if none.
     */
    public static final int VINTR = 1;

    /**
     * The quit character (sends SIGQUIT signal on POSIX systems).
     */
    public static final int VQUIT = 2;

    /**
     * Erase the character to left of the cursor.
     */
    public static final int VERASE = 3;

    /**
     * Kill the current input line.
     */
    public static final int VKILL = 4;

    /**
     * End-of-file character (sends EOF from the terminal).
     */
    public static final int VEOF = 5;

    /**
     * End-of-line character in addition to carriage return and/or linefeed.
     */
    public static final int VEOL = 6;

    /**
     * Additional end-of-line character.
     */
    public static final int VEOL2 = 7;

    /**
     * Continues paused output (normally control-Q).
     */
    public static final int VSTART = 8;

    /**
     * Pauses output (normally control-S).
     */
    public static final int VSTOP = 9;

    /**
     * Suspends the current program.
     */
    public static final int VSUSP = 10;

    /**
     * Another suspend character.
     */
    public static final int VDSUSP = 11;

    /**
     * Reprints the current input line.
     */
    public static final int VREPRINT = 12;

    /**
     * Erases a word left of cursor.
     */
    public static final int VWERASE = 13;

    /**
     * Enter the next character typed literally, even if it is a special character
     */
    public static final int VLNEXT = 14;

    /**
     * Character to flush output.
     */
    public static final int VFLUSH = 15;

    /**
     * Switch to a different shell layer.
     */
    public static final int VSWITCH = 16;

    /**
     * Prints system status line (load, command, pid, etc).
     */
    public static final int VSTATUS = 17;

    /**
     * Toggles the flushing of terminal output.
     */
    public static final int VDISCARD = 18;

    /**
     * The ignore parity flag.  The parameter SHOULD be 0 if this flag is FALSE,
     * and 1 if it is TRUE.
     */
    public static final int IGNPAR = 30;

    /**
     * Mark parity and framing errors.
     */
    public static final int PARMRK = 31;

    /**
     * Enable checking of parity errors.
     */
    public static final int INPCK = 32;

    /**
     * Strip 8th bit off characters.
     */
    public static final int ISTRIP = 33;

    /**
     * Map NL into CR on input.
     */
    public static final int INLCR = 34;

    /**
     * Ignore CR on input.
     */
    public static final int IGNCR = 35;

    /**
     * Map CR to NL on input.
     */
    public static final int ICRNL = 36;

    /**
     * Translate uppercase characters to lowercase.
     */
    public static final int IUCLC = 37;

    /**
     * Enable output flow control.
     */
    public static final int IXON = 38;

    /**
     * Any char will restart after stop.
     */
    public static final int IXANY = 39;

    /**
     * Enable input flow control.
     */
    public static final int IXOFF = 40;

    /**
     * Ring bell on input queue full.
     */
    public static final int IMAXBEL = 41;

    
    /**
     * Output is assumed to be UTF-8
     */
    public static final int IUTF8 = 42;
    
    /**
     * Enable signals INTR, QUIT, [D]SUSP.
     */
    public static final int ISIG = 50;

    /**
     * Canonicalize input lines.
     */
    public static final int ICANON = 51;

    /**
     * Enable input and output of uppercase characters by preceding their lowercase
     * equivalents with "\".
     */
    public static final int XCASE = 52;

    /**
     * Enable echoing.
     */
    public static final int ECHO = 53;

    /**
     * Visually erase chars.
     */
    public static final int ECHOE = 54;

    /**
     * Kill character discards current line.
     */
    public static final int ECHOK = 55;

    /**
     *  Echo NL even if ECHO is off.
     */
    public static final int ECHONL = 56;

    /**
     * Don't flush after interrupt.
     */
    public static final int NOFLSH = 57;

    /**
     * Stop background jobs from output.
     */
    public static final int TOSTOP = 58;


    /**
     * Enable extensions.
     */
    public static final int IEXTEN = 59;


    /**
     * Echo control characters as ^(Char).
     */
    public static final int ECHOCTL = 60;


    /**
     * Visual erase for line kill.
     */
    public static final int ECHOKE = 61;


    /**
     * Retype pending input.
     */
    public static final int PENDIN = 62;


    /**
     * Enable output processing.
     */
    public static final int OPOST = 70;


    /**
     * Convert lowercase to uppercase.
     */
    public static final int OLCUC = 71;

    /**
     * Map NL to CR-NL.
     */
    public static final int ONLCR = 72;

    /**
     *  Translate carriage return to newline (output).
     */
    public static final int OCRNL = 73;

    /**
     * Translate newline to carriage return-newline (output).
     */
    public static final int ONOCR = 74;

    /**
     * Newline performs a carriage return (output).
     */
    public static final int ONLRET = 75;

    /**
     * 7 bit mode.
     */
    public static final int CS7 = 90;

    /**
     * 8 bit mode.
     */
    public static final int CS8 = 91;


    /**
     * Parity enable.
     */
    public static final int PARENB = 92;

    /**
     * Odd parity, else even.
     */
    public static final int PARODD = 93;


    /**
     * Specifies the input baud rate in bits per second.
     */
    public static final int TTY_OP_ISPEED = 128;

    /**
     * Specifies the output baud rate in bits per second.
     */
    public static final int TTY_OP_OSPEED = 129;

    /*
     * Will be removed, and {@link #output} made final. 
     */
    @Deprecated(since = "3.1.0", forRemoval = true)
    private ByteArrayWriter encodedModes = new ByteArrayWriter();
    
    private byte[] output;
    
    /**
     * Construct a new empty set of modes, and use {@link #setTerminalMode(int, boolean)} and {@link #setTerminalMode(int, int)}
     * to set this modes.
     * 
     * This is deprecated, use {@link PseudoTerminalModesBuilder} instead. 
     * 
     * @param modes 
     * @throws IOException
     */
    @Deprecated(since = "3.1.0", forRemoval = true)
    public PseudoTerminalModes() {
    }
    
    /**
     * Construct modes from the encoded modes data.
     * 
     * @param modes 
     * @throws IOException
     */
    @Deprecated(since = "3.1.0", forRemoval = true)
    public PseudoTerminalModes(byte[] modes) throws IOException {
    	this.output = modes;
    }

    private PseudoTerminalModes(PseudoTerminalModesBuilder builder) {
    	builder.codes.forEach((k,v) -> {
    		try {
				setTerminalMode(k, v);
			} catch (SshException e) {
				throw new IllegalArgumentException("Failed to set modes.", e);
			}
    	});
	}

	/**
     * Clear the modes
     */
    @Deprecated(since = "3.1.0", forRemoval = true)
    public void reset() {
        output = null;
        encodedModes.reset();
    }

    /**
     * Set an integer value mode
     * @param mode int
     * @param value int
     * @throws SshException
     */
    @Deprecated(since = "3.1.0", forRemoval = true)
    public void setTerminalMode(int mode, int value) throws SshException {
        try {

            encodedModes.write(mode);
            encodedModes.writeInt(value);

        } catch (IOException ex) {
            throw new SshException(SshException.INTERNAL_ERROR, ex);
        }
    }

    /**
     * Set a boolean value mode
     * @param mode int
     * @param value boolean
     * @throws SshException
     */
    @Deprecated(since = "3.1.0", forRemoval = true)
    public void setTerminalMode(int mode, boolean value) throws SshException {
        setTerminalMode(mode, value ? 1 : 0);
    }

    /**
     * Returns the encoded modes for use by the {@link SshSession}.
     * @return byte[]
     */
    public byte[] toByteArray() {

        if(output==null) {
            encodedModes.write(0);
            return output = encodedModes.toByteArray();
        }
		return output;
    }


}
