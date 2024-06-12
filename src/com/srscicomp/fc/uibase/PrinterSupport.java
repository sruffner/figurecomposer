package com.srscicomp.fc.uibase;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.print.CancelablePrintJob;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PrinterIsAcceptingJobs;
import javax.print.event.PrintJobEvent;
import javax.print.event.PrintJobListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingWorker;

import com.srscicomp.common.g2dviewer.Graph2DViewer;
import com.srscicomp.common.ui.JPreferredSizePanel;
import com.srscicomp.common.ui.NumericTextField;
import com.srscicomp.fc.fig.FGraphicModel;
import com.srscicomp.fc.fig.PSDoc;
import com.srscicomp.fc.fig.PSTransformable;

/**
 * TODO: Consider revising so that the three private dialogs are reused. Then clean up comments throughout.
 * 
 * <code>PrinterSupport</code> is a helper class encapsulating support for printing <em>DataNav</em> figures to a 
 * selected printer available on the host system. It uses the Java Print Service (JPS) API to locate any printer 
 * services that are available, and it provides substitutes for the standard "Page Setup" and "Print" dialogs of 
 * <code>PrinterJob</code> in the <code>java.awt.print</code> package -- since we cannot customize them to ask the user 
 * to print native vs. <em>Phyplot</em>-generated Postscript. It also maintains the notion of a "currently selected" 
 * printer. At startup, this is initialized to the host system's default printer.
 * 
 * <p><em>Mac OSX-specific notes</em>: 
 * <ul>
 * 	<li>[OBSOLETE - <em>DataNav</em> requires JRE v1.5.0 or better] Apple does not fully support JPS until its 
 *    J2SE5.0 (1.5.0) Release. JDK v1.4.2_09 does support some aspects of JPS, but only for printing Java2D graphics 
 *    (via <code>Printable</code> or <code>Pageable</code> interface). Thus, it is not possible to send a 
 *    <em>Phyplot</em>-generated PS page description to a PS printer under Mac OSX if the JRE is not 1.5.0 or better.</li>
 * 	<li>[OBSOLETE - <em>DataNav</em> requires JRE v1.5.0 or better] A serious bug exists when trying to use JPS 
 *    prior to the 1.5.0 release, causing <em>Phyplot</em> to deadlock during the second print job (something to do 
 *    with the background thread). To circumvent this bug, <code>PrinterSupport</code> checks the platform and 
 *    java.version number. If running on Mac OSX in a JRE prior to the 1.5.0 release, then we use the older 
 *    <code>PrinterJob</code> to execute the print job.  Also, this job is printed in foreground, blocking the GUI.</li>
 * 	<li>Under the 1.5.0 release, when we delivered a single-page Encapsulated Postscript page description (with the 
 * 	"EPSF-3.0" tag in the header line) as the print job, the job succeeds but merely prints a blank page. Removing 
 * 	the "EPSF-3.0" tag fixed the problem, which has something to do with filtering by the Common UNIX Printing System 
 * 	that is employed on Mac OSX and Linux. The tag is now omitted regardless of the platform.</li>
 *  	<li>Apple's implementation of the JPS (as of J2SE5.0 Rel 3) uses the human-readable description of the printer 
 * 	resource as the printer's real name. Technically, the underlying CUPS system has a <em>printers.conf</em> file 
 *    that includes, for each printer, an entry of the form: "<Printer realName> Info printerDesc ...</Printer>". 
 * 	Apple's JPS currently uses "printerDesc" as the printer resource's name instead of "realName". If "printerDesc" 
 * 	is not identical to "realName", the print job will fail. Therefore, it is imperative that users configure all 
 * 	printers so that "printerDesc" == "realName"!!! <strong>NOTE: Fixed as of J2SE5.0 Rel 6</strong>.</li>
 * </ul>
 * </p>
 * 
 * @author sruffner
 */
public class PrinterSupport
{
   /** The one and only instance of <code>PrinterSupport</code>. */
   private static PrinterSupport thePrinter = null;
   
   /**
    * Get the singleton instance of <em>DataNav</em> print support facility.
    * @return The singleton <code>PrinterSupport</code>.
    */
   public static PrinterSupport getInstance() 
   { 
      if(thePrinter == null) thePrinter = new PrinterSupport();
      return(thePrinter); 
   }
   

	/** Absolute resource pathname for file containing GUI properties used by <code>PrinterSupport</code>. */
	private final static String UIPROPS_FILE = "/com/srscicomp/fc/resources/printersupportui.properties";

	/** Properties of the page and print dialogs provided by <code>PrinterSupport</code>. Loaded at class-load time. */
	private static Properties uiProperties = null;
	static 
	{
		uiProperties = new Properties();
		try
		{
			uiProperties.load( PrinterSupport.class.getResourceAsStream(UIPROPS_FILE) );
		}
		catch( IOException ioe ) { }
	}

	
	/** The print services found on host system when this <code>PrinterSupport</code> was instantiated. */
	private PrintService[] prnServices = null;

	/**
	 * The currently selected printer. Initialized to the host system's default printer at construction time. If no 
	 * print services are available on the host, this is <code>null</code>.
	 */
	private PrintService prnServ = null;

	/**
	 * The current page format for all <em>DataNav</em> figures printed via <code>PrinterSupport</code>. Initialized to 
    * standard letter size (8.5x11in) with 1/2-in margins all around.
	 */
	private PageFormat prnPageFmt = null;

	/** Flag set once printer services have been queries and set up. Print support is unavailable until then. */
   private boolean initialized = false;

   /**
	 * Private constructor, since <code>PrinterSupport</code> is a singleton. The default page layout is initialized at 
	 * this time, and a background thread is started to compile a list of available print services and get the current 
	 * default printer. This task is done on the background because an offline network printer will hang the JVM for many 
	 * minutes, at least through JRE version 1.5.0_10. Print support is unavailable until this task is completed.
	 */
	private PrinterSupport()
	{
      SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {
         @Override protected Object doInBackground() throws Exception
         {
            prnServices = PrintServiceLookup.lookupPrintServices(null, null);
            prnServ = PrintServiceLookup.lookupDefaultPrintService();
            return(null);
         }
         @Override protected void done()
         {
            initialized = true;
         }
      };
      worker.execute();
      
		prnPageFmt = new PageFormat();
		Paper letterPaper = new Paper();
		letterPaper.setImageableArea(36, 36, 540, 720);
		prnPageFmt.setPaper(letterPaper);
	}

   /**
    * Is there at least one printer available on the host system? This method merely checks for the existence of at 
    * least one print service. It does not check whether the print service(s) is accepting print jobs.
    * @return <code>True</code> if a print service exists on the host system.
    */
   public boolean printerAvailable()
   {
      return(initialized && (prnServices.length > 0));
   }

   /**
	 * Get the list of all media sizes supported by the specified printer. 
	 * 
	 * @param prn The print service to be queried. May be <code>null</code>.
	 * @return List of names identifying the media sizes supported on the specified printer. If the print service is 
	 * <code>null</code>, or if it does not provide media size information, then the method returns an array of length 1 
    * containing the standard "letter" size, MediaSizeName.NA_LETTER.
	 */
	static public MediaSizeName[] getAvailableMediaSizesFor(PrintService prn)
	{
		List<Media> available = new ArrayList<Media>(10);

		if(prn != null)
		{
			Media[] media = (Media[]) prn.getSupportedAttributeValues(Media.class, null, null);
			if(media != null) for(int i=0; i<media.length; i++)
			{
				if((media[i] instanceof MediaSizeName) && (MediaSize.getMediaSizeForName((MediaSizeName) media[i]) != null))
					available.add(media[i]);
			}
		}

		if(available.size() == 0) available.add(MediaSizeName.NA_LETTER);

		MediaSizeName[] msn = new MediaSizeName[available.size()];
		for(int i=0; i<msn.length; i++) msn[i] = (MediaSizeName) available.get(i);
		return(msn);
	}

