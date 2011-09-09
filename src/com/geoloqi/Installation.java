package com.geoloqi;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import android.content.Context;
import android.util.Pair;

public class Installation {
	private static Pair<Long, Long> uuid;
	private static final String FILE = "INSTALLATION";

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
		Pair<Long, Long> uuid = getUUID(context);
		return "" + Long.toHexString(uuid.first) + Long.toHexString(uuid.second);
	}

	private static Pair<Long, Long> getUUID(Context context) {
		if (uuid != null) {
			return uuid;
		} else {
			File uuidFile = new File(context.getFilesDir(), FILE);
			if (!uuidFile.exists()) {
				writeUUID(uuidFile);
			}
			return readUUID(uuidFile);
		}
	}

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
			writeUUID(installation);
			return readUUID(installation);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}