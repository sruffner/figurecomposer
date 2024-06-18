package com.srscicomp.fc.ui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;

import com.srscicomp.common.g2dviewer.Graph2DViewer;
import com.srscicomp.common.ui.JUnicodeCharacterMap;
import com.srscicomp.fc.fig.FGraphicModel;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.uibase.StyledTextEditor;
import com.srscicomp.fc.uibase.FCIcons;

/**
 * This modal dialog serves as a pop-up, "in-place" editor for updating the <i>title</i> attribute of a <i>FypML</i> 
 * graphic node. An application singleton, it is intended only to be raised when double-clicking on a node's 
 * representation within the figure canvas or node tree of the {@link FigComposer}.
 * 
 * <p>The pop-up dialog reconfigures its widgets to handle 3 distinct uses of the <i>title</i> property among the 
 * various <i>FypML</i> graphic node types:
 * <ul>
 * <li><i>Short single-line, plain-text string with no line breaks</i>. Most node types fall into this category. A 
 * simple text field suffices to edit the title. Pressing the <b>Enter</b> key -- or clicking the "OK" button -- saves 
 * the changes (if any) and extinguishes the dialog.</li>
 * <li><i>Multi-line plain-text title strings</i>. Axis nodes allow line breaks in the title string. In this case,
 * the text field is replaced by a scrollable text area. Pressing the <b>Enter</b> enter key in this configuration 
 * merely adds a new line to the text area; to save the changes and hide the popup, the user must click the "OK" button
 * in the right-hand corner of the dialog. <b>NO LONGER USED AS OF V5.4.0 -- see below.</b></li>
 * <li><i>Single-line attributed text.</i> As of v5.4.0, select graphic nodes support an "attributed text" string in the
 * <i>title</i> property. This allows the user to change certain character attributes -- font style, color, underline,
 * and superscript or subscript -- on a per-character basis in the title string. The attributed text format is
 * "S|[N1:codes1,N2:codes2,...]", where S is the actual character sequence and the rest encodes intra-string attribute 
 * changes. This format requires a custom editor -- {@link StyledTextEditor}. See that class for details. You must 
 * click the "OK" button to confirm the changes and hide the dialog. Applicable to label, shape, trace, and scatter plot
 * nodes.</li>
 * <li><i>Multi-line attributed text.</i> As of V5.4.0, select graphic nodes support a multi-line "attributed text"
 * string: text boxes, 2D and 3D axes, and color bars. In this case, we use an {@link StyledTextEditor} configured 
 * to allow linefeeds in the text.</li>
 * </ul>
 * In addition to the "OK" button in the dialog's top right-hand corner, all 3 configurations include a character map 
 * tool ({@link JUnicodeCharacterMap}) located below the text component so that the user can add special characters to 
 * the edited text.
 * </p>
 * 
 * <p>To extinguish the dialog without making any changes, click the "Close" button in the dialog's title bar, or press
 * the <b>ESC</b>ape key.</p>
 * 
 * @author sruffner
 */
class TitlePopupDlg extends JDialog implements WindowFocusListener, ActionListener, PropertyChangeListener
{
   /**
    * Raise a modal "pop-up" dialog so that user can edit the <i>title</i> attribute of the specified <i>FypML</i>
    * graphic node. The pop-up dialog is an application singleton; if it is already raised when this method is called,
    * no action is taken. If the specified node lacks a title, the dialog is not raised.
    * 
    * <p>As described in the class header, the dialog is configured with the appropriate widget to edit the title
    * attribute of the specified graphic node.</p>
    * 
    * @param n The graphic node.
    * @param src The source component that has requested the pop-up be shown. By design, this should be either the
    * figure canvas or node tree within the {@link FigComposer}. If so, the method will attempt to raise the dialog
    * centered over the node's representation within the canvas or node tree. If the source component is something else,
    * the dialog will appear centered over that component.
    */
   static void raiseEditTitlePopup(FGraphicNode n, JComponent src)
   {
      if(n == null || src == null) return;
      if(!n.hasTitle()) return;
      
      if(singleton == null)
      {
         Container c = src.getTopLevelAncestor();
         Window owner = (c instanceof Window) ? ((Window) c) : null;
         singleton = new TitlePopupDlg(owner);
      }
      if(singleton.isVisible()) return;
      
      Rectangle rBounds = null;
      if(src instanceof Graph2DViewer)
      {
         Graph2DViewer canvas = (Graph2DViewer) src;
         Rectangle2D r = canvas.logicalToDevice(n.getCachedGlobalBounds());
         if(r != null && !r.isEmpty())
         {
            Point pUL = new Point((int) r.getMinX(), (int) r.getMinY());
            SwingUtilities.convertPointToScreen(pUL, canvas);
            Point pBR = new Point((int) r.getMaxX(), (int) r.getMaxY());
            SwingUtilities.convertPointToScreen(pBR, canvas);
            rBounds = new Rectangle(pUL.x, pUL.y, pBR.x-pUL.x, pBR.y-pUL.y);
            
         }
      }
      else if(src instanceof FigNodeTree)
         rBounds = ((FigNodeTree) src).getTextRectangleForNodeRow(n);
      
      if(rBounds == null)
      {
         if(!src.isVisible()) return;  // component must be visible to get its location on screen!
         Point pUL = src.getLocationOnScreen();
         Dimension sz = src.getSize();
         pUL.x += sz.width / 2 - 10;
         pUL.y += sz.height / 2 - 10;
         rBounds = new Rectangle(pUL.x, pUL.y, 20, 20);
      }
      
      singleton.raise(n, rBounds, !(src instanceof FigNodeTree));
   }
   
