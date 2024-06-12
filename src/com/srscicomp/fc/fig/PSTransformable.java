package com.srscicomp.fc.fig;

import java.util.Map;

/**
 * The <code>PSTransformable</code> interface defines the requirements on a <em>DataNav</em> graphic node that must be 
 * met in order to render the graphic's equivalent in the context of a <em>DataNav</em>-specific Postscript language 
 * program, as encapsulated by <code>PSDoc</code>.
 * 
 * @see PSDoc
 * @author sruffner
 */
public interface PSTransformable
{
	/**
	 * Return a brief description of this object, suitable for use as a comment line in the Postscript output.
	 * @return The comment string.
	 */
	public String getPSComment();

	/**
	 * Return the name of the Postscript Latin text font which should be used to render any text associated with this 
	 * object. The font name provided must be recognized by <code>PSDoc</code>, ie, it should be one of the 32 Latin text
    * font faces in the "Standard 35" font set typical of PS Level 2 devices.
	 * 
	 * <p>Note that <code>PSDoc</code> does not use the specified font directly. Instead, it creates a composite font 
    * containing three fonts: the base font using the built-in <em>StandardEncoding</em>, the base font using the 
    * built-in <em>ISOLatin1Encoding</em>, and the <em>Symbol</em> font. This composite font effectively covers the 
    * entire set of characters supported in <em>DataNav</em>.</p>
	 * 
    * <p>Use <code>PSDoc.getStandardFontFace()</code> to get the exact name of the Postscript font face corresponding 
    * to a particular style variant (plain, bold, italic or bold-italic) of any Postscript font family enumerated by 
    * <code>PSFont</code>.</p>
    * 
	 * @see PSDoc#getStandardFontFace(PSFont,boolean,boolean)
	 * @return The Postscript language name for the Latin text font to be used to render any text associated with this 
	 * <code>PSTransformable</code> object. If <code>null</code>, then it is assumed that the object does not render any 
    * text. Otherwise, the name provided MUST be a valid font face from the "Standard 35" font set.
	 */
	public String getPSFontFace();

	/**
	 * Return the names of all Postscript Latin text fonts which are ACTUALLY used to render any text associated with 
	 * this element and -- optionally -- its descendants. The font names provided must be recognized by <b>PSDoc</b>, ie, 
	 * they must be among the 32 Latin text font faces in the "Standard 35" font set typical of PS Level 2 devices.
	 * 
	 * <p><b>PSDoc</b> invokes this method (with <i>traverse=true</i>) when starting an individual page within a 
	 * document. This font usage information is required to generate Document Structuring Convention (DSC) comments in 
	 * the document setup and page setup sections of the Postscript document. In addition, all of the composite fonts 
	 * required to render a given page are created during page setup rather than within the page body.</p>
	 * 
	 * @param fontFaceMap A map keyed by Postscript font face names; the map values are ignored and can be set to 
	 * <b>null</b>. Implementations should only put font face names into the map; do NOT remove anything from the map. 
	 * This simplifies traversing an entire tree of graphic objects to get the font faces used by all the objects in the
	 * tree.
	 * @param traverse If false, only include the PS font faces utilized by this node itself; otherwise, traverse the
	 * node's descendants and include the font faces they use as well.
	 */
	public void getAllPSFontFacesUsed(Map<String, String> fontFaceMap, boolean traverse);

	/**
	 * Return the font size of the Postscript-compatible font associated with this object.
	 * @return Current font size in milli-inches.
	 */
	public double getPSFontSize();

	/**
	 * Return the line width applicable to this object.
	 * @return Current line width in milli-inches. 
	 */
	public double getPSStrokeWidth();

	/**
	 * Return the line endcap decoration to be applied to unclosed paths and dash segments when stroking this object.
	 * @return The line endcap style: 0 = butt end (no decoration), 1 = rounded (radius = 1/2 stroke width), 2 = square
	 * projection extending 1/2 stroke width past butt end.
	 */
	public int getPSStrokeEndcap();
	
