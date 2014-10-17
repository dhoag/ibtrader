package com.davehoag.ib.greybox;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JButton;

import java.awt.BorderLayout;

import javax.swing.JTextPane;
import javax.swing.JPanel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;

import com.davehoag.ib.IBClientRequestExecutor;
import com.davehoag.ib.QuoteRouter;
import com.davehoag.ib.ResponseHandler;
import com.davehoag.ib.strategies.DefenseStrategy;
import com.davehoag.ib.strategies.OutputQuotesStrategy;
import com.ib.client.EClientSocket;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JTextField;
import javax.swing.JLabel;

public class Urlacher {

	private JFrame frame;
	IBClientRequestExecutor clientInterface;
	private JTextField tfContractExpiration;
	private JTextPane tpStatus;
	private DefenseStrategy defense;
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Urlacher window = new Urlacher();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public Urlacher() {
		initialize();
		connect();
		tfContractExpiration.setText("201412");
	}

	public void connect(){
		ResponseHandler rh = new ResponseHandler();
		
		EClientSocket m_client = new EClientSocket(rh);
		clientInterface = new IBClientRequestExecutor(m_client, rh);
		clientInterface.connect();
		clientInterface.initializePortfolio( );
		tpStatus.setText("Connected!");

	}
	/**
	 * Get the market data for the given contract.
	 */
	public void requestQuotes(){
		QuoteRouter router = clientInterface.getQuoteRouter("ES", tfContractExpiration.getText());
		router.dontGetHistoricalData();
		//OutputQuotesStrategy dis = new OutputQuotesStrategy();
		//router.addStrategy(dis);

		defense = new DefenseStrategy();
		router.addStrategy(defense);
		//Need to request quotes after the router is created and strategies set
		clientInterface.requestQuotes();
		tpStatus.setText("Requested Quotes");
	}
	public void cancelQuotes(){
		clientInterface.cancelMktData();
	}
	public void goLong(){
		if(defense == null) return;

		tpStatus.setText("Submitting order to go long");
		defense.goLong();
	}
	public void goShort(){
		if(defense == null) return;
		tpStatus.setText("Submitting order to go short");
		defense.goShort();
	}
	public void playDefense(){
		if(defense == null) return;
		defense.playDefense();
	}
	public void halt(){
		if(defense == null) return;

		tpStatus.setText("Halting all trading!");
		defense.haltTrading();
	}
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel panel = new JPanel();
		frame.getContentPane().add(panel, BorderLayout.WEST);
		
		JButton btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				connect();
			}
		});
		
		JButton btnOff = new JButton("Off");
		btnOff.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancelQuotes();
			}
		});
		
		JButton btnOn = new JButton("On");
		btnOn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				requestQuotes();
			}
		});
		panel.setLayout(new GridLayout(5, 1, 0, 0));
		panel.add(btnConnect);
		panel.add(btnOff);
		
		JButton btnGoLong = new JButton("Go Long");
		btnGoLong.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				goLong();
			}
		});
		panel.add(btnGoLong);
		panel.add(btnOn);
		
		JButton btnGoShort = new JButton("Go Short");
		btnGoShort.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				goShort();
			}
		});
		panel.add(btnGoShort);
		
		JPanel panel_1 = new JPanel();
		frame.getContentPane().add(panel_1, BorderLayout.NORTH);
		
		JLabel lblExpirationDateYyyymm = new JLabel("Expiration Date: YYYYMM");
		panel_1.add(lblExpirationDateYyyymm);
		
		tfContractExpiration = new JTextField();
		panel_1.add(tfContractExpiration);
		tfContractExpiration.setColumns(10);
		
		JPanel panel_2 = new JPanel();
		frame.getContentPane().add(panel_2, BorderLayout.SOUTH);
		panel_2.setLayout(new BorderLayout(0, 0));
		
		JLabel lblStatus = new JLabel("Status;");
		panel_2.add(lblStatus, BorderLayout.WEST);
		
		tpStatus = new JTextPane();
		panel_2.add(tpStatus, BorderLayout.CENTER);
		tpStatus.setText("Status Pane");
		
		JButton btnCancelAll = new JButton("Cancel All");
		btnCancelAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				halt();
			}
		});
		frame.getContentPane().add(btnCancelAll, BorderLayout.EAST);
		
		JPanel panel_3 = new JPanel();
		frame.getContentPane().add(panel_3, BorderLayout.CENTER);
		panel_3.setLayout(new GridLayout(3, 0, 0, 0));
		
		JButton btnPlayDefense = new JButton("Play Defense");
		btnPlayDefense.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				playDefense();
			}
		});
		panel_3.add(btnPlayDefense);
		JButton btnSellClose = new JButton("Sell Close");
		btnSellClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(defense != null)
					defense.sellClose();
			}
		});
		panel_3.add(btnSellClose );
		JButton btnBuyClose = new JButton("Buy Close");
		btnBuyClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(defense != null)
					defense.buyClose();
			}
		});
		panel_3.add(btnBuyClose);
	}

}