	/** 
	 * Get preferred units for the specified media size.
	 * 
	 * <p>Print media sizes are generally measured in inches or millimeters. This method assumes that millimeters are 
	 * the preferred units if both width and height are integral values in millimeters but not so in inches. Otherwise, 
	 * inches are the preferred units.</p>
	 * 
	 * @see MediaSize
	 * @param ms Media size.
	 * @return Defined constant indicating the preferred units: <code>MediaSize.MM</code> or <code>MediaSize.INCH</code>.
	 */
	public static int getPreferredUnitsForMediaSize(MediaSize ms)
	{
		double xMM = ms.getX(MediaSize.MM);
		double yMM = ms.getY(MediaSize.MM);
		double xIN = ms.getX(MediaSize.INCH);
		double yIN = ms.getY(MediaSize.INCH);
		boolean isIntegerMM = (Math.rint(xMM) == xMM && Math.rint(yMM) == yMM);
		boolean isIntegerIN = (Math.rint(xIN) == xIN && Math.rint(yIN) == yIN);
		return((isIntegerMM && !isIntegerIN) ? MediaSize.MM : MediaSize.INCH);
	}

	/**
	 * Change the paper size of the specified page layout. If possible, the imageable area on the page is adjusted so 
	 * that the margins remain the same. 
	 * 
	 * @param ms The new paper size.
	 * @param pgFmt The page layout to be modified.
	 * @return <code>False</code> if the current margins could <em>NOT</em> be preserved.
	 */
	private static boolean changePaperSize(MediaSize ms, PageFormat pgFmt)
	{
		// get the current imageable area and paper size, all in points (1/72in)
		Paper paper = pgFmt.getPaper();
		double w = paper.getWidth();
		double h = paper.getHeight();
		double xImg = paper.getImageableX();
		double yImg = paper.getImageableY();
		double wImg = paper.getImageableWidth();
		double hImg = paper.getImageableHeight();
			
		// calculate current margin values, which depend on orientation
		boolean isPortrait = (pgFmt.getOrientation() == PageFormat.PORTRAIT);
		double left = isPortrait ? xImg : h - yImg - hImg;
		double right = isPortrait ? w - xImg - wImg : yImg;
		double top = isPortrait ? yImg : xImg;
		double bottom = isPortrait ? h - yImg - hImg : w - xImg - wImg;

		// get the new paper width and height, in 1/72in
		w = ms.getX(MediaSize.INCH) * 72.0;
		h = ms.getY(MediaSize.INCH) * 72.0;

		// adjust imageable area dimensions to preserve margins, if possible
		boolean preservedMargins = true;
		double minW = isPortrait ? left + right : top + bottom;
		double minH = isPortrait ? top + bottom : left + right;
		if(minW >= w)
		{
			preservedMargins = false;
			xImg = 0.1 * w;
			wImg = 0.8 * w;
		}
		else
			wImg = w - minW;
		if(minH >= h)
		{
			preservedMargins = false;
			yImg = 0.1 * h;
			hImg = 0.8 * h;
		}
		else
			hImg = h - minH;

		// finally, update paper size and imageable area of our page layout
		paper.setSize(w, h);
		paper.setImageableArea(xImg, yImg, wImg, hImg);
		pgFmt.setPaper(paper);

		return(preservedMargins);
	}

	/**
	 * Get this <code>PrinterSupport</code>'s current page layout, which will be compatible with the currently selected 
	 * printer.
	 * @return A copy of the current page layout.
	 */
	public PageFormat getCurrentPageFormat() { return((PageFormat) prnPageFmt.clone()); }

	/**
	 * Convert the specified page layout to a set of <code>javax.print.attribute.PrintRequestAttribute</code>s. The page 
    * orientation is expressed as a <code>OrientationRequested</code> attribute and is restricted to portrait or 
    * landscape mode (reverse landscape and reverse portrait not supported). The paper dimensions are translated to a 
    * <code>MediaSizeName</code> attribute, if a matching media size exists. The page's imageable area is translated as 
    * a <code>MediaPrintableArea</code> attribute.
	 * 
	 * @param pf The page layout.
	 * @return Set of print attributes that describe the page layout.
	 */
	public static PrintRequestAttributeSet getPageFormatAsPrintRequestAttributeSet(PageFormat pf)
	{
		PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();

		// orientation -- only LANDSCAPE or PORTRAIT is supported
		pras.add( pf.getOrientation()==PageFormat.PORTRAIT ? 
							OrientationRequested.PORTRAIT : OrientationRequested.LANDSCAPE );

		// translate paper dimensions to a MediaSizeName, if we can
		Paper paper = pf.getPaper();
		MediaSizeName msn = 
         MediaSize.findMedia((float)(paper.getWidth()/72.0), (float)(paper.getHeight()/72.0), MediaSize.INCH);
		if(msn != null) pras.add(msn);

		// translate imageable area (accounts for margins) to a MediaPrintableArea
		MediaPrintableArea mpa = new MediaPrintableArea( 
			(float)(paper.getImageableX()/72.0), (float)(paper.getImageableY()/72.0), 
			(float)(paper.getImageableWidth()/72.0), (float)(paper.getImageableHeight()/72.0), MediaPrintableArea.INCH);
		pras.add(mpa);

		return(pras);
	}

	/**
	 * Present a modal dialog by which the user can change the current page layout.
	 * 
	 * @param m The graphic model to be displayed in the thumbnail preview of the page layout. If <code>null</code>, an 
    * empty graphic is used. If the model is currently registered in another viewer, a copy of the model is displayed 
    * instead.
    * @param frame The frame window on top of which the dialog should be displayed.
	 * @return The updated page layout, or <code>null</code> if the user cancelled out of the dialog, or if print support 
    * is currently unavailable.
	 */
	public PageFormat pageDialog(FGraphicModel m, Frame frame) 
	{
      if((!initialized) || prnServ == null)
      {
         String msg = initialized ? "No printers available" : "Still initializing print services";
         JOptionPane.showMessageDialog(frame, msg, "Printer unavailable", JOptionPane.WARNING_MESSAGE);
         return(null);
      }
      
		PageSetupDialog dlg = new PageSetupDialog(frame, m);
		dlg.pack();
		dlg.setLocationRelativeTo(frame);
		dlg.setVisible(true);

		return(!dlg.wasCancelled() ? (PageFormat)prnPageFmt.clone() : null);
	}

	/**
	 * Present a modal dialog by which the user can print one or more <i>DataNav</i> figures. The dialog permits the 
	 * user to change the current printer. If the selected printer is Postscript-compatible, the user also chooses 
	 * between sending a <em>DataNav</em>-generated Postscript document to the printer, or using the native Postscript 
	 * facilities. More advanced printer properties are not exposed by this dialog.
	 * <p>If the user does not cancel out of the dialog, a background thread is started to execute the print jobs (one
	 * per figure) while a simple message dialog displays print job progress and allows the user to cancel the job. Note,
	 * however, that a cancelled print job may still print if the job has already been delivered to the print queue.</p>
	 * @param figs The <i>DataNav</i> figure(s) to be printed. If null or zero length, no action taken. Any null entries
	 * in the array are ignored.
    * @param frame The frame window on top of which the dialog should be displayed.
	 */
	public void printDialog(FGraphicModel[] figs, Frame frame)
	{
	   int count = 0;
	   if(figs != null)
	   {
	      for(int i=0; i<figs.length; i++) if(figs[i] != null) ++count;
	   }
      if(count == 0) return;
      
      FGraphicModel[] nonNullFigs = new FGraphicModel[count];
      int j=0;
      for(int i=0; i<figs.length; i++) if(figs[i] != null)
         nonNullFigs[j++] = figs[i];

		// if there are no print services installed on the host system, bag it
		if((!initialized) || prnServ == null)
		{
         String msg = initialized ? "No printers available" : "Still initializing print services";
         JOptionPane.showMessageDialog(frame, msg, "Printer unavailable", JOptionPane.WARNING_MESSAGE);
         return;
		}
      
		// if the user changes the printer and this forces a significant change in the page format, then we want the 
		// changes to affect THIS PRINT JOB ONLY. So remember the old printer and page format.
		lastPrn = prnServ;
		lastPgFmt = (PageFormat) prnPageFmt.clone();

		// present the PrintDialog to get user's choice of printer, print range
		PrintDialog dlg = new PrintDialog(frame, nonNullFigs);
		dlg.pack();
		dlg.setLocationRelativeTo(frame);
		dlg.setVisible(true);
	}