   /** The singleton dialog instance, lazily created. */
   private static TitlePopupDlg singleton = null;
   
   /** Font size (in points) at which text is displayed in the attributed text editor widget. */
   private final static int DSPFONTSZ = 16;
   
   /** 
    * Private constructor. Use {@link #raiseEditTitlePopup(FGraphicNode, JComponent)}
    * @param owner The dialog owner. 
    */
   private TitlePopupDlg(Window owner)
   {
      super(owner, "", ModalityType.APPLICATION_MODAL);
      setResizable(false);
      setAlwaysOnTop(true);
      
      titleField = new JTextField();
      titleField.addActionListener(this);
      
      titleArea = new JTextArea();
      titleArea.setLineWrap(true);
      titleArea.setWrapStyleWord(true);
      areaScroller = new JScrollPane(titleArea);
      areaScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
      
      singleATEditor = new StyledTextEditor(1);
      singleATEditor.setFontSize(DSPFONTSZ, false);
      
      multiATEditor = new StyledTextEditor(3, false);
      multiATEditor.setFontSize(DSPFONTSZ, false);
      
      okBtn = new JButton(FCIcons.V4_ENTER_22);
      okBtn.setToolTipText("Save changes and hide pop-up");
      okBtn.setBorder(null);
      okBtn.setBorderPainted(false);
      okBtn.setFocusable(false);
      okBtn.addActionListener(this);
      
      mapper = new JUnicodeCharacterMap(
            new Font("Arial", Font.PLAIN, DSPFONTSZ), 
            FGraphicModel.getSupportedUnicodeSubsets(), 3, 18);
      mapper.setFocusable(false);
      mapper.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      mapper.addPropertyChangeListener(JUnicodeCharacterMap.SELCHAR_PROPERTY, this);
      
      contentPane = new JPanel(new SpringLayout());
      contentPane.setBorder(BorderFactory.createRaisedBevelBorder());
      SpringLayout layout = (SpringLayout) contentPane.getLayout();
      
      contentPane.add(titleField);
      contentPane.add(okBtn);
      contentPane.add(mapper);
      
      layout.putConstraint(SpringLayout.WEST, titleField, 3, SpringLayout.WEST, contentPane);
      layout.putConstraint(SpringLayout.EAST, titleField, FIELDW+3, SpringLayout.WEST, contentPane);
      layout.putConstraint(SpringLayout.WEST, okBtn, 5, SpringLayout.EAST, titleField);
      layout.putConstraint(SpringLayout.EAST, contentPane, 3, SpringLayout.EAST, okBtn);
      layout.putConstraint(SpringLayout.WEST, mapper, 3, SpringLayout.WEST, contentPane);
      
      layout.putConstraint(SpringLayout.NORTH, titleField, 3, SpringLayout.NORTH, contentPane);
      layout.putConstraint(SpringLayout.NORTH, mapper, 5, SpringLayout.SOUTH, titleField);
      layout.putConstraint(SpringLayout.SOUTH, contentPane, 3, SpringLayout.SOUTH, mapper);
      layout.putConstraint(SpringLayout.NORTH, okBtn, 3, SpringLayout.NORTH, contentPane);
      

      add(contentPane);
      
      // dialog is initially configured to edit a plain-text string that cannot contain line breaks!
      currentCfg = Cfg.SINGLE;
      
      // set up key binding to cancel and extinguish dialog if the ESCape key is pressed
      contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
      contentPane.getActionMap().put("cancel", new AbstractAction() {
         @Override public void actionPerformed(ActionEvent e) { extinguish(true); }
      });
   }
   
