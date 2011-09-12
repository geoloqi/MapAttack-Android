package com.geoloqi;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.UUID;

import android.content.Context;
import android.util.Pair;

public class Installation {
	private static Pair<Long, Long> uuid;
	private static final String FILE = "INSTALLATION";

	private static void writeUUID(File installation) {
		try {
			UUID uuid = UUID.randomUUID();
			DataOutputStream out = new DataOutputStream(new FileOutputStream(installation));
			out.writeLong(uuid.getMostSignificantBits());
			out.writeLong(uuid.getLeastSignificantBits());
			out.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static Pair<Long, Long> getUUID(Context context) {
		if (uuid != null) {
			return uuid;
		} else {
			File uuidFile = new File(context.getFilesDir(), FILE);
			if (!uuidFile.exists()) {
				writeUUID(uuidFile);
			}
			uuid = readUUID(uuidFile);
			return uuid;
		}
	}

	public synchronized static byte[] getIDAsBytes(Context context) {
		try {
			Pair<Long, Long> uuid = getUUID(context);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeLong(uuid.first);
			dos.writeLong(uuid.second);
			return baos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized static String getIDAsString(Context context) {
		byte[] b = getIDAsBytes(context);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(baos);
		printStream.printf("%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x", b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7], b[8], b[9], b[10], b[11], b[12], b[13], b[14], b[15]);
		return new String(baos.toByteArray());
	}

	private static Pair<Long, Long> readUUID(File installation) {
		if (uuid != null) {
			return uuid;
		}
		try {
			DataInputStream in = new DataInputStream(new FileInputStream(installation));
			Long msb = in.readLong();
			Long lsb = in.readLong();
			in.close();
			return new Pair<Long, Long>(msb, lsb);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}