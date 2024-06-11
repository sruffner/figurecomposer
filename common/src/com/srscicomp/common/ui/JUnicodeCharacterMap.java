package com.srscicomp.common.ui;

import com.srscicomp.common.util.Utilities;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;


/**
 * JUnicodeCharacterMap is a self-contained character map utility that displays the set of characters available in a 
 * chosen font that are drawn from a list of Unicode character subsets. It extends {@link JPanel} and contains two 
 * components, a read-only table which displays all characters drawn from a selected {@link UnicodeSubset} that are 
 * available in the currently mapped font, and a dropdown combo box that selects the <b>UnicodeSubset</b>. The list of 
 * <b>UnicodeSubset</b>s handled by the character map is specified at construction time, while the mapped font may be 
 * changed dynamically by {@link #setMappedFont(Font)}.
 * 
 * <p>The character table is displayed within a scroll pane and has a fixed number of columns.  Each available 
 * character in the selected <b>UnicodeSubset</b> is rendered in a separate cell (fixed size) of the table using the 
 * mapped font -- which gives the user an idea of what a given character looks like in that font.</p>
 * 
 * <p> While the table is read-only, <b>JUnicodeCharacterMap</b> provides a framework for allowing the user to "select" 
 * a character in the table which can then be inserted into a text field, copied to the clipboard, etcetera. To detect 
 * character selection changes, simply register as a {@link PropertyChangeListener} on the character map for the named 
 * property {@link #SELCHAR_PROPERTY}.  The method {@link #getSelectedCharacter()} returns the character selected. 
 * Note that this method, by convention, returns the null character (\u0000) when no character is selected, and property
 * change events will be fired when the selected character is reset because the mapped font or the displayed 
 * <b>UnicodeSubset</b> is changed.</p>
 * 
 * @see		UnicodeSubset
 * @author 	sruffner
 */
public class JUnicodeCharacterMap extends JPanel implements ActionListener, HierarchyListener, MouseListener
{
   private static final long serialVersionUID = 1L;

   /**
	 * The name of the bound property to which PropertyChangeListeners can attach in order to detect changes in the 
	 * character currently selected in the JUnicodeCharacterMap.
	 */
	public final static String SELCHAR_PROPERTY = "selectedCharacter";

	/**
	 * fixed width of each column of character table -- table font size is adjusted so that most characters of the font
	 * fit comfortably into a character cell
	 */
	public final static int FIXED_COL_W = 25;

	/**
	 * fixed height of each row of character table
	 */
	public final static int FIXED_ROW_H = 33;

	/**
	 * The minimum number of rows in the character table.
	 */
	private int nRowsMin;

	/**
	 * The fixed number of columns in the character table.
	 */
	private int nColsFixed;

	/** Custom table model for the character table; all objects in the model are delivered as MutableCharacters. */
	private DisplayableCharSetModel characterModel;

	/** The character table. We always set its display font to the font we wish to map. */
	private final JTable characterTable;

	/**
	 * The last character selected by double-clicking on a cell in the character table.  Initially set to the null 
	 * character (0), indicating that no selection has been made.
	 */
	private char selectedCharacter = 0;

	/**
	 * The row index of cell in character table that displays the last selected character (-1 means no selection).
	 */
	private int selCharRow = -1;

	/**
	 * The column index of cell in character table that displays the last selected character (-1 means no selection).
	 */
	private int selCharCol = -1;

	/**
	 * Chooses the Unicode character subset currently displayed in the character table; the table displays only the 
	 * characters from this subset that are displayable in the current mapped font.
	 */
	private JComboBox<UnicodeSubset> charSetCombo = null;


	/**
	 * Construct a JUnicodeCharacterMap that is initially configured to display common Latin, Greek, or punctuation 
	 * character subsets of the Unicode character set using the panel's own display font.  The table is set up with 10 
	 * columns and a minimum of 5 rows.
	 */
	public JUnicodeCharacterMap()
	{
		this( null, null, 5, 10 );
	}

	/**
	 * Construct a JUnicodeCharacterMap that is initially configured to display characters drawn from the specified 
	 * subsets of the Unicode character set that are also available in the the specified font. 
	 * @param f Characters are mapped to glyphs in this font. If <code>null</code>, then panel's own font is used.
	 * @param charSets The list of Unicode character subsets that should be displayed in the map. If <code>null</code> or
	 * empty, then the panel is configured to display several predefined character sets, including standard Latin, Greek, 
	 * and punctuation characters. 
	 * @param nRows The minimum number of rows displayed in the character table, which determines the height of the 
	 * scroll pane in which the table is embedded.  Allowed range = [2..20].
	 * @param nCols The *fixed* number of columns in the character table, which determines the width of the scroll pane
	 * in which the table is embedded.  Allowed range = [5..20].
	 */
	public JUnicodeCharacterMap(Font f, UnicodeSubset[] charSets, int nRows, int nCols) 
	{
		this.nRowsMin = Utilities.rangeRestrict(2, 20, nRows);
		this.nColsFixed = Utilities.rangeRestrict(5, 20, nCols);

		// handle null or empty arguments
		Font mappedFont = (f==null) ? getFont() : f;
		UnicodeSubset[] unicodeSets = charSets;
		if(unicodeSets == null || unicodeSets.length == 0)
		{
			unicodeSets = new UnicodeSubset[3];
			unicodeSets[0] = UnicodeSubset.LATIN_LETTERS;
			unicodeSets[1] = UnicodeSubset.GREEK_LETTERS;
			unicodeSets[2] = UnicodeSubset.PUNCTUATION;
		}
		
		// create a readonly table for listing characters drawn from a particular character subset that are displayable 
		// in the current mapped font.  since this table is readonly, we prevent column & row resizing and turn off 
		// selection.  we also install a renderer that displays each character centered horizontally and vertically in 
		// its cell, adjusting the font size used to be compatible with the table's fixed cell size.
		characterModel = new DisplayableCharSetModel( mappedFont, unicodeSets[0] );
		characterTable = new JTable( characterModel );
		TableColumnModel tcm = characterTable.getColumnModel();
		for( int i=0; i<characterTable.getColumnCount(); i++ )
		{
			TableColumn tc = tcm.getColumn(i);
			tc.setMinWidth(FIXED_COL_W);
			tc.setMaxWidth(FIXED_COL_W);
			tc.setPreferredWidth(FIXED_COL_W);
		}
		characterTable.setRowHeight( FIXED_ROW_H );
		characterTable.setDefaultRenderer( MutableCharacter.class, new CharacterCellRenderer() );
		characterTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		characterTable.setColumnSelectionAllowed( false );
		characterTable.setRowSelectionAllowed( false );
		characterTable.setTableHeader( null );
		characterTable.setPreferredScrollableViewportSize( new Dimension(FIXED_COL_W*nColsFixed, FIXED_ROW_H*nRowsMin) );
		characterTable.setFont( mappedFont );
		characterTable.addHierarchyListener( this );
		characterTable.addMouseListener( this );
		characterTable.setFocusable(false);
		
		JScrollPane scroller = new JScrollPane( characterTable );
		scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
      scroller.setFocusable(false); 
      
		// create the combo box that contains the Unicode character subsets that may be selected for display in the 
		// character table.  if the map displays only one char set, this combo box is disabled.
		charSetCombo = new JComboBox<>();
        for (UnicodeSubset unicodeSet : unicodeSets) charSetCombo.addItem(unicodeSet);
		charSetCombo.setSelectedIndex( 0 );
		charSetCombo.setEnabled( unicodeSets.length > 1 );
		charSetCombo.addActionListener( this );
		charSetCombo.setFocusable(false);
		
		setBorder( BorderFactory.createCompoundBorder( 
					BorderFactory.createTitledBorder( "Character Map" ), BorderFactory.createEmptyBorder(3,3,3,3) ) );
		setLayout( new BorderLayout() );
		add( scroller, BorderLayout.CENTER );
		add( charSetCombo, BorderLayout.NORTH );
	}

