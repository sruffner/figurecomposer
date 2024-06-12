package com.srscicomp.fc.fypml;

/**
 * This interface collects string constants exposing the XML names for elements, attributes, and enumerated attribute 
 * values recognized in <em>DataNav/Phyplot</em> schema version 0, ie, the schema in effect just prior to the 
 * introduction of schema versioning in <em>Phyplot</em> 0.7.0. 
 * <p>[<em>DataNav</em> superceded <em>Phyplot</em> in 2008.]</p>
 * @author 	sruffner
 */
interface Schema0Constants
{
	//
	// Elements 
	//

	/** 
	 * Originally the tag name for the root element. As of schema version 5, no longer used -- but it does appear in
	 * the processing instruction and serves to help identify the XML document as a FypML figure document. 
	 */
	public final static String EL_FYP = "fyp";

	/** Unique tag name for a figure element. As of schema version 5, this is the root element. */
	public final static String EL_FIGURE = "figure";

	/** Unique tag name for the a graph element. */
	public final static String EL_GRAPH = "graph";

	/** Unique tag name for a single-line text label element. */
	public final static String EL_LABEL = "label";

	/** Unique tag name for a graph axis element. */
	public final static String EL_AXIS = "axis";

	/** Maximum number of tick set children defined on a graph axis element. */
	public final static int MAX_TICKSETS = 4;

	/** Unique tag name for a tick set element. */
	public final static String EL_TICKS = "ticks";

	/** Unique tag name for an element encapsulating arbitrary set of data points {x,y}. */
	public final static String EL_POINTSET = "pointSet";

	/** Unique tag name for an element encapsulating a sampled data set {y(x)}, x={x0,x0+dx,x0+2*dx,...}. */
	public final static String EL_SERIES = "series";

	/** Unique tag name for an element encapsulating a mathematical function f(x), x=[x0,x0+dx,...,x1]. */
	public final static String EL_FUNCTION = "function";

	/**
	 * Unique tag name for an element representing a line segment with optional decorations at its endpoints and 
	 * midpoint.
	 */
	public final static String EL_LINE = "line";

	/** Unique tag name for a graph's calibration bar element. */
	public final static String EL_CALIB = "calib";

	/** Unique tag name for a graph's automated legend element. */
	public final static String EL_LEGEND = "legend";


	//
	// Attributes
	//

	/** Regular expression that encapsulates the format of a floating-pt measure attribute. */
	public final static String MEASURE_REGEX = "(\\+|\\-)?([0-9]+|[0-9]*\\.[0-9]+(E(\\+|\\-)?[0-9]+)?)(in|cm|mm|pt|%|u)";

	/** The token strings for each unit identifier supported by a floating-pt measure attribute. */
	public final static String[] MEASURE_TOKENS = {"in", "cm", "mm", "pt", "%", "u"};

	/** Regular expression that encapsulates the format of a color attribute. */
	public final static String COLOR_REGEX = "[0-9a-fA-F]{6}";

	/**
	 * The "font" attribute, which defines 1st- and 2nd-choice font families to be used when rendering the SVG 
	 * representation of any text in the owning element. Inheritable, with an explicit value always set in the root 
	 * element of the <em>DataNav</em> graphics model.
	 */
	public final static String A_FONT = "font";

	/**
	 * The "psfont" attribute, which defines a Postscript font to be used when rendering the Postscript representation 
	 * of any text in the owning element. Values are restricted to only the most commonly available Postcript fonts 
	 * (from the so-called "Standard 35" set). Inheritable, with an explicit value always set in the root element of 
	 * the <em>DataNav</em> graphics model.
	 */
	public final static String A_PSFONT = "psfont";

