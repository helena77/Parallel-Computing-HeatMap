package hw5;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * Step 4 from HW5
 */
public class SomeData4 {
	public static void main(String[] args) {
		uniformSpray();
	}
	public static void uniformSpray() {
		final String FILENAME = "obs_uniform_spray.dat";
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILENAME));
			int t = 0;
			for (double r = -0.95; r <= 0.95; r += 0.1) {
				t++; // each row has the same value of t so we can see differences in step 7
				for (double c = -0.95; c <= 0.95; c += 0.1)
					out.writeObject(new Observation(t, c, r));
			}
			out.writeObject(new Observation());  // to mark EOF
			out.close();
		} catch (IOException e) {
			System.out.println("writing to " + FILENAME + "failed: " + e);
         e.printStackTrace();
         System.exit(1);
		}
		System.out.println("wrote " + FILENAME);		
	}


}
