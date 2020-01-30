package hw5;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;

import javax.swing.JButton;
import javax.swing.JFrame;

/**
 * Quick hacked-together demo of the ColorGrid and a heat map
 */
public class CGDemo {
	private static final int DIM = 150;
	private static final String REPLAY = "Replay";
	private static JFrame application;
	private static JButton button;
	private static Color[][] grid;

	public static void main(String[] args) throws FileNotFoundException, InterruptedException {
		grid = new Color[DIM][DIM];
		application = new JFrame();
		application.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		fillGrid(grid);
		
		ColoredGrid gridPanel = new ColoredGrid(grid);
		application.add(gridPanel, BorderLayout.CENTER);
		
		button = new JButton(REPLAY);
		button.addActionListener(new BHandler());
		application.add(button, BorderLayout.PAGE_END);
		
		application.setSize(DIM * 4, (int)(DIM * 4.4));
		application.setVisible(true);
		application.repaint();
		animate();
	}
	
	private static void animate() throws InterruptedException {
		button.setEnabled(false);
 		for (int i = 0; i < DIM; i++) { 
			fillGrid(grid);
			application.repaint();
			Thread.sleep(50);
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

	static private final Color COLD = new Color(0x0a, 0x37, 0x66), HOT = Color.RED;
	static private int offset = 0;
	private static void fillGrid(Color[][] grid) {
		int pixels = grid.length * grid[0].length;
		for (int r = 0; r < grid.length; r++)
			for (int c = 0; c < grid[r].length; c++) {
				grid[r][c] = interpolateColor((r*c+offset)%pixels / (double)pixels, COLD, HOT);
				System.out.println((r*c)%pixels);
			}
		offset += DIM;
	}
	
	private static Color interpolateColor(double ratio, Color a, Color b) {
		int ax = a.getRed();
		int ay = a.getGreen();
		int az = a.getBlue();
		int cx = ax + (int) ((b.getRed() - ax) * ratio);
		int cy = ay + (int) ((b.getGreen() - ay) * ratio);
		int cz = az + (int) ((b.getBlue() - az) * ratio);
		return new Color(cx, cy, cz);
	}

}