	/** GUI name for the Postscript standard font family "Avant Garde". */
	public final static String AVANTGARDE = "Avant Garde";
	/** GUI name for the Postscript standard font family "Bookman". */
	public final static String BOOKMAN = "Bookman";
	/** GUI name for the Postscript standard font family "Courier". */
	public final static String COURIER = "Courier";
	/** GUI name for the Postscript standard font family "Helvetica". */
	public final static String HELVETICA = "Helvetica";
	/** GUI name for the Postscript standard font family "Helvetica Narrow". */
	public final static String HELVNARROW = "Helvetica Narrow";
	/** GUI name for the Postscript standard font family "New Century Schoolbook". */
	public final static String NCSCHOOLBOOK = "New Century Schoolbook";
	/** GUI name for the Postscript standard font family "Palatino". */
	public final static String PALATINO = "Palatino";
	/** GUI name for the Postscript standard font family "Times". */
	public final static String TIMES = "Times";

	/** Set of possible values for the <code>A_PSFONT</code> attribute. */
	public final static String[] PSFONT_CHOICES = {
		AVANTGARDE, BOOKMAN, COURIER, HELVETICA, HELVNARROW, NCSCHOOLBOOK, PALATINO, TIMES
	};

	/**
	 * The "substfont" attribute, which specifies a generic font to use as a fallback when neither font family listed in 
	 * an element's "font" attribute is installed on the system.  It is defined only on the root element of the 
	 * <em>DataNav</em> graphics model.
	 */
	public final static String A_SUBSTFONT = "substfont";

	public final static String SUBSTFONT_SERIF = "serif";
	public final static String SUBSTFONT_SANSSERIF = "sans-serif";
	public final static String SUBSTFONT_MONOSPACE = "monospace";

	/** Set of possible values for the <code>A_SUBSTFONT</code> attribute: "serif", "sans-serif", or "monospace". */
	public final static String[] SUBSTFONT_CHOICES = {SUBSTFONT_SERIF, SUBSTFONT_SANSSERIF, SUBSTFONT_MONOSPACE};

	/**
	 * The "fontSize" attribute, the size of the font used to render any text in the owning element. In <em>DataNav</em>, 
	 * font size is specified as a numeric value with measurement units, such a "10pt". The supported measurement units 
	 * are "in", "cm", "mm", and "pt". Inheritable, with an explicit value always set in the root element of the 
	 * graphics model.
	 */
	public final static String A_FONTSIZE = "fontSize";

	/**
	 * The "fontStyle" attribute, which defines the style of the font used to render any text in the owning element. 
	 * Inheritable, with an xplicit value always set in the root element of the <em>DataNav</em> graphics model.
	 */
	public final static String A_FONTSTYLE = "fontStyle";

	public final static String FONTSTYLE_PLAIN = "plain";
	public final static String FONTSTYLE_BOLD = "bold";
	public final static String FONTSTYLE_ITALIC = "italic";
	public final static String FONTSTYLE_BOLDITALIC = "bolditalic";

	/** Set of possible values for the <code>A_FONTSTYLE</code> attribute: "plain", "italic", "bold", or "bolditalic". */
	public final static String[] FONTSTYLE_CHOICES = 
		{FONTSTYLE_PLAIN, FONTSTYLE_BOLD, FONTSTYLE_ITALIC, FONTSTYLE_BOLDITALIC};

	/**
	 * The "strokeColor" attribute, which defines the color used to stroke all paths drawn when rendering the attribute's 
	 * owner element. It has the form of a six hexadecimal digit string "RRGGBB", where "RR" is the intensity of the 
	 * red color coordinate (0 is minimum), etcetera. Inheritable, with an explicit value always set in the root 
	 * element of the <em>DataNav</em> graphics model.
	 */
	public final static String A_STROKECOLOR = "strokeColor";

	/**
	 * The "strokeWidth" attribute, specifying the width of the pen used to stroke all paths drawn when rendering the 
	 * attribute's owner element. Formatted as an absolute measurement -- eg, "0.01in" -- where the supported  
	 * measurement units are restricted to "in", "cm", "mm", and "pt". Inheritable, with an explicit value always set 
	 * in the root element of the <em>DataNav</em> graphics model.
	 */
	public final static String A_STROKEWIDTH = "strokeWidth";