   /**
    * Change the layout of this <code>JUnicodeCharacterMap</code>'s character table.
    * @param nRows The minimum number of rows displayed in the character table, which determines the height of the 
    * scroll pane in which the table is embedded. Allowed range = [2..20].
    * @param nCols The *fixed* number of columns in the character table, which determines the width of the scroll pane
    * in which the table is embedded. Allowed range = [5..20].
    */
   public void setRowsAndColumns(int nRows, int nCols)
   {
      this.nRowsMin = Utilities.rangeRestrict(2, 20, nRows);
      this.nColsFixed = Utilities.rangeRestrict(5, 20, nCols);

      characterModel = new DisplayableCharSetModel(characterTable.getFont(), getSelectedCharSubset());
      characterTable.setModel(characterModel);
      TableColumnModel tcm = characterTable.getColumnModel();
      for( int i=0; i<characterTable.getColumnCount(); i++ )
      {
         TableColumn tc = tcm.getColumn(i);
         tc.setMinWidth(FIXED_COL_W);
         tc.setMaxWidth(FIXED_COL_W);
         tc.setPreferredWidth(FIXED_COL_W);
      }
      
      characterTable.setPreferredScrollableViewportSize(new Dimension(FIXED_COL_W*nColsFixed, FIXED_ROW_H*nRowsMin));
      characterTable.revalidate();
   }

   /**
	 * Return the last character selected by a double-click in the JUnicodeCharacterMap's character table.  A null 
	 * character (0) indicates that no character is selected.  The last character selected is always reset whenever the 
	 * mapped font is changed by invoking {@link #setMappedFont(Font)}, or the current displayed character subset is
	* changed via the embedded combo box.
	 * 
	 * <p>To detect when the character selection changes, register as a {@link PropertyChangeListener} for the property
	* named {@link #SELCHAR_PROPERTY}.</p>
	 * 
	 * @return	last character selected in the character map's table. A value of 0 is reserved to indicate no selection.
	 */
	public char getSelectedCharacter()
	{
		return( selectedCharacter );
	}

	/**
	 * Set the font that is mapped in the character table.  The table's appearance is updated to reflect the set of 
	 * displayable characters in the new font.
	 * 
	 * @param f the font; if <code>null</code>, then the panel's own display font is used instead.
	 */
	public void setMappedFont( Font f ) 
	{
		final Font mappedFont = adjustFontToFitCell( (f==null) ? getFont() : f );
		selectedCharacter = 0;
		selCharRow = -1;
		selCharCol = -1;
		characterTable.setFont( mappedFont );
		
		// found it necessary to update the table on the event dispatch thread because otherwise the previous call to 
		// setFont() also triggers a revalidation that fouls up the revalidation associated with updating the table...
		Runnable runner = () -> characterModel.updateDisplayableCharacters( mappedFont, getSelectedCharSubset() );
		SwingUtilities.invokeLater( runner );
	}

	/**
	 * Set the list of different Unicode character subsets that should be displayed in the character table.
	 * 
	 * @param sets The list of Unicode character subsets that should be made available in the map. If <code>null</code> 
	 * or empty, then the panel is configured to display several predefined character sets, including standard Latin, 
	 * Greek, and puncutation characters. 
	 */
	public void setAvailableCharacterSets(UnicodeSubset[] sets)
	{
	   UnicodeSubset[] unicodeSets = sets;
      if( unicodeSets == null || unicodeSets.length == 0 )
      {
         unicodeSets = new UnicodeSubset[3];
         unicodeSets[0] = UnicodeSubset.LATIN_LETTERS;
         unicodeSets[1] = UnicodeSubset.GREEK_LETTERS;
         unicodeSets[2] = UnicodeSubset.PUNCTUATION;
      }
	      
      charSetCombo.removeActionListener(this);
      charSetCombo.removeAllItems();
	  for (UnicodeSubset unicodeSet : unicodeSets) charSetCombo.addItem(unicodeSet);
      charSetCombo.setSelectedIndex(0);
      charSetCombo.setEnabled(unicodeSets.length > 1);
      charSetCombo.addActionListener(this);

      characterModel.updateDisplayableCharacters(characterTable.getFont(), getSelectedCharSubset());
	}
	
