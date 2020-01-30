package hw5;
/*
 * author: Helena
 * version: 1
 * This class is used to test the GeneralScan1
 */
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Little test of hw5.GeneralScan1
 */
public class ScanTest1 extends GeneralScan1<Double, Double> {

	public ScanTest1(List<Double> raw) {
		super(raw);
	}
	
	@Override
	protected Double init() {
		return 0.0;
	}

	@Override
	protected Double prepare(Double datum) {
		return datum;
	}

	@Override
	protected Double combine(Double left, Double right) {
		return left + right;
	}

	/**
	 * Test
	 * @param args
	 */
	public static void main(String[] args) {
		final int N = 1<<20; 
		final double EPSILON = 1e-3;
		List<Double> data = randomArray(N);
		ScanTest1 scanner = new ScanTest1(data);
		List<Double> prefixSums = scanner.getScan();
		double check = 0.0;
		for (int i = 0; i < N; i++) {
			check += data.get(i);
			if (Math.abs(check - prefixSums.get(i)) > EPSILON) {
				System.out.printf("FAILED at " + i + " " + check + " vs. " + prefixSums.get(i));
				break;
			}
			if (i < 10)
				System.out.printf("+ %8.2f = %8.2f%n", data.get(i), prefixSums.get(i));
			else if (i == N-1 || i%10000 == 0)
				System.out.printf("...(%d)%n+ %8.2f = %8.2f%n", i, data.get(i), prefixSums.get(i));
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
