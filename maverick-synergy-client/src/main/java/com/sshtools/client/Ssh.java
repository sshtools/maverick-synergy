package com.sshtools.client;

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

import java.io.File;

import com.sshtools.client.SshClient.SshClientBuilder;
import com.sshtools.client.tasks.DownloadFileTask.DownloadFileTaskBuilder;
import com.sshtools.client.tasks.UploadFileTask.UploadFileTaskBuilder;
import com.sshtools.common.logger.Log;

public class Ssh {

	public static boolean getFile(String host, int port, String username, char[] password, String fromPath, File toFile) {
		
		try (var client = SshClientBuilder.create().
				withTarget(host, port).
				withUsername(username).
				withPassword(password).build()) {
			
			return client.addTask(DownloadFileTaskBuilder.create().
					withClient(client). 
					withRemotePath(fromPath).
					withLocalFile(toFile).build()).waitForever().isDoneAndSuccess();
		} catch (Throwable e) {
			Log.error("getFile failed", e);
			return false;
		}
	}

	public static boolean putFile(String host, int port, String username, char[] password, File fromFile, String toPath) {
		try (var client = SshClientBuilder.create().
				withTarget(host, port).
				withUsername(username).
				withPassword(password).build()) {
			
			return client.addTask(UploadFileTaskBuilder.create().
				withClient(client).
				withLocalFile(fromFile).
				withRemotePath(toPath).build()).waitForever().isDoneAndSuccess();
			
		} catch (Throwable e) {
			Log.error("putFile failed", e);
			return false;
		}
	}
}