   /**
    * Raise the text pop-up dialog to edit the title attribute of the <i>FypML</i> graphic node specified. If the dialog
    * is already visible, no action is taken. The dialog is centered vertically over the specified bounding rectangle;
    * horizontally, it is either centered or left-aligned with the rectangle. Regardless, the dialog's location will be
    * adjusted as necessary to ensure it is fully on screen.
    * 
    * @param n The graphic node to be edited.
    * @param r The node's bounding rectangle on the component that invoked the pop-up, in screen coordinates.
    * @param hCtr True to horizontally center pop-up dialog WRT bounding rectangle, false to align left edges.
    */
   private void raise(FGraphicNode n, Rectangle r, boolean hCtr)
   {
      if(isVisible() || n == null || r == null) return;
      editedNode = n;
      
      // if necessary, update configuration of pop-up to use the appropriate widget(s) depending on the type of
      // node being edited. Load the widget appropriately.
      Cfg oldCfg = currentCfg;
      if(n.allowStyledTextInTitle())
         currentCfg = n.allowLineBreaksInTitle() ? Cfg.ATTRIB_MULTI : Cfg.ATTRIB;
      else
         currentCfg = n.allowLineBreaksInTitle() ? Cfg.MULTI : Cfg.SINGLE;

      SpringLayout layout = (SpringLayout) contentPane.getLayout();
      if(currentCfg != oldCfg)
      {
         contentPane.removeAll();
         if(currentCfg == Cfg.ATTRIB)
         {
            contentPane.add(singleATEditor);
            contentPane.add(okBtn);
            contentPane.add(mapper);
            
            layout.putConstraint(SpringLayout.WEST, singleATEditor, 3, SpringLayout.WEST, contentPane);
            layout.putConstraint(SpringLayout.EAST, singleATEditor, FIELDW+3, SpringLayout.WEST, contentPane);
            layout.putConstraint(SpringLayout.WEST, okBtn, 5, SpringLayout.EAST, singleATEditor);
            layout.putConstraint(SpringLayout.EAST, contentPane, 3, SpringLayout.EAST, okBtn);
            layout.putConstraint(SpringLayout.WEST, mapper, 3, SpringLayout.WEST, contentPane);
            
            layout.putConstraint(SpringLayout.NORTH, singleATEditor, 3, SpringLayout.NORTH, contentPane);
            layout.putConstraint(SpringLayout.NORTH, mapper, 5, SpringLayout.SOUTH, singleATEditor);
            layout.putConstraint(SpringLayout.SOUTH, contentPane, 3, SpringLayout.SOUTH, mapper);
            layout.putConstraint(SpringLayout.NORTH, okBtn, 3, SpringLayout.NORTH, contentPane);
         }
         else if(currentCfg == Cfg.ATTRIB_MULTI)
         {
            contentPane.add(multiATEditor);
            contentPane.add(okBtn);
            contentPane.add(mapper);
            
            layout.putConstraint(SpringLayout.WEST, multiATEditor, 3, SpringLayout.WEST, contentPane);
            layout.putConstraint(SpringLayout.EAST, multiATEditor, FIELDW+3, SpringLayout.WEST, contentPane);
            layout.putConstraint(SpringLayout.WEST, okBtn, 5, SpringLayout.EAST, multiATEditor);
            layout.putConstraint(SpringLayout.EAST, contentPane, 3, SpringLayout.EAST, okBtn);
            layout.putConstraint(SpringLayout.WEST, mapper, 3, SpringLayout.WEST, contentPane);
            
            layout.putConstraint(SpringLayout.NORTH, multiATEditor, 3, SpringLayout.NORTH, contentPane);
            layout.putConstraint(SpringLayout.NORTH, mapper, 5, SpringLayout.SOUTH, multiATEditor);
            layout.putConstraint(SpringLayout.SOUTH, contentPane, 3, SpringLayout.SOUTH, mapper);
            layout.putConstraint(SpringLayout.NORTH, okBtn, 3, SpringLayout.NORTH, contentPane);
            
         }
         else if(currentCfg == Cfg.SINGLE)
         {
            contentPane.add(titleField);
            contentPane.add(okBtn);
            contentPane.add(mapper);
            
            layout.putConstraint(SpringLayout.WEST, titleField, 3, SpringLayout.WEST, contentPane);
            layout.putConstraint(SpringLayout.EAST, titleField, FIELDW+3, SpringLayout.WEST, contentPane);
            layout.putConstraint(SpringLayout.WEST, okBtn, 5, SpringLayout.EAST, titleField);
            layout.putConstraint(SpringLayout.EAST, contentPane, 3, SpringLayout.EAST, okBtn);
            layout.putConstraint(SpringLayout.WEST, mapper, 3, SpringLayout.WEST, contentPane);
            
            layout.putConstraint(SpringLayout.NORTH, titleField, 3, SpringLayout.NORTH, contentPane);
            layout.putConstraint(SpringLayout.NORTH, mapper, 5, SpringLayout.SOUTH, titleField);
            layout.putConstraint(SpringLayout.SOUTH, contentPane, 3, SpringLayout.SOUTH, mapper);
            layout.putConstraint(SpringLayout.NORTH, okBtn, 3, SpringLayout.NORTH, contentPane);
         }
         else
         {
            contentPane.add(areaScroller);
            contentPane.add(okBtn);
            contentPane.add(mapper);
            
            layout.putConstraint(SpringLayout.WEST, areaScroller, 3, SpringLayout.WEST, contentPane);
            layout.putConstraint(SpringLayout.EAST, areaScroller, FIELDW+3, SpringLayout.WEST, contentPane);
            layout.putConstraint(SpringLayout.WEST, okBtn, 5, SpringLayout.EAST, areaScroller);
            layout.putConstraint(SpringLayout.EAST, contentPane, 3, SpringLayout.EAST, okBtn);
            
            layout.putConstraint(SpringLayout.NORTH, areaScroller, 3, SpringLayout.NORTH, contentPane);
            layout.putConstraint(SpringLayout.SOUTH, areaScroller, 63, SpringLayout.NORTH, contentPane);
            layout.putConstraint(SpringLayout.NORTH, mapper, 5, SpringLayout.SOUTH, areaScroller);
            layout.putConstraint(SpringLayout.SOUTH, contentPane, 3, SpringLayout.SOUTH, mapper);
            layout.putConstraint(SpringLayout.NORTH, okBtn, 3, SpringLayout.NORTH, contentPane);
         }
      }

      if(currentCfg == Cfg.ATTRIB)
      {
         singleATEditor.loadContent(editedNode.getAttributedTitle(false), editedNode.getFontFamily(), DSPFONTSZ, 
               editedNode.getFillColor(), editedNode.getFontStyle());
      }
      else if(currentCfg == Cfg.ATTRIB_MULTI)
      {
         multiATEditor.loadContent(editedNode.getAttributedTitle(false), editedNode.getFontFamily(), DSPFONTSZ, 
               editedNode.getFillColor(), editedNode.getFontStyle());
      }
      else if(currentCfg == Cfg.SINGLE)
      { 
         titleField.setText(editedNode.getTitle());
      }
      else
      {
         titleArea.setText(editedNode.getTitle());
      }
      
      mapper.setMappedFont(editedNode.getFont());
      
      pack();
            
      // fix location of dialog so that it is centered horizontally WRT bounding rect or with left edges aligned.
      // always centered vertically WRT bounding rect.
      Dimension dlgSz = getPreferredSize();
      Dimension screenSz = Toolkit.getDefaultToolkit().getScreenSize();
      int y = ((int) r.getCenterY()) - dlgSz.height / 2;
      if(y < 5) y = 5;
      else if(y + dlgSz.height + 5 >= screenSz.height) y = screenSz.height - dlgSz.height - 5;

      int x = hCtr ? (((int) r.getCenterX()) - dlgSz.width / 2) : r.x;
      if(x < 5) x = 5;
      else if(x + dlgSz.width + 5 >= screenSz.width) x = screenSz.width - dlgSz.width - 5;
      setLocation(x, y);
      
      setTitle("Edit title of: " + editedNode.getNodeType().getNiceName());
      addWindowFocusListener(this);
      setVisible(true);
      requestFocusInWindow();
   }
   