   /**
    * <code>PrintDialog</code> is a simple dialog that lets the user select a printer available on the host system, then
    * print one or more figures on the selected printer. 
    * 
    * <p>If the targeted printer is Postscript-compatible, the user is given the choice of sending either a
    * <em>DataNav</em>-generated Postscript stream, or letting the native OS translate Java2D graphics into PS (typ. 
    * via a Postscript driver of some sort, the implementation of which is platform-dependent). The latter choice will 
    * yield printed output that is more WYSIWYG, but the print job is generally much larger and slower to print.</p>
    * 
    * <p><em>Mac OSX-specific</em>: JDK 1.4.* releases for Mac OSX do not fully support the Java Print Service API.  
    * By 1.4.2_09 we can query and select different printers, but we are still unable to determine whether that printer 
    * supports Postscript. If a Mac OSX user wishes to send <em>Phyplot</em> PS to the target printer, he must run 
    * <em>Phyplot</em> under JDK 1.5+.</p>
    * 
    * <p>NOTE: The Java Print API provides access to the native OS print dialog as well as a cross-platform version.  
    * However, in J2SE 1.4.2, the native OS dialog interferes with repainting of the application and does not act 
    * modally. In addition, neither dialog allows for tailoring its appearance in the way <code>PrintDialog</code> 
    * does.</p>
    * 
    * @author sruffner
    */
   class PrintDialog extends JDialog implements ActionListener
   {
      private static final long serialVersionUID = 1L;
 
      // some labels on the dialog
      private final static String TITLE = "Print";
      private final static String PRINTER_LABEL = "Printer";
      private final static String NAME_LABEL = "Name:";
      private final static String ACCEPT_LABEL = "Accepting jobs";
      private final static String PS_LABEL = "Postscript-compatible";
      private final static String USENATIVEPS_LABEL = "Use native Java2D->PS converter?";
      private final static String OK = "OK";
      private final static String CANCEL = "Cancel";

      /** Current choice for printer service. */
      private JComboBox<String> cbPrinter = null;

      /** Static label with icon that indicates whether or not current printer is accepting jobs. */
      private JLabel lblAcceptingJobs = null;

      /** Static label with icon that indicates whether or not current printer is Postscript-compatible. */
      private JLabel lblPostscript = null;

      /**
       * If checked, then native Java2D-to-PS converter is used rather than <em>Phyplot</em>-generated Postscript code. 
       * Enabled only if printer is Postscript-compatible.
       */
      private JCheckBox chkNativePS = null;

      /** The figure(s) being printed. */
      private FGraphicModel[] figures = null;

      /**
       * Construct a modal <code>PrintDialog</code> to select a printer and specify several other attributes of a print 
       * job. To use the dialog, pack it and make it visible after invoking this constructor.
       * @param f Owner frame for dialog.
       * @param figs The figure(s) to be printed.
       */
      PrintDialog(Frame f, FGraphicModel[] figs)
      {
         super(f, TITLE, true);
         setResizable(false);
         setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
         
         figures = figs;
         
         // there should be at least one print service available, else we should never get here
         if(prnServices.length == 0)
            throw new Error( "Internal error: PrintDialog raised when no print services are available!" );

         // create combo box for selecting printer 
         String[] psNames = new String[prnServices.length];
         int nCurrServ = 0;
         for(int i=0; i<psNames.length; i++) 
         {
            psNames[i] = prnServices[i].getName();
            if(prnServ.equals(prnServices[i])) nCurrServ = i;
         }
         cbPrinter = new JComboBox<String>(psNames);
         cbPrinter.setSelectedIndex(nCurrServ);
         cbPrinter.setActionCommand(NAME_LABEL);
         cbPrinter.addActionListener(this);

         // create labels reflecting printer status/info:  accepting jobs, Postscript-compatible
         lblAcceptingJobs = new JLabel(ACCEPT_LABEL);
         lblPostscript = new JLabel(PS_LABEL);

         // create "Use native Java2D-to-PS converter" check box, initially unchecked.
         chkNativePS = new JCheckBox(USENATIVEPS_LABEL);
         chkNativePS.setActionCommand(USENATIVEPS_LABEL);

         // update state of all widgets IAW properties of selected printer
         updateUIOnPrinterChange();

         // the OK and Cancel buttons
         JButton okBtn = new JButton(OK);
         okBtn.setActionCommand(OK);
         okBtn.addActionListener(this);
         getRootPane().setDefaultButton(okBtn);
      
         JButton cancelBtn = new JButton(CANCEL);
         cancelBtn.setActionCommand(CANCEL);
         cancelBtn.addActionListener(this);

         //
         // layout dialog content pane in two panels
         //

         Dimension hSpace10 = new Dimension(10, 0);
         Dimension hSpace5 = new Dimension(5, 0);

         // "Printer" panel contains widgets for selecting printer and reporting its status
         JPanel column1 = new JPanel();
         column1.setLayout(new GridLayout(0, 1, 0, 2));
         column1.add(new JLabel(NAME_LABEL, JLabel.TRAILING));
         column1.add(Box.createRigidArea(hSpace5));
         column1.add(Box.createRigidArea(hSpace5));
         column1.add(Box.createRigidArea(hSpace5));
         
         JPanel column2 = new JPanel();
         column2.setLayout(new GridLayout(0, 1, 0, 2));
         column2.add(cbPrinter);
         column2.add(lblAcceptingJobs);
         column2.add(lblPostscript);
         column2.add(chkNativePS);

         JPanel printerPanel = new JPanel();
         printerPanel.setLayout(new BoxLayout(printerPanel, BoxLayout.LINE_AXIS));
         printerPanel.add(column1);
         printerPanel.add(Box.createRigidArea(hSpace5));
         printerPanel.add(column2);
         printerPanel.setBorder(BorderFactory.createCompoundBorder( 
            BorderFactory.createTitledBorder(PRINTER_LABEL), BorderFactory.createEmptyBorder(2,2,2,2)));

         // panel holding the "OK" and "Cancel" buttons
         JPanel okPanel = new JPreferredSizePanel();
         okPanel.setLayout(new BoxLayout(okPanel, BoxLayout.LINE_AXIS));
         okPanel.add(okBtn);
         okPanel.add(Box.createRigidArea(hSpace10));
         okPanel.add(cancelBtn);

         // now arrange above panels in one vertical column: printer panel, ok/cancel btn panel
         printerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
         okPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

         JPanel content = new JPanel();
         content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
         content.add(printerPanel);
         content.add(Box.createRigidArea(new Dimension(0, 10)));
         content.add(okPanel);
         content.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

         getContentPane().add(content, BorderLayout.CENTER);
      }

      /** 
       * Should the print job specified on this <code>PrintDialog</code> use the native Java2D graphics-to-Postscript 
       * conversion facility rather than <em>Phyplot</em> Postscript? This option is supported only if the selected 
       * printer is Postscript-compatible. In typical usage, this method is called after the dialog has closed, unless 
       * the user cancelled the operation.
       * 
       * @return <code>True</code> if current printer is Postscript-compatible and the "Use native Java2D to PS 
       * conversion" check box is checked.
       */
      public boolean useNativePS()
      {
         return(prnServ.isDocFlavorSupported(DocFlavor.INPUT_STREAM.POSTSCRIPT) && chkNativePS.isSelected());
      }

      /**
       * Update state of all widgets on this <code>PrintDialog</code> commensurate with the properties of the currently 
       * selected printer. This method is typically called when the printer is changed.
       */
      private void updateUIOnPrinterChange()
      {
         // set icon that indicates whether or not current printer supports Postscript
         boolean isPSCompatible = prnServ.isDocFlavorSupported(DocFlavor.INPUT_STREAM.POSTSCRIPT);
         Icon icon = isPSCompatible ? FCIcons.V4_OK_16 : FCIcons.V4_NOTOK_16;
         lblPostscript.setIcon(icon);

         // set icon that indicates whether or not current printer is accepting jobs.  If unable to determine, an 
         // "unknown" icon is used.
         PrinterIsAcceptingJobs accepting = (PrinterIsAcceptingJobs) prnServ.getAttribute(PrinterIsAcceptingJobs.class);
         icon = FCIcons.V4_HELP_16;
         if(accepting != null)
            icon = (accepting == PrinterIsAcceptingJobs.ACCEPTING_JOBS) ? FCIcons.V4_OK_16 : FCIcons.V4_NOTOK_16;
         lblAcceptingJobs.setIcon(icon);

         // choosing the native Java2D->PS converter is relevant only if printer is PS-compatible.
         if(!isPSCompatible) chkNativePS.setSelected(false);
         chkNativePS.setEnabled(isPSCompatible);
      }

      public void actionPerformed(ActionEvent e)
      {
         String cmd = e.getActionCommand();
         if(cmd.equals(OK) || cmd.equals(CANCEL))
            extinguish(cmd.equals(CANCEL));
         else if(cmd.equals(NAME_LABEL))
         {
            // user changed current printer
            int sel = cbPrinter.getSelectedIndex();
            PrintService prn = (PrintService) prnServices[ sel ];
            if(!prn.equals(prnServ))
            {
               prnServ = prn;
               updateUIOnPrinterChange();
            }
         }
      }
      
