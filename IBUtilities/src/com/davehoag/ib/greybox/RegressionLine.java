package com.davehoag.ib.greybox;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

import com.davehoag.ib.dataTypes.DoubleCache;

public class RegressionLine extends JPanel {
	final int startX = 0;
	final int startY = 10;
	final int endX = 40;
	final int endY = 10;
	final Color [] colors = { Color.WHITE, Color.YELLOW, Color.PINK, Color.BLACK, Color.GRAY, Color.BLUE, Color.BLACK };
	DoubleCache priceHistory;
	Runnable repainter = null;
	public void setPriceHistory(DoubleCache dc){
		priceHistory = dc;
	}
	@Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        int x = startX, y = startY, eX = endX, eY = endY;
    	double slope = priceHistory == null ? 0 : priceHistory.getPercentageSlope();
    	double coefOfDeterm = priceHistory == null ? 0 : priceHistory.getRSquared();
    	int idx = (int)(7*coefOfDeterm);
    	x = (int) (x + Math.abs(20*slope));
    	eX = (int) (eX - Math.abs(20*slope));
    	y = (int) (y + slope*10);
    	eY = (int) (eY - slope*10);
        g.setColor(colors[idx]);
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(1 + (int)(10*coefOfDeterm)));
        g.drawLine(x, y, eX, eY);
        if(repainter == null) setupRepainter();
    }
	void setupRepainter(){
		repainter = new Runnable(){
			public void run(){
				while(true){
					try {
						Thread.currentThread().sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					RegressionLine.this.repaint();
				}
			}
		};
		Thread t = new Thread(repainter);
		t.setDaemon(true);
		t.start();
	}
    @Override
    public Dimension getPreferredSize(){
    	return new Dimension(45,25);
    }
}
