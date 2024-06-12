package com.srscicomp.fc.fig;

/**
 * An enumeration of the various properties defined by the different graphic nodes in the <em>DataNav</em> graphics 
 * model. Some properties are shared among most or all of the graphic node classes -- these are generally implemented 
 * by <code>FGraphicNode</code>, the base class for all <em>DataNav</em> graphic node classes. Most other attributes 
 * are unique to a single class. 
 * 
 * <p>This enumeration is primarily for reference purposes, but it can be used in certain situations to generically 
 * identify a property -- eg., to signal that a particular node property has changed.</p>
 * 
 * @author 	sruffner
 */
public enum FGNProperty
{
   FONTFAMILY("font"), FONTSTYLE("font style"), FONTSIZE("font size"), ALTFONT("alternate font"), 
   PSFONT("Postscript font"), FILLC ("text/fill color"), STROKEC("stroke color"), STROKEW("stroke width"), 
   STROKECAP("stroke endcap decoration"), STROKEJOIN("stroke join decoration"),
   STROKEPATN("stroke dash/gap pattern"), X("x-coordinate"), Y("y-coordinate"), WIDTH("width"), HEIGHT("height"), 
   ROTATE("rotation angle"), BORDER("border width"), TITLE("title"), HALIGN("horizontal alignment"), 
   VALIGN("vertical alignment"), TYPE("type"), SIZE("size"), X2("x2"), Y2("y2"), LAYOUT("quadrant layout"), 
   CLIP("clip"), HIDE("hide"), INTV("interval"), START("start"), END("end"), SCALE("scale factor"), 
   PERLOGINTV("perLogIntv"), DIR("dir"), 
   LEN("len"), GAP("gap"), FMT("fmt"), UNITS("units"), SPACER("spacer"), LABELOFFSET("label offset"), 
   CAP("endcap type"), CAPSIZE("endcap size"), PRIMARY("primary"), AUTO("auto"), MID("mid"), X0("x0"), X1("x1"), 
   DX("dx"), LEGEND("includeInLegend"), MODE("display mode"), XOFF("X offset"), YOFF("Y offset"), BARWIDTH("bar width"),
   BASELINE("baseline"), SKIP("plot skip interval"), SRC("data source"), NBINS("number of bins"),
   CMAP("color map"), CMODE("color mapping mode"), EDGE("edge"), CMAPNAN("NaN color"), SMOOTH("image smoothing"),
   AVG("display average"), LOG2("log base2"), AUTORANGE("auto-range axes"), BKGC("background fill"), 
   IMG("source image"), CROP("cropping rectangle"), LINEHT("text line height"), CUSTOMTCKLBL("custom tick mark labels"),
   DGCOLOR("data group color"), DGLABEL("data group label"), ID("object ID"), NOTE("note"),
   IRAD("inner radius"), ORAD("outer radius"), SLICEOFS("relative slice offset"), 
   DGSLICE("data group slice displaced flag"), DEPTH("depth"), PSCALE("projection scale"), ELEVATE("elevation angle"),
   RANGE("range"), MESHLIMIT("mesh size limit"), LEVELS("contour level list"), STEMMED("stemmed"),
   PROJSZ("projected dot size"), PROJC("projected dot color"), BOXC("box background color"), GRIDONTOP("grid on top"),
   PDIVS("polar grid divisions"), ANGLE("reference angle");
   
   private String niceName;
   
   FGNProperty(String s) { niceName = s; }
   
   @Override
   public String toString() { return(super.toString().toLowerCase()); }

   /**
    * The name of the property as it should appear in GUI components.
    * 
    * @return Human-readable name of property type for GUI display.
    */
   public String getNiceName() { return(niceName); }

   /**
    * Is specified property one of the supported font-related properties?
    * 
    * @param prop The graphics node property to check.
    * @return <code>True</code> if it is one of <code>FONTFAMILY, FONTSTYLE, FONTSIZE, ALTFONT, PSFONT</code>.
    */
   static boolean isFontProperty(FGNProperty prop)
   {
      return(prop==FONTFAMILY || prop==FONTSTYLE || prop==FONTSIZE || prop==ALTFONT || prop==PSFONT);
   }
}
