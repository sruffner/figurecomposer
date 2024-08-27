package com.srscicomp.fc.uibase;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

import com.srscicomp.common.g2dutil.Marker;
import com.srscicomp.common.g2dutil.StrokeCap;
import com.srscicomp.common.g2dutil.StrokeJoin;
import com.srscicomp.common.ui.*;
import com.srscicomp.fc.fig.AxisNode;
import com.srscicomp.fc.fig.CalibrationBarNode;
import com.srscicomp.fc.fig.ColorLUT;
import com.srscicomp.fc.fig.ColorMap;
import com.srscicomp.fc.fig.ErrorBarNode;
import com.srscicomp.fc.fig.FGNPreferences;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.fig.LegendNode;
import com.srscicomp.fc.fig.LogTickPattern;
import com.srscicomp.fc.fig.PSFont;
import com.srscicomp.fc.fig.TickSetNode;

/**
 * <b>PreferencesDlg</b> is the modal dialog by which the user modifies selected <i>Figure Composer</i> application
 * preferences maintained in the user's workspace. Current settings exposed in this dialog include screen resolution 
 * ("dots per inch"), the preferred default values for various graphic node properties, and the user's set of custom
 * (vs built-in) color maps.
 * 
 * <p>Each application preference value is updated on the fly as the user interacts with the various controls on the 
 * dialog; there is no "Cancel" or "OK" button. Use the standard "X" decoration in the title bar to extinguish the 
 * dialog. To use the dialog, simply invoke {@link #editPreferences(JFrame)}. A singleton <code>PreferencesDlg</code> is
 * created the first time this method is called and is reused thereafter -- invisible when not in use.</p>
 * 
 * <p>Only the screen resolution setting can have an immediate effect on the current state of the application. If the 
 * user makes a change in this setting, the workspace singleton is updated after the preference dialog is hidden.</p>
 * 
 * @author  sruffner
 */
public class PreferencesDlg extends JDialog implements ActionListener, PropertyChangeListener, ItemListener, TabStripModel
{
   /** The one and only <i>Preferences</i> dialog in the application. Lazily created. */
   private static PreferencesDlg prefDlg = null;

   /**
    * Raise the modal <code>PreferencesDlg</code> to allow user to review/edit application preferences.
    * 
    * @param appFrame The main application frame window. The dialog is centered WRT this frame.
    */
   public static void editPreferences(JFrame appFrame)
   {
      if(appFrame == null) return;
      if(prefDlg == null) prefDlg = new PreferencesDlg(appFrame);
      else if(prefDlg.isVisible()) return;
      
      prefDlg.reload();
      prefDlg.pack();
      prefDlg.setLocationRelativeTo(prefDlg.getOwner());
      prefDlg.setVisible(true);
      
      // update screen DPI immediately after dialog extinguished (this triggers a property change in the workspace)
      FCWorkspace.getInstance().setScreenDPI( prefDlg.screenDPIField.getValue().floatValue() );
   }

   /** Dialog title. */
   private final static String TITLE = "Preferences";

   /**
    * Private constructor. To invoke the <code>PreferencesDlg</code>, use the <code>editPreferences()</code> method.
    * @param appFrame The main application frame window.
    * @see PreferencesDlg#editPreferences(JFrame)
    */
   private PreferencesDlg(JFrame appFrame)
   {
      super(appFrame, TITLE, true);
      setResizable(false);
      setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

      workspace = FCWorkspace.getInstance();
      fgnPrefs = FGNPreferences.getInstance();
      
      createComponents();
      layoutComponents();
   }

   /** The user's workspace, which persists all application settings. */
   private final FCWorkspace workspace;
   
   /** Runtinme store for style preferences relevant to a <i>DataNav</i> figure. */
   private final FGNPreferences fgnPrefs;
   
   //
   // Widgets
   //

   /** Edits screen resolution in pixels per inch. */
   private NumericTextField screenDPIField = null;
   
   /** Edits preferred default value for the <em>font family</em> style property. */
   private FontFamilyButton fontFamilyBtn = null;
   /** Edits preferred default value for the <em>alternate font</em> style property. */
   private JComboBox<GenericFont> altFontCombo = null;
   /** Edits preferred default value for the <em>Postscript font</em> style property. */
   private JComboBox<PSFont> psFontCombo = null;
   /** Edits preferred default value for the <em>font size</em> style property. */
   private NumericTextField fontSizeField = null;
   /** Edits preferred default value for the <i>font style</i> property. */
   private MultiButton<FontStyle> fontStyleMB = null;
   /** Edits preferred default value for the <em>text/fill color</em> property. */
   private RGBColorPicker fillColorPicker = null;
   /** Edits preferred default value for the <em>stroke color</em> property. */
   private RGBColorPicker strokeColorPicker = null;
   /** Edits preferred default value for the <em>stroke width</em> property. */
   private MeasureEditor strokeWidthEditor = null;
   /** Edits preferred default value for the <i>stroke endcap style</i> property. */
   private MultiButton<StrokeCap> strokeCapMB = null;
   /** Edits preferred default value for the <i>stroke join style</i> property. */
   private MultiButton<StrokeJoin> strokeJoinMB = null;

   /** Edits the preferred default value for a legend's <i>mid</i> property (symbol at midpoint or endpoints). */
   private MultiButton<Boolean> midPtMB = null;
   /** Edits preferred default value for a legend's <em>spacer</em> property. */
   private MeasureEditor legendSpacerEditor = null;
   /** [As of v4.4.0] Edits preferred default value for a legend's <em>labelOffset</em> property. */
   private MeasureEditor legendLabelOffsetEditor = null;
   
   /** Edits preferred default value for an axis's <em>spacer</em> property. */
   private MeasureEditor axisSpacerEditor = null;
   /** Edits preferred default value for an axis's <em>label offset</em> property. */
   private MeasureEditor axisLabelOffsetEditor = null;
   
   /** Array of 9 check boxes to edit preferred default value for a tick set's <em>perLogIntv</em> property. */
   private JCheckBox[] logTickCheckBoxes = null;
   /** Shared action command from the check boxes controlling the <em>perLogIntv</em> property's default value. */
   private final static String LOGTICKCMD = "LogTick";
   /** Edits preferred default for the tick mark orientation (in, out, or thru) of a tick set. */
   private MultiButton<TickSetNode.Orientation> tickOriMB = null; 
   /** Mutually exclusive set of buttons that select the preferred default of a tick set's <em>fmt</em> property.  */
   private JRadioButton[] tickLblFmtBtns = null;
   /** Shared action command from the buttons controlling the <em>fmt</em> property's default value. */
   private final static String FMTCMD = "FmtTick";
   /** Edits preferred default value for a tick set's <em>len</em> property. */
   private MeasureEditor tickLenEditor = null;
   /** Edits preferred default value for a tick set's <em>gap</em> property. */
   private MeasureEditor tickGapEditor = null;
   
