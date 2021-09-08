package main;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import forks.Fork;
import forks.ForkView;
import util.Ico;
import util.jtattoo.hifi.HiFiLookAndFeel;

public class ForkFarmer {
	public static JFrame FRAME;
	public static String[] args;

	public static void main(String[] args) {
		try {
			Properties props = new Properties();
			props.put("logoString", "");
			HiFiLookAndFeel.setCurrentTheme(props);
			UIManager.setLookAndFeel(new HiFiLookAndFeel());
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		Args.args = args;
		javax.swing.SwingUtilities.invokeLater(ForkFarmer::startGUI);
	}
	
	
	private static void startGUI() {
		FRAME = new JFrame("ForkFarmer 1.4");
		FRAME.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		FRAME.setIconImage(Ico.LOGO.getImage());

		//Create and set up the content pane.
		final MainGui mainPanel = new MainGui();
		mainPanel.setOpaque(true); //content panes must be opaque
		FRAME.setContentPane(mainPanel);

		FRAME.pack();
		FRAME.setLocationRelativeTo(null);
		FRAME.setVisible(true);
		
		/*
		TrayIcon trayIcon = new TrayIcon(Ico.LOGO.getImage());
		
		SystemTray st = SystemTray.getSystemTray();
	    
		try {
			st.add(trayIcon);
		} catch (AWTException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		*/
	     
		
		FRAME.addWindowListener(new WindowAdapter() {
		    public void windowClosing(WindowEvent e) {
		        
		    	Settings s = new Settings();
		    	s.forkList = Fork.LIST;
		    	s.fvColList = ForkView.MODEL.colList;
		    	s.Save();
		    }
		});
		
		
	}

	public static void showMsg(String title,String message) {
		JOptionPane.showMessageDialog(FRAME,message,title,JOptionPane.PLAIN_MESSAGE);
	}
	
	public static boolean showPopup(String s, JPanel jp) {
		return (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(FRAME,jp,s,JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE));
	}

}
