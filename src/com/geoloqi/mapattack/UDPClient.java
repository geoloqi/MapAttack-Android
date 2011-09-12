package com.geoloqi.mapattack;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;

import com.geoloqi.Installation;
import com.geoloqi.data.Fix;
import com.geoloqi.interfaces.GeoloqiFixSocket;

public class UDPClient implements GeoloqiFixSocket {

	public final String uploadHost = "loki.geoloqi.com";
	public final int uploadPort = 40000;

	private ReentrantLock lock = new ReentrantLock(true);
	private DatagramSocket uploadSocket;

	private static UDPClient client = null;

	Context context;

	public static UDPClient getApplicationClient(Context context) {
		if (client == null) {
			client = new UDPClient(context);
		}
		return client;
	}

	private UDPClient(Context context) {
		this.context = context;
		try {
			uploadSocket = new DatagramSocket();
			uploadSocket.connect(new InetSocketAddress(uploadHost, uploadPort));
			uploadSocket.send(new DatagramPacket(new byte[] { 0, 0, 0 }, 3));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void pushFix(Fix fix) {
		lock.lock();
		try {
			uploadSocket.send(encode(fix));
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			lock.unlock();
		}
	}

	public void close() {
		lock.lock();
		try {
			uploadSocket.close();
		} finally {
			lock.unlock();
		}
	}

	static MathContext math = MathContext.DECIMAL64;
	private static final BigDecimal NINETY = new BigDecimal(90., math);
	private static final BigDecimal ONE_EIGHTY = new BigDecimal(180., math);
	private static final BigDecimal THREE_SIXTY = new BigDecimal(360., math);
	private static final BigDecimal MAX = new BigDecimal(4294967295., math);

	private DatagramPacket encode(Fix fix) {
		byte[] bytes = new byte[39];
		long lat = new BigDecimal(fix.getLatitude()).add(NINETY).divide(ONE_EIGHTY, math).multiply(MAX).round(math).longValue();
		long lng = new BigDecimal(fix.getLongitude()).add(ONE_EIGHTY).divide(THREE_SIXTY, math).multiply(MAX).round(math).longValue();

		bytes[0] = 0x41;
		blitInt(fix.getTime() / 1000L, bytes, 1);
		blitInt(lat, bytes, 5);
		blitInt(lng, bytes, 9);
		blitShort(fix.getSpeed(), bytes, 13);
		blitShort(fix.getBearing(), bytes, 15);
		blitShort(fix.getAltitude(), bytes, 17);
		blitShort(fix.getAccuracy(), bytes, 19);
		blitShort(fix.getExtras() == null ? 0 : fix.getExtras().getInt("battery"), bytes, 21);
		blitUUID(bytes, 23);
		return new DatagramPacket(bytes, bytes.length);
	}

	private void blitInt(long val, byte[] out, int offset) {
		out[offset] = (byte) ((val & 0xff000000) >>> 24);
		out[offset + 1] = (byte) ((val & 0x00ff0000) >>> 16);
		out[offset + 2] = (byte) ((val & 0x0000ff00) >>> 8);
		out[offset + 3] = (byte) ((val & 0x000000ff));
	}

	private void blitShort(int integer, byte[] out, int offset) {
		out[offset] = (byte) ((integer & 0x0000ff00) >>> 8);
		out[offset + 1] = (byte) ((integer & 0x000000ff));
	}

	private void blitShort(double val, byte[] out, int offset) {
		blitShort((int) val, out, offset);
	}

	private void blitUUID(byte[] out, int offset) {
		byte[] uuid = Installation.getIDAsBytes(context);
		for (int i = 0; i < uuid.length; i++) {
			out[offset + i] = uuid[i];
		}
	}
}
