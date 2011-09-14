package com.tipsolutions.jacket.file;

import java.io.File;
import java.io.IOException;

import android.os.Environment;

public class FileUtils {

	static public File GetExternalFile(String file, boolean createDir) throws IOException {
		File dir = new File(Environment.getExternalStorageDirectory(), "Android/data");
		if (dir == null) {
			return null;
		}
		if (createDir) {
			ForceMkdir(dir);
		}
		return new File(dir, file);
	}

	public static void ForceMkdir(File directory) throws IOException {
		if (directory.exists()) {
			if (directory.isFile()) {
				String message =
					"File "
					+ directory
					+ " exists and is "
					+ "not a directory. Unable to create directory.";
				throw new IOException(message);
			}
		} else {
			if (!directory.mkdirs()) {
				String message =
					"Unable to create directory " + directory;
				throw new IOException(message);
			}
		}
	}
}