	/**
	 * The "fillColor" attribute, which defines the color used to paint text or to fill closed paths. (Note that, in 
	 * <em>DataNav</em> text is NOT separately stroked and filled.) It takes the same form as <code>A_STROKECOLOR</code>. 
	 * Inheritable, with an explicit value always set in the root element of the  <em>DataNav</em> graphics model.
	 */
	public final static String A_FILLCOLOR = "fillColor";

	/**
	 * The "loc" attribute, which defines the location of the owning element with respect to its parent's "viewport". 
	 * Generally, it defines the bottom-left corner of a bounding-box, but usage varies with element class. It takes 
	 * the form of two linear measurements, representing the coordinates of the location: "0in 1in". Note that a unit 
	 * of measure is specified for each coordinate (they can be different). In the most general case, the supported 
	 * units of measurement are the absolute units "in", "cm", "mm", and "pt" as well as the relative units "%" and "u". 
	 * The last token stands for "user units", referring to the native coordinate system defined by the axes of a graph 
	 * element. User units only make sense in the context of a graph or axis parent element; otherwise, they are 
	 * interpreted as percentages. Again, the supported measurement units for "loc" will vary with the class of the 
	 * owning element.
	 */
	public final static String A_LOC = "loc";

	/**
	 * The "width" attribute, which specifies the width of the owner element. For figure elements, it defines the width 
	 * of the figure's bounding box; all children are clipped to the inside of that box. For graph elements, it defines 
	 * the width of the graph's data box to which the graph's native coordinate system is mapped. Like other attributes 
	 * representing linear measurements, its specified in a form like "5in" or "75%". Supported units are "in", "cm", 
	 * "mm", "pt", or "%"; user units are not permitted.
	 */
	public final static String A_WIDTH = "width";

	/**
	 * The "height" attribute, which specifies the height of the owner element. 
	 * @see Schema0Constants#A_WIDTH
	 */
	public final static String A_HEIGHT = "height";

	/**
	 * The "rotate" attribute, which specifies the owning element's orientation within its parent viewport as a rotation 
	 * about the element's BOTTOM-LEFT corner. The attribute is a float-valued string representing the rotation angle, 
	 * restricted to [-180..180] degrees (the units are implied). Positive angles correspond to clockwise rotations.
	 */
	public final static String A_ROTATE = "rotate";

	/** The "align" attribute, which specifies horizontal alignment for a text element. */
	public final static String A_HALIGN = "align";

	public final static String HALIGN_LEFT = "left";
	public final static String HALIGN_RIGHT = "right";
	public final static String HALIGN_CENTER = "center";

	/** Set of values for the <code>A_HALIGN</code attribute: "left", "right", and "center". */
	public final static String[] HALIGN_CHOICES = {HALIGN_LEFT, HALIGN_RIGHT, HALIGN_CENTER};

	/** The "valign" attribute, which specifies vertical alignment for a text element. */
	public final static String A_VALIGN = "valign";

	public final static String VALIGN_TOP = "top";
	public final static String VALIGN_BOTTOM = "bottom";
	public final static String VALIGN_CENTER = "center";

   /** Set of values for the <code>A_VALIGN</code attribute: "top", "bottom", and "center". */
	public final static String[] VALIGN_CHOICES = {VALIGN_TOP, VALIGN_BOTTOM, VALIGN_CENTER};

	/**
	 * The "border" attribute specifies the width of the border around the bounding box of a figure element. It is 
	 * specified as an absolute linear measure (eg, "0.01in").
	 */
	public final static String A_BORDER = "border";

	/** The generic attribute value "none". */
	public final static String ATTRVAL_NONE = "none";

