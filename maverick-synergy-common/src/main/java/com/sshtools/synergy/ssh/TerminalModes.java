package com.sshtools.synergy.ssh;

/*-
 * #%L
 * Client API
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
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

/**
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
 * session.requestPseudoTerminal("vt100", 80, 24, 0, 0, TerminalModesBuilder.create().
 * 			// Turning off echo
 * 			withMode(TerminalModes.Mode.ECHO, false).
 * 			// Setting the Input/Output baud rate
 * 			withMode(TerminalModes.Mode.TTY_OP_ISPEED, 38400).
 * 			withMode(TerminalModes.Mode.TTY_OP_OSPEED, 38400).
 * 			build());
 * </pre></blockquote>
 * 
 * @author Brett Smith
 * @author Lee David Painter
 */
public final class TerminalModes {
	
	/**
	 * Enumeration of supported modes.
	 */
	public enum Mode {

		/**
	     * Interrupt character; 255 if none.
	     */
	    VINTR,

	    /**
	     * The quit character (sends SIGQUIT signal on POSIX systems).
	     */
	    VQUIT,

	    /**
	     * Erase the character to left of the cursor.
	     */
	    VERASE,

	    /**
	     * Kill the current input line.
	     */
	    VKILL,

	    /**
	     * End-of-file character (sends EOF from the terminal).
	     */
	    VEOF,

	    /**
	     * End-of-line character in addition to carriage return and/or linefeed.
	     */
	    VEOL,

	    /**
	     * Additional end-of-line character.
	     */
	    VEOL2,

	    /**
	     * Continues paused output (normally control-Q).
	     */
	    VSTART,

	    /**
	     * Pauses output (normally control-S)..
	     */
	    VSTOP,

	    /**
	     * Suspends the current program.
	     */
	    VSUSP,

	    /**
	     * Another suspend character.
	     */
	    VDSUSP,

	    /**
	     * Reprints the current input line.
	     */
	    VREPRINT,

	    /**
	     * Erases a word left of cursor.
	     */
	    VWERASE,

	    /**
	     * Enter the next character typed literally, even if it is a special character
	     */
	    VLNEXT,

	    /**
	     * Character to flush output.
	     */
	    VFLUSH,

	    /**
	     * Switch to a different shell layer.
	     */
	    VSWITCH,

	    /**
	     * Prints system status line (load, command, pid, etc).
	     */
	    VSTATUS,

	    /**
	     * Toggles the flushing of terminal output.
	     */
	    VDISCARD,

	    /**
	     * The ignore parity flag.  The parameter SHOULD be 0 if this flag is FALSE,
	     * and 1 if it is TRUE.
	     */
	    IGNPAR,

	    /**
	     * Mark parity and framing errors.
	     */
	    PARMRK,

	    /**
	     * Enable checking of parity errors.
	     */
	    INPCK,

	    /**
	     * Strip 8th bit off characters.
	     */
	    ISTRIP,

	    /**
	     * Map NL into CR on input.
	     */
	    INLCR,

	    /**
	     * Ignore CR on input.
	     */
	    IGNCR,

	    /**
	     * Map CR to NL on input.
	     */
	    ICRNL,

	    /**
	     * Translate uppercase characters to lowercase.
	     */
	    IUCLC,

	    /**
	     * Enable output flow control.
	     */
	    IXON,

	    /**
	     * Any char will restart after stop.
	     */
	    IXANY,

	    /**
	     * Enable input flow control.
	     */
	    IXOFF,

	    /**
	     * Ring bell on input queue full.
	     */
	    IMAXBEL,
	    
	    /**
	     * Output is assumed to be UTF-8
	     */
	    IUTF8,
	    
	    /**
	     * Enable signals INTR, QUIT, [D]SUSP.
	     */
	    ISIG,

	    /**
	     * Canonicalize input lines.
	     */
	    ICANON,

	    /**
	     * Enable input and output of uppercase characters by preceding their lowercase
	     * equivalents with "\".
	     */
	    XCASE,

	    /**
	     * Enable echoing.
	     */
	    ECHO,

	    /**
	     * Visually erase chars.
	     */
	    ECHOE,

