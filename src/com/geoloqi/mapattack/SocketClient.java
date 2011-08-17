package com.geoloqi.mapattack;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.content.Context;

import com.geoloqi.Installation;
import com.geoloqi.data.Fix;
import com.geoloqi.interfaces.GeoloqiFixSocket;

public class SocketClient implements GeoloqiFixSocket {

	private InetAddress uploadAddress;
	private InetAddress downloadAddress;
	public final int uploadPort = 40000;
	public final int downloadPort = 40001;

	private DatagramSocket uploadSocket;

	private static SocketClient client = null;
	byte[] uuid;

	public static SocketClient getApplicationClient(Context context) {
		if (client == null) {
			client = new SocketClient(Installation.id(context));
		}
		return client;
	}

	private SocketClient(String uuid) {
		this.uuid = uuid.getBytes();
		try {
			uploadAddress = InetAddress.getByName("loki.geoloqi.com");
			downloadAddress = uploadAddress;
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void pushFixes(Fix[] fixes) {
		for (int i = 0; i < fixes.length; i++) {
			DatagramPacket datagram = encode(fixes[i]);
		}
	}

	public DatagramPacket encode(Fix fix) {
		byte[] bytes = new byte[24];
		bytes[0] = 0X0;
		bytes[1] = 0x41;
		blitInt(fix.getTime(), bytes, 2);
		blitInt(Math.pow(((fix.getLatitude() + 90) / 180) * 2, 32.), bytes, 6);
		blitInt(Math.pow(((fix.getLatitude() + 90) / 360) * 2, 32.), bytes, 10);
		blitShort(fix.getSpeed(), bytes, 12);
		blitShort(fix.getBearing(), bytes, 14);
		blitShort(fix.getAltitude(), bytes, 16);
		blitShort(fix.getAccuracy(), bytes, 18);
		blitShort(fix.getExtras().getInt("battery"), bytes, 20);
		blitUUID(bytes, 22);
		return new DatagramPacket(bytes, bytes.length, uploadAddress, uploadPort);
	}

	private void blitInt(int integer, byte[] out, int offset) {
		out[offset] = (byte) ((integer & 0xff000000) >>> 24);
		out[offset + 1] = (byte) ((integer & 0x00ff0000) >>> 16);
		out[offset + 2] = (byte) ((integer & 0x0000ff00) >>> 8);
		out[offset + 3] = (byte) ((integer & 0x000000ff));
	}

	private void blitInt(long val, byte[] out, int offset) {
		blitInt((int) val, out, offset);
	}

	private void blitInt(double val, byte[] out, int offset) {
		blitInt((int) val, out, offset);
	}

	private void blitShort(int integer, byte[] out, int offset) {
		out[offset] = (byte) ((integer & 0x0000ff00) >>> 8);
		out[offset + 1] = (byte) ((integer & 0x000000ff));
	}

	private void blitShort(double val, byte[] out, int offset) {
		blitShort((int) val, out, offset);
	}

	private void blitUUID(byte[] out, int offset) {
		for (int i = 0; i < uuid.length; i++) {
			out[offset + i] = uuid[i];
		}
	}

}