	public final static String ADORN_LINETHRU = "linethru";
	public final static String ADORN_LINEUP = "lineup";
	public final static String ADORN_LINEDOWN = "linedown";
	public final static String ADORN_BRACKET = "bracket";
	public final static String ADORN_ARROW = "arrow";
	public final static String ADORN_FILLARROW = "fillArrow";
	public final static String ADORN_THINARROW = "thinArrow";
	public final static String ADORN_FILLTHINARROW = "fillThinArrow";
	public final static String ADORN_WIDEARROW = "wideArrow";
	public final static String ADORN_FILLWIDEARROW = "fillWideArrow";
	public final static String ADORN_REV_ARROW = "reverseArrow";
	public final static String ADORN_REV_FILLARROW = "reverseFillArrow";
	public final static String ADORN_REV_THINARROW = "reverseThinArrow";
	public final static String ADORN_REV_FILLTHINARROW = "reverseFillThinArrow";
	public final static String ADORN_REV_WIDEARROW = "reverseWideArrow";
	public final static String ADORN_REV_FILLWIDEARROW = "reverseFillWideArrow";
	public final static String ADORN_CIRCLE = "circle";
	public final static String ADORN_FILLCIRCLE = "fillcircle";
	public final static String ADORN_BOX = "box";
	public final static String ADORN_FILLBOX = "fillbox";
	public final static String ADORN_DIAMOND = "diamond";
	public final static String ADORN_FILLDIAMOND = "filldiamond";
	public final static String ADORN_TEE = "tee";
	public final static String ADORN_XHAIR = "xhair";
	public final static String ADORN_STAR = "star";

	/**
	 * The names of all adornments supported in schema version 0. Each of the following enumerated attributes may take 
	 * on any value from this set: <code>CAP_ATTRNAME, P0CAP_ATTRNAME, P1CAP_ATTRNAME, MIDCAP_ATTRNAME</code>.
	 */
	public final static String[] ADORN_CHOICES = {
		ATTRVAL_NONE, ADORN_LINETHRU, ADORN_LINEUP, ADORN_LINEDOWN, ADORN_BRACKET, ADORN_ARROW, ADORN_FILLARROW, 
		ADORN_THINARROW, ADORN_FILLTHINARROW, ADORN_WIDEARROW, ADORN_FILLWIDEARROW, ADORN_REV_ARROW, ADORN_REV_FILLARROW, 
		ADORN_REV_THINARROW, ADORN_REV_FILLTHINARROW, ADORN_REV_WIDEARROW, ADORN_REV_FILLWIDEARROW, ADORN_CIRCLE, 
		ADORN_FILLCIRCLE, ADORN_BOX, ADORN_FILLBOX, ADORN_DIAMOND, ADORN_FILLDIAMOND, ADORN_TEE, ADORN_XHAIR, ADORN_STAR
	};

	/**
	 * The names of all data point symbols supported in schema version 0, representing the allowed values for the 
	 * <code>SYMBOL_ATTRNAME</code> attribute. This is a small subset of the full list of supported adornments. Starting 
	 * with schema version 1, any adornment can also be used as a data point symbol as well.
	 */
	public final static String[] SYMBOL_CHOICES = {
		ATTRVAL_NONE, ADORN_CIRCLE, ADORN_FILLCIRCLE, ADORN_BOX, ADORN_FILLBOX, ADORN_DIAMOND, ADORN_FILLDIAMOND, 
		ADORN_TEE, ADORN_XHAIR, ADORN_STAR
	};

	/** The "symbol" attribute specifies the decoration used to mark the data points in any of the data set elements. */
	public final static String A_SYMBOL = "symbol";

	/**
	 * The "symbolSize" attribute specifies the size of a square box bounding the symbols used to mark the data points 
	 * in any of the data set elements. It is formatted as an absolute linear measure (eg, "0.1in").
	 */
	public final static String A_SYMBOLSIZE = "symbolSize";

	/**
	 * The "hideErrors" attribute is a boolean-valued attribute attached to point set and series elements. If set, error
	 * bars reflecting standard deviations in the sampled data are omitted when rendering the data.
	 */
	public final static String A_HIDEERRORS = "hideErrors";

	/**
	 * The "cap" attribute is an enumerated attribute specifying the decoration to use at the endpoints of error bars 
	 * rendered with a point set or series element, or at the endpoints of a calibration bar's line segment.
	 */
	public final static String A_CAP = "cap";