	    /**
	     * Kill character discards current line.
	     */
	    ECHOK,

	    /**
	     *  Echo NL even if ECHO is off.
	     */
	    ECHONL,

	    /**
	     * Don't flush after interrupt.
	     */
	    NOFLSH,

	    /**
	     * Stop background jobs from output.TerminalModes
	     */
	    TOSTOP,


	    /**
	     * Enable extensions.
	     */
	    IEXTEN,


	    /**
	     * Echo control characters as ^(Char).
	     */
	    ECHOCTL,


	    /**
	     * Visual erase for line kill.
	     */
	    ECHOKE,


	    /**
	     * Retype pending input.
	     */
	    PENDIN,


	    /**
	     * Enable output processing.
	     */
	    OPOST,


	    /**
	     * Convert lowercase to uppercase.
	     */
	    OLCUC,

	    /**
	     * Map NL to CR-NL.
	     */
	    ONLCR,

	    /**
	     *  Translate carriage return to newline (output).
	     */
	    OCRNL,

	    /**
	     * Translate newline to carriage return-newline (output).
	     */
	    ONOCR,

	    /**
	     * Newline performs a carriage return (output).
	     */
	    ONLRET,

	    /**
	     * 7 bit mode.
	     */
	    CS7,

	    /**
	     * 8 bit mode.
	     */
	    CS8,


	    /**
	     * Parity enable.
	     */
	    PARENB,

	    /**
	     * Odd parity, else even.
	     */
	    PARODD,


	    /**
	     * Specifies the input baud rate in bits per second.
	     */
	    TTY_OP_ISPEED,

	    /**
	     * Specifies the output baud rate in bits per second.
	     */
	    TTY_OP_OSPEED;
		
		public int toMode() {
			switch(this) {
			case VINTR:
				return 1;
			case VQUIT:
				return 2;
			case VERASE:
				return 3;
			case VKILL:
				return 4;
			case VEOF:
				return 5;
			case VEOL:
				return 6;
			case VEOL2:
				return 7;
			case VSTART:
				return 8;
			case VSTOP:
				return 9;
			case VSUSP:
				return 10;
			case VDSUSP:
				return 11;
			case VREPRINT:
				return 12;
			case VWERASE:
				return 13;
			case VLNEXT:
				return 14;
			case VFLUSH:
				return 15;
			case VSWITCH:
				return 16;
			case VSTATUS:
				return 17;
			case VDISCARD:
				return 18;
			case IGNPAR:
				return 30;
			case PARMRK:
				return 31;
			case INPCK:
				return 32;
			case ISTRIP:
				return 33;
			case INLCR:
				return 34;
			case IGNCR:
				return 35;
			case ICRNL:
				return 36;
			case IUCLC:
				return 37;
			case IXON:
				return 38;
			case IXANY:
				return 39;
			case IXOFF:
				return 40;
			case IMAXBEL:
				return 41;
			case IUTF8:
				return 42;
			case ISIG:
				return 50;
			case ICANON:
				return 51;
			case XCASE:
				return 52;
			case ECHO:
				return 53;
			case ECHOE:
				return 54;
			case ECHOK:
				return 55;
			case ECHONL:
				return 56;
			case NOFLSH:
				return 57;
			case TOSTOP:
				return 58;
			case IEXTEN:
				return 59;
			case ECHOCTL:
				return 60;
			case ECHOKE:
				return 61;
			case PENDIN:
				return 62;
			case OPOST:
				return 70;
			case OLCUC:
				return 71;
			case ONLCR:
				return 72;
			case OCRNL:
				return 73;
			case ONOCR:
				return 74;
			case ONLRET:
				return 75;
			case CS7:
				return 90;
			case CS8:
				return 91;
			case PARENB:
				return 92;
			case PARODD:
				return 93;
			case TTY_OP_ISPEED:
				return 128;
			case TTY_OP_OSPEED:
				return 129;
			}
			throw new IllegalStateException();
		}
		