   /** Edits preferred default value for a calibration bar's <em>cap</em> property. */
   private JComboBox<Marker> calibCapCombo = null;
   /** Edits preferred default value for a calibration bar's <em>cap size</em> property. */
   private MeasureEditor calibCapSizeEditor = null;
   /** Edits preferred default value for the type of endcaps on error bars of a data trace. */
   private JComboBox<Marker> ebarCapCombo = null;
   /** Edits preferred default value for the size of endcaps on error bars of a data trace. */
   private MeasureEditor ebarCapSizeEditor = null;

   /** Check box sets preferred default value for the contour node property that enables/disabled image smoothing. */
   private JCheckBox enaHMSmoothCB = null;

   /**
    * Container for each of the "cards" in which graphic node property default widgets are housed, with only one card
    * visible at a time. The user selects a particular card via the tab strip that sits above this panel.
    */
   private JPanel propTabPanel = null;

   /** Index of the currently selected tab, determining which property defaults "card" is displayed. */
   private int iSelectedTab = 0;

   /** List of all change listeners registered with the tab strip model. */
   private final EventListenerList tabListeners = new EventListenerList();

   /** Card ID and tab label for tab pane displaying text/draw style defaults. */
   private final static String CARD_TEXT = "Text/Draw Styles";
   /** Card ID and tab label for tab pane displaying tick mark set property defaults. */
   private final static String CARD_TICKS = "Tick Marks";
   /** Card ID and tab label for tab pane displaying all other property defaults. */
   private final static String CARD_OTHER = "Other";

   /** The tab labels for the tab strip (they never change). */
   private final static String[] TABLABELS = new String[] {
         CARD_TEXT, CARD_TICKS, CARD_OTHER
   };

   /** The "Color Maps" dialog section. */
   private ColorMapPanel cmapPanel = null;


   /** Helper method creates all the widgets managed by <code>PreferencesDlg</code>. */
   private void createComponents()
   {
      // screen resolution
      screenDPIField = new NumericTextField(FCWorkspace.MIN_DPI, FCWorkspace.MAX_DPI, 4);
      
      // most style properties...
      fontFamilyBtn = new FontFamilyButton(180);
      fontFamilyBtn.addPropertyChangeListener(FontFamilyButton.FONTFAMILY_PROPERTY, this);

      altFontCombo = new JComboBox<>(GenericFont.values());
      altFontCombo.setToolTipText("Alternate font");
      altFontCombo.addActionListener(this);
      
      psFontCombo = new JComboBox<>(PSFont.values());
      psFontCombo.setToolTipText("Postscript font");
      psFontCombo.addActionListener(this);
      
      fontSizeField = new NumericTextField(FGraphicNode.MINFONTSIZE, FGraphicNode.MAXFONTSIZE);
      fontSizeField.setToolTipText("Font size (pt)");
      fontSizeField.addActionListener(this);

      fontStyleMB = new MultiButton<>();
      fontStyleMB.addChoice(FontStyle.PLAIN, FCIcons.V4_FSPLAIN_16, "Plain");
      fontStyleMB.addChoice(FontStyle.ITALIC, FCIcons.V4_FSITALIC_16, "Italic");
      fontStyleMB.addChoice(FontStyle.BOLD, FCIcons.V4_FSBOLD_16, "Bold");
      fontStyleMB.addChoice(FontStyle.BOLDITALIC, FCIcons.V4_FSBOLDITALIC_16, "Bold Italic");
      fontStyleMB.addItemListener(this);
      fontStyleMB.setToolTipText("Font style");

      fillColorPicker = new RGBColorPicker();
      fillColorPicker.setToolTipText("Text/Fill Color");
      fillColorPicker.addPropertyChangeListener(RGBColorPicker.COLOR_PROPERTY, this);

      strokeColorPicker = new RGBColorPicker();
      strokeColorPicker.setToolTipText("Stroke Color");
      strokeColorPicker.addPropertyChangeListener(RGBColorPicker.COLOR_PROPERTY, this);

      strokeWidthEditor = new MeasureEditor(0, FGraphicNode.STROKEWCONSTRAINTS);
      strokeWidthEditor.setToolTipText("Stroke Width");
      strokeWidthEditor.addActionListener(this);

      strokeCapMB = new MultiButton<>();
      strokeCapMB.addChoice(StrokeCap.BUTT, FCIcons.V4_CAPBUTT_16, "butt");
      strokeCapMB.addChoice(StrokeCap.SQUARE, FCIcons.V4_CAPSQUARE_16, "square");
      strokeCapMB.addChoice(StrokeCap.ROUND, FCIcons.V4_CAPROUND_16, "round");
      strokeCapMB.setToolTipText("Stroke end cap");
      strokeCapMB.addItemListener(this);
      
      strokeJoinMB = new MultiButton<>();
      strokeJoinMB.addChoice(StrokeJoin.MITER, FCIcons.V4_JOINMITER_16, "miter");
      strokeJoinMB.addChoice(StrokeJoin.BEVEL, FCIcons.V4_JOINBEVEL_16, "bevel");
      strokeJoinMB.addChoice(StrokeJoin.ROUND, FCIcons.V4_JOINROUND_16, "round");
      strokeJoinMB.setToolTipText("Stroke join");
      strokeJoinMB.addItemListener(this);

      // selected properties for a graph axis, tick set, and legend...
      midPtMB = new MultiButton<>();
      midPtMB.addChoice(Boolean.TRUE, FCIcons.V4_MIDPT_16, "midpoint");
      midPtMB.addChoice(Boolean.FALSE, FCIcons.V4_ENDPT_16, "endpoints");
      midPtMB.setToolTipText("Location of marker symbol in legend entry");
      midPtMB.addItemListener(this);
      
      legendSpacerEditor = new MeasureEditor(0, LegendNode.LENCONSTRAINTS);
      legendSpacerEditor.setToolTipText("Enter distance between legend entries (0-5in)");
      legendSpacerEditor.addActionListener(this);
      
      legendLabelOffsetEditor = new MeasureEditor(0, LegendNode.LENCONSTRAINTS);
      legendLabelOffsetEditor.setToolTipText("Enter legend label offset (0-5in)");
      legendLabelOffsetEditor.addActionListener(this);
      
      axisSpacerEditor = new MeasureEditor(0, AxisNode.SPACERCONSTRAINTS);
      axisSpacerEditor.setToolTipText("Enter distance between axis line and nearest edge of graph's data window (0..2in)");
      axisSpacerEditor.addActionListener(this);
      
      axisLabelOffsetEditor = new MeasureEditor(0, AxisNode.SPACERCONSTRAINTS);
      axisLabelOffsetEditor.setToolTipText("Enter distance between axis line and its label (0..2in)");
      axisLabelOffsetEditor.addActionListener(this);

      logTickCheckBoxes = new JCheckBox[9];
      for(int i=0; i<logTickCheckBoxes.length; i++)
      {
         logTickCheckBoxes[i] = new JCheckBox(Integer.toString(i+1));
         logTickCheckBoxes[i].addActionListener(this);
         logTickCheckBoxes[i].setActionCommand(LOGTICKCMD);
      }

      tickOriMB = new MultiButton<>();
      tickOriMB.addChoice(TickSetNode.Orientation.OUT, FCIcons.V4_TICKOUT_16, "outward");
      tickOriMB.addChoice(TickSetNode.Orientation.IN, FCIcons.V4_TICKIN_16, "inward");
      tickOriMB.addChoice(TickSetNode.Orientation.THRU, FCIcons.V4_TICKTHRU_16, "bisecting");
      tickOriMB.setToolTipText("Direction of tick marks with respect to axis");
      tickOriMB.addItemListener(this);

      ButtonGroup bg = new ButtonGroup();
      TickSetNode.LabelFormat[] tickFmt = TickSetNode.LabelFormat.values();
      tickLblFmtBtns = new JRadioButton[tickFmt.length];
      for(int i=0; i<tickFmt.length; i++)
      {
         tickLblFmtBtns[i] = new JRadioButton(tickFmt[i].getGUILabel());
         tickLblFmtBtns[i].addActionListener(this);
         tickLblFmtBtns[i].setActionCommand(FMTCMD);
         bg.add(tickLblFmtBtns[i]);
      }

      tickLenEditor = new MeasureEditor(0, TickSetNode.TICKLENCONSTRAINTS);
      tickLenEditor.setToolTipText("Enter tick mark length (0-1in)");
      tickLenEditor.addActionListener(this);

      tickGapEditor = new MeasureEditor(0, TickSetNode.TICKLENCONSTRAINTS);
      tickGapEditor.setToolTipText("Enter size of gap between tick marks and labels (0-1in)");
      tickGapEditor.addActionListener(this);

      // endcap adornment and size for calibration bars, error bars on a data trace...
      calibCapCombo = new JComboBox<>(Marker.values());
      calibCapCombo.setToolTipText("Select endcap shape");
      calibCapCombo.addActionListener(this);

      calibCapSizeEditor = new MeasureEditor(0, CalibrationBarNode.ENDCAPSIZECONSTRAINTS);
      calibCapSizeEditor.setToolTipText("Enter size of endcap's bounding box");
      calibCapSizeEditor.addActionListener(this);
      
      ebarCapCombo = new JComboBox<>(Marker.values());
      ebarCapCombo.setToolTipText("Select endcap shape");
      ebarCapCombo.addActionListener(this);

      ebarCapSizeEditor = new MeasureEditor(0, ErrorBarNode.EBARCAPSIZECONSTRAINTS);
      ebarCapSizeEditor.setToolTipText("Enter size of endcap's bounding box");
      ebarCapSizeEditor.addActionListener(this);

      enaHMSmoothCB = new JCheckBox("Enable heatmap image smoothing");
      enaHMSmoothCB.addActionListener(this);

      propTabPanel = new JPanel(new CardLayout());

      cmapPanel = new ColorMapPanel();
   }