	/**
	 * The "capSize" attribute specifies the size of a square box bounding the endpoint decorations of error bars in a 
	 * rendered point set or series element, or the endpoints of a calibration bar. It is formatted as an absolute 
	 * linear measure (eg, "0.1in").
	 */
	public final static String A_CAPSIZE = "capSize";

	/**
	 * The "p0" attribute is the location of the first endpoint of a <code>EL_LINE</code> element, relative to the parent 
	 * viewport of that element. Formatted like the <code>A_LOC</code> attribute.
	 */
	public final static String A_P0 = "p0";

	/**
	 * The "p1" attribute is the location of the second endpoint of a <code>EL_LINE</code> element, relative to the 
	 * parent viewport of that element. Formatted like the <code>A_LOC</code> attribute.
	 */
	public final static String A_P1 = "p1";

	/**
	 * The "p0Cap" attribute is an enumerated attribute specifying the adornment to use at the first endpoint "p0" of 
	 * a <code>EL_LINE</code> element.
	 */
	public final static String A_P0CAP = "p0Cap";

	/**
	 * The "p0CapSize" attribute specifies the size of a square box bounding the adornment for the first endpoint "p0" 
	 * of <code>EL_LINE</code> element. It is formatted as an absolute linear measure (eg, "0.1in"); relative units 
	 * ("%" or "u") not permitted.
	 */
	public final static String A_P0CAPSIZE = "p0CapSize";

	/**
	 * The "p1Cap" attribute is an enumerated attribute specifying the adornment to use at the second endpoint "p1" of 
	 * <code>EL_LINE</code> element.
	 */
	public final static String A_P1CAP = "p1Cap";

	/**
	 * The "p1CapSize" attribute specifies the size of a square box bounding the adornment for the second endpoint "p1" 
	 * <code>EL_LINE</code> element. It is formatted as an absolute linear measure (eg, "0.1in"); relative units 
	 * ("%" or "u") not permitted.
	 */
	public final static String A_P1CAPSIZE = "p1CapSize";

	/**
	 * The "midCap" attribute is an enumerated attribute specifying the adornment to use at the midpoint of a 
	 * <code>EL_LINE</code> element.
	 */
	public final static String A_MIDCAP = "midCap";

	/**
	 * The "midCapSize" attribute specifies the size of a square box bounding the adornment for the midpoint of a 
	 * <code>EL_LINE</code> element. It is formatted as an absolute linear measure (eg, "0.1in"); relative units 
	 * ("%" or "u") not permitted.
	 */
	public final static String A_MIDCAPSIZE = "midCapSize";

	/**
	 * The "lineType" attribute defines the dash array to use when stroking the polylines that connect data points in 
	 * the rendering of any of the data set elements, or the line segment of a <code>EL_LINE</code> element.
	 */
	public final static String A_LINETYPE = "lineType";

	public final static String LINETYPE_HIDDEN = "hidden";
	public final static String LINETYPE_SOLID = "solid";
	public final static String LINETYPE_DOTTED = "dotted";
	public final static String LINETYPE_DASHED = "dashed";
	public final static String LINETYPE_DASHDOT = "dashdot";

	/** Recognized synonyms for the <code>A_LINETYPE</code> attribute. */
	public final static String[] LINETYPE_SYNONYMS = {
		LINETYPE_HIDDEN, LINETYPE_SOLID, LINETYPE_DOTTED, LINETYPE_DASHED, LINETYPE_DASHDOT};

	/**
	 * The "horiz" attribute is a boolean attribute specifying the orientation of a calibration bar. If "true", the 
	 * bar is horizontal, else vertical.
	 */
	public final static String A_HORIZ = "horiz";

	/**
	 * The "auto" attribute on a <code>EL_CALIB</code> element is a boolean-valued attribute.  If "true", a label of the 
	 * form "len u" is automatically generated for the calibration bar, where "len" is the length of the bar in the 
	 * native coordinate system attached to the parent graph element and "u" is the units token associated with the 
	 * relevant graph axis.
	 */
	public final static String A_AUTO = "auto";

