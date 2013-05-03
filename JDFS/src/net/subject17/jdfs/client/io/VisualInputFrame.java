package net.subject17.jdfs.client.io;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;

import net.subject17.jdfs.client.UserNode;

import java.awt.event.ActionEvent;
/* usage:
public class ButtonTest {
   public static void main(String[] args) {
      ButtonFrame frame = new ButtonFrame();
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setVisible(true);
   }
}
	*/
public class VisualInputFrame  extends JFrame {
	private static final long serialVersionUID = 539953823671872088L;
	private JTextField userInput;
	
   public VisualInputFrame() {

      setTitle("JDFS -- Control panel");
      setSize(900, 130);
      VisualInputPanel panel = new VisualInputPanel();
      panel.add(new JLabel("Input:"));
      userInput = new JTextField(40);
      panel.add(userInput);

      add(panel, BorderLayout.SOUTH);
   }

   // !! create a public method to get JTextField's text
   // !! without exposing the JTextField itself.
   public String getInputString() {
      return userInput.getText();
   }

	private Component frame;
   class VisualInputPanel extends JPanel implements ActionListener {
	private static final long serialVersionUID = -2681291298064133529L;

      public VisualInputPanel() {

         final JButton inputButton = new JButton("Enter Input");
         add(inputButton, BorderLayout.SOUTH);
         inputButton.setActionCommand("enterInput");
         inputButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               if ("enterInput".equals(e.getActionCommand())) {
            	   UserInput.getInstance().setInput(VisualInputFrame.this.getInputString());
                  //!! call public method on ButtonFrame object
                  //JOptionPane.showMessageDialog(frame, VisualInputFrame.this.getInputString());
               }
            }

         });
         
         final JButton quitButton = new JButton("Exit");
         add(quitButton, BorderLayout.SOUTH);
         quitButton.setActionCommand("exitProgram");
         quitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               // !! ButtonFrame bf = new ButtonFrame();
               if ("exitProgram".equals(e.getActionCommand())) {

                  //!! call public method on ButtonFrame object
                  UserNode.exitProgram();
               }
            }

         });
         
         

      }

      @Override
      public void actionPerformed(ActionEvent ae) {
         throw new UnsupportedOperationException("Not supported yet.");
      }
      public void showModal(String messageToDisplay) {
    		// TODO Auto-generated method stub
    		JOptionPane.showMessageDialog(frame, VisualInputFrame.this.getInputString());
      }
  }

   public void showModal(String messageToDisplay) {
		// TODO Auto-generated method stub
		JOptionPane.showMessageDialog(frame, VisualInputFrame.this.getInputString());
	}
}
