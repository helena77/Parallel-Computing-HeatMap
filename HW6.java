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
 * provide an alternative better parallelization for the HW5
 * first pass: reduces the observatiions into a group-by-timestamp-range tally
 */
public class HW6 {
	private static final int DIM = 20;
	private static final String FILENAME = "obs_uniform_spray.dat";
	private static final Color COLD = new Color(0x0a, 0x37, 0x66);
	private static final Color HOT = Color.RED;
	private static final int TIMERANGE = 20;
	private static final int SECONDPASS = 100;
	private static final int THREAD_NUM = 4;
	
	private static final String REPLAY = "Replay";
	private static JFrame application;
	private static JButton button;
	private static Color[][] grid;
	private static int current;
	private static Tally reduction;
	private static List<int[]> heatmaps;
	
	static class Tally {
		private List<int[]> cellsList;
		//private int[] cells;
		
		public Tally() {
			cellsList = new ArrayList<int[]>();
		}
		
		public Tally(Observation datum) {
			this();
			accum(datum);
		}
		
		private int place(double where) {
			return (int) ((where + 1.0) / (2.0 / DIM));
		}
		
		private void incrCell(int r, int c, int[] cells) {
			cells[r * DIM + c]++;
		}
		
		public int getCell(int r, int c, int[] cells) {
			return cells[r * DIM + c];
		}
		
		public static Tally combine(Tally a, Tally b) {
			Tally tally = new Tally();
			int maxSize = Math.max(a.cellsList.size(), b.cellsList.size());
			for (int i = 0; i < maxSize; i++) {					
				tally.cellsList.add(new int[DIM * DIM]);
			}	
			int size = Math.min(a.cellsList.size(), b.cellsList.size());
			for(int i = 0; i < size; i++) {
				for (int j = 0; j < tally.cellsList.get(i).length; j++)
					tally.cellsList.get(i)[j] = a.cellsList.get(i)[j] + b.cellsList.get(i)[j];
			}
			if (a.cellsList.size() > size) {
				int count = size;
				while (count < a.cellsList.size() - 1) {
					tally.cellsList.add(a.cellsList.get(count));
					count += 1;
				}
			}
			if (b.cellsList.size() > size) {
				int count = size;
				while (count < b.cellsList.size() - 1) {
					tally.cellsList.add(b.cellsList.get(count));
					count += 1;
				}
			}
			return tally;
		}
		
		public void accum(Observation datum) {
			int index = (int)datum.time;
			if (cellsList.size() - 1 < index) {
				for (int i = cellsList.size(); i <= index; i++) {					
						cellsList.add(new int[DIM * DIM]);
				}				
			}
			incrCell(place(datum.x), place(datum.y),cellsList.get(index));
		}
	}
	
	static class HeatReduction extends GeneralScan3<Observation, Tally> {

		public HeatReduction(List<Observation> raw) {
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
		
		HeatReduction reduce = new HeatReduction(data);
		reduction = reduce.getReduction();
//		for (int i = 0; i < heatmaps.cellsList.size(); i++) {
//			for (int j = 0; j < heatmaps.cellsList.get(i).length; j++) {
//				System.out.println(heatmaps.cellsList.get(i)[j]);
//			}
//		}
		
		//heatmaps = reduction.cellsList;
		heatmaps = produceHeatMap(TIMERANGE, SECONDPASS);
			
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
	
	private static List<int[]> produceHeatMap(int range, int pass) throws InterruptedException{
		Thread[] threads = new Thread[THREAD_NUM];
		List<int[]> data = new ArrayList<int[]>(range);
//		for (int i = 0; i < range; i++) {
//			data.add(i, reduction.cellsList.get(i));
//		}
		data = reduction.cellsList;
		//System.out.println(data.size());
		int piece = data.size() / THREAD_NUM;
		//System.out.println(piece);
		
		int start = 0;
		for (int i = 0; i < THREAD_NUM; i++) {
			int end = i == THREAD_NUM - 1 ? reduction.cellsList.size() : start + piece;
			//System.out.println("start: " + start + "; end: " + end);
			threads[i] = new Thread(new Compute(start, end, pass, data));
			threads[i].start();
			start = end;
		}
		for (int i = 0; i < THREAD_NUM; i++)
			threads[i].join();	
		
		return data;
	}
	
	static class Compute implements Runnable {
		protected int start, end, pass;
		protected List<int[]> data;
		
		public Compute(int start, int end, int pass, List<int[]> data) {
			this.start = start;
			this.end = end;
			this.pass = pass;
			this.data = data;
		}
		
		@Override
		public void run() {
			for (int i = start; i < end; i++) {
				//System.out.println("i: " + i);
				if (start <= pass) {
					for (int j = i - 1; j >= 0; j--) {
						//System.out.println("j: " + j);
						double weight = (double) 1 / (1 + i - j);
						//System.out.println("weight: " + weight);
						for (int k = 0; k < data.get(i).length ; k++)
							data.get(i)[k] += (data.get(j)[k] * weight);
					}					
				} else {
					for (int j = i - 1; j > start - pass; j--) {
						double weight = (double) 1 / (1 + i - j);
						for (int k = 0; k < data.get(i).length ; k++)
							data.get(i)[k] += (data.get(j)[k] * weight);
					}		
				}				
			}			
		}
		
	}
	
	private static void animate() throws InterruptedException {
		button.setEnabled(false);
		current = 0; 			
 		for (int i = 0; i < DIM; i++) {
 			for (int j = 0; j < DIM; j++) { 	
 				if (current < heatmaps.size())
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
				grid[c][r] = interpolateColor(heatmaps.get(current)[r * DIM + c], COLD, HOT);
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