      private void extinguish(boolean cancel)
      {
         Frame frame = (Frame) getOwner();
         setVisible(false);
         dispose();
         if(cancel) return;
         
         // set up print request attributes for the print job to reflect paper size, orientation, and margins. For Java2D 
         // printing, the page format attributes will be used to set the clipping rectangle correctly and get the 
         // orientation correct.
         PrintRequestAttributeSet printAttrs = PrinterSupport.getPageFormatAsPrintRequestAttributeSet(prnPageFmt);

         
         // start the print job in background thread while blocking GUI with modal dialog that displays progress messages 
         // and offers a "Cancel" option
         boolean psNative = prnServ.isDocFlavorSupported(DocFlavor.INPUT_STREAM.POSTSCRIPT) && chkNativePS.isSelected();
         PrintJobThread worker = new PrintJobThread(figures, printAttrs, psNative);
         PrintProgressDlg msgDlg = new PrintProgressDlg(frame, worker);
         msgDlg.pack();
         msgDlg.setLocationRelativeTo(frame);
         worker.execute();
         msgDlg.setVisible(true);
      }
   }

  /**
    * This simple modal message dialog blocks the user interface while a <code>PrintJobThread</code> prepares and sends
    * a print job to print a <em>DataNav</em> figure.
    * <p>The <code>PrintJobThread</code> updates the dialog's message (on the event dispatch thread) to report progress.
    * During this time, a <em>Cancel</em> button is available to cancel the print job if it hangs. The job is most 
    * likely to hang while waiting for a "job complete" message from the printer service, and a cancellation at that 
    * point may not actually cancel the job; but at least the user can resume normal GUI operations. If the print job
    * fails for any reason, the dialog displays an error message along with the standard "OK" confirmation button to 
    * dismiss the dialog after reading the message. If print job succeeds, the dialog extinguishes itself and normal GUI
    * operations can resume.</p>
    * @author sruffner
    */
   private class PrintProgressDlg extends JDialog implements ActionListener
   {
      private static final long serialVersionUID = 1L;

      private String CANCEL_LABEL = "Cancel";
      private String DISMISS_LABEL = "OK";
      
