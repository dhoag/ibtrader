package com.davehoag.ib.greybox;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JButton;

import java.awt.BorderLayout;

import javax.swing.JTextPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import com.davehoag.ib.IBClientRequestExecutor;
import com.davehoag.ib.QuoteRouter;
import com.davehoag.ib.ResponseHandler;
import com.davehoag.ib.dataTypes.DoubleCache;
import com.davehoag.ib.strategies.DefenseStrategy;
import com.ib.client.EClientSocket;

import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Stack;

import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JCheckBox;
import java.awt.FlowLayout;

public class Urlacher {

	private JFrame frame;
	IBClientRequestExecutor clientInterface;
	private JTextField tfContractExpiration;
	private JTextField tfStopDistance;
	private JTextPane tpStatus;
	private DefenseStrategy defense;
	private JToggleButton btnPlayDefense;
	private JTextArea textArea;
	Stack<String> status = new Stack<String>();
	private JCheckBox chckbxMkt;
	private final JButton btnNewButton = new JButton("Stats");
	private final RegressionLine lineWindow = new RegressionLine();
	private JButton btnOff;
	private JButton btnOn;
	private JButton btnConnect;
	DoubleCache dc;
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
		//connect();
		tfContractExpiration.setText("201412");
		
		textArea = new JTextArea();
		frame.getContentPane().add(textArea, BorderLayout.CENTER);
		
		JPanel panel_5 = new JPanel();
		frame.getContentPane().add(panel_5, BorderLayout.EAST);
		panel_5.setLayout(new GridLayout(3, 1, 0, 0));
		
		JButton btnCancelAll = new JButton("Cancel All");
		panel_5.add(btnCancelAll);
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				displayStats();
			}
		});
		panel_5.add(btnNewButton);
		btnCancelAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				halt();
			}
		});
	}

	protected void displayStats() {
		if(defense == null) return;
		defense.getStats();
	}

	public void connect(){
		ResponseHandler rh = new ResponseHandler();
		
		EClientSocket m_client = new EClientSocket(rh);
		clientInterface = new IBClientRequestExecutor(m_client, rh);
		clientInterface.connect();
		clientInterface.initializePortfolio( );
		
		tpStatus.setText("Connected!");
		btnOn.setEnabled(true);
		btnConnect.setEnabled(false);
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
		dc = defense.getPriceHistory();
		lineWindow.setPriceHistory(dc);
		router.addStrategy(defense);
		//Need to request quotes after the router is created and strategies set
		clientInterface.requestQuotes();
		tpStatus.setText("Requested Quotes");
		btnOff.setEnabled(true);
		chckbxMkt.setEnabled(true);
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
		defense.playDefense( btnPlayDefense.isSelected() );
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
		frame.setBounds(100, 100, 484, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel panel = new JPanel();
		frame.getContentPane().add(panel, BorderLayout.WEST);
		panel.setLayout(new GridLayout(3, 2, 0, 0));
		
		JButton btnGoLong = new JButton("Go Long");
		btnGoLong.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				goLong();
			}
		});
		panel.add(btnGoLong);
		JButton btnSellClose = new JButton("Sell Close");
		panel.add(btnSellClose);
		btnSellClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(defense != null)
					defense.sellClose();
			}
		});
		
		JButton btnGoShort = new JButton("Go Short");
		btnGoShort.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				goShort();
			}
		});
		panel.add(btnGoShort);
		JButton btnBuyClose = new JButton("Buy Close");
		panel.add(btnBuyClose);
		
		btnPlayDefense = new JToggleButton("Play Defense");
		panel.add(btnPlayDefense);
		btnPlayDefense.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				playDefense();
			}
		});
		btnBuyClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(defense != null)
					defense.buyClose();
			}
		});
		
		JPanel panel_1 = new JPanel();
		frame.getContentPane().add(panel_1, BorderLayout.NORTH);
		panel_1.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_3 = new JPanel();
		panel_1.add(panel_3, BorderLayout.CENTER);
		
		JLabel lblExpirationDateYyyymm = new JLabel("Expiration Date: YYYYMM");
		panel_3.add(lblExpirationDateYyyymm);
		
		tfContractExpiration = new JTextField();
		panel_3.add(tfContractExpiration);
		tfContractExpiration.setColumns(10);
		
		btnOff = new JButton("Off");
		btnOff.setEnabled(false);
		panel_3.add(btnOff);
		
		btnOn = new JButton("On");
		btnOn.setEnabled(false);
		panel_3.add(btnOn);
		
		JPanel panel_4 = new JPanel();
		panel_1.add(panel_4, BorderLayout.NORTH);
		
		panel_4.add(lineWindow);
		
		chckbxMkt = new JCheckBox("mkt");
		chckbxMkt.setEnabled(false);
		chckbxMkt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(defense != null){
					defense.setMkt(chckbxMkt.isSelected());
				}
			}
		});
		
		btnConnect = new JButton("Connect");
		panel_4.add(btnConnect);
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				connect();
			}
		});
		panel_4.add(chckbxMkt);
		btnOn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				requestQuotes();
			}
		});
		btnOff.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancelQuotes();
			}
		});
		
		JPanel panel_2 = new JPanel();
		frame.getContentPane().add(panel_2, BorderLayout.SOUTH);
		panel_2.setLayout(new BorderLayout(0, 0));
		
		JLabel lblStatus = new JLabel("Status;");
		panel_2.add(lblStatus, BorderLayout.WEST);
		
		tpStatus = new JTextPane();
		panel_2.add(tpStatus, BorderLayout.CENTER);
		tpStatus.setText("Status Pane");
	}

}
