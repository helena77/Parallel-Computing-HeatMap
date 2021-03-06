package hw5;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
/**
 * Step 7a from HW5
 *
 */
public class HeatMap7a {
	private static final int DIM = 20;
	private static final int THREAD_THRESHOLD = 15;
	private static final int IRRELEVANCE_THRESHOLD = 20;
	private static final int SAMPLING_INTERVAL = 5;
	private static final String FILENAME = "obs_uniform_spray.dat";
	private static final Color COLD = new Color(0x0a, 0x37, 0x66);
	private static final Color HOT = Color.RED;
	private static final String REPLAY = "Replay";
	private static JFrame application;
	private static JButton button;
	private static Color[][] grid;
	private static int current;
	private static List<Tally> heatmaps;
	
	static class Tally {
		private int[] cells;
		private Observation[] history;
		private int front, back;
		
		public Tally() {
			cells = new int[DIM * DIM];
			history = new Observation[IRRELEVANCE_THRESHOLD];
			front = 0;
			back = 0;
		}
		
		public Tally(Observation datum) {
			this();
			accum(datum);
		}
		
		private int place(double where) {
			return (int) ((where + 1.0) / (2.0 / DIM));
		}
		
		private void incrCell(int r, int c) {
			cells[r * DIM + c]++;
		}
		
		private void decrCell(int r, int c) {
			cells[r * DIM + c]--;
		}
		
		public int getCell(int r, int c) {
			return cells[r * DIM + c];
		}
		
		public static Tally combine(Tally a, Tally b) {
			Tally tally = new Tally();
				for(int afront = a.front; afront != a.back; afront = nextqueue(afront)) {
					tally.accum(a.history[afront]);
				}
				for(int bfront = b.front; bfront != b.back; bfront = nextqueue(bfront)) {
					tally.accum(b.history[bfront]);
				}
			return tally;
		}
		
		public void accum(Observation datum) {
			if(datum != null) {
				incrCell(place(datum.x), place(datum.y));
				if(fullqueue()) {
					Observation old = dequeue();
					if (old != null)
						decrCell(place(old.x), place(old.y));
				}
				enqueue(datum);
			} 
		}
		
		private void enqueue(Observation datum) {
			if (!fullqueue()) {
				history[back] = datum;
				back = nextqueue(back);
 			}
		}
		
		private Observation dequeue() {
			Observation old = null;
			if (!emptyqueue()) {
				old = history[front];
				front = nextqueue(front);
			}
			return old;
		}
		
		private boolean fullqueue() {
			return nextqueue(back) == front;
		}
		
		private boolean emptyqueue() {
			return front == back;
		}
		
		private static int nextqueue(int i) {
			return (i + 1) % (IRRELEVANCE_THRESHOLD);
		}
		
		public String toString() {
			return Integer.toString(front) + ": " + Integer.toString(back) + Arrays.toString(history);
		}
	}
	
	static class HeatScan extends GeneralScan3<Observation, Tally> {

		public HeatScan(List<Observation> raw) {
			super(raw, THREAD_THRESHOLD);
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
	
	public static void main(String[] args) throws FileNotFoundException, InterruptedException{
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
		heatmaps = scanner.getScan();
		current = 0;
		
		grid = new Color[DIM][DIM];
		application = new JFrame();
		application.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		fillGrid(grid);
		
		ColoredGrid gridPanel = new ColoredGrid(grid);
		application.add(gridPanel, BorderLayout.CENTER);
		
		button = new JButton(REPLAY);
		button.addActionListener(new BHandler());
		application.add(button, BorderLayout.PAGE_END);
		
		application.setSize(DIM * 8, (int)(DIM * 8));
		application.setVisible(true);
		application.repaint();
		animate();
	}
	
	private static void animate() throws InterruptedException {
		button.setEnabled(false);
		current = 0;
 		for (int i = 0; i < DIM; i++) { 
 			for (int j = 0; j < DIM; j++) {				
 				//if(current < DIM * DIM)
 				fillGrid(grid);
 				application.repaint();
 				Thread.sleep(50);
 			}
		}
		button.setEnabled(true);
		application.repaint();
	}
	
	static class BHandler implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (REPLAY.equals(e.getActionCommand())) {
				new Thread() {
			        public void run() {
			            try {
								animate();
							} catch (InterruptedException e) {
								System.exit(0);
							}
			        }
			    }.start();
			}
		}
	};

	private static void fillGrid(Color[][] grid) {
		//for (int r = 0; r < grid.length; r++)			
			for (int c = 0; c < grid[SAMPLING_INTERVAL].length; c++) {
				grid[c][SAMPLING_INTERVAL] = interpolateColor(heatmaps.get(current).getCell(SAMPLING_INTERVAL, c), COLD, HOT);	
				//System.out.println(heatmaps.get(current).getCell(r, c));
			}
		current +=  1;
	}
	
	private static Color interpolateColor(double ratio, Color a, Color b) {
		ratio = Math.min(ratio, 1.0);
		int ax = a.getRed();
		int ay = a.getGreen();
		int az = a.getBlue();
		int cx = ax + (int) ((b.getRed() - ax) * ratio);
		int cy = ay + (int) ((b.getGreen() - ay) * ratio);
		int cz = az + (int) ((b.getBlue() - az) * ratio);
		return new Color(cx, cy, cz);
	}

}