   /** When title pop-up dialog gains the focus, put focus on the text component and select all text therein. */
   @Override public void windowGainedFocus(WindowEvent e) 
   {
      if(currentCfg == Cfg.ATTRIB)
         singleATEditor.requestFocus();
      else if(currentCfg == Cfg.ATTRIB_MULTI)
         multiATEditor.requestFocus();
      else if(currentCfg == Cfg.SINGLE)
      {
         titleField.requestFocusInWindow();
         titleField.selectAll();
      }
      else
      {
         titleArea.requestFocusInWindow();
         titleArea.selectAll();
      }
   }

   /**
    * When title edit pop-up dialog loses the focus, extinguish the pop-up without changing the graphic node. Since the
    * dialog is application-modal, this should only happen when the FC application loses the focus.
    */
   @Override public void windowLostFocus(WindowEvent e) 
   {
      // when this popup dialog loses focus to the popup dialog associated with the color picker embedded in an 
      // attributed text editor, we don't want to extinguish this dialog!
      if((currentCfg == Cfg.ATTRIB && singleATEditor.isColorPickerPopupRaised()) ||
            (currentCfg == Cfg.ATTRIB_MULTI && multiATEditor.isColorPickerPopupRaised()))
         return;
      extinguish(true); 
   }

   /** 
    * When "ok" button is clicked, extinguish the title pop-up dialog and update the graphic node accordingly. Do the
    * same if the user presses the "Enter" key in the {@link Cfg#SINGLE} configuration.
    */
   @Override public void actionPerformed(ActionEvent e) 
   { 
      Object src = e.getSource();
      if(src == titleField || src == okBtn) extinguish(false); 
   }
   
