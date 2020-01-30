package hw5;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Represents an observation from our detection device. When a location on the
 * sensor triggers, the time and the location of the detected event are recorded
 * in one of these Observation objects.
 */
public class Observation implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final long EOF = Long.MAX_VALUE;  // our convention to mark EOF with a special object

	public long time; // number of milliseconds since turning on the detector device
	public double x, y; // location of the detected event on the detection grid
	
	public Observation(long time, double x, double y) {
		this.time = time;
		this.x = x;
		this.y = y;
	}
	
	public Observation() {
		this.time = EOF;
		this.x = this.y = 0.0;
	}
	
	public boolean isEOF() {
		return time == EOF;
	}
	
	public String toString() {
		//return "Observation(" + time + ", " + x + ", " + y + ")";
		return String.format("(%d,%.2f,%.2f)", time, x, y);
	}

	/**
	 * Example with serialization of a series of Observation to a local file.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		final String FILENAME = "observation_test.dat";
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILENAME));
			for (long t = 0; t < 6; t++)
				for (double x = 0.0; x <= 1000.0; x += 100.0)
					for (double y = -3.0; y >= -50.0; y -= 9.0)
						out.writeObject(new Observation(t, x, y));
			out.writeObject(new Observation());  // to mark EOF
			out.close();
		} catch (IOException e) {
			System.out.println("writing to " + FILENAME + "failed: " + e);
         e.printStackTrace();
         System.exit(1);
		}
		
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILENAME));
			int count = 0;
			Observation obs = (Observation) in.readObject();
			while (!obs.isEOF()) {
				System.out.println(++count + ": " + obs);
				obs = (Observation) in.readObject();
			}
			in.close();
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("reading from " + FILENAME + "failed: " + e);
         e.printStackTrace();
         System.exit(1);
		}
	}

}