		public static Mode fromMode(int mode) {
			switch(mode) {
			case 1:
				return VINTR;
			case 2:
				return VQUIT;
			case 3:
				return VERASE;
			case 4:
				return VKILL;
			case 5:
				return VEOF;
			case 6:
				return VEOL;
			case 7:
				return VEOL2;
			case 8:
				return VSTART;
			case 9:
				return VSTOP;
			case 10:
				return VSUSP;
			case 11:
				return VDSUSP;
			case 12:
				return VREPRINT;
			case 13:
				return VWERASE;
			case 14:
				return VLNEXT;
			case 15:
				return VFLUSH;
			case 16:
				return VSWITCH;
			case 17:
				return VSTATUS;
			case 18:
				return VDISCARD;
			case 30:
				return IGNPAR;
			case 31:
				return PARMRK;
			case 32:
				return INPCK;
			case 33:
				return ISTRIP;
			case 34:
				return INLCR;
			case 35:
				return INLCR;
			case 36:
				return ICRNL;
			case 37:
				return IUCLC;
			case 38:
				return IXON;
			case 39:
				return IXANY;
			case 40:
				return IXOFF;
			case 41:
				return IMAXBEL;
			case 42:
				return IUTF8;
			case 50:
				return ISIG;
			case 51:
				return ICANON;
			case 52:
				return XCASE;
			case 53:
				return ECHO;
			case 54:
				return ECHOE;
			case 55:
				return ECHOK;
			case 56:
				return ECHONL;
			case 57:
				return NOFLSH;
			case 58:
				return TOSTOP;
			case 59:
				return IEXTEN;
			case 60:
				return ECHOCTL;
			case 61:
				return ECHOKE;
			case 62:
				return PENDIN;
			case 70:
				return OPOST;
			case 71:
				return OLCUC;
			case 72:
				return ONLCR;
			case 73:
				return OCRNL;
			case 74:
				return ONOCR;
			case 75:
				return ONLRET;
			case 90:
				return CS7;
			case 91:
				return CS8;
			case 92:
				return PARENB;
			case 93:
				return PARODD;
			case 128:
				return TTY_OP_ISPEED;
			case 129:
				return TTY_OP_OSPEED;
			}
			throw new IllegalStateException("" + mode);
		}
	}
	
	/**
	 * Builds {@link TerminalModes}.
	 * 
	 * <p>
	 * You can reuse an instance of this class providing that you do not want to
	 * change any of the modes. If you do want to change modes you can call the
	 * reset method to clear out old modes.
	 * </p>
	 */
	public final static class TerminalModesBuilder {
		private final Map<Mode, Integer> codes = new LinkedHashMap<>();
		
		/**
		 * Clear all modes set in this builder.
		 * 
		 * @return this for chaining
		 */
		public TerminalModesBuilder reset() {
			codes.clear();
			return this;
		}
		
		/**
		 * Set one or more modes to <code>true</code>,  i.e. a value of 1.
		 *  
		 * @param modes modes
		 * @return this for chaining
		 */
		public TerminalModesBuilder withModes(Mode... modes) {
			Arrays.asList(modes).forEach(m -> withMode(m, true));
			return this;
		}
		
		/**
		 * Set one or more modes to <code>false</code>,  i.e. a value of 0.
		 *  
		 * @param modes modes
		 * @return this for chaining
		 */
		public TerminalModesBuilder withoutModes(Mode... modes) {
			Arrays.asList(modes).forEach(m -> withMode(m, false));
			return this;
		}
		
		/**
		 * Set a <code>boolean</code> mode.
		 *  
		 * @param mode mode
		 * @param value value to set
		 * @return this for chaining
		 */
		public TerminalModesBuilder withMode(int mode, boolean value) {
			return withMode(Mode.fromMode(mode), value);
		}
		
		/**
		 * Set a <code>boolean</code> mode.
		 *  
		 * @param mode mode
		 * @param value value to set
		 * @return this for chaining
		 */
		public TerminalModesBuilder withMode(Mode mode, boolean value) {
			return withMode(mode, value ? 1 : 0);
		}
		
		/**
		 * Set an <code>integer</code> mode.
		 *  
		 * @param mode mode
		 * @param value value to set
		 * @return this for chaining
		 */
		public TerminalModesBuilder withMode(int mode, int value) {
			return withMode(Mode.fromMode(mode), value);
		}
		