	/**
	 * Adjust the specified font so that its style is {@link Font#PLAIN} and its size is such that most
	 * displayable characters will fit within the character table's fixed-size cell with insets of 3pixels all around.
	 * 
	 * @param 	f the font to adjust; must not be <code>null</code>
	 * @return	a new Font object adjusted so that most characters fits within a fixed-size cell
	 */
	private Font adjustFontToFitCell( Font f )
	{
		// make sure font style is PLAIN
		Font adjustedFont = f;
//		if( !adjustedFont.isPlain() ) 
//			adjustedFont = adjustedFont.deriveFont( Font.PLAIN );

		// rendering of the font's Latin uppercase W should leave 3-pixel insets all around
		int wMax = FIXED_COL_W - 6;
		int hMax = FIXED_ROW_H - 6;

		// we need a graphics context to get font metrics
		Graphics2D g2 = (Graphics2D) characterTable.getGraphics();
		if( g2 == null )
			return( adjustedFont );

		// adjust point size of font until *most* displayable characters fit within the character table's fixed cell size. 
		// the algorithm uses the width of the Latin uppercase 'W' (note that symbolic fonts will typically return the 
		// width of the "not-supported" character) and the sum of the font's ascent & descent.  we search for a font size 
		// between 8pts and a size that would fill *two* cells on a 75dpi monitor; hopefully that will be sufficient to 
		// account for variation in monitor resolution
		boolean adjusted = false;
		float ptsTooBig = 144f * ((float)FIXED_COL_W) / 75f;
		float ptsTooSmall = 8f;
		adjustedFont = adjustedFont.deriveFont( (ptsTooBig + ptsTooSmall)/2f );
		while( !adjusted )
		{
			FontMetrics fm = g2.getFontMetrics( adjustedFont );
			int w = 2 + fm.charWidth('W');
			int h = fm.getAscent() + fm.getDescent();
			if( w > wMax || h > hMax )
				ptsTooBig = adjustedFont.getSize2D();
			else 
				ptsTooSmall = adjustedFont.getSize2D();

			if( ptsTooBig - ptsTooSmall <= 1f )
			{
				adjustedFont = adjustedFont.deriveFont( ptsTooSmall ); 
				adjusted = true;
			}
			else
				adjustedFont = adjustedFont.deriveFont( (ptsTooBig + ptsTooSmall)/2f );
		}
		g2.dispose();

		return( adjustedFont );
	}

	/**
	 * Returns the font that is currently mapped in the character table.  The font size and style may be different than 
	 * that which was passed into the constructor or {@link #setMappedFont(Font)}.
	 * 
	 * @return	the currently mapped font 
	 */
	public Font getMappedFont()
	{
		return( characterTable.getFont() );
	}

	/**
	 * Retrieve the character subset currently selected in the combo box associated with this character map.
	 * 
	 * @return	the character subset selected
	 */
	private UnicodeSubset getSelectedCharSubset()
	{
		return( (UnicodeSubset) charSetCombo.getSelectedItem() );
	}


	//
	// ActionListener, HierarchyListener, MouseListener
	//

