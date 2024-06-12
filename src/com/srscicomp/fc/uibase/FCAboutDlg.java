package com.srscicomp.fc.uibase;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.srscicomp.common.util.Utilities;

/**
 * This is a simple generic "About" dialog for the <i>Figure Composer</i> application. The dialog displays an 
 * application icon and title, version number, a description of the host OS, and some other information. It includes an 
 * "OK" button to extinguish the dialog.
 * 
 * <p>To use it, simply construct an instance and call setVisible(true). When it is closed (either via the OK button
 * or the window "X"), it is hidden and disposed.</p>
 * 
 * @author sruffner
 */
public class FCAboutDlg extends JDialog
{
   /**
	 * Construct the generic "About" dialog.
	 * @param owner Owner frame.
	 */
	public FCAboutDlg(JFrame owner) //, Icon appIcon)
	{  
		super(owner, "About " + FCWorkspace.getApplicationTitle(), true);         
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		String copyright = FCWorkspace.getCopyrightLine();
		String versionInfo = "Version: " + FCWorkspace.getApplicationVersion() + ", " + 
         FCWorkspace.getApplicationVersionReleaseDate();
		String inquiries = "Email inquiries to: " + FCWorkspace.getEmailAddress();

      String osDesc = "Operating System: " + Utilities.getOSDescription();
      
      String jreDesc =  "Java Runtime Environment: " + System.getProperty("java.version", "?");
      
		// a label with application icon and title, using html to get larger font
		JLabel iconLbl = new JLabel(FCWorkspace.getApplicationTitle(), FCIcons.FC_APP32, JLabel.LEFT);
		iconLbl.setFont(new Font("Serif", Font.ITALIC, 16));

		// an HTML label with information about the app
		JLabel infoLbl = new JLabel( "<html><hr>" + copyright + "<br>" + versionInfo + "<br>" + 
		      osDesc + "<br>" + jreDesc + "<br><br>" + inquiries + "</html>");

		// an OK button to close the dialog
		JButton okBtn = new JButton("OK");
		okBtn.addActionListener(event -> { setVisible(false); dispose(); });

		// layout L->R with border layout
		Container contentPane = getContentPane();
		JPanel colGroup = new JPanel();
		colGroup.setLayout(new BoxLayout(colGroup, BoxLayout.PAGE_AXIS));
		colGroup.add(iconLbl);
		colGroup.add(infoLbl);
		JPanel lineGroup = new JPanel();
		lineGroup.setLayout(new BoxLayout(lineGroup, BoxLayout.LINE_AXIS));
		lineGroup.add(colGroup);
		lineGroup.add(Box.createRigidArea(new Dimension(10, 0)));
		lineGroup.add(okBtn);
		lineGroup.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		contentPane.add(lineGroup, BorderLayout.CENTER);
		pack();
	}
	
   private static final long serialVersionUID = 1L;
}