	/**
	 * The "len" attribute applies to legend, tick set, and calibration bar elements. For legends, it sets the length 
	 * of each sample trace line. For tick sets, it defines the length of each tick mark. In both cases it has the form
	 * of a linear measure (eg, "1in"); relative units not allowed. For a calibration bar, it defines the length of the
	 * calibration bar in native (user) units and has the form of a float-valued attribute (eg, "10" or "20.7").
	 */
	public final static String A_LEN = "len";

	/**
	 * The "mid" attribute is a boolean-valued attribute for the <code>EL_LEGEND</code> element. Each entry in a legend 
	 * includes a short sample of the line used to render the associated data element in the parent graph, along with 
	 * the symbol (if any) used to mark the individual data points. If this attribute is "true", then the symbol is 
	 * rendered at the midpoint of the line segment in the legend entry; else, symbols are rendered at the endpoints.
	 */
	public final static String A_MID = "mid";

	/**
	 * The "title" attribute defines a title for the figure, graph, axis, text label, and any of the data set elements 
	 * in <em>DataNav</em>. Intended usage of the attribute varies with the owning element, but in all cases its value 
	 * is an arbitrary but valid XML string.
	 */
	public final static String A_TITLE = "title";

	/** The "hide" attribute is a boolean-valued attribute of an axis element. If "true", the axis is not rendered. */
	public final static String A_HIDE = "hide";

	/**
	 * The "spacer" attribute applies to axis and legend elements. For an axis, it specifies the orthogonal distance 
	 * between the axis line and the nearest parallel edge of the parent graph's data box. For a legend, it specifies 
	 * the vertical distance between the midlines of successive legend entries. In both cases, it takes the form of a 
	 * linear measure (eg, "0.25in"); relative units "%" and "u" not allowed.
	 */
	public final static String A_SPACER = "spacer";

	/**
	 * The "start" attribute of an axis or tick set element is the beginning of the axis or tick set range in native, or 
	 * user units. Value is a floating-point number in string form. A tick set element can inherit the value of its 
	 * "start" attribute from its parent axis.
	 */
	public final static String A_START = "start";

	/**
	 * The "end" attribute of an axis or tick set element is the end of the axis or tick set range in native, or user 
	 * units. Value is a floating-point number in string form. A tick set element can inherit the value of its "end"
	 * attribute from its parent axis.
	 */
	public final static String A_END = "end";

	/**
	 * The "units" attribute specifies the units associated with a graph's axis. The attribute value is any valid 
	 * XML string token. 
	 */
	public final static String A_UNITS = "units";

	/**
	 * The "labelOffset" attribute on an axis element specifies the orthogonal distance between the middle of the axis 
	 * line and the bounding box for the auto-generated axis label. This axis label is always centered WRT the axis line
	 * and is always positioned on the side of the axis that is further from the graph's data window in the four single-
	 * quadrant graph layouts. This attribute is used to fine-tune the label's position so that it does not overlap any 
	 * tick marks or tick mark labels.
	 * <p>31dec2013: As an optional attribute on the legend element, specifies offset from the right edge of a trace 
	 * line segment to the start of the text label for that entry. Previously, the offset was hard-coded at 0.1in.</p>
	 * <p>The attribute value takes the form of a linear measure (eg, "1in"); relative units not allowed.</p>
	 */
	public final static String A_LABELOFFSET = "labelOffset";

	/**
	 * The "intv" attribute of a tick set element specifies the interval between ticks in user units. The interval is 
	 * additive when the parent axis is linear. When logarithmic, the nominal value should be a power of 10 greater than
	 * 0; if it is not, the "validated" value is the greater of 10 or 10^(floor(log(abs(nominalVal)))). It is a 
	 * floating-point number in string form.
	 */
	public final static String A_INTV = "intv";

