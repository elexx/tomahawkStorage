package network.tests;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import network.tests.NioServerTester.TimeResult;

public class NioServerTester implements Callable<TimeResult> {
	private final int num;

	public NioServerTester(int num) {
		this.num = num;
	}

	@Override
	public TimeResult call() throws IOException {
		long timeBeforeConnect, timeAfterConnect, timeBeforeSendReceive, timeAfterSendReceive;
		Socket socket = new Socket();
		SocketAddress endpoint = new InetSocketAddress("localhost", 9090);

		timeBeforeConnect = System.currentTimeMillis();
		socket.connect(endpoint);
		timeAfterConnect = System.currentTimeMillis();

		byte[] line = new byte[] { 0x0, 0x0, 0x0, 0x4, 0x40, 0x11, 0x12, 0x13, 0x14 };
		// (num + "testittestitblubbirgendwastestittestitblubbirgendwastestitblubbirgendwastestittestitblubbirgendwas" + num).getBytes();
		byte[] buffer = new byte[line.length];
		BufferedOutputStream outStream = new BufferedOutputStream(socket.getOutputStream());
		BufferedInputStream inStream = new BufferedInputStream(socket.getInputStream());

		timeBeforeSendReceive = System.currentTimeMillis();
		for (int i = 0; i < 1; i++) {
			outStream.write(line);
			outStream.flush();
			inStream.read(buffer);
			if (!Arrays.equals(line, buffer)) {
				outStream.close();
				inStream.close();
				socket.close();
				throw new IOException("send and received data doesnot match");
			}
		}
		timeAfterSendReceive = System.currentTimeMillis();

		outStream.close();
		inStream.close();
		socket.close();

		TimeResult time = new TimeResult();
		time.connect = (int) (timeAfterConnect - timeBeforeConnect);
		time.sendReceive = (int) (timeAfterSendReceive - timeBeforeSendReceive);
		return time;
	}

	public static void main(String[] args) {
		ExecutorService eservice = Executors.newFixedThreadPool(1000);

		List<Future<TimeResult>> results = new LinkedList<Future<TimeResult>>();
		List<Integer> connectTimes = new ArrayList<Integer>();
		List<Integer> sendReceiveTimes = new ArrayList<Integer>();

		int currentThreads = 0;
		int maxThreads = 0;
		int countExceptions = 0;

		double startTime = System.currentTimeMillis();
		do {
			results.add(eservice.submit(new NioServerTester(currentThreads + (int) System.currentTimeMillis())));
			currentThreads++;
			maxThreads = Math.max(currentThreads, maxThreads);
			Iterator<Future<TimeResult>> resultIterator = results.iterator();
			while (resultIterator.hasNext()) {
				Future<TimeResult> f = resultIterator.next();
				if (f.isDone()) {
					try {
						connectTimes.add(f.get().connect);
						sendReceiveTimes.add(f.get().sendReceive);
					} catch (ExecutionException | InterruptedException e) {
						countExceptions++;
					} finally {
						resultIterator.remove();
						currentThreads--;
					}
				}

			}
		} while (!(System.currentTimeMillis() - startTime > 1000 || maxThreads >= 1));

		long midTime = System.currentTimeMillis();

		while (results.size() > 0) {
			Iterator<Future<TimeResult>> resultIterator = results.iterator();
			while (resultIterator.hasNext()) {
				Future<TimeResult> f = resultIterator.next();
				if (f.isDone()) {
					try {
						connectTimes.add(f.get().connect);
						sendReceiveTimes.add(f.get().sendReceive);
					} catch (ExecutionException | InterruptedException e) {
						countExceptions++;
					} finally {
						resultIterator.remove();
						currentThreads--;
					}
				}

			}
		}
		long endTime = System.currentTimeMillis();

		eservice.shutdown();
		Collections.sort(connectTimes);
		Collections.sort(sendReceiveTimes);

		System.out.printf("Count  %4d %4d\n", connectTimes.size(), sendReceiveTimes.size());
		System.out.printf("Mean   %4.0f %4.0f\n", mean(connectTimes), mean(sendReceiveTimes));
		System.out.printf("Median %4.0f %4.0f\n", median(connectTimes), median(sendReceiveTimes));
		System.out.printf("Stddev %4.0f %4.0f\n", sd(connectTimes), sd(sendReceiveTimes));
		System.out.printf("Threads %d\n", maxThreads);
		System.out.printf("Exceptions %d\n", countExceptions);
		System.out.printf("Runtime [mid %.2f] [total %.2f]\n", (midTime - startTime) / 1000, (endTime - startTime) / 1000);
	}

	public static int sum(List<Integer> a) {
		if (a.size() > 0) {
			int sum = 0;

			for (Integer i : a) {
				sum += i;
			}
			return sum;
		}
		return 0;
	}

	public static double mean(List<Integer> a) {
		int sum = sum(a);
		double mean = 0;

		if (sum > 0) {
			mean = sum / (a.size() * 1.0);
		}
		return mean;
	}

	public static double median(List<Integer> a) {
		int middle = a.size() / 2;

		if (a.size() % 2 == 1) {
			return a.get(middle);
		} else {
			return (a.get(middle - 1) + a.get(middle)) / 2.0;
		}
	}

	public static double sd(List<Integer> a) {
		int sum = 0;
		double mean = mean(a);

		for (Integer i : a) {
			sum += Math.pow((i - mean), 2);
		}
		return Math.sqrt(sum / (a.size() - 1)); // sample
	}

	public class TimeResult {
		public int connect, sendReceive;
	}
}
