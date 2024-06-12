package com.srscicomp.fc.matlab;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import com.srscicomp.common.g2dviewer.Graph2DViewer;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.fig.FGraphicModel;


/**
 * A simple modal dialog that displays a {@link FGraphicModel} and asks the user whether or not the figure should be
 * saved to the FypML file specified. The dialog constructor is private; instead, use the static method {@link 
 * #raiseConfirmSaveDialog(FGraphicModel, File)}.
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
public class DNFigureSaveDlg extends JDialog implements ActionListener
{
   /**
    * Raise an application-modal dialog that displays the specified FypML figure and asks the user if the figure should
    * be saved to the file specified. The full pathname of the destination file is displayed below the rendered figure,
    * next to "Save" and "Cancel" buttons. This method does not actually save the file; that is left to the caller.
    * @param fgm The FypML figure to be displayed.
    * @param dst The proposed destination file. Must end with the ".fyp" extension.
    * @return True if user confirmed that figure should be saved, else false. Returns false if figure is invalid or
    * destination file name does not end in ".fyp", or destination file's parent directory does not exist. The 
    * destination file itself need not exist.
    */
   public static boolean raiseConfirmSaveDialog(FGraphicModel fgm, File dst)
   {
      if(fgm == null || dst == null || dst.getParentFile() == null || !dst.getParentFile().isDirectory() ||
            !"fyp".equals(Utilities.getExtension(dst)))
         return(false);
      
      DNFigureSaveDlg dlg = new DNFigureSaveDlg(fgm, dst);
      dlg.pack();
      dlg.setResizable(true);
      dlg.setSize(800, 600);
      dlg.setLocationRelativeTo(null);
      dlg.setVisible(true);
      
      return(dlg.saveConfirmed);
   }

   /**
	 * Construct a simple dialog that displays the specified figure in a {@link Graph2DViewer} and
	 * asks the user to confirm that the figure be saved to the specified file. The dialog is configured to be displayed
	 * modally, centered on the screen.
	 * @param fgm The FypML figure to be displayed.
	 * @param dst The destination file to which the figure is to be saved.
	 */
	private DNFigureSaveDlg(FGraphicModel fgm, File dst)
	{  
		super(null, "Save figure as FypML file?", ModalityType.APPLICATION_MODAL);         
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		Graph2DViewer viewer = new Graph2DViewer(false, true, true);
		viewer.setResolution(100);
		viewer.setModel(fgm);
		
		JLabel fileLabel = new JLabel(dst.getAbsolutePath());
      JButton saveBtn = new JButton("Save");
      saveBtn.setActionCommand("Save");
      saveBtn.addActionListener(this);
      JButton cancelBtn = new JButton("Cancel");
      cancelBtn.setActionCommand("Cancel");
      cancelBtn.addActionListener(this);

      JPanel p = new JPanel();
      SpringLayout layout = new SpringLayout();
      p.add(fileLabel);
      p.add(saveBtn);
      p.add(cancelBtn);
      layout.putConstraint(SpringLayout.WEST, fileLabel, 5, SpringLayout.WEST, p);
      layout.putConstraint(SpringLayout.EAST, fileLabel, -30, SpringLayout.WEST, saveBtn);
      layout.putConstraint(SpringLayout.EAST, saveBtn, -10, SpringLayout.WEST, cancelBtn);
      layout.putConstraint(SpringLayout.EAST, p, 5, SpringLayout.EAST, cancelBtn);
      layout.putConstraint(SpringLayout.NORTH, cancelBtn, 20, SpringLayout.NORTH, p);
      layout.putConstraint(SpringLayout.SOUTH, p, 5, SpringLayout.SOUTH, cancelBtn);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, saveBtn, 0, SpringLayout.VERTICAL_CENTER, cancelBtn);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, fileLabel, 0, SpringLayout.VERTICAL_CENTER, cancelBtn);

		Container contentPane = getContentPane();
		contentPane.add(viewer);
		contentPane.add(p, BorderLayout.SOUTH);
	}

   @Override public void actionPerformed(ActionEvent e)
   {
      saveConfirmed = "Save".equals(e.getActionCommand());
      setVisible(false);
      dispose();
   }
   
   /** Initially false, this flag is set if user confirms that the figure should be saved to file. */
   private boolean saveConfirmed = false;
}