	/**
	 * The "perLogIntv" attribute of a tick set element. Applicable only when the parent axis is logarithmic. Defines 
	 * the locations of ticks within each logarithmic interval, which is a power of 10 greater than 0 and is defined by 
	 * the <code>A_INTV</code> attribute. Formatted as a list of 1 to 9 integers taken from the set of digits {1..9}, or 
	 * as one of several possible synonyms.
	 */
	public final static String A_PERLOGINTV = "perLogIntv";

	/** Recognized synonyms for the <code>A_PERLOGINTV</code> attribute. */
	public final static String[] PERLOGINTV_SYNONYMS = {"all", "even", "odd"};

	/** The "dir" attribute of a tick set element specifies the direction of the tick marks along the axis. */
	public final static String A_DIR = "dir";

	/** Specifies that tick should start at axis line and be drawn toward the parent graph's data box. */
	public final static String TICK_IN = "in";
	/** Specifies that tick should start at axis line and be drawn away from the parent graph's data box. */
	public final static String TICK_OUT = "out";
	/** Specifies that tick should bisect axis line. */
	public final static String TICK_THRU = "thru";

	/** Set of possible values for the <code>A_DIR</code> attribute: "in", "out", and "thru". */
	public final static String[] DIR_CHOICES = {TICK_IN, TICK_OUT, TICK_THRU};

	/**
	 * The "fmt" attribute of a tick set element specifies the format of the numeric labels associated with tick marks 
	 * in the set.
	 */
	public final static String A_FMT = "fmt";

	/** Integer tick label format. Non-integral tick locations will be rounded to nearest integer. */
	public final static String FMT_INT = "int";
	/** Floating-point tick label format, with 1 decimal digit. */
	public final static String FMT_F1 = "f1";
	/** Floating-point tick label format, with 2 decimal digit. */
	public final static String FMT_F2 = "f2";
	/** Floating-point tick label format, with 3 decimal digit. */
	public final static String FMT_F3 = "f3";

	/** Set of possible values for the <code>A_FMT</code> attribute: "int", "f1", "f2", and "f3". */
	public final static String[] FMT_CHOICES = {ATTRVAL_NONE, FMT_INT, FMT_F1, FMT_F2, FMT_F3};

	/**
	 * The "gap" attribute of a tick set element specifies the size of the gap between a tick mark label and the end of
	 * the corresponding tick or the axis line, whichever is closer. It is formatted as a linear measure (eg, "0.1in"); 
	 * relative units "%" and "u" not allowed.
	 */
	public final static String A_GAP = "gap";

	/**
	 * The "type" attribute is an enumerated attribute that applies to graph and series elements. For a graph element, 
	 * it defines the type of graph. For a series element, it selects how the series data is displayed in the graph.
	 */
	public final static String A_TYPE = "type";

	/** Graph type:  Standard cartesian. */
	public final static String GRAPH_TYPE_CARTESIAN = "cartesian";
	/** Graph type:  Cartesian with logarithmic x-axis. */
	public final static String GRAPH_TYPE_SEMILOGX = "semilogX";
	/** Graph type:  Cartesian with logarithmic y-axis. */
	public final static String GRAPH_TYPE_SEMILOGY = "semilogY";
	/** Graph type:  Cartesian with both axes logarithmic. */
	public final static String GRAPH_TYPE_LOGLOG = "loglog";
	/** Graph type:  Polar. */
	public final static String GRAPH_TYPE_POLAR = "polar";
	/** Graph type:  Polar with logarithmic radial axis. */
	public final static String GRAPH_TYPE_SEMILOGR = "semilogR";

	/** The set of all supported graph types. */
	public final static String[] GRAPH_TYPE_CHOICES = {GRAPH_TYPE_CARTESIAN, GRAPH_TYPE_SEMILOGX, 
		GRAPH_TYPE_SEMILOGY, GRAPH_TYPE_LOGLOG, GRAPH_TYPE_POLAR, GRAPH_TYPE_SEMILOGR};

	/**
	 * Data series display type: a set of individual points connected by line segments, possibly with symbols and/or 
	 * error bars drawn at the locations of the data samples
	 */
	public final static String SERIES_TYPE_PTS = "points";

