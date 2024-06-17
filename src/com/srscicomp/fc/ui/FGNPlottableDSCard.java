package com.srscicomp.fc.ui;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.text.AbstractDocument;

import com.srscicomp.fc.data.DataSet;
import com.srscicomp.fc.data.DataSetIDFilter;
import com.srscicomp.fc.fig.FGNPlottableData;
import com.srscicomp.fc.uibase.FCChooser;
import com.srscicomp.fc.uibase.FCIcons;


/**
 * <b>FGNPlottableDSCard</b> provides a means of changing the data set assigned to a data presentation node, an
 * instance of {@link FGNPlottableData}. <i>It provides a reusable editor panel that can be embedded in the node 
 * properties editor for any type of data presentation node.</i>
 * 
 * <p>The editor panel consists only of three pushbuttons, and a text field to edit the dataset's current identifier. 
 * The pushbuttons invoke modal dialogs by which the user can:
 * <ul>
 *    <li>Raise file chooser and load a data set from the selected data set source file.</li>
 *    <li>Raise file chooser and export the data set to the data set source file specified.</li>
 *    <li>View/edit the data set ID, meta-data attributes, and the raw data itself, via the modeless dialog {@link 
 *    DSEditorToolDlg}. The "Edit" button merely raises this dialog, passing the data presentation node that contains
 *    the data set to be displayed/edited.</li>
 * </ul>
 * </p>
 * <p>All of the widgets are arranged in a single row and are vertically centered WRT each other. The panel has a 
 * titled border, with the label "Data Set".</p>
 * 
 * @author sruffner
 */
final class FGNPlottableDSCard extends JPanel implements ActionListener, FocusListener
{
   /** Construct an editor panel used to edit the data set rendered by a data presentation node.. */
   public FGNPlottableDSCard()
   {
      super();
      setOpaque(false);
      
      idField = new JTextField("setID", 10);
      ((AbstractDocument)idField.getDocument()).setDocumentFilter(new DataSetIDFilter());
      idField.setToolTipText("Enter dataset ID (1-" + DataSet.MAXIDLEN + " alphanumeric or certain puncuation chars)");
      idField.addActionListener(this);
      idField.addFocusListener(this);
      add(idField);
      
      viewBtn = new JButton("Edit");
      viewBtn.setToolTipText("Click this button to view and/or manually edit this dataset");
      viewBtn.setOpaque(false);  // Needed this b/c default bkg showed around edges in Windows.
      viewBtn.addActionListener(this);
      add(viewBtn);
      
      loadBtn = new JButton(FCIcons.V4_LOADDATASET_16);
      loadBtn.setToolTipText("Click this button to load a different dataset from workspace or file system");
      loadBtn.setMargin(new Insets(0,0,0,0));
      loadBtn.setOpaque(false);  // Needed this b/c default bkg showed around edges in Windows.
      loadBtn.addActionListener(this);
      add(loadBtn);
      
      exportBtn = new JButton(FCIcons.V4_EXPDATASET_16);
      exportBtn.setToolTipText("Click this button to export this dataset to file");
      exportBtn.setMargin(new Insets(0,0,0,0));
      exportBtn.setOpaque(false);  // Needed this b/c default bkg showed around edges in Windows.
      exportBtn.addActionListener(this);
      add(exportBtn);
      
      SpringLayout layout = new SpringLayout();
      setLayout(layout);
      
      layout.putConstraint(SpringLayout.WEST, idField, GAP*2, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, viewBtn, GAP*2, SpringLayout.EAST, idField);
      layout.putConstraint(SpringLayout.WEST, loadBtn, GAP*6, SpringLayout.EAST, viewBtn);
      layout.putConstraint(SpringLayout.WEST, exportBtn, GAP*2, SpringLayout.EAST, loadBtn);
      layout.putConstraint(SpringLayout.EAST, this, GAP*2, SpringLayout.EAST, exportBtn);
      
      layout.putConstraint(SpringLayout.NORTH, idField, GAP, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.SOUTH, this, GAP, SpringLayout.SOUTH, idField);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, viewBtn, 0, SpringLayout.VERTICAL_CENTER, idField);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, loadBtn, 0, SpringLayout.VERTICAL_CENTER, idField);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, exportBtn, 0, SpringLayout.VERTICAL_CENTER, idField);
      
      setBorder(BorderFactory.createTitledBorder("Data Set"));
   }


   /**
    * Reload this edit panel to view/edit the data set referenced by the specified data presentation node.
    * @param n The data presentation node that uses the data set exposed in this editor. If this argument is null or
    * is not contained in a valid FypML figure model, the editor is disabled.
    */
   public void reload(FGNPlottableData n)
   {
      plottableNode = (n == null || n.getGraphicModel() == null) ? null : n;
      ignoreActions = true;
      boolean ena = (plottableNode != null);
      if(!ena)
         idField.setText("unknown");
      else
         idField.setText(plottableNode.getDataSet().getInfo().getID());

      idField.setEnabled(ena);
      viewBtn.setEnabled(ena);
      loadBtn.setEnabled(ena); 
      exportBtn.setEnabled(ena);
      ignoreActions = false;
   }

   /** Flag set when we need to temporarily ignore any actions from the controls on this editor. */
   private transient boolean ignoreActions = false;

   public void actionPerformed(ActionEvent e)
   {
      if(ignoreActions || plottableNode == null) return;
      
      Object src = e.getSource();
      if(src == idField)
      {
         if(!plottableNode.setDataSetID(idField.getText()))
            idField.setText(plottableNode.getDataSetID());
      }
      else if(src == viewBtn)
      {
         DSEditorToolDlg.editDataFor(plottableNode, this);
      }
      else if(src == loadBtn || src == exportBtn)
      {
         if(src == loadBtn)
         {
            DataSet ds = FCChooser.getInstance().selectDataSet(this, plottableNode.getSupportedDataFormats());
            if(ds != null) plottableNode.setDataSet(ds);
         }
         else
            FCChooser.getInstance().saveDataSet(this, plottableNode.getDataSet());
      }
   }
   
   @Override public void focusGained(FocusEvent e) 
   {
      if(e.getSource() == idField && !e.isTemporary()) idField.selectAll();
   }
   @Override public void focusLost(FocusEvent e)
   {
      if(e.getSource() == idField && !e.isTemporary()) idField.postActionEvent();
   }


   /** The data presentation node referencing the data set exposed in this editor panel. */
   private FGNPlottableData plottableNode = null;

   /** Text field that displays/edits the data set's identifier. */
   private final JTextField idField;
   
   /** <i>View</i> button invokes a modal dialog by which user can view or edit the raw data. */
   private JButton viewBtn = null;

   /** 
    * <i>Load</i> button invokes a modal dialog by which the user can load the data set from a data set source file in
    * the user's <i>DataNav</i> workspace, or another source file not found in the workspace.
    */
   private JButton loadBtn = null;
   
   /** <i>Export</i> button invokes a chooser by which user can save the data set to a file. */
   private JButton exportBtn = null;
   
   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;
}