		/**
		 * Set an <code>integer</code> mode.
		 *  
		 * @param mode mode
		 * @param value value to set
		 * @return this for chaining
		 */
		public TerminalModesBuilder withMode(Mode mode, int value) {
			codes.put(mode, value);
			return this;
		}
		
		/**
		 * Set a <code>boolean>code> mode to <code>true</code>.
		 * 
		 * @param mode mode
		 * @return this for chaining
		 */
		public TerminalModesBuilder withMode(int mode) {
			return withMode(mode, true);
		}

		/**
		 * Set a <code>boolean>code> mode to <code>false</code>.
		 * 
		 * @param mode mode
		 * @return this for chaining
		 */
		public TerminalModesBuilder withoutMode(int mode) {
			return withMode(mode, false);
		}
		
		/**
		 * Create a new {@link TerminalModesBuilder}
		 *
		 * @return builder
		 */
		public static TerminalModesBuilder create() {
			return new TerminalModesBuilder();
		}
		
		/**
		 * Build a new {@link TerminalModes}.
		 * 
		 * @return modes
		 * @throws IOException 
		 */
		public TerminalModes build() {
			return new TerminalModes(this);
		}

		public TerminalModesBuilder fromBytes(byte[] modes) {
			return read(new ByteArrayReader(modes));
		}

		public TerminalModesBuilder read(ByteArrayReader reader) {
			while(true) {
				/* Hrm why does ByteArrayWriter throw IOExceptions? */
				try {
					var mode = reader.read();
					if(mode < 1)
						break;
					withMode(Mode.fromMode(mode), (int)reader.readInt());
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
			return this;
		}
	}

	private final Map<Mode, Integer> modes;
    
    private TerminalModes(TerminalModesBuilder builder) {
    	this.modes = Collections.unmodifiableMap(new LinkedHashMap<>(builder.codes));
	}
    
    /**
     * Get all of the modes and their values as a map. 
     * 
     * @return modes
     */
    public Map<Mode, Integer> modes() {
    	return modes;
    }

    /**
     * Returns the encoded modes for use by the {@link SshSession}.
     * @return byte[]
     */
    public byte[] toByteArray() {
    	var baw = new ByteArrayWriter();
    	write(baw);
    	return baw.toByteArray();
    }
    
    /**
     * Get the value of a numeric node, or zero if it does not exist
     * 
     * @param mode
     * @param default
     * @return value
     */
    public int get(Mode mode) {
    	return get(mode, 0);
    }
    
    /**
     * Get the value of a number node, or a default if it does not exist.
     * 
     * @param mode
     * @param default
     * @return value
     */
    public int get(Mode mode, int defaultValue) {
    	return modes.getOrDefault(mode, defaultValue);
    }
    
    /**
     * Get if a mode is set (i.e. is non-zero). If the mode is not in the set,
     * <code>false</code> will be returned.
     * 
     * @param mode mode
     * @return mode is preent and non-zero
     */
    public boolean is(Mode mode) {
    	return is(mode, false);
    }
    
    /**
     * Get if a mode is set (i.e. is non-zero) or a default if it does not exist.
     * 
     * @param mode mode
     * @param defaultValue default value
     * @return mode is non-zero
     */
    public boolean is(Mode mode, boolean defaultValue) {
    	var m = modes.get(mode);
    	return m == null ? defaultValue : m > 0;
    }
    
    /**
     * Get if a mode is present in the set. 
     * 
     * @param mode mode
     * @return mode is present and non-zero
     */
    public boolean present(Mode mode) {
    	return modes.containsKey(mode);
    }
    
    /**
     * Write the modes to the writer.
     * 
     * @param writer
     */
    public void write(ByteArrayWriter writer) {
    	modes.forEach((k, v) -> { 
    		writer.write(k.toMode());  
    		try {
				writer.writeInt(v);
			} catch (IOException e) {
				/* Hrm why does ByteArrayWriter throw IOExceptions? */
				throw new UncheckedIOException(e);
			}
    	});
    }


}
