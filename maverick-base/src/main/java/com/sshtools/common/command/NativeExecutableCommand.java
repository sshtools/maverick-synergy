package com.sshtools.common.command;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.Channel;
import com.sshtools.common.ssh.ChannelEventListener;
import com.sshtools.common.util.IOStreamConnector;

/**
 * A further extension of the {@link AbstractExecutableCommand} that provides the
 * ability to execute a native process. Caution should be taken with this
 * current implementation as no processing of EOL is provided and results may
 * vary on different platforms. It is recommended that only automated clients
 * use this feature due to the many differences in user terminals which may
 * cause problems with command output not performing as expected.
 *
 * 
 */
public class NativeExecutableCommand extends AbstractExecutableCommand {

	
	Process process;
	String[] commandLine;
	String[] env;
	ProcessThread thread;

	int exitValue = STILL_ACTIVE;

	public NativeExecutableCommand() {
	}

	public void onStart() {
		thread.start();
	}

	public int getExitCode() {
		return exitValue;
	}

	public boolean createProcess(String[] commandLine, Map<String, String> environment) {

		if(commandLine.length==0) {
			return false;
		}
		
		if(Log.isDebugEnabled())
			Log.debug("Creating native process: {}", commandLine[0]);

		this.commandLine = commandLine;

		// Now process the environment
		Map.Entry<String, String> entry;
		Vector<String> tmp = new Vector<String>();
		if (environment != null) {
			for (Iterator<Map.Entry<String, String>> it = environment.entrySet().iterator(); it.hasNext();) {
				entry = it.next();
				tmp.add(entry.getKey().toString() + "=" + entry.getValue().toString());
			}
		}

		env = new String[tmp.size()];
		tmp.copyInto(env);

		try {
			process = Runtime.getRuntime().exec(commandLine, env);
			thread = new ProcessThread();
			
			session.addEventListener(new ChannelEventListener() {
				public void onChannelDataIn(Channel channel, ByteBuffer data) {
					
					byte[] tmp = new byte[data.remaining()];
					data.get(tmp);
					try {
						process.getOutputStream().write(tmp);
						process.getOutputStream().flush();
					} catch (IOException e) {
						Log.error("Faild to write to process", e);
						session.close();
					}
				}
			});
			
			return true;
		} catch (IOException ex) {
			return false;
		}
	}

	public void kill() {
		process.destroy();
	}

	class ProcessThread extends Thread {
		IOStreamConnector stdout;
		IOStreamConnector stderr;

		public void run() {
			try {

				Log.info("Starting reading I/O");
				stdout = new IOStreamConnector(process.getInputStream(), getOutputStream());
				stderr = new IOStreamConnector(process.getErrorStream(), getOutputStream());
				
				exitValue = process.waitFor();
				Log.info("Command exited with {}", exitValue);
			} catch (Throwable ex) {
				if(Log.isDebugEnabled())
					Log.debug("Native process transfer thread failed", ex);
			}
		}
	}

}