	/**
	 * Data series display type: a trace-like display, with samples connected by line segments. Two additional connected 
	 * line segments are drawn above and below the data trace to represent one standard deviation about the samples in 
	 * the trace. No symbols or error bars are rendered with this display type.
	 */
	public final static String SERIES_TYPE_TRACE = "trace";

	/**
	 * Data series display type: a histogram display, with each sample represented by a vertical bar drawn from a 
	 * specified baseline. If the bar width is equal to the series' bin size, then a staircase representation of the 
	 * histogram is drawn. Data point symbols and error bars may be rendered on top of this histogram display IAW the 
	 * relevant attributes of the data series element.
	 */
	public final static String SERIES_TYPE_HIST = "histogram";

	/** The set of choices for a data series element's display type. */
	public final static String[] SERIES_TYPE_CHOICES = {SERIES_TYPE_PTS, SERIES_TYPE_TRACE, SERIES_TYPE_HIST};

	/** The "layout" attribute for a graph element selects the axis layout: 1st quadrant, 2nd quadrant, etc. */
	public final static String A_LAYOUT = "layout";

	public final static String GRAPH_LAYOUT_QUAD1 = "quad1";
	public final static String GRAPH_LAYOUT_QUAD2 = "quad2";
	public final static String GRAPH_LAYOUT_QUAD3 = "quad3";
	public final static String GRAPH_LAYOUT_QUAD4 = "quad4";
	public final static String GRAPH_LAYOUT_ALLQUAD = "allQuad";

	/** The set of choices for a graph element's axis layout. */
	public final static String[] GRAPH_LAYOUT_CHOICES = {GRAPH_LAYOUT_QUAD1, GRAPH_LAYOUT_QUAD2, 
		GRAPH_LAYOUT_QUAD3, GRAPH_LAYOUT_QUAD4, GRAPH_LAYOUT_ALLQUAD};

	/**
	 * The "showGrid" attribute is a boolean attribute governing the presence/absence of grid lines in the data box of 
	 * a graph element. If "true", the grid lines are rendered IAW the locations of major tick marks along the graph's 
	 * axes.
	 */
	public final static String A_SHOWGRID = "showGrid";

	/**
	 * The "clip" attribute is the boolean attribute of a graph element that determines whether or not data sets (and 
	 * any other children specified in native coordinates) are clipped to the graph's "data window".
	 */
	public final static String A_CLIP = "clip";

	/** The "x0" attribute specifies the x-coordinate of the first sample/datum in a function or series data element. */
	public final static String A_X0 = "x0";

	/** The "x1" attribute specifies the x-coordinate of the last datum in a function element. */
	public final static String A_X1 = "x1";

	/** The "dx" attribute specifies the sample interval for a function or series data element. */
	public final static String A_DX = "dx";

	/**
	 * The "barWidth" attribute of a series element. If the series element is displayed as a histogram, this attribute 
	 * specifies the width of each vertical bar in the display in user units. Nonnegative bar widths are enforced. If 
	 * greater than the absolute value of the histogram bin size, then it is assumed that bar width = abs(bin size). If 
	 * zero bar width, the histogram bars are rendered as lines instead.
	 */
	public final static String A_BARWIDTH = "barWidth";

	/**
	 * The "baseline" attribute of a series element. If the series element is displayed as a histogram, this attribute 
	 * specifies the histogram's baseline in user units. If the specified baseline is outside the range of the parent 
	 * graph's secondary axis, then either the start or the end point of the range will be used instead.
	 */
	public final static String A_BASELINE = "baseline";

	/**
	 * The "filled" attribute of a series element. If the series element is displayed as a histogram, the histogram bars 
	 * are filled with the current fill color if this attribute is set; otherwise they are hollow. Similarly, if the 
	 * display type is <code>SERIES_TYPE_TRACE</code>, the band representing +/-1 standard deviation about the sampled 
	 * data trace is filled with the current color if this attribute is set; otherwise, it is not painted.
	 */
	public final static String A_FILLED = "filled";
}