   /**
    * Return the line join style to be applied when joining segments in a path or when closing a path as this object is
    * stroked.
    * @return The line join style: 0 = mitered (fixed miter limit of 10), 1 = rounded (radius = 1/2 stroke width), 
    * 2 = beveled.
    */
	public int getPSStrokeJoin();
	
	/**
	 * Return the dash-gap pattern to be applied when stroking this object. The pattern is specified as an array of 
	 * alternating dash and gap lengths. All lengths must be strictly positive and are assumed to be in milli-inches.
	 * IMPORTANT:  An empty or single-element array is interpreted as a solid stroke, while a <code>null</code> array is 
	 * interpreted as no stroke whatsoever -- the same as setting the stroke width to 0!
	 * @return The current stroke dash-gap pattern, as described.
	 */
	public int[] getPSStrokeDashPattern();

	/**
	 * Return the initial offset into the dash-gap pattern as a distance in milli-inches (<em>NOT</em> an index into the 
	 * integer array returned by <code>getPSStrokeDashPattern()</code>).
	 * @return The stroke dash-gap pattern offset, as described.
	 */
	public int getPSStrokeDashOffset();

	/**
	 * Return the current RGB color with which paths are stroked in the rendering of this object. <i><b>Note</b>. The
	 * return value does not include an "alpha" channel because Postscript does not support translucent colors.</i>
	 * @return Current stroke color as an opaque RGB value packed into a 32bit integer: 00rrggbb.
	 */
	public int getPSStrokeColor();

	/**
	 * Return the current RGB color with which paths are filled and text is drawn in the rendering of this object. 
	 * <i><b>Note</b>. The return value does not include an "alpha" channel because Postscript does not support 
	 * translucent colors.</i>
	 * @return Current text/fill color as an opaque RGB value packed into a 32bit integer: 00rrggbb.
	 */
	public int getPSTextAndFillColor();

   /**
    * Is the actual text/fill color for this PS transformable fully transparent (alpha == 0)? Postscript does not 
    * support transparency, yet <i>FypML</i> allows for transparent stroke and text/fill colors. {@link PSDoc} relies
    * on this method to check for a transparent text/fill.
    * @return True if the current text/fill color is fully transparent. FypML does not allow for translucent colors.
    */
   public boolean isPSNoFill();
   
   /**
    * Is the actual stroke color for this PS transformable fully transparent (alpha == 0)? Postscript does not support 
    * transparency, yet <i>FypML</i> allows for transparent stroke and text/fill colors. {@link PSDoc} relies on this 
    * method to check for a transparent stroke color. A transparent stroke may also be emulated by setting the stroke 
    * width to zero.
    * @return True if the current stroke color is fully transparent. FypML does not allow for translucent colors.
    */
   public boolean isPSNoStroke();
   
	/**
	 * Update the <em>DataNav</em> Postscript page description document's current state with a Postscript code fragment 
    * that renders this Postscript-transformable object. All "rendering" is performed by calling methods of the 
    * <code>PSDoc</code> object -- which encapsulates all commands in the Postscript language that are required to 
    * render a <em>DataNav</em> figure in Postscript.
	 * 
	 * <p>It can be assumed that the object is being rendered within the context of its parent. It may also be assumed 
    * that the Postscript document's coordinate transformation matrix has been setup so that:
	 * <ul>
	 * 	<li>The units for both coordinates in user space are milli-inches.</li>
	 * 	<li>The y-axis increases in the upward direction.</li>
	 * 	<li>The current origin lies at the bottom-left corner of the parent's viewport.</li>
	 * </ul>
	 * Each object must initialize its rendering with a call to <code>PSDoc.startElement()</code> and complete rendering
	 * with a call to <code>PSDoc.endElement()</code>. This ensures that the current graphics state is preserved and 
    * restored properly. Also, graphic objects which establish a new viewport must translate and rotate so that the 
    * above assumptions about "user space" remain valid. <em>Note that no scaling is permitted!</em></p>
	 * 
	 * <p>If the object is not rendered, then the Postscript document must not be modified.</p>
	 * 
	 * @param psDoc The <em>DataNav</em> Postscript page description document in which the object is to be rendered.
	 * @throws UnsupportedOperationException if any method invoked on the document object is not supported.
	 */
	public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException;
}
