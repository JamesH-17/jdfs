package net.subject17.jdfs.client.io;

import javax.swing.JFrame;

public class UserGUI implements Runnable {

	VisualInputFrame visInput;
	@Override
	public void run() {
		visInput = new VisualInputFrame();

		visInput.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		visInput.setVisible(true);
		
	}
	
	public void cleanup() {
		visInput.removeAll();
		visInput.setEnabled(false);
		visInput.dispose();
	}

	public void showModal(String messageToDisplay) {
		visInput.showModal(messageToDisplay);
	}

}
