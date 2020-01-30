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
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
/**
 * Step 5 from HW5
 *
 */
public class HeatMap5 {
	private static final int DIM = 20;
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
		
		public Tally() {
			cells = new int[DIM * DIM];
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
		
		public int getCell(int r, int c) {
			return cells[r * DIM + c];
		}
		
		public static Tally combine(Tally a, Tally b) {
			Tally tally = new Tally();
			for(int i = 0; i < tally.cells.length; i++) {
				tally.cells[i] = a.cells[i] + b.cells[i];
			}
			return tally;
		}
		
		public void accum(Observation datum) {
			incrCell(place(datum.x), place(datum.y));
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
		for (int r = 0; r < grid.length; r++)			
			for (int c = 0; c < grid[r].length; c++) {
				grid[c][r] = interpolateColor(heatmaps.get(current).getCell(r, c), COLD, HOT);	
				//System.out.println(heatmaps.get(current).getCell(r, c));				
			}	
		current += 1;
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
