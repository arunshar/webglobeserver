package edu.buffalo.webglobe.server.utils;

public class Printer {
	public static void printArrayInt(int[] a) {
		System.out.print("[");
		for (int i : a) {
			System.out.print(i + ", ");
		}
		System.out.println("]");
	}
}
