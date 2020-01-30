package hw5;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Little test of hw5.GeneralScan3
 */
public class ScanTest3 {
	
	static class Tally {
		public double d;
		public Tally() {
			d = 0.0;
		}
		
		public Tally(double d) {
			this.d = d;
		}
		
		public static Tally combine(Tally a, Tally b) {
			return new Tally(a.d + b.d);
		}
		
		public void accum(double datum) {
			this.d += datum;
		}
	}

	static class SumScan extends GeneralScan3<Double, Tally> {

		public SumScan(List<Double> raw) {
			super(raw);
		}

		@Override
		protected Tally init() {
			return new Tally();
		}

		@Override
		protected Tally prepare(Double datum) {
			return new Tally(datum);
		}

		@Override
		protected Tally combine(Tally left, Tally right) {
			return Tally.combine(left, right);
		}

		@Override
		protected void accum(Tally tally, Double datum) {
			tally.accum(datum);
		}
	}

	/**
	 * Test
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		final int N = 10_200_002;
		final double EPSILON = 1e-3;
		List<Double> data = randomArray(N);
		SumScan scanner = new SumScan(data);
		List<Tally> prefixSums = scanner.getScan();
		double check = 0.0;
		for (int i = 0; i < N; i++) {
			check += data.get(i);
			if (Math.abs(check - prefixSums.get(i).d) > EPSILON) {
				System.out.printf("FAILED at " + i + " " + check + " vs. " + prefixSums.get(i).d);
				break;
			}
			if (i < 10)
				System.out.printf("+ %8.2f = %8.2f%n", data.get(i), prefixSums.get(i).d);
			else if (i == N - 1 || i % 10000 == 0)
				System.out.printf("...(%d)%n+ %8.2f = %8.2f%n", i, data.get(i), prefixSums.get(i).d);
		}
	}

	/*
	 * Helper stuff for tests to follow...
	 */
	private static Random rand = new Random();

	private static List<Double> randomArray(int n) {
		List<Double> ret = new ArrayList<>(n);
		for (int i = 0; i < n; i++)
			ret.add(rand.nextDouble() * 100.0);
		return ret;
	}
}
