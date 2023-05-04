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
package com.sshtools.common.sshd.config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.concurrent.Callable;


public class SshdConfigFileWriter {
	
	private OutputStream stream;
	private String newline = System.lineSeparator();
	
	public SshdConfigFileWriter(OutputStream stream) {
		this.stream = stream;
	}
	
	public SshdConfigFileWriter(OutputStream stream, String newline) {
		this.stream = stream;
		this.newline = newline;
	}

	public void write(final SshdConfigFile sshdConfigFile, final boolean indentMatchEntries) throws IOException {
		if (this.stream == null) {
			throw new IllegalStateException("Stream not initiallized.");
		}
		
		if (sshdConfigFile == null) {
			throw new IllegalStateException("SshdConfigFile not initiallized.");
		}
		
		sshdConfigFile.executeRead(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(SshdConfigFileWriter.this.stream))) {
					
					boolean start = false;
					
					for(SshdConfigFileEntry keyEntry : sshdConfigFile.getGlobalConfiguration().getKeyEntries().values()) {
						while(true) {
							if (start) {
								bw.write(newline);
							}
							start = true;
							bw.write(keyEntry.getFormattedLine());
							if(keyEntry.hasNext()) {
								keyEntry = keyEntry.getNext();
								continue;
							}
							break;
						}
					}
					
					Iterator<MatchEntry> matchEntriesIterator = sshdConfigFile.getMatchEntriesIterator();
					
					while(matchEntriesIterator.hasNext()) {
						
						MatchEntry matchEntry = matchEntriesIterator.next();
						
						if (matchEntry.matchEntryCriteriaAsString() == null || 
								matchEntry.matchEntryCriteriaAsString().trim().length() == 0) {
							throw new IllegalStateException("Match entry criteria string cannot be empty.");
						}
						// Match criteria comments
						Iterator<CommentEntry> commentEntryIterator = matchEntry.getMatchCriteriaCommentEntriesIterator();
						while (commentEntryIterator.hasNext()) {
							CommentEntry commentEntry = commentEntryIterator.next();
							if (start) {
								bw.write(newline);
							}
							bw.write(commentEntry.getFormattedLine());
							start = true;
						}
						
						//The match criteria
						if (start) {
							bw.write(newline);
						}
						bw.write(matchEntry.getFormattedLine());
						start = true;
						
						//The entries
						for(SshdConfigFileEntry keyEntry : matchEntry.getKeyEntries().values()) {
							
							while(true) {
								if (start) {
									bw.write(newline);
								}
								bw.write(keyEntry.getFormattedLine());
								if(keyEntry.hasNext()) {
									keyEntry = keyEntry.getNext();
									continue;
								}
								break;
							}
						}
					}
					
					bw.write(newline);
				}
				return null;
			}
		});
		
		
	}
	
	public synchronized void write(SshdConfigFile sshdConfigFile) throws IOException {
		write(sshdConfigFile, true);
	}
	
}
