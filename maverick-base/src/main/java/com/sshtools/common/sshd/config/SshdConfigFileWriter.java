package com.sshtools.common.sshd.config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.concurrent.Callable;


public class SshdConfigFileWriter {
	
	private OutputStream stream;
	
	public SshdConfigFileWriter(OutputStream stream) {
		this.stream = stream;
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
					
					boolean start = false;//tracker to check we have to add new line before we write.
					//we add new line before not after we write line, else we will end up with
					//new line at the end of file, which may lead to extra line at the end of the file, compared to original file,
					//hence better check before.
					for(SshdConfigFileEntry keyEntry : sshdConfigFile.getGlobalConfiguration().getKeyEntries().values()) {
						while(true) {
							if (start) {
								bw.newLine();
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
								bw.newLine();
							}
							bw.write(commentEntry.getFormattedLine());
							start = true;
						}
						
						//The match criteria
						if (start) {
							bw.newLine();
						}
						bw.write(matchEntry.getFormattedLine());
						start = true;
						
						//The entries
						for(SshdConfigFileEntry keyEntry : matchEntry.getKeyEntries().values()) {
							
							while(true) {
								if (start) {
									bw.newLine();
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
					
					bw.newLine();
				}
				return null;
			}
		});
		
		
	}
	
	public synchronized void write(SshdConfigFile sshdConfigFile) throws IOException {
		write(sshdConfigFile, true);
	}
	
}