   /** Helper method that lays out the widgets within the <code>PreferencesDlg</code>. */
   private void layoutComponents()
   {
      int gap = 5;
      
      JPanel p = new JPanel();
      p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
      String s = "Screen DPI [" + FCWorkspace.MIN_DPI + ".." + FCWorkspace.MAX_DPI + "]";
      p.add(new JLabel(s));
      p.add(Box.createHorizontalStrut(gap));
      p.add(screenDPIField);
      JPanel row0 = new JPanel(new BorderLayout());
      row0.add(p, BorderLayout.WEST);
      
      JPanel textGroup1 = new JPreferredSizePanel();
      textGroup1.setLayout(new BoxLayout(textGroup1, BoxLayout.LINE_AXIS));
      textGroup1.add(fillColorPicker);
      textGroup1.add(Box.createHorizontalStrut(gap));
      textGroup1.add(fontFamilyBtn);
      textGroup1.add(Box.createHorizontalStrut(gap));
      textGroup1.add(fontStyleMB);
      textGroup1.add(Box.createHorizontalStrut(gap));
      textGroup1.add(fontSizeField);
      fillColorPicker.setAlignmentY(JPanel.CENTER_ALIGNMENT);
      fontFamilyBtn.setAlignmentY(JPanel.CENTER_ALIGNMENT);
      fontStyleMB.setAlignmentY(JPanel.CENTER_ALIGNMENT);
      fontSizeField.setAlignmentY(JPanel.CENTER_ALIGNMENT);

      JPanel textGroup2 = new JPreferredSizePanel();
      textGroup2.setLayout(new BoxLayout(textGroup2, BoxLayout.LINE_AXIS));
      textGroup2.add(Box.createHorizontalStrut(gap*2));
      textGroup2.add(altFontCombo);
      textGroup2.add(Box.createHorizontalStrut(gap));
      textGroup2.add(psFontCombo);
      altFontCombo.setAlignmentY(JPanel.CENTER_ALIGNMENT);
      psFontCombo.setAlignmentY(JPanel.CENTER_ALIGNMENT);

      JPanel drawGroup = new JPreferredSizePanel();
      drawGroup.setLayout(new BoxLayout(drawGroup, BoxLayout.LINE_AXIS));
      drawGroup.add(new JLabel("Stroke:"));
      drawGroup.add(Box.createHorizontalStrut(gap));
      drawGroup.add(strokeColorPicker);
      drawGroup.add(Box.createHorizontalStrut(gap));
      drawGroup.add(strokeWidthEditor);
      drawGroup.add(Box.createHorizontalStrut(gap*2));
      drawGroup.add(strokeCapMB);
      drawGroup.add(Box.createHorizontalStrut(gap));
      drawGroup.add(strokeJoinMB);
      strokeColorPicker.setAlignmentY(JPanel.CENTER_ALIGNMENT);
      strokeWidthEditor.setAlignmentY(JPanel.CENTER_ALIGNMENT);
      strokeCapMB.setAlignmentY(JPanel.CENTER_ALIGNMENT);
      strokeJoinMB.setAlignmentY(JPanel.CENTER_ALIGNMENT);

      JPanel styleGroup = new JPreferredSizePanel();
      styleGroup.setLayout(new BoxLayout(styleGroup, BoxLayout.PAGE_AXIS));
      styleGroup.add(Box.createVerticalStrut(gap*2));
      styleGroup.add(textGroup1);
      styleGroup.add(Box.createVerticalStrut(gap*2));
      styleGroup.add(textGroup2);
      styleGroup.add(Box.createVerticalStrut(gap*2));
      styleGroup.add(drawGroup);
      textGroup1.setAlignmentX(JPanel.LEFT_ALIGNMENT);
      textGroup2.setAlignmentX(JPanel.LEFT_ALIGNMENT);
      drawGroup.setAlignmentX(JPanel.LEFT_ALIGNMENT);
      styleGroup.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

      JPanel textDrawCard = new JPanel(new BorderLayout());
      textDrawCard.add(styleGroup, BorderLayout.WEST);

      JPanel tickLenGrp = new JPreferredSizePanel();
      tickLenGrp.setLayout(new BoxLayout(tickLenGrp, BoxLayout.LINE_AXIS));
      tickLenGrp.add(new JLabel("Tick length:"));
      tickLenGrp.add(Box.createHorizontalStrut(gap));
      tickLenGrp.add(tickLenEditor);
      tickLenGrp.add(Box.createHorizontalStrut(gap*2));
      tickLenGrp.add(new JLabel("gap:"));
      tickLenGrp.add(Box.createHorizontalStrut(gap*2));
      tickLenGrp.add(tickGapEditor);
      tickLenGrp.add(Box.createHorizontalStrut(gap*2));
      tickLenGrp.add(new JSeparator(JSeparator.VERTICAL));
      tickLenGrp.add(Box.createHorizontalStrut(gap*2));
      tickLenGrp.add(tickOriMB);

      JPanel tickLblGrp = new JPreferredSizePanel();
      tickLblGrp.setLayout(new BoxLayout(tickLblGrp, BoxLayout.LINE_AXIS));
      tickLblGrp.add(new JLabel("Label format:"));
      tickLblGrp.add(Box.createHorizontalStrut(gap*2));
      for(int i=0; i<tickLblFmtBtns.length; i++)
      {
         tickLblGrp.add(tickLblFmtBtns[i]);
         if(i < tickLblFmtBtns.length - 1) tickLblGrp.add(Box.createHorizontalStrut(gap));
      }

      JPanel perLogGrp = new JPreferredSizePanel();
      perLogGrp.setLayout(new BoxLayout(perLogGrp, BoxLayout.LINE_AXIS));
      perLogGrp.add(new JLabel("Ticks per logarithmic decade:"));
      perLogGrp.add(Box.createHorizontalStrut(gap*2));
      for(JCheckBox cb : logTickCheckBoxes)
         perLogGrp.add(cb);
      
      JPanel tickGroup = new JPreferredSizePanel();
      tickGroup.setLayout(new BoxLayout(tickGroup, BoxLayout.PAGE_AXIS));
      tickGroup.add(Box.createVerticalStrut(gap*2));
      tickGroup.add(tickLenGrp);
      tickGroup.add(Box.createVerticalStrut(gap*2));
      tickGroup.add(tickLblGrp); 
      tickGroup.add(Box.createVerticalStrut(gap*2));
      tickGroup.add(perLogGrp); 
      tickLenGrp.setAlignmentX(JPanel.LEFT_ALIGNMENT);
      tickLblGrp.setAlignmentX(JPanel.LEFT_ALIGNMENT);
      perLogGrp.setAlignmentX(JPanel.LEFT_ALIGNMENT);
      tickGroup.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

      JPanel tickMarksCard = new JPanel(new BorderLayout());
      tickMarksCard.add(tickGroup, BorderLayout.WEST);

      JPanel axisGroup = new JPreferredSizePanel();
      axisGroup.setLayout(new BoxLayout(axisGroup, BoxLayout.LINE_AXIS));
      axisGroup.add(new JLabel("Axis:"));
      axisGroup.add(Box.createHorizontalStrut(gap*2));
      axisGroup.add(new JLabel("spacer"));
      axisGroup.add(Box.createHorizontalStrut(gap));
      axisGroup.add(axisSpacerEditor);
      axisGroup.add(Box.createHorizontalStrut(gap*2));
      axisGroup.add(new JLabel("label offset"));
      axisGroup.add(Box.createHorizontalStrut(gap));
      axisGroup.add(axisLabelOffsetEditor);
      
      JPanel legendGroup = new JPreferredSizePanel();
      legendGroup.setLayout(new BoxLayout(legendGroup, BoxLayout.LINE_AXIS));
      legendGroup.add(new JLabel("Legend:"));
      legendGroup.add(Box.createHorizontalStrut(gap*2));
      legendGroup.add(new JLabel("spacer"));
      legendGroup.add(Box.createHorizontalStrut(gap));
      legendGroup.add(legendSpacerEditor);
      legendGroup.add(Box.createHorizontalStrut(gap*2));
      legendGroup.add(new JLabel("label offset"));
      legendGroup.add(Box.createHorizontalStrut(gap));
      legendGroup.add(legendLabelOffsetEditor);
      legendGroup.add(Box.createHorizontalStrut(gap*2));
      legendGroup.add(new JLabel("symbol placement"));
      legendGroup.add(Box.createHorizontalStrut(gap));
      legendGroup.add(midPtMB);

      JPanel calibGroup = new JPreferredSizePanel();
      calibGroup.setLayout(new BoxLayout(calibGroup, BoxLayout.LINE_AXIS));
      calibGroup.add(new JLabel("Calibration bar endcaps:"));
      calibGroup.add(Box.createHorizontalStrut(gap*2));
      calibGroup.add(calibCapCombo);
      calibGroup.add(Box.createHorizontalStrut(gap*2));
      calibGroup.add(calibCapSizeEditor);

      JPanel ebarGroup = new JPreferredSizePanel();
      ebarGroup.setLayout(new BoxLayout(ebarGroup, BoxLayout.LINE_AXIS));
      ebarGroup.add(new JLabel("Error bar endcaps:"));
      ebarGroup.add(Box.createHorizontalStrut(gap*2));
      ebarGroup.add(ebarCapCombo);
      ebarGroup.add(Box.createHorizontalStrut(gap*2));
      ebarGroup.add(ebarCapSizeEditor);

      JPanel miscGroup = new JPanel();
      miscGroup.setLayout(new BoxLayout(miscGroup, BoxLayout.PAGE_AXIS));
      miscGroup.add(enaHMSmoothCB);
      miscGroup.add(Box.createVerticalStrut(gap*2));
      miscGroup.add(axisGroup);
      miscGroup.add(Box.createVerticalStrut(gap*2));
      miscGroup.add(legendGroup); 
      miscGroup.add(Box.createVerticalStrut(gap*2));
      miscGroup.add(calibGroup);
      miscGroup.add(Box.createVerticalStrut(gap*2));
      miscGroup.add(ebarGroup); 
      enaHMSmoothCB.setAlignmentX(JPanel.LEFT_ALIGNMENT);
      axisGroup.setAlignmentX(JPanel.LEFT_ALIGNMENT);
      legendGroup.setAlignmentX(JPanel.LEFT_ALIGNMENT);
      calibGroup.setAlignmentX(JPanel.LEFT_ALIGNMENT);
      ebarGroup.setAlignmentX(JPanel.LEFT_ALIGNMENT);
      miscGroup.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

      JPanel otherCard = new JPanel(new BorderLayout());
      otherCard.add(miscGroup, BorderLayout.WEST);

      propTabPanel.add(textDrawCard, CARD_TEXT);
      propTabPanel.add(tickMarksCard, CARD_TICKS);
      propTabPanel.add(otherCard, CARD_OTHER);

      TabStrip tabStrip = new TabStrip(this);
      propTabPanel.setOpaque(false);
      propTabPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 2, 2, 2, tabStrip.getSelectionColor()),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
      ));

      JPanel row1 = new JPanel(new BorderLayout());
      row1.add(tabStrip, BorderLayout.NORTH);
      row1.add(propTabPanel, BorderLayout.CENTER);
      row1.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Graphic Property Defaults"),
            BorderFactory.createEmptyBorder(3, 3, 3, 3))
      );

      JPanel row2 = new JPanel(new BorderLayout());
      row2.add(cmapPanel, BorderLayout.WEST);
      row2.setBorder(BorderFactory.createTitledBorder("Color Maps"));
      
      JPanel content = new JPanel();
      content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
      content.add(row0);
      content.add(Box.createVerticalStrut(gap*2));
      content.add(row1);
      content.add(Box.createVerticalStrut(gap*2));
      content.add(row2);
      content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
      
      getContentPane().setLayout(new BorderLayout());
      getContentPane().add(content, BorderLayout.CENTER);
   }

   /** 
    * Helper method reloads all widgets on the <code>PreferencesDlg</code> IAW the current user preferences as 
    * maintained in {@link FGNPreferences FGNPreferences}.
    */
   private void reload()
   {
      screenDPIField.setValue(workspace.getScreenDPI());
      
      fontFamilyBtn.setFontFamily(fgnPrefs.getPreferredFont(), false);
      altFontCombo.setSelectedItem(fgnPrefs.getPreferredAltFont());
      psFontCombo.setSelectedItem(fgnPrefs.getPreferredPSFont());
      fontStyleMB.setCurrentChoice(fgnPrefs.getPreferredFontStyle());
      fontSizeField.setValue(fgnPrefs.getPreferredFontSize());
      fillColorPicker.setCurrentColor(fgnPrefs.getPreferredFillColor(), false);
      strokeColorPicker.setCurrentColor(fgnPrefs.getPreferredStrokeColor(), false);
      strokeWidthEditor.setMeasure(fgnPrefs.getPreferredStrokeWidth());      
      strokeCapMB.setCurrentChoice(fgnPrefs.getPreferredStrokeEndcap());
      strokeJoinMB.setCurrentChoice(fgnPrefs.getPreferredStrokeJoin());
      
      midPtMB.setCurrentChoice(fgnPrefs.getPreferredLegendSymbolAtMidPoint());
      legendSpacerEditor.setMeasure(fgnPrefs.getPreferredLegendSpacer());
      legendLabelOffsetEditor.setMeasure(fgnPrefs.getPreferredLegendLabelOffset());
      axisSpacerEditor.setMeasure(fgnPrefs.getPreferredAxisSpacer());
      axisLabelOffsetEditor.setMeasure(fgnPrefs.getPreferredAxisLabelOffset());
      
      LogTickPattern ltp = fgnPrefs.getPreferredLogTickPattern();
      for(int i=0; i<logTickCheckBoxes.length; i++)
         logTickCheckBoxes[i].setSelected(ltp.isTickEnabledAt(i+1));
      
      tickOriMB.setCurrentChoice(fgnPrefs.getPreferredTickOrientation());
      
      TickSetNode.LabelFormat[] formats = TickSetNode.LabelFormat.values();
      TickSetNode.LabelFormat fmt = fgnPrefs.getPreferredTickLabelFormat();
      for(int i=0; i<formats.length; i++)
      {
         if(fmt == formats[i])
         {
            tickLblFmtBtns[i].setSelected(true);
            break;
         }
      }
      
      tickLenEditor.setMeasure(fgnPrefs.getPreferredTickLength());
      tickGapEditor.setMeasure(fgnPrefs.getPreferredTickGap());

      calibCapCombo.setSelectedItem(fgnPrefs.getPreferredCalibCap());
      calibCapSizeEditor.setMeasure(fgnPrefs.getPreferredCalibCapSize());
      ebarCapCombo.setSelectedItem(fgnPrefs.getPreferredEBarCap());
      ebarCapSizeEditor.setMeasure(fgnPrefs.getPreferredEBarCapSize());
      
      enaHMSmoothCB.setSelected(fgnPrefs.getPreferredHeatMapImageSmoothingEnable());
      
      cmapPanel.reload();
   }

   public void actionPerformed(ActionEvent e)
   {
      Object src = e.getSource();
      String cmd = e.getActionCommand();
      
      if(src == altFontCombo)
         fgnPrefs.setPreferredAltFont((GenericFont) altFontCombo.getSelectedItem());
      else if(src == psFontCombo)
         fgnPrefs.setPreferredPSFont((PSFont) psFontCombo.getSelectedItem());
      else if(src == fontSizeField)
         fgnPrefs.setPreferredFontSize(fontSizeField.getValue().intValue());
      else if(src == strokeWidthEditor)
         fgnPrefs.setPreferredStrokeWidth(strokeWidthEditor.getMeasure());
      else if(src == legendSpacerEditor)
         fgnPrefs.setPreferredLegendSpacer(legendSpacerEditor.getMeasure());
      else if(src == legendLabelOffsetEditor)
         fgnPrefs.setPreferredLegendLabelOffset(legendLabelOffsetEditor.getMeasure());
      else if(src == axisSpacerEditor)
         fgnPrefs.setPreferredAxisSpacer(axisSpacerEditor.getMeasure());
      else if(src == axisLabelOffsetEditor)
         fgnPrefs.setPreferredAxisLabelOffset(axisLabelOffsetEditor.getMeasure());
      else if(LOGTICKCMD.equals(cmd) && (src instanceof JCheckBox))
      {
         int enaLogTicks = 0;
         for(int i=0; i<logTickCheckBoxes.length; i++) 
            if(logTickCheckBoxes[i].isSelected())
               enaLogTicks |= (1<<(i+1));
         fgnPrefs.setPreferredLogTickPattern(new LogTickPattern(enaLogTicks));
      }
      else if(FMTCMD.equals(cmd) && (src instanceof JRadioButton))
      {
         TickSetNode.LabelFormat fmt = TickSetNode.LabelFormat.NONE;
         for(int i=0; i<tickLblFmtBtns.length; i++) if(tickLblFmtBtns[i].isSelected())
         {
            fmt = TickSetNode.LabelFormat.values()[i];
            break;
         }
         fgnPrefs.setPreferredTickLabelFormat(fmt);
      }
      else if(src == tickLenEditor)
         fgnPrefs.setPreferredTickLength(tickLenEditor.getMeasure());
      else if(src == tickGapEditor)
         fgnPrefs.setPreferredTickGap(tickGapEditor.getMeasure());
      else if(src == calibCapCombo)
         fgnPrefs.setPreferredCalibCap((Marker) calibCapCombo.getSelectedItem());
      else if(src == calibCapSizeEditor)
         fgnPrefs.setPreferredCalibCapSize(calibCapSizeEditor.getMeasure());
      else if(src == ebarCapCombo)
         fgnPrefs.setPreferredEBarCap((Marker) ebarCapCombo.getSelectedItem());
      else if(src == ebarCapSizeEditor)
         fgnPrefs.setPreferredEBarCapSize(ebarCapSizeEditor.getMeasure());
      else if(src == enaHMSmoothCB)
         fgnPrefs.setPreferredHeatMapImageSmoothingEnable(enaHMSmoothCB.isSelected());
   }

   public void propertyChange(PropertyChangeEvent e)
   {
      String prop = e.getPropertyName();
      Object src = e.getSource();

      if(prop.equals(RGBColorPicker.COLOR_PROPERTY))
      {
         if(src == fillColorPicker)
            fgnPrefs.setPreferredFillColor(fillColorPicker.getCurrentColor());
         else if(src == strokeColorPicker)
            fgnPrefs.setPreferredStrokeColor(strokeColorPicker.getCurrentColor());
      }
      else if(prop.equals(FontFamilyButton.FONTFAMILY_PROPERTY) && src == fontFamilyBtn)
      {
         fgnPrefs.setPreferredFont(fontFamilyBtn.getFontFamily());
      }
   }
   
   @Override public void itemStateChanged(ItemEvent e)
   {
      Object src = e.getSource();
      if(src == fontStyleMB) fgnPrefs.setPreferredFontStyle(fontStyleMB.getCurrentChoice());
      else if(src == strokeCapMB)
         fgnPrefs.setPreferredStrokeEndcap(strokeCapMB.getCurrentChoice());
      else if(src == strokeJoinMB)
         fgnPrefs.setPreferredStrokeJoin(strokeJoinMB.getCurrentChoice());
      else if(src == midPtMB)
         fgnPrefs.setPreferredLegendSymbolAtMidPoint(midPtMB.getCurrentChoice());
      else if(src == tickOriMB)
         fgnPrefs.setPreferredTickOrientation(tickOriMB.getCurrentChoice());
   }

   @Override public int getNumTabs() { return TABLABELS.length; }
   @Override public int getSelectedTab() { return iSelectedTab; }
   @Override public void setSelectedTab(int tabPos)
   {
      if(tabPos < 0 || tabPos >= getNumTabs() || tabPos == iSelectedTab) return;

      iSelectedTab = tabPos;
      CardLayout cl = (CardLayout) propTabPanel.getLayout();
      cl.show(propTabPanel, TABLABELS[iSelectedTab]);
   }
   @Override public void addChangeListener(ChangeListener l)
   {
      if(l != null) tabListeners.add(ChangeListener.class, l);
   }
   @Override public void removeChangeListener(ChangeListener l)
   {
      if(l != null) tabListeners.remove(ChangeListener.class, l);
   }
   @Override public String getTabLabel(int tabPos)
   {
      return(tabPos >= 0 && tabPos<getNumTabs() ? TABLABELS[tabPos] : null);
   }
   @Override public String getTabToolTip(int tabPos) { return(null); }
   @Override public Icon getTabIcon(int tabPos) { return(null); }
   @Override public boolean isTabClosable(int tabPos) { return(false); }
   @Override public String getCloseTabToolTip(int tabPos) { return(null); }
   @Override public void closeTab(int tabPos) {}
   @Override public boolean supportsTabRepositioning() { return(false); }
   @Override public boolean moveTab(int fromPos, int toPos) { return(false); }

   /**
    * Helper class implements the dialog section which lets user browse all available color maps -- both built-in and
    * custom --, and provides widgets by which the user can generate and add new custom maps, or remove any existing
    * custom color map (built-ins cannot be removed.
    */
   private static class ColorMapPanel extends JPanel implements ActionListener
   {
      ColorMapPanel()
      {
         JPanel p = new JPanel(new SpringLayout());
         SpringLayout layout = (SpringLayout) p.getLayout();
         
         cmapCombo = new JComboBox<>(FGNPreferences.getInstance().getAllAvailableColorMaps());
         ColorMapGradientRenderer renderer = new ColorMapGradientRenderer();
         renderer.setPreferredSize(new Dimension(500, 25));
         cmapCombo.setRenderer(renderer);
         cmapCombo.addActionListener(this);
         p.add(cmapCombo);
         
         rmvBtn = new JButton(FCIcons.V4_DELETE_16);
         rmvBtn.setDisabledIcon(FCIcons.V4_DEL_DISABLED_16);
         rmvBtn.setBorder(null); 
         rmvBtn.setContentAreaFilled(false); 
         rmvBtn.setMargin(new Insets(0,0,0,0));
         rmvBtn.setFocusable(false);
         rmvBtn.setToolTipText("Remove the selected color map (user-defined maps only)");
         rmvBtn.addActionListener(this);
         p.add(rmvBtn);
         
         clipBtn = new JButton(FCIcons.V4_INSPASTE_22);
         clipBtn.setBorder(null); 
         clipBtn.setContentAreaFilled(false); 
         clipBtn.setMargin(new Insets(0,0,0,0));
         clipBtn.setFocusable(false);
         clipBtn.setToolTipText("Construct color map definition from current contents of system clipboard");
         clipBtn.addActionListener(this);
         p.add(clipBtn);
         
         JLabel nameLbl = new JLabel("Name: ");
         p.add(nameLbl);
         nameField = new JTextField(8);
         nameField.setToolTipText("Enter a descriptive name for color map");
         p.add(nameField);
         JLabel numFramesLbl = new JLabel("#Frames: ");
         p.add(numFramesLbl);
         numFramesField = new NumericTextField(2, 10);
         numFramesField.setToolTipText("Number of defining key frames [2 .. 10]");
         numFramesField.addActionListener(this);
         p.add(numFramesField);
         
         addBtn = new JButton(FCIcons.V4_ADD_22);
         addBtn.setBorder(null); 
         addBtn.setContentAreaFilled(false); 
         addBtn.setMargin(new Insets(0,0,0,0));
         addBtn.setFocusable(false);
         addBtn.setToolTipText("Save as custom color map");
         addBtn.addActionListener(this);
         p.add(addBtn);
         
         keyFramesTable = new JTable(new KeyFramesTM());
         keyFramesTable.setRowSelectionAllowed(false);
         keyFramesTable.setDefaultRenderer(Color.class, new ColorCellRenderer());
         keyFramesTable.setDefaultEditor(Color.class, new ColorCellEditor(new Dimension(200, 22), false));
         keyFramesTable.setRowHeight(25);
         keyFramesTable.setShowHorizontalLines(true);
         keyFramesTable.setGridColor(Color.LIGHT_GRAY);
         keyFramesTable.setColumnModel(new KeyFramesTCM());
         keyFramesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
         keyFramesTable.getTableHeader().setResizingAllowed(false);
         keyFramesTable.getTableHeader().setReorderingAllowed(false);
         keyFramesTable.setPreferredScrollableViewportSize(new Dimension(200, 10*keyFramesTable.getRowHeight()));
         
         JScrollPane scroller = new JScrollPane(keyFramesTable);
         scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
         scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
         p.add(scroller);
         
         cmapSwatch = new JPanel() {
            @Override protected void paintComponent(Graphics g)
            {
               if(currColorMap == null)
               {
                  super.paintComponent(g);
                  return;
               }
               
               int w = getWidth();
               int h = getHeight();

               Graphics2D g2d = (Graphics2D) g.create();
               try
               {
                  ColorLUT lut = new ColorLUT(currColorMap, false);
                  g2d.setPaint(lut.getGradientPaint(((float) w)/2f, 0, ((float) w)/2f, h));
                  g2d.fillRect(0,  0,  w,  h);
               }
               finally { if(g2d != null) g2d.dispose(); }
            }
         };
         cmapSwatch.setBorder(BorderFactory.createSoftBevelBorder(BevelBorder.RAISED));
         p.add(cmapSwatch);
         
         layout.putConstraint(SpringLayout.WEST, cmapCombo, 5, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, rmvBtn, 10, SpringLayout.EAST, cmapCombo);
         layout.putConstraint(SpringLayout.EAST, p, 5, SpringLayout.EAST, rmvBtn);
         
         layout.putConstraint(SpringLayout.WEST, clipBtn, 0, SpringLayout.WEST, cmapCombo);
         layout.putConstraint(SpringLayout.WEST, nameLbl, 10, SpringLayout.EAST, clipBtn);
         layout.putConstraint(SpringLayout.WEST, nameField, 0, SpringLayout.EAST, nameLbl);
         layout.putConstraint(SpringLayout.WEST, numFramesLbl, 5, SpringLayout.EAST, nameField);
         layout.putConstraint(SpringLayout.WEST, numFramesField, 0, SpringLayout.EAST, numFramesLbl);
         layout.putConstraint(SpringLayout.WEST, addBtn, 10, SpringLayout.EAST, numFramesField);
         
         
         layout.putConstraint(SpringLayout.WEST, scroller, 0, SpringLayout.WEST, cmapCombo);
         layout.putConstraint(SpringLayout.WEST, cmapSwatch, 10, SpringLayout.EAST, scroller);
         layout.putConstraint(SpringLayout.EAST, cmapSwatch, 0, SpringLayout.EAST, rmvBtn);
         
         layout.putConstraint(SpringLayout.NORTH, cmapCombo, 5, SpringLayout.NORTH, p);
         layout.putConstraint(SpringLayout.NORTH, nameField, 10, SpringLayout.SOUTH, cmapCombo);
         layout.putConstraint(SpringLayout.NORTH, scroller, 5, SpringLayout.SOUTH, nameField);
         layout.putConstraint(SpringLayout.SOUTH, p, 5, SpringLayout.SOUTH, scroller);
         layout.putConstraint(SpringLayout.NORTH, cmapSwatch, 0, SpringLayout.NORTH, scroller);
         layout.putConstraint(SpringLayout.SOUTH, cmapSwatch, 0, SpringLayout.SOUTH, scroller);
         
         layout.putConstraint(SpringLayout.VERTICAL_CENTER, rmvBtn, 0, SpringLayout.VERTICAL_CENTER, cmapCombo);
         layout.putConstraint(SpringLayout.VERTICAL_CENTER, clipBtn, 0, SpringLayout.VERTICAL_CENTER, nameField);
         layout.putConstraint(SpringLayout.VERTICAL_CENTER, nameLbl, 0, SpringLayout.VERTICAL_CENTER, nameField);
         layout.putConstraint(SpringLayout.VERTICAL_CENTER, numFramesLbl, 0, SpringLayout.VERTICAL_CENTER, nameField);
         layout.putConstraint(SpringLayout.VERTICAL_CENTER, numFramesField, 0, SpringLayout.VERTICAL_CENTER, nameField);
         layout.putConstraint(SpringLayout.VERTICAL_CENTER, addBtn, 0, SpringLayout.VERTICAL_CENTER, nameField);

         
         setLayout(new BorderLayout());
         add(p, BorderLayout.NORTH);
         
         currColorMap = (ColorMap) cmapCombo.getSelectedItem();
         rmvBtn.setEnabled((currColorMap != null) && currColorMap.isCustom());
         refresh();
      }
      
      /** Reload the contents of this dialog section. Called each time the Preferences dialog is raised. */
      private void reload()
      {
         // we reload the combo box with all available color maps, in case any changes were made outside the context
         // of the Preferences dialog
         cmapCombo.removeActionListener(this);
         cmapCombo.removeAllItems();
         ColorMap[] allMaps = FGNPreferences.getInstance().getAllAvailableColorMaps();
         boolean found = false;
         for(ColorMap cm : allMaps)
         {
            if(!found) found = cm.equals(currColorMap) && cm.getName().equals(currColorMap.getName());
            cmapCombo.addItem(cm);
         }
         if(!found) currColorMap = allMaps[0];
         cmapCombo.setSelectedItem(currColorMap);
         cmapCombo.addActionListener(this);
         rmvBtn.setEnabled((currColorMap != null) && currColorMap.isCustom());
         refresh();
      }
      
      /** Refresh the panel widgets' state whenever a different color map is selected from the color map combo box. */
      private void refresh()
      {
         nameField.setText(currColorMap != null ? currColorMap.getName() : "");
         numFramesField.setValue(currColorMap != null ? currColorMap.getNumKeyFrames() : 2);
         ((KeyFramesTM) keyFramesTable.getModel()).fireTableStructureChanged();
         cmapSwatch.repaint();
      }

      @Override public void actionPerformed(ActionEvent e)
      {
         Object src = e.getSource();
         if(src == cmapCombo)
         {
            currColorMap = (ColorMap) cmapCombo.getSelectedItem();
            rmvBtn.setEnabled((currColorMap != null) && currColorMap.isCustom());
            refresh();
         }
         else if(src == numFramesField)
         {
            int n = numFramesField.getValue().intValue();
            if(currColorMap != null && n != currColorMap.getNumKeyFrames())
            {
               currColorMap = currColorMap.deriveColorMap(n);
               refresh();
               
            }
         }
         else if(src == addBtn)
         {
            boolean ok = currColorMap != null && currColorMap.isCustom();
            if(ok)
            {
               String name = nameField.getText();
               if(name.isEmpty() || ColorMap.isBuiltInName(name)) name = "new_cmap";
               currColorMap = currColorMap.rename(name);
               ok = FGNPreferences.getInstance().addCustomColorMap(currColorMap);
            }
            if(ok)
            {
               cmapCombo.addItem(currColorMap);
               cmapCombo.setSelectedItem(currColorMap);
            }
            else
               Toolkit.getDefaultToolkit().beep();
         }
         else if(src == rmvBtn)
         {
            ColorMap cm = (ColorMap) cmapCombo.getSelectedItem();
            boolean ok = cm != null && cm.isCustom();
            if(ok) ok = FGNPreferences.getInstance().removeCustomColorMap(cm);
            if(ok)
               cmapCombo.removeItem(cm);
            else
               Toolkit.getDefaultToolkit().beep();
         }
         else if(src == clipBtn)
         {
            ColorMap cm = ColorMap.fromClipboardContents();
            if(cm == null)
               Toolkit.getDefaultToolkit().beep();
            else
            {
               currColorMap = cm;
               refresh();
            }
         }
      }
      
      
      /** 
       * The current color map. This is the color map specified in the key frames table and displayed in the "swatch
       * panel". Its is not necessarily the map currently selected in the combo box, since the user may be in the 
       * process of defining a new map. 
       */
      private ColorMap currColorMap = null;
      
      /** Combo box selects a color map from the list of all built-in and custom color maps. */
      private final JComboBox<ColorMap> cmapCombo;
      /** Press this button to delete the map selected in the combo box (unless it is a built-in map). */
      private JButton rmvBtn = null;

      /** A custom-painted "swatch" panel depicting the current color map. */
      private JPanel cmapSwatch = null;
      
      /** Press this button to initialize current color map by parsing the contents of the system clip board. */
      private JButton clipBtn = null;
      /** The name of the current color map. */
      private JTextField nameField = null;
      /** Indicates number of key frames in the current color map. */
      private NumericTextField numFramesField = null;
      /** Table displaying the key frames (index + color) for the current color map. */
      private JTable keyFramesTable = null;
      /** Press this button to save the current color map (reflected in key frames table) as a new custom map. */
      private JButton addBtn = null;
      
      /** 
       * Simple mutable model for the key frames table lets the user alter the key frame LUT indices and colors via
       * in-place editors.
       */
      private class KeyFramesTM extends AbstractTableModel
      {
         @Override public int getRowCount() { return(currColorMap == null ? 0 : currColorMap.getNumKeyFrames()); }
         @Override public int getColumnCount() { return(2); }
         @Override public Class<?> getColumnClass(int c) { return(c==0 ? Integer.class : Color.class); }
         @Override public String getColumnName(int c) { return(c==0 ? "Index" : "Color"); }
         
         @Override public Object getValueAt(int r, int c)
         {
            Object out = null;
            if(currColorMap != null && r >= 0 && r<getRowCount() && c >= 0 && c < getColumnCount())
               out = (c==0) ? Integer.valueOf(currColorMap.getKeyFrameIndex(r)) : currColorMap.getKeyFrameColor(r);
            return(out);
         }
         
         @Override public boolean isCellEditable(int r, int c) 
         { 
            return(currColorMap != null && (c == 1 || (r != 0 && r != getRowCount()-1))); 
         }
         
         @Override public void setValueAt(Object value, int r, int c)
         {
            if(currColorMap == null) return;
            if(c == 1)
            {
               currColorMap = currColorMap.deriveColorMap(r, (Color) value);
               refresh();
            }
            else if(c == 0 && r > 0 && r < getRowCount()-1)
            {
               currColorMap = currColorMap.deriveColorMap(r, (Integer) value);
               refresh();
            }
         }
      }
      
      /** Simple column model intended to fix the width of the "Index" column at 60pix. */
      private static class KeyFramesTCM extends DefaultTableColumnModel
      {
         @Override public void addColumn(TableColumn column)
         {
            int nCols = getColumnCount();
            if(nCols == 0)
            {
               column.setPreferredWidth(60);
               column.setMinWidth(60);
               column.setMaxWidth(60);
               column.setResizable(false);
            }
            else
               column.setPreferredWidth(180);
            super.addColumn(column);
         }
      }
   }
}
