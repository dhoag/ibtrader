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
	}

	public void connect(){
		ResponseHandler rh = new ResponseHandler();
		
		EClientSocket m_client = new EClientSocket(rh);
		
		clientInterface = new IBClientRequestExecutor(m_client, rh);

		clientInterface.connect();
		clientInterface.initializePortfolio( );
		tpStatus.setText("Connected!");
	}
	public void requestQuotes(){

		QuoteRouter strat = clientInterface.getQuoteRouter("ES", tfContractExpiration.getText());
		OutputQuotesStrategy dis = new OutputQuotesStrategy();
		strat.addStrategy(dis);

		clientInterface.requestQuotes();
		tpStatus.setText("Requested Quotes");
	}
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		tpStatus = new JTextPane();
		frame.getContentPane().add(tpStatus, BorderLayout.SOUTH);
		
		JPanel panel = new JPanel();
		frame.getContentPane().add(panel, BorderLayout.WEST);
		
		JButton btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				connect();
			}
		});
		
		JButton btnOff = new JButton("Off");
		
		JButton btnOn = new JButton("On");
		btnOn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				requestQuotes();
			}
		});
		panel.setLayout(new GridLayout(3, 1, 0, 0));
		panel.add(btnConnect);
		panel.add(btnOff);
		panel.add(btnOn);
		
		JPanel panel_1 = new JPanel();
		frame.getContentPane().add(panel_1, BorderLayout.NORTH);
		
		JLabel lblExpirationDateYyyymm = new JLabel("Expiration Date: YYYYMM");
		panel_1.add(lblExpirationDateYyyymm);
		
		tfContractExpiration = new JTextField();
		panel_1.add(tfContractExpiration);
		tfContractExpiration.setColumns(10);
	}

}