      PrintProgressDlg(Frame owner, PrintJobThread pjt) 
      { 
         super(owner, true); 
         setUndecorated(true);
         setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
         msgLabel = new JLabel(STARTING_JOB, JLabel.LEADING);
         confirmBtn = new JButton(CANCEL_LABEL);
         confirmBtn.addActionListener(this);

         JPanel p = new JPanel();
         p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
         p.add(Box.createHorizontalStrut(400));
         p.add(Box.createVerticalStrut(50));
         p.add(msgLabel);
         p.add(Box.createVerticalStrut(FCIcons.UIGAPSZ*4));
         p.add(confirmBtn);
         p.add(Box.createVerticalStrut(50));
         msgLabel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
         confirmBtn.setAlignmentX(JPanel.CENTER_ALIGNMENT);
         p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK),
               BorderFactory.createRaisedBevelBorder()));
         
         add(p, BorderLayout.CENTER);
         
         printTask = pjt;
         if(printTask != null) printTask.setProgressDlg(this);
      }
      
      private JLabel msgLabel = null;
      private JButton confirmBtn = null;

      private PrintJobThread printTask = null;
      
      private void printJobUpdate(String msg, boolean done)
      {
         if(!done)
            msgLabel.setText(msg);
         else if(msg != null)
         {
            msgLabel.setText(msg);
            confirmBtn.setText(DISMISS_LABEL);
         }
         else
            setVisible(false);
      }
      
      public void actionPerformed(ActionEvent e)
      {
         Object src = e.getSource();
         if(src == confirmBtn)
         {
            setVisible(false);
            if(printTask != null) printTask.cancel(true);
         }
      }
   }


	private final static String STARTING_JOB = "Starting print job...";
	private final static String PS_BUILDING = "Building page description...";
	private final static String SENDING = "Sending print data...";
	private final static String PRNTOFILE = "Printing to file...";

	/**
	 * The print service that was current when the <code>PrinterSupport.PrintDialog</code> was raised. The user may 
    * choose a different printer, which may force a change in page layout. If this happens, the original printer and 
    * page layout are restored when the print job terminates.
	 */
	private PrintService lastPrn = null;

	/**
	 * The page layout that was current when the <code>PrinterSupport.PrintDialog</code> was raised. The user may choose 
    * a different printer, which may force a change in page layout. If this happens, the original printer and page 
    * layout are restored when the print job terminates.
	 */
	private PageFormat lastPgFmt = null;

	/**
	 * <code>PrintJobThread</code> encapsulates the worker thread which generates and sends one or more print jobs to 
	 * the printer.
	 * 
	 * <p>[NOTE: This warning is obsolete because <em>DataNav</em> requires JVM1.5+.] <em>Mac OSX-specific</em>: Do 
	 * <strong>NOT</strong> use this thread under Mac OSX if the JVM version is less than 1.5.0. Full support for the 
	 * Java Print Service API (javax.print) was not introduced until 1.5.0. While the PS API is available in 1.4.*, it 
	 * does not work correctly. One particularly bad problem: <em>Phyplot</em> would hang forever after calling 
	 * <code>DocPrintJob.print()</code>, for reasons unknown. This almost always happened after one successful print job.
	 * The deadlock does not occur in JVM 1.5+.</p>
	 * 
	 * <p><em>DataNav</em>-generated PS stream will NOT include the "EPSF-3.0" tag -- because this causes problems with 
	 * printing under CUPS (Common UNIX Printing System), which is used on both MacOSX and Linux platforms.</p>
	 * 
	 * @author 	sruffner
	 */
	private class PrintJobThread extends SwingWorker<Object, String> implements PrintJobListener
	{
	   PrintJobThread(FGraphicModel[] figures, PrintRequestAttributeSet attrs, boolean useNativePS)
	   {
	      this.figures = figures;
	      printAttrs = attrs;
	      this.useNativePS = useNativePS;
	   }
	   
      /**
       * Here's where the Java print job (or jobs, if more than one figure is to be printed) is actually prepared and 
       * sent to the printer. The method does not return until all print jobs complete or an error is detected, or the
       * operation is cancelled.
       */
	   @Override protected Object doInBackground() throws Exception
      {
         boolean doesPS = prnServ.isDocFlavorSupported(DocFlavor.INPUT_STREAM.POSTSCRIPT);
         boolean doesJava2D = prnServ.isDocFlavorSupported(DocFlavor.SERVICE_FORMATTED.PRINTABLE);
         if(!(doesPS || doesJava2D))
         {
            errMsg = "Printer not compatible.";
            return(errMsg);
         }

         publish(STARTING_JOB);
         
         // if both formats are supported, we opt for PS by default. However, if the user wants native Java2D-to-PS 
         // conversion, then we must print through the Printable DocFlavor. Native print service will automatically 
         // convert the Java2D graphics to PS code.
         boolean doPSJob = doesPS;
         if(doesPS && doesJava2D && useNativePS) doPSJob = false;

         // run a separate print job for each figure
         for(int i=0; i<figures.length; i++)
         {
            publish("Printing " + (i+1) + " of " + figures.length + " figures...");
            FGraphicModel graphic = figures[i];
            
            // create the print job and register to listen for print job events
            printJob = prnServ.createPrintJob(); 
            if(printJob == null)
            {
               errMsg = "Unable to open print job!";
               return( errMsg );
            }
            printJob.addPrintJobListener(this);

            prnJobDone = false;
            
            try
            {
               if(doPSJob)
               {
                  PSTransformable pst = graphic.getRoot();
                  publish(PS_BUILDING);
                  PSDoc psDoc = PSDoc.createPostscriptDoc(pst, prnPageFmt, false);
                  publish(PRNTOFILE);
                  
                  is = psDoc.asInputStream(); 
                  Doc simpleDoc = new SimpleDoc(is, DocFlavor.INPUT_STREAM.POSTSCRIPT, null);
                  printJob.print(simpleDoc, printAttrs);
               }
               else
               {
                  // our Phyplot document serves as the Java2D-compatible Printable!
                  publish(SENDING);
                  Doc simpleDoc = new SimpleDoc(graphic, DocFlavor.SERVICE_FORMATTED.PRINTABLE, null);
                  printJob.print(simpleDoc, printAttrs);
               }

               // WAIT UNTIL PRINT JOB FINISHES OR WE GET INTERRUPTED BY USER CANCEL!
               try{ while(!(isCancelled() || isCurrentPrnJobDone())) Thread.sleep(100); }
               catch(InterruptedException ie) {}
            }
            catch(UnsupportedOperationException uoe)
            {
               errMsg = "Postscript error: " + uoe.getMessage();
            }
            catch(IOException ioe)
            {
               errMsg = "I/O error: " + ioe.getMessage();
            }
            catch(PrintException pe)
            {
               errMsg = "Print error: " + pe.getMessage();
            }
            catch(Throwable t)
            {
               errMsg = "Unexpected failure: " + t.getMessage();
            }
            finally
            {
               // we'll no longer listen to any more events from print job
               if(printJob != null) printJob.removePrintJobListener(this);

               // if user cancelled and job is cancellable, we do so in a separate thread and hope it works out!
               if((printJob instanceof CancelablePrintJob) && (!prnJobDone) && isCancelled())
               {
                  final DocPrintJob job = printJob;
                  Thread cancelThread = new Thread() {
                     public void run() {
                        try
                        {
                           ((CancelablePrintJob)job).cancel();
                        }
                        catch(Throwable t) { }
                     }
                  };
                  cancelThread.setDaemon(true);
                  cancelThread.start();
               }

               // make sure stream that fed print job is closed (SimpleDoc does not do this)
               if(is != null)
               {
                  try {is.close(); is = null;} catch(IOException ioe) {}
               }

               printJob = null;
            }
            
            if(isCancelled()) errMsg = "User cancelled.";
            
            if(errMsg != null) break;
         }
         
         return(null);            // return value not used
      }

      @Override protected void process(List<String> chunks)
      {
         if(chunks == null || progressDlg == null) return;
         for(String s : chunks) progressDlg.printJobUpdate(s, false);
      }

      @Override protected void done()
      {
         // if user changed printer for this print job and it forced a change in paper size, restore old printer & page 
         // layout. Otherwise, the selected printer becomes the current one.
         if(!prnServ.equals(lastPrn))
         {
            Paper oldPaper = lastPgFmt.getPaper();
            Paper currPaper = prnPageFmt.getPaper();
            if(oldPaper.getWidth() != currPaper.getWidth() || oldPaper.getHeight() != currPaper.getHeight())
            {
               prnServ = lastPrn;
               prnPageFmt = lastPgFmt;
            }
         }

         if(progressDlg != null)
            progressDlg.printJobUpdate(errMsg, true);

         // release resources
         for(int i=0; i<figures.length; i++) figures[i] = null;
         figures = null; 
         printAttrs = null;
         printJob = null;
         lastPrn = null;
         lastPgFmt = null;
      }

      /**
	    * List of <em>DataNav</em> figure to be printed. Each figure serves as the <code>Printable</code> target when 
	    * printing a figure as Java2D graphics (rather than using <em>DataNav</em>-generated Postscript). The reference 
	    * to each figure is released as soon as the corresponding print job terminates.
	    */
	   private FGraphicModel[] figures = null;

	   /**
	    * If set, this flag tells the <code>PrintJobThread</code> to use native Java2D-to-Postscript conversion, rather 
	    * than sending <em>DataNav</em>-generated PS code to the targeted printer. It is relevant only if the printer is 
	    * PS-compatible.
	    */
	   private boolean useNativePS = false;

	   /** Print job attributes to be submitted with each print job. */
	   private PrintRequestAttributeSet printAttrs = null;

	   /** Print job progress messages are posted to this dialog on the event dispatch thread. */
	   private PrintProgressDlg progressDlg = null;
	   
		/** The current print job. */
		private DocPrintJob printJob = null;

		/** Error message initialized if print job fails for some reason; otherwise, it is <code>null</code>. */
		private String errMsg = null;

		/** Print data stream for Postscript print jobs using <em>DataNav</em>-generated PS. */
		InputStream is = null;

		/**
		 * This flag set if we get confirmation that the current print job went to completion -- ie, if we get a job 
		 * completed, job failed, job cancelled, or no more job events message.
		 */
		private boolean prnJobDone = false;

      /**
       * Set flag to indicate that the current  print job has completed in some fashion: the job actually finished, 
       * user cancelled it, job failed, or a "no more job events" message was received.
       * <p>The <code>PrintJobListener</code> implementation uses this method to release the worker thread from its 
       * infinite wait loop.</p>
       */
		private synchronized void setCurrentPrnJobDone() { prnJobDone = true; }

      /**
       * Is the current print job done?
       * @return <code>True</code> if current print job is done.
       */
      public synchronized boolean isCurrentPrnJobDone() { return(prnJobDone); }

      void setProgressDlg(PrintProgressDlg dlg) { progressDlg = dlg; }
      
		public void printDataTransferCompleted(PrintJobEvent pje) 
      { 
         publish("Print data transfer complete...");
		}
		public void printJobCompleted(PrintJobEvent pje) { setCurrentPrnJobDone(); }
		public void printJobFailed(PrintJobEvent pje)
		{
			errMsg = "Print job failed for unknown reason.";
			setCurrentPrnJobDone();
		}
		public void printJobCanceled(PrintJobEvent pje) { setCurrentPrnJobDone(); }
		public void printJobNoMoreEvents(PrintJobEvent pje) { setCurrentPrnJobDone(); }
		public void printJobRequiresAttention(PrintJobEvent pje)
		{
			publish("Printer may require attention!");
		}
	}


	/**
	 * <code>PageSetupDialog</code> is a simple dialog that lets user modify the current page layout for the 
    * <code>PrinterSupport</code> object, specifying the size, orientation, and margins of the printed page. The user 
    * can only choose from among those paper sizes that are supported on the currently selected printer. If there is 
    * no printer on the host system, the only available paper size is 8.5x11in letter. If there are multiple printers 
    * available, a combo box is included for changing the current printer -- since different printers may support 
    * different paper sizes.
	 * 
	 * <p>NOTE: The Java Print API provides access to the native OS page setup dialog as well as a cross-platform 
	 * version.  However, in J2SE 1.4.2, both dialogs have serious bugs that prohibit their use. The native OS dialog, 
	 * invoked by <code>PrinterJob.pageDialog(PageFormat)</code>, interferes with repainting of the application and does 
    * not act modally. The cross-platform dlg, invoked by <code>PrinterJob.pageDialog(PrintRequestAttributeSet)</code>, 
    * returns an erroneous <code>PageFormat</code> (the imageable area of the page is slightly larger than specified 
    * because of a roundoff error). These are both known bugs that MAY be addressed in J2SE 5.0, the latest release of 
    * Java as of 25Oct2004. MORE IMPORTANTLY, we decided to stick with this custom page setup dialog because it includes
    * a thumbnail preview that shows the figure's bounding box on the printed page!</p>
	 * 
	 * @author sruffner
	 */
	class PageSetupDialog extends JDialog implements ActionListener
	{
      private static final long serialVersionUID = 1L;

      private final static String TITLE = "Page Setup";
		private final static String OK = "OK";
		private final static String CANCEL = "Cancel";
		private final static String PAPER_LABEL = "Paper";
		private final static String PRINTER_LABEL = "Printer:";
		private final static String SIZE_LABEL = "Size:";
		private final static String ORI_LABEL = "Orientation:";
		private final static String MARGIN_IN_LABEL = "Margins (inches)";
		private final static String MARGIN_MM_LABEL = "Margins (mm)";
		private final static String LEFT_LABEL = "left";
		private final static String RIGHT_LABEL = "right";
		private final static String TOP_LABEL = "top";
		private final static String BOTTOM_LABEL = "bottom";

		private final static int THUMBNAIL_MAXDIM = 140;

		/** The page layout parameters as modified by this <code>PageSetupDialog</code>. */
		private PageFormat pageFormat = null;

		/**
		 * The print service that is selected in this <code>PageSetupDialog</code>. If the host system has multiple 
       * printers available, the user may choose a different printer which supports the desired paper size.
		 */
		private PrintService printer = null;

		/** Flag indicating whether or not the user cancelled out of the dialog. */
		private boolean wasCancelled = false;

		/** A thumbnail preview of the current page layout. */
		private Graph2DViewer thumbnail = null;

		/**
		 * Current choice for printer service. Changing the printer service may affect the list of available media sizes.
		 * Shown ONLY if there are multiple printers available.
		 */
		private JComboBox<String> cbPrinter = null;

		/** Combo box displays the available media sizes for the currently selected printer. */
		private JComboBox<String> cbMediaSize = null;

		/**
		 * The array of available media sizes for the currently selected printer. There is a one-to-one correspondence 
		 * between this list and the media size labels appearing as items in the dialog's "paper size" combo box.
		 */
		private MediaSize[] availableMediaSizes = null;

		/**
		 * Current "best" units in which to display/edit margins, either <code>MediaSize.INCH</code> or 
		 * <code>MediaSize.MM</code>. Will vary with the currently selected paper size.
		 */
		private int currentUnits = MediaSize.INCH;

		/** Radio button selects the "Portrait" orientation. */
		private JRadioButton portraitBtn = null;

		/** Radio button selects the "Landscape" orientation. */
		private JRadioButton landscapeBtn = null;

		/** Numeric text field for editing the left margin. */
		private NumericTextField tfLeft = null;

		/** Numeric text field for editing the right margin. */
		private NumericTextField tfRight = null;

		/** Numeric text field for editing the top margin. */
		private NumericTextField tfTop = null;

		/** Numeric text field for editing the bottom margin. */
		private NumericTextField tfBottom = null;

		/** 
       * Panel containing the four margins widget. Its title is adjusted to display the units of measure for the
		 * margins, either "in" or "mm" depending upon the current paper size selected.
		 */
		private JPanel marginsPanel = null;


		/**
		 * Construct a modal <code>PageSetupDialog</code> to modify the current page layout in the enclosing  
       * <code>PrinterSupport</code> instance, while ensuring the layout remains compatible with the currently selected 
       * printer. To use the dialog, pack it and make it visible after invoking this constructor.
		 * 
		 * @param f Owner frame for dialog.
		 * @param model The <em>Phyplot</em> graphic model to be previewed in the thumbnail canvas displayed on the 
       * dialog. A <em>copy</em> of this model is previewed, stripped of all data to ensure it is rendered quickly. If 
       * <code>null</code>, a blank graphic is used.
		 */
		PageSetupDialog(Frame f, FGraphicModel model)
		{
			super(f, TITLE, true);
			setResizable(false);

			// make a copy of the currently selected page format.  We'll manipulate the copy, then make it the current 
			// layout once this dialog is extinguished -- unless the user cancels.
			pageFormat = (PageFormat) prnPageFmt.clone();

			// the currently selected printer, which may be manipulated on this dialog.  Initialized to whatever is the 
			// current printer at the moment the dialog is constructed.
			printer = prnServ;

			// create thumbnail canvas for previewing the page layout.  The canvas is not interactive. 
			thumbnail = new Graph2DViewer(false);
			thumbnail.setPageFormat(pageFormat);
         thumbnail.setPrintPreviewEnabled(true);
			setThumbnailSize();

			// if a figure is provided for preview, install it in the preview canvas. However, if it is already installed
			// in another viewer, we must make a copy. If no figure provided, we'll use a default empty figure.
         FGraphicModel thumbGraphic = null;
         if(model != null) thumbGraphic = (model.getViewer() != null) ? FGraphicModel.copy(model) : model;
         if(thumbGraphic == null) thumbGraphic = new FGraphicModel();

			// display preview graphic on thumbnail canvas by installing the model that contains it. 
			thumbnail.setModel(thumbGraphic);

			// if there are multiple printers available on host, create combo box for selecting one and initialize it to 
			// the currently selected printer.
			if(prnServices.length > 0)
			{
				String[] psNames = new String[prnServices.length];
				int nCurrServ = 0;
				for(int i=0; i<psNames.length; i++) 
				{
					psNames[i] = prnServices[i].getName();
					if(prnServ.equals(prnServices[i])) nCurrServ = i;
				}
				cbPrinter = new JComboBox<String>(psNames);
				cbPrinter.setSelectedIndex(nCurrServ);
				cbPrinter.setActionCommand(PRINTER_LABEL);
				cbPrinter.addActionListener(this);
			}
			else
				cbPrinter = null;

			// create, initialize, and hook up widgets to display editable parameters in the PageSetup
			cbMediaSize = new JComboBox<String>();
			reloadPaperSizes();
			cbMediaSize.setActionCommand(SIZE_LABEL);
			cbMediaSize.addActionListener(this);

			tfLeft = new NumericTextField(0, 99, 4, 2);
         tfLeft.setValue(getMargin(LEFT_LABEL));
			tfLeft.setActionCommand(LEFT_LABEL);
			tfLeft.addActionListener(this);

			tfRight = new NumericTextField(0, 99, 4, 2);
			tfRight.setValue(getMargin(RIGHT_LABEL));
			tfRight.setActionCommand(RIGHT_LABEL);
			tfRight.addActionListener( this );

			tfTop = new NumericTextField(0, 99, 4, 2);
			tfTop.setValue(getMargin(TOP_LABEL));
			tfTop.setActionCommand(TOP_LABEL);
			tfTop.addActionListener( this );

			tfBottom = new NumericTextField(0, 99, 4, 2);
			tfBottom.setValue(getMargin(BOTTOM_LABEL));
			tfBottom.setActionCommand(BOTTOM_LABEL);
			tfBottom.addActionListener( this );

			portraitBtn = new JRadioButton("Portrait");
			portraitBtn.setSelected(pageFormat.getOrientation() == PageFormat.PORTRAIT);
			portraitBtn.addActionListener(this);

			landscapeBtn = new JRadioButton("Landscape");
			landscapeBtn.setSelected(pageFormat.getOrientation() == PageFormat.LANDSCAPE);
			landscapeBtn.addActionListener(this);

			ButtonGroup btnGroup = new ButtonGroup();
			btnGroup.add(portraitBtn);
			btnGroup.add(landscapeBtn);

			JButton okBtn = new JButton(OK);
			okBtn.setActionCommand(OK);
			okBtn.addActionListener(this);
			getRootPane().setDefaultButton(okBtn);
		
			JButton cancelBtn = new JButton(CANCEL);
			cancelBtn.setActionCommand(CANCEL);
			cancelBtn.addActionListener(this);


			Dimension hSpace10 = new Dimension(10,0);
			Dimension hSpace5 = new Dimension(5,0);

			// "Paper" panel containing widgets for selecting paper size and orientation. The combo box for selecting the 
			// current printer is included here as well (if there's more than one printer to choose from).
			JPanel column1 = new JPanel();
			column1.setLayout(new GridLayout(0, 1, 0, 2));
			if(cbPrinter != null) column1.add(new JLabel(PRINTER_LABEL, JLabel.TRAILING));
			column1.add(new JLabel(SIZE_LABEL, JLabel.TRAILING));
			column1.add(new JLabel(ORI_LABEL, JLabel.TRAILING));

			JPanel column2 = new JPanel();
			column2.setLayout(new GridLayout(0, 1, 0, 2));
			if(cbPrinter != null) column2.add(cbPrinter);
			column2.add(cbMediaSize);
			JPanel lineGroup = new JPreferredSizePanel();
			lineGroup.setLayout( new BoxLayout(lineGroup, BoxLayout.LINE_AXIS));
			lineGroup.add(portraitBtn);
			lineGroup.add(Box.createRigidArea(hSpace5));
			lineGroup.add(landscapeBtn);
			column2.add(lineGroup);

			JPanel paperPanel = new JPanel();
			paperPanel.setLayout(new BoxLayout(paperPanel, BoxLayout.LINE_AXIS));
			paperPanel.add(column1);
			paperPanel.add(Box.createRigidArea(hSpace5));
			paperPanel.add(column2);
			paperPanel.setBorder(BorderFactory.createCompoundBorder( 
				BorderFactory.createTitledBorder(PAPER_LABEL), BorderFactory.createEmptyBorder(2,2,2,2)));

			// "Margins" panel containing widgets for defining page margins
			column1 = new JPanel();
			column1.setLayout(new GridLayout(0, 1, 0, 2));
			column1.add(new JLabel( LEFT_LABEL, JLabel.TRAILING));
			column1.add(new JLabel( TOP_LABEL, JLabel.TRAILING));

			column2 = new JPanel();
			column2.setLayout(new GridLayout(0, 1, 0, 2));
			JPanel tfPanel = new JPanel(new BorderLayout());		// so that text field doesn't grow to fit extra space
			tfPanel.add(tfLeft, BorderLayout.WEST);
			column2.add(tfPanel);
			tfPanel = new JPanel(new BorderLayout()); 
			tfPanel.add(tfTop, BorderLayout.WEST);
			column2.add(tfPanel);

			JPanel column3 = new JPanel();
			column3.setLayout(new GridLayout(0, 1, 0, 2));
			column3.add(new JLabel(RIGHT_LABEL, JLabel.TRAILING));
			column3.add(new JLabel(BOTTOM_LABEL, JLabel.TRAILING));

			JPanel column4 = new JPanel();
			column4.setLayout(new GridLayout(0, 1, 0, 2));
			tfPanel = new JPanel(new BorderLayout()); 
			tfPanel.add(tfRight, BorderLayout.WEST);
			column4.add(tfPanel);
			tfPanel = new JPanel(new BorderLayout()); 
			tfPanel.add(tfBottom, BorderLayout.WEST);
			column4.add(tfPanel);

			marginsPanel = new JPanel();
			marginsPanel.setLayout(new BoxLayout(marginsPanel, BoxLayout.LINE_AXIS));
			marginsPanel.add(column1);
			marginsPanel.add(Box.createRigidArea(hSpace5));
			marginsPanel.add(column2);
			marginsPanel.add(Box.createRigidArea(hSpace10));
			marginsPanel.add(column3);
			marginsPanel.add(Box.createRigidArea(hSpace5));
			marginsPanel.add(column4);
			String label = (currentUnits==MediaSize.INCH) ? MARGIN_IN_LABEL : MARGIN_MM_LABEL;
			marginsPanel.setBorder(BorderFactory.createCompoundBorder( 
				BorderFactory.createTitledBorder(label), BorderFactory.createEmptyBorder(2,2,2,2)));

			// put thumbnail in a blank panel. Fix panel's size such that it can house the thumbnail in either 
			// orientation. Thumbnail is always centered in the blank panel.
			JPanel thumbPanel = new JPanel();
			Dimension fixedSz = new Dimension(THUMBNAIL_MAXDIM+10,THUMBNAIL_MAXDIM+10);
			thumbPanel.setMinimumSize(fixedSz);
			thumbPanel.setMaximumSize(fixedSz);
			thumbPanel.setPreferredSize(fixedSz);
			JPanel p = new JPreferredSizePanel();
			p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
			p.add(Box.createRigidArea(new Dimension(5, THUMBNAIL_MAXDIM)));
			p.add(thumbnail);
			p.add(Box.createRigidArea(new Dimension(5, THUMBNAIL_MAXDIM)));
			thumbPanel.setLayout(new BoxLayout(thumbPanel, BoxLayout.PAGE_AXIS));
 			thumbPanel.add(Box.createRigidArea(new Dimension(THUMBNAIL_MAXDIM+10, 5)));
			thumbPanel.add(p);
			thumbPanel.add(Box.createRigidArea(new Dimension(THUMBNAIL_MAXDIM+10, 5)));

			// create panel to hold the "OK" and "Cancel" buttons
			JPanel okPanel = new JPreferredSizePanel();
			okPanel.setLayout(new BoxLayout(okPanel, BoxLayout.LINE_AXIS));
			okPanel.add(okBtn);
			okPanel.add(Box.createRigidArea(hSpace10));
			okPanel.add(cancelBtn);

			// now arrange above panels in one vertical column: preview, paper, margins, ok/cancel btns
			thumbPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
			paperPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
			marginsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
			okPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

			JPanel content = new JPanel();
			content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
			content.add(thumbPanel);
			Dimension vSpace20 = new Dimension(0, 20);
			content.add(Box.createRigidArea(vSpace20));
			content.add(paperPanel);
			content.add(Box.createRigidArea(new Dimension(0, 5)));
			content.add(marginsPanel);
			content.add(Box.createRigidArea(vSpace20));
			content.add(okPanel);
			content.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

			getContentPane().add(content, BorderLayout.CENTER);
		}

		/**
		 * Did the user cancel out of this <code>PageSetupDialog</code>?
		 * @return <code>True</code> if user cancelled out of dialog. Returns <code>false</code> if invoked before 
		 * dialog is extinguished.
		 */
		public boolean wasCancelled() { return(wasCancelled); }

		/**
		 * Set the dimensions and "dots-per-inch" parameter of the thumbnail preview canvas to reflect the current paper 
       * size and orientation of the page layout displayed in this <code>PageSetupDialog</code>. 
		 */
		private void setThumbnailSize()
		{
			// calculate DPI such that paper height (which is always the larger dimension) maps to max allowed size of 
			// thumbnail.  Use this to calculate paper width in pixels.
			Paper paper = pageFormat.getPaper();
			double w = paper.getWidth() / 72.0;
			double h = paper.getHeight() / 72.0;
			double dpi = ((double)THUMBNAIL_MAXDIM)/h;
			int wPix = (int) Math.rint(w*dpi);
			int hPix = THUMBNAIL_MAXDIM;

			// set thumbnail's DPI and force it to the fixed size; flip dimensions if in landscape mode.
			boolean isPortrait = (pageFormat.getOrientation() == PageFormat.PORTRAIT);
			Dimension fixedSz = new Dimension( isPortrait ? wPix : hPix, isPortrait ? hPix : wPix );
			thumbnail.setMaximumSize(fixedSz);
			thumbnail.setMinimumSize(fixedSz);
			thumbnail.setPreferredSize(fixedSz);
			thumbnail.setResolution(dpi);
		}

		/**
		 * Get the current value for the specified margin in the current page layout displayed in this dialog, in the 
		 * preferred units for the currently selected paper size.
		 * 
		 * @param which String identifying which margin to retrieve.
		 * @return The margin's value in the preferred units for the currently selected paper size.
		 */
		private double getMargin(String which)
		{
			boolean isPortrait = (pageFormat.getOrientation() == PageFormat.PORTRAIT);
			Paper paper = pageFormat.getPaper();
			double m = 0;
			if(which.equals(LEFT_LABEL))
			{
				m = isPortrait ? paper.getImageableX() : 
				                 paper.getHeight() - (paper.getImageableY() + paper.getImageableHeight());
			}
			else if(which.equals(RIGHT_LABEL))
			{
				m = isPortrait ? paper.getWidth() - (paper.getImageableX() + paper.getImageableWidth()) :
									  paper.getImageableY();
			}
			else if(which.equals(TOP_LABEL))
			{
				m = isPortrait ? paper.getImageableY() : paper.getImageableX();
			}
			else if(which.equals(BOTTOM_LABEL))
			{
				m = isPortrait ? paper.getHeight() - (paper.getImageableY() + paper.getImageableHeight()) :
									  paper.getWidth() - (paper.getImageableX() + paper.getImageableWidth());
			}
			else 	// we should NEVER get here
				throw new Error("Internal error in " + PageSetupDialog.class.getName());

			return((currentUnits == MediaSize.INCH) ? m / 72.0 : m * 25.4 / 72.0);
		}

		/**
		 * Update the page layout so that the specified margin is set to the specified value. If the margin value is 
		 * negative or otherwise incompatible with the current page layout, the margin is left unchanged.
		 * 
		 * @param which String identifying which margin to adjust.
		 * @param value Desired value for margin, in the preferred units of the currently selected paper size.
       * @return <code>True</code> iff margin was successfully changed.
		 */
		private boolean setMargin(String which, double value)
		{
			// convert to pts; negative margins are NEVER valid
			if(value < 0) return(false);
			value *= (currentUnits == MediaSize.INCH) ? 72.0 : 72.0/25.4;

			boolean isPortrait = (pageFormat.getOrientation() == PageFormat.PORTRAIT);
			Paper paper = pageFormat.getPaper();
			boolean ok = false;
			double w = paper.getWidth();
			double h = paper.getHeight();
			double x = paper.getImageableX();
			double y = paper.getImageableY();
			double imgW = paper.getImageableWidth();
			double imgH = paper.getImageableHeight();

			// in each case, we adjust the imageable width or height IAW the new value of the specified margin, while 
			// keeping its opposite margin unchanged.  If this results in a non-positive imageable width or height, 
			// then the change is rejected.
			if(which.equals(LEFT_LABEL))
			{
				if(isPortrait) 
				{
					double right = w - x - imgW;
					x = value;
					imgW = w - x - right;
					ok = imgW > 0;
				} 
				else
				{
					imgH = h - y - value;
					ok = imgH > 0;
				}
			}
			else if(which.equals(RIGHT_LABEL))
			{
				if(isPortrait) 
				{
					imgW = w - x - value;
					ok = imgW > 0;
				} 
				else
				{
					double left = h - y - imgH;
					y = value;
					imgH = h - y - left;
					ok = imgH > 0;
				}
			}
			else if(which.equals(TOP_LABEL))
			{
				if(isPortrait) 
				{
					double bottom = h - y - imgH;
					y = value;
					imgH = h - y - bottom;
					ok = imgH > 0;
				} 
				else
				{
					double bottom = w - x - imgW;
					x = value;
					imgW = w - x - bottom;
					ok = imgW > 0;
				}
			}
			else if(which.equals(BOTTOM_LABEL))
			{
				if(isPortrait) 
				{
					imgH = h - y - value;
					ok = imgH > 0;
				} 
				else
				{
					imgW = w - x - value;
					ok = imgW > 0;
				}
			}
			else 	// we should NEVER get here
				throw new Error("Internal error in " + PageSetupDialog.class.getName());

			// if change is valid, update the page layout accordingly
			if(ok)
			{
				paper.setImageableArea(x,y,imgW,imgH);
				pageFormat.setPaper(paper);
			}
			return(ok);
		}

		/**
		 * Reload the list of media sizes available on the currently selected printer, and select the one size that 
		 * matches the current page layout displayed in this <code>PageSetupDialog</code>. If a matching media size does 
       * not exist, a substitute is chosen and the page layout is updated accordingly, as is the appearance of relevant 
       * components on the dialog.
		 * 
		 * <p>This method should be called whenever the selected printer changes. It is also used to initialize the paper 
       * size combo box when the dialog is constructed.</p>
		 */
		private void reloadPaperSizes()
		{
			// get the media sizes available on the currently selected printer. Build a list of GUI labels representing 
			// these sizes. If the label is found in the GUI properties file, we use that; else we just use the value 
			// returned by toString().
			MediaSizeName[] media = getAvailableMediaSizesFor(printer);
			availableMediaSizes = new MediaSize[media.length];
			String[] mediaLabels = new String[media.length];
			for(int i=0; i<media.length; i++)
			{
				String key = media[i].toString();
				mediaLabels[i] = uiProperties.getProperty(key, key);
				availableMediaSizes[i] = MediaSize.getMediaSizeForName(media[i]);
			}

			// find the available media size that most closely matches the dimensions of the current page layout. 
			Paper paper = pageFormat.getPaper();
			double wIn = paper.getWidth()/72.0;
			double hIn = paper.getHeight()/72.0;
			int iBestSize = -1;
			double minSqDiff = 1e6;
			for(int i=0; i<availableMediaSizes.length; i++)
			{
				double dx = wIn - availableMediaSizes[i].getX(MediaSize.INCH);
				double dy = hIn - availableMediaSizes[i].getY(MediaSize.INCH);
				dx = dx*dx + dy*dy;
				if(dx < minSqDiff)
				{
					minSqDiff = dx;
					iBestSize = i;
				}
			}

			// reload the combo box that displays paper size
			cbMediaSize.removeAllItems();
			for(int i=0; i<mediaLabels.length; i++) cbMediaSize.addItem(mediaLabels[i]);
			cbMediaSize.setSelectedIndex(iBestSize);

			// get preferred units for the currently selected media size (in or mm)
			int currentUnits = PrinterSupport.getPreferredUnitsForMediaSize(availableMediaSizes[iBestSize]);

			// if we did not find an exact match, then we update the page format, reload the margin controls, and refresh 
			// the thumbnail preview.  A match is considered exact if neither dimension is off by more that 0.02in
			MediaSize ms = availableMediaSizes[iBestSize];
			if(Math.abs(wIn - ms.getX(MediaSize.INCH)) < 0.02 && Math.abs(hIn - ms.getY(MediaSize.INCH)) < 0.02)
				return;

			changePaperSize(ms, pageFormat);
			if(marginsPanel != null)
			{
				String label = (currentUnits==MediaSize.INCH) ? MARGIN_IN_LABEL : MARGIN_MM_LABEL;
				marginsPanel.setBorder(BorderFactory.createCompoundBorder( 
					BorderFactory.createTitledBorder(label), BorderFactory.createEmptyBorder(2,2,2,2)));

				tfLeft.setValue(getMargin(LEFT_LABEL));
				tfRight.setValue(getMargin(RIGHT_LABEL));
				tfTop.setValue(getMargin(TOP_LABEL));
				tfBottom.setValue(getMargin(BOTTOM_LABEL));
			}

			if(thumbnail != null)
			{
				thumbnail.setVisible(false);
				setThumbnailSize();
				thumbnail.setPageFormat(pageFormat);
				thumbnail.setVisible(true);
			}

		}


		//
		// ActionListener, FocusListener
		//

		public void actionPerformed(ActionEvent e)
		{
			boolean done = false;
			String cmd = e.getActionCommand();
			if(cmd.equals(OK))
			{
				// the user has OK'd any changes made, so make the modified page layout the current layout and update the 
				// current printer.
				prnPageFmt = pageFormat;
				if(printer != null) prnServ = printer;
				done = true;
			}
			else if(cmd.equals(CANCEL))
			{
				done = true;
				wasCancelled = true;
			}
			else if(cmd.equals(LEFT_LABEL) || cmd.equals(RIGHT_LABEL) || cmd.equals(TOP_LABEL) || cmd.equals(BOTTOM_LABEL))
			{
				// a margin was changed.  Try to set the new margin value, unless text entered. Update relevant text field 
            // if the validated margin value is not the same as what was entered.
				NumericTextField tf = (NumericTextField) e.getSource();
				double oldMargin = getMargin(cmd);
				boolean ok = setMargin(cmd, tf.getValue().doubleValue());
				if(!ok) tf.setValue(oldMargin);
				else thumbnail.setPageFormat(pageFormat);
			}
			else if(cmd.equals(SIZE_LABEL))
			{
				int sel = cbMediaSize.getSelectedIndex();
				MediaSize msChosen = availableMediaSizes[sel<0 ? 0 : sel];
				boolean reloadMargins = !changePaperSize(msChosen, pageFormat);

				// if the preferred units for the selected media is different, update the label on the "Margins" panel
				// to indicate the change in units.
				int units = PrinterSupport.getPreferredUnitsForMediaSize(msChosen);
				if(units != currentUnits)
				{
					reloadMargins = true;
					currentUnits = units;
					String label = (currentUnits==MediaSize.INCH) ? MARGIN_IN_LABEL : MARGIN_MM_LABEL;
					marginsPanel.setBorder(BorderFactory.createCompoundBorder( 
						BorderFactory.createTitledBorder(label), BorderFactory.createEmptyBorder(2,2,2,2)));
				}

				// reload margin widgets if the paper size change affected their contents
				if(reloadMargins)
				{
					tfLeft.setValue(getMargin(LEFT_LABEL));
					tfRight.setValue(getMargin(RIGHT_LABEL) );
					tfTop.setValue(getMargin(TOP_LABEL));
					tfBottom.setValue(getMargin(BOTTOM_LABEL));
				}

				// refresh thumbnail IAW new paper size
				thumbnail.setVisible(false);
				setThumbnailSize();
				thumbnail.setPageFormat(pageFormat);
				thumbnail.setVisible(true);
			}
			else if(cmd.equals(PRINTER_LABEL))
			{
				// user changed current printer
				int sel = cbPrinter.getSelectedIndex();
				PrintService prn = (PrintService) prnServices[sel];
				if(!prn.equals(printer))
				{
					printer = prn;

					// reload available paper sizes and revise current page layout if there's no exact match available.  The 
					// relevant components on the dialog are also refreshed.
					reloadPaperSizes();
				}
			}
			else
			{
				// page orientation changed. Reload margins because their values are different in the two orientations.
				boolean isPortrait = portraitBtn.isSelected();
				pageFormat.setOrientation(isPortrait ? PageFormat.PORTRAIT : PageFormat.LANDSCAPE);
				tfLeft.setValue(getMargin(LEFT_LABEL));
				tfRight.setValue(getMargin(RIGHT_LABEL));
				tfTop.setValue(getMargin(TOP_LABEL));
				tfBottom.setValue(getMargin(BOTTOM_LABEL));

				// refresh thumbnail IAW new paper orientation
				thumbnail.setVisible(false);
				setThumbnailSize();
				thumbnail.setPageFormat(pageFormat);
				thumbnail.setVisible(true);
			}

			// if user OK'd or CANCEL'd out of dialog, hide it!
			if(done) setVisible(false);
		}
	}
}
