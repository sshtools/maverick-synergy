package com.sshtools.common.files;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileUtils {

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

	public static void closeQuietly(InputStream in) {
		try {
			if (in != null) {
				in.close();
			}
		} catch (IOException e) {
		}
	}

	public static void closeQuietly(OutputStream out) {
		try {
			if (out != null) {
				out.close();
			}
		} catch (IOException e) {
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

	public static boolean hasParents(String sourcePath) {
		return sourcePath.indexOf('/') > -1;
	}

}
