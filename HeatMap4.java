package hw5;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
/**
 * Step 4 from HW5
 *
 */
public class HeatMap4 {
	static class Tally {
		public double x, y;
		public Tally() {
			this(0.0, 0.0);
		}
		public Tally(Observation datum) {
			this(datum.x, datum.y);
		}
		public Tally(double x, double y) {
			this.x = x;
			this.y = y;
		}
		
		public String toString() {
			return "Tally(" + x + ", " + y + ")";
		}
		
		public static Tally combine(Tally left, Tally right) {
			return new Tally(left.x + right.x, left.y + right.y);
		}
		
		public void accum(Observation datum) {
			this.x += datum.x;
			this.y += datum.y;
		}
		
	}
	
	static class HeatScan extends GeneralScan3<Observation, Tally> {

		public HeatScan(List<Observation> raw) {
			super(raw);
		}

		@Override
		protected Tally init() {
			return new Tally();
		}

		@Override
		protected Tally prepare(Observation datum) {
			return new Tally(datum);
		}

		@Override
		protected Tally combine(Tally left, Tally right) {
			return Tally.combine(left, right);
		}

		@Override
		protected void accum(Tally tally, Observation datum) {
			tally.accum(datum);
		}
	}
		
	public static void main(String[] args) {
		final String FILENAME = "obs_uniform_spray.dat";
		List<Observation> data = new ArrayList<Observation>();
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILENAME));
			Observation obs = (Observation) in.readObject();
			while (!obs.isEOF()) {
				data.add(obs);
				obs = (Observation) in.readObject();
			}
			in.close();
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("reading from " + FILENAME + "failed: " + e);
			e.printStackTrace();
			System.exit(1);				
		}
		
		HeatScan scanner = new HeatScan(data);
		System.out.println("reduction: " + scanner.getReduction());
		List<Tally> prefixSums = scanner.getScan();
		for (int i = 0; i < data.size(); i++) {
			System.out.printf("+ %s = %s%n", data.get(i), prefixSums.get(i));
		}
	}
}
