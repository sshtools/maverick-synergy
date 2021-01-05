/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.common.util;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FileUtils {

	public static String addTrailingSlash(String str) {
		if(!str.endsWith("/")) {
			return str + "/";
		} else {
			return str;
		}	
	}
	
	public static String removeTrailingSlash(String str) {
		if(str.endsWith("/")) {
			return str.substring(0, str.length()-1);
		} else {
			return str;
		}	
	}

	public static String removeStartingSlash(String str) {
		if(str.startsWith("/")) {
			return str.substring(1);
		}
		return str;
	}
	
	public static String addStartingSlash(String str) {
		if(str.startsWith("/")) {
			return str;
		}
		return "/" + str;
	}

	public static String getFilename(String path) {
		int idx = path.lastIndexOf("/");
		if(idx > -1) {
			return path.substring(idx+1);
		} else {
			return path;
		}
	}
	
	static SimpleDateFormat dateFormat = new SimpleDateFormat(
			"dd MMM yyyy HH:mm");

	public static String convertBackslashToForwardSlash(String str) {
		return str.replace('\\', '/');
	}
	
	public static String checkStartsWithSlash(String str) {
		if (str.startsWith("/")) {
			return str;
		} else {
			return "/" + str;
		}
	}

	public static String checkStartsWithNoSlash(String str) {
		if (str.startsWith("/")) {
			return str.substring(1);
		} else {
			return str;
		}
	}

	public static String checkEndsWithSlash(String str) {
		if (str.endsWith("/")) {
			return str;
		} else {
			return str + "/";
		}
	}

	public static String checkEndsWithNoSlash(String str) {
		if (str.endsWith("/")) {
			return str.substring(0, str.length() - 1);
		} else {
			return str;
		}
	}

	public static String stripParentPath(String rootPath, String path)
			throws IOException {
		path = checkEndsWithSlash(path);
		rootPath = checkEndsWithSlash(rootPath);
		if (!path.startsWith(rootPath)) {
			throw new IOException(path + " is not a child path of " + rootPath);
		} else {
			return path.substring(rootPath.length());
		}
	}
	
	public static String stripFirstPathElement(String path) {
		
		path = checkStartsWithNoSlash(path);
		int idx;
		if ((idx = path.indexOf('/',1)) > -1) {
			return path.substring(idx);
		} else {
			return path;
		}
	}

	public static String stripLastPathElement(String path) {
		
		path = checkEndsWithNoSlash(path);
		int idx;
		if ((idx = path.lastIndexOf('/')) > -1) {
			return path.substring(0, idx);
		} else {
			return path;
		}
	}
	
	public static String firstPathElement(String path) {
		
		path = checkStartsWithNoSlash(path);
		int idx;
		if ((idx = path.indexOf('/')) > -1) {
			return path.substring(0, idx);
		} else {
			return path;
		}
	}

	public static void deleteFolder(File folder) {

		if (folder != null) {
			File[] files = folder.listFiles();
			if (files != null) {
				for (File f : files) {
					if (f.isDirectory()) {
						deleteFolder(f);
					} else {
						f.delete();
					}
				}
			}
			folder.delete();
		}
	}

	public static String formatSize(long size) {
		if (size <= 0)
			return "0";
		final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size
				/ Math.pow(1024, digitGroups))
				+ " " + units[digitGroups];
	}

	public static String formatLastModified(long lastModifiedTime) {
		return dateFormat.format(new Date(lastModifiedTime));
	}
	
	public static String getParentPath(String originalFilename) {
		
		originalFilename = checkEndsWithNoSlash(originalFilename);

		int idx;
		if ((idx = originalFilename.lastIndexOf('/')) > -1) {
			originalFilename = originalFilename.substring(0, idx+1);
		} else if ((idx = originalFilename.lastIndexOf('\\')) > -1) {
			originalFilename = originalFilename.substring(0, idx+1);
		}

		return originalFilename;
	}
	
	public static boolean hasParents(String sourcePath) {
		return checkEndsWithNoSlash(sourcePath).indexOf('/') > -1;
	}
	
	public static List<String> getParentPaths(String sourcePath) {
		List<String> results = new ArrayList<>();
		while(hasParents(sourcePath)) {
			sourcePath = getParentPath(sourcePath);
			results.add(sourcePath);
		}
		return results;
	}

	public static String stripPath(String originalFilename) {

		originalFilename = checkEndsWithNoSlash(originalFilename);

		int idx;
		if ((idx = originalFilename.lastIndexOf('/')) > -1) {
			originalFilename = originalFilename.substring(idx + 1);
		} else if ((idx = originalFilename.lastIndexOf('\\')) > -1) {
			originalFilename = originalFilename.substring(idx + 1);
		}

		return originalFilename;
	}

	public static String lastPathElement(String originalFilename) {
		int idx;
		if ((idx = originalFilename.lastIndexOf('/')) > -1) {
			return originalFilename.substring(idx+1);
		} else {
			return originalFilename;
		}
	}

}