   /**
    * When a special character is selected in the embedded character map, that character is inserted into the current
    * text component -- either the plain text field, the text area, or the custom attributed text editor.
    */
   @Override public void propertyChange(PropertyChangeEvent e)
   {
      if(e.getSource() == mapper)
      {
         char c = mapper.getSelectedCharacter();
         if(c != 0)
         {
            String s = "" + c;
            if(currentCfg == Cfg.ATTRIB)
               singleATEditor.insertString(s, false);
            else if(currentCfg == Cfg.ATTRIB_MULTI)
               multiATEditor.insertString(s, false);
            else if(currentCfg == Cfg.SINGLE)
               titleField.replaceSelection(s);
            else
               titleArea.replaceSelection(s);
         }
      }
   }

   /**
    * Extinguish the title edit pop-up dialog, optionally updating the title of the graphic node. 
    * @param cancel True if in-place edit operation was cancelled, in which case the graphic node is left unchanged.
    */
   private void extinguish(boolean cancel)
   {
      if(!isVisible()) return;
      removeWindowFocusListener(this);
      setVisible(false);
      if(!cancel)
      {
         String text;
         if(currentCfg == Cfg.ATTRIB)
            text = FGraphicNode.toStyledText(singleATEditor.getCurrentContents(), editedNode.getFontStyle(), 
                  editedNode.getFillColor());
         else if(currentCfg == Cfg.ATTRIB_MULTI)
            text = FGraphicNode.toStyledText(multiATEditor.getCurrentContents(), editedNode.getFontStyle(), 
                  editedNode.getFillColor());
         else if(currentCfg == Cfg.SINGLE)
            text = titleField.getText();
         else
            text = titleArea.getText();
         editedNode.setTitle(text);
      }
   }
   
   /** Text field in which a single-line plain-text title string is displayed and edited. */
   private final JTextField titleField;
   /** Text area in which a possible multiple-line plain-text title string is displayed an edited. */
   private JTextArea titleArea = null;
   /** Scroll pane housing the text area widget. */
   private JScrollPane areaScroller = null;
   /** Custom widget for editing the single-line attributed text title of a LabelNode. */
   private StyledTextEditor singleATEditor = null;
   /** Custom widget for editing the multi-line attributed text title of a TextBoxNode. */
   private StyledTextEditor multiATEditor = null;

   /** 
    * Pressing this button saves any changes and hides the pop-up. To hide the popup without making any changes,
    * hit the dialog close button or press the ESC key.
    */
   private JButton okBtn = null;
   
   /** The character map tool for inserting special characters. */
   private JUnicodeCharacterMap mapper = null;
   
   /** Simple panel housing the widgets -- so we can adjust its size and configuration when we raise pop-up. */
   private JPanel contentPane = null;
   
   /** Indicates how the dialog is currently configured. */
   private Cfg currentCfg;
   
   /** The graphic node being edited. */
   private FGraphicNode editedNode = null;
   
   /** Fixed width of text editing component in the title edit pop-up dialog. */
   private final int FIELDW = 500;
   
   private enum Cfg
   {
      /** Single-line plain text string. */ SINGLE,
      /** Multi-line plain text string. */ MULTI,
      /** Single-line attributed text string (LabelNode only). */ ATTRIB,
      /** Multi-line attributed text string (TextBoxNode only). */ ATTRIB_MULTI
   }

}