	/**
	 * When the user selects a different character subset via the embedded combo box, the character table is updated 
	 * accordingly.
	 *
	 * @see ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e)
	{
		selectedCharacter = 0;
		selCharRow = -1;
		selCharCol = -1;
		characterModel.updateDisplayableCharacters( characterTable.getFont(), getSelectedCharSubset() );
	}

	/**
	 * <p>When the character table becomes displayable, we need to adjust the font size so that the characters displayed 
	 * in the table's cells fill as much of the fixed cell size as possible.  We can't make this adjustment while it is 
	 * not displayable, since we cannot obtain a graphics context!</p>
	 * 
	 * @see HierarchyListener#hierarchyChanged(HierarchyEvent)
	 */
	public void hierarchyChanged(HierarchyEvent e)
	{
		if( (e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0 )
		{
			if( characterTable.isDisplayable() )
			{
				characterTable.setFont( adjustFontToFitCell(getMappedFont()) );
			}
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent e)
	{
		if( e.getSource() == characterTable && e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2 )
		{
			Point p = e.getPoint();
			int row = characterTable.rowAtPoint(p);
			int col = characterTable.columnAtPoint(p);
			if( row >= 0 && row < characterTable.getRowCount() && col >= 0 && col < characterTable.getColumnCount() )
			{
				MutableCharacter mc = (MutableCharacter) characterModel.getValueAt(row,col);
				if(mc != null)
				{
               boolean selectedNewChar = (selCharRow != row || selCharCol != col);
					if(selectedNewChar && selCharRow != -1 && selCharCol != -1)
						characterTable.repaint(characterTable.getCellRect(selCharRow,selCharCol,true));
					selectedCharacter = mc.getCharValue();
					firePropertyChange(SELCHAR_PROPERTY, null, selectedCharacter);
					if(selectedNewChar) characterTable.repaint(characterTable.getCellRect(row,col,true));
					selCharRow = row;
					selCharCol = col;
				}
			}
		}
		
	}

	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}


	/**
	 * A minor extension of {@link DefaultTableCellRenderer} that centers the text of the underlying
	 * label and sets a tooltip to reflect the Unicode code point of the character displayed.
	 * 
	 * @author 	sruffner
	 */
	private final class CharacterCellRenderer extends DefaultTableCellRenderer
	{
      private static final long serialVersionUID = 1L;

      public CharacterCellRenderer()
		{
			super();
			setHorizontalAlignment( JLabel.CENTER );
			setVerticalAlignment( JLabel.CENTER );
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
		 */
		public Component getTableCellRendererComponent( 
			JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column )
		{
			MutableCharacter mc = (value instanceof MutableCharacter) ? ((MutableCharacter) value) : null;

			// render the character cell last selected by double-click using the default selection bkg/fgnd colors
			boolean select = (mc!=null) && (getSelectedCharacter()!=0) && (getSelectedCharacter() == mc.getCharValue());
			Component c = super.getTableCellRendererComponent(table, value, select, hasFocus, row, column);
			if( c instanceof JComponent )
			{
				String tip = null;
				if( mc != null )
				{
					String hex = Integer.toHexString(mc.getCharValue()).toUpperCase();
					tip = "\\U0000".substring(0, 6-hex.length()) + hex;
				}
				((JComponent)c).setToolTipText( tip );
			}
			return( c );
		}
		/* (non-Javadoc)
		 * @see java.awt.Component#paint(java.awt.Graphics)
		 */
		public void paint(Graphics g)
		{
			Graphics2D g2 = (Graphics2D)g;
			g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
			g2.setRenderingHint( RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY );
			super.paint(g2);
		}

	}


	/**
	 * A read-only model of the list of characters drawn from a specified Unicode character subset that can also be 
	 * displayed in a specified font.
	 * 
	 * @author 	sruffner
	 */
	private final class DisplayableCharSetModel extends AbstractTableModel
	{
      private static final long serialVersionUID = 1L;

      /**
		 * tracks current number of rows in the character table; we don't want to calculate it each time
		 */
		private int nRows = JUnicodeCharacterMap.this.nRowsMin;

		/**
		 * cache of characters from the Unicode subset specified at construction that are displayable in the font that 
		 * was specified
		 */
		private StringBuffer displayableCharacters = new StringBuffer(256);

		/**
		 * rather than storing a whole bunch of immutable Character objects representing the displayable characters, we 
		 * just reuse this mutable object to encapsulate each character returned by getValueAt()
		 */
		private final MutableCharacter aCharacter = new MutableCharacter( '\u0000' );


		/**
		 * Construct a character table model representing the list of characters that are both displayable in the 
		 * specified font and drawn from the specified subset of 16bit Unicode characters.
		 * 
		 * @param 	f each character in the table will be displayable in this font.  If <code>null</code>, the "dialog" 
		 * 	logical font is assumed.
		 * @param 	subset each character in the table will also be a member of this subset.  If <code>null</code>, 
		 * 	an empty character set is assumed -- meaning that the table will also be empty!
		 */
		public DisplayableCharSetModel( Font f, UnicodeSubset subset )
		{
			super();
			update( f, subset );
		}

		/**
		 * Reinitialize the character table to represent those characters in the specified set that are displayable in 
		 * the specified font.
		 * 
		 * @see	#updateDisplayableCharacters(Font,UnicodeSubset)
		 */
		private void update( Font f, UnicodeSubset subset )
		{
			Font font = (f==null) ? Font.decode(null) : f;
			UnicodeSubset set = (subset==null) ? UnicodeSubset.EMPTY : subset;

			displayableCharacters = set.getDisplayableCharactersIn(font);
			nRows = displayableCharacters.length() / JUnicodeCharacterMap.this.nColsFixed;
			if( displayableCharacters.length() % JUnicodeCharacterMap.this.nColsFixed != 0 ) ++nRows;
			if( nRows < JUnicodeCharacterMap.this.nRowsMin ) nRows = JUnicodeCharacterMap.this.nRowsMin;
		}

		/**
		 * Update the character table to represent only those characters in the specified set that are displayable in the 
		 * specified font.  Table listeners are notified of this wholesale change in the model.
		 * 
		 * @param 	f the font to check against
		 * @param 	subset the character set checked for displayability
		 */
		public void updateDisplayableCharacters( Font f, UnicodeSubset subset )
		{
			update( f, subset );
			fireTableDataChanged();
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getRowCount()
		 */
		public int getRowCount()
		{
			return( nRows );
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getColumnCount()
		 */
		public int getColumnCount()
		{
			return( JUnicodeCharacterMap.this.nColsFixed );
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			int i = JUnicodeCharacterMap.this.nColsFixed * rowIndex + columnIndex;
			if( i >= displayableCharacters.length() )
				return( null );
			aCharacter.setCharValue( displayableCharacters.charAt(i) );
			return( aCharacter );
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getColumnClass(int)
		 */
		public Class<?> getColumnClass(int columnIndex)
		{
			return( MutableCharacter.class );
		}

	}


	/**
	 * For test/debug only.
	 * <p>
	 * This program displays a JUnicodeCharacterMap in a frame window along with an additional combo box for selecting 
	 * different fonts.... 
	 * 
	 * @param 	args	the command-line arguments (NOT USED)
	 */
	public static void main(String[] args)
	{
		GUIUtilities.initLookAndFeel();
		final JFrame appFrame = new JFrame( "TESTING JUnicodeCharacterMap" );

		final JUnicodeCharacterMap mapper = 
			new JUnicodeCharacterMap( null, 
				new UnicodeSubset[] { UnicodeSubset.LATIN_LETTERS, UnicodeSubset.GREEK_LETTERS,
										    UnicodeSubset.PUNCTUATION, UnicodeSubset.ARROWS,
										    UnicodeSubset.LETTER_SYMBOLS, UnicodeSubset.NUMBERS,
										    UnicodeSubset.MATHOPS, UnicodeSubset.PS_SYMBOL },
				5, 10 );
		String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		final JComboBox<String> fontChooser = new JComboBox<>(fonts);
		final JComboBox<String> styleChooser = new JComboBox<>(
                new String[]{"PLAIN", "BOLD", "ITALIC", "BOLDITALIC"});
		final JLabel psFontNameLabel = new JLabel();
		final JLabel famFontNameLabel = new JLabel();
		final JLabel faceNameLabel = new JLabel();
		final JLabel postureWtLabel = new JLabel();

		final ActionListener action = e -> {
            Font f = new Font( (String) fontChooser.getSelectedItem(), styleChooser.getSelectedIndex(), 8 );
            mapper.setMappedFont( f );
            psFontNameLabel.setText( f.getPSName() );
            famFontNameLabel.setText( f.getFamily() );
            faceNameLabel.setText( f.getFontName() );
            Object attr = f.getAttributes().get( TextAttribute.POSTURE );
            String s = (attr == null) ? "none" : attr.toString();
            attr = f.getAttributes().get( TextAttribute.WEIGHT );
            s += "; " + ((attr == null) ? "none" : attr.toString());
            postureWtLabel.setText( s );
        };
		fontChooser.addActionListener( action );
		styleChooser.addActionListener( action );

		final PropertyChangeListener charChanged = e -> {
            if( e.getSource() == mapper && e.getPropertyName().equals(JUnicodeCharacterMap.SELCHAR_PROPERTY) &&
                 mapper.getSelectedCharacter() != 0 )
            {
                String hex = Integer.toHexString(mapper.getSelectedCharacter());
                String tip = "+U0000".substring(0, 6-hex.length()) + hex;
                System.out.println( "User selected char " + tip );
            }
        };
		mapper.addPropertyChangeListener( SELCHAR_PROPERTY, charChanged );

		Font f = new Font( fonts[0], Font.PLAIN, 8 );
		styleChooser.setSelectedIndex( 0 );
		mapper.setMappedFont( f );
		psFontNameLabel.setText( f.getPSName() );
		famFontNameLabel.setText( f.getFamily() );
		faceNameLabel.setText( f.getFontName() );
		Object attr = f.getAttributes().get( TextAttribute.POSTURE );
		String s = (attr == null) ? "none" : attr.toString();
		attr = f.getAttributes().get( TextAttribute.WEIGHT );
		s += "; " + ((attr == null) ? "none" : attr.toString());
		postureWtLabel.setText( s );

		JPanel panel = new JPreferredSizePanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.PAGE_AXIS ) );
		panel.add( fontChooser );
		panel.add( Box.createRigidArea( new Dimension(0,10) ) );
		panel.add( styleChooser );
		panel.add( Box.createRigidArea( new Dimension(0,10) ) );

		JPanel colPane1 = new JPanel();
		colPane1.setLayout( new GridLayout(0,1,0,5) );
		colPane1.add( new JLabel( "Postscript Name:", JLabel.TRAILING ) );
		colPane1.add( new JLabel( "Family Name:", JLabel.TRAILING ) );
		colPane1.add( new JLabel( "Face Name:", JLabel.TRAILING ) );
		colPane1.add( new JLabel( "Posture, Wt:", JLabel.TRAILING ) );
		JPanel colPane2 = new JPanel();
		colPane2.setLayout( new GridLayout(0,1,0,5) );
		colPane2.add( psFontNameLabel );
		colPane2.add( famFontNameLabel );
		colPane2.add( faceNameLabel );
		colPane2.add( postureWtLabel );

		JPanel boxPane = new JPreferredSizePanel();
		boxPane.setLayout( new BoxLayout( boxPane, BoxLayout.LINE_AXIS) );
		boxPane.add( colPane1 );
		boxPane.add( Box.createRigidArea(new Dimension(2,0)) );
		boxPane.add( colPane2 );
		JPanel grp2 = new JPanel( new BorderLayout() );
		grp2.add( boxPane, BorderLayout.WEST );

		panel.add( grp2 );
		panel.add( Box.createRigidArea( new Dimension(0,10) ) );
		panel.add( mapper );
		panel.setBorder( BorderFactory.createEmptyBorder(5,5,5,5) );

		appFrame.getContentPane().add( panel, BorderLayout.CENTER );

		appFrame.addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent e ) 
			{
				System.exit(0);
			}
		});

		Runnable runner = new MainFrameShower( appFrame );
		SwingUtilities.invokeLater( runner );
	}

}
