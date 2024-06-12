package com.srscicomp.fc.fig;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.srscicomp.common.g2dutil.Marker;
import com.srscicomp.common.g2dutil.StrokeCap;
import com.srscicomp.common.g2dutil.StrokeJoin;
import com.srscicomp.common.ui.BkgFill;
import com.srscicomp.common.ui.FontStyle;
import com.srscicomp.common.ui.GenericFont;
import com.srscicomp.common.util.Utilities;

/**
 * <code>FGNPreferences</code> is an application singleton encapsulating all user-defined style preferences relevant to
 * the <i>DataNav</i> figure model. It should be initialized from current user workspace settings at startup, updated 
 * whenever the user changes any style preference value, and persisted to the workspace settings at application exit.
 * The various graphic node constructors access this singleton to initialize those node properties that have 
 * user-specified preferences.
 * 
 * <p>NOTE: This class was created to remove dependency of the com.srscicomp.fc.fig package on the workspace
 * singleton in com.srscicomp.fc.uibase. <b>[Don't remember why this was done. Probably unnecessary.</b></p>
 * 
 * <p>As of v4.1, there's no user-defined preference for whether auto-ranging is enabled on a newly constructed graph. 
 * With this release, it was decided that a user-defined default was unnecessary; auto-ranging is always disabled for 
 * all axes of a new graph.</p>.
 * 
 * @author sruffner
 */
public class FGNPreferences
{
   /**
    * Retrieve the application singleton encapsulating user-specified style preferences for <i>DataNav</i>.
    * @return Singleton object holding current state of all <i>DataNav</i> user preferences.
    */
   public static FGNPreferences getInstance()
   {
      if(theSingleton == null) theSingleton = new FGNPreferences();
      return(theSingleton);
   }
   
   /** The singleton instance, lazily created. */
   private static FGNPreferences theSingleton = null;
   
   /** Dummy constructor. All style preferences are set to default values. */
   private FGNPreferences() {}


   //
   // Preferred values for selected node properties in DataNav's figure model
   //
   
   private String font = "Arial";
   
   /**
    * Get user's preference for the font of a newly created figure.
    * @return The name of the preferred font family (does not necessarily correspond to a real font on host!)
    */
   public String getPreferredFont() { return(font); }
   
   /**
    * Set user's preference for the font of a newly created figure.
    * @param fontName The preferred font family name. Rejected silently if <code>null</code> or empty.
    */
   public void setPreferredFont(String fontName)
   {
      if(fontName != null && fontName.length() > 0) font = fontName;
   }
   
   private PSFont psfont = PSFont.HELVETICA;
   
   /**
    * Get user's preference for the Postscript font of a newly created figure.
    * @return The preferred Postscript font.
    */
   public PSFont getPreferredPSFont() { return(psfont); }
   
   /**
    * Set user's preference for the Postscript font of a newly created figure.
    * @param f The preferred Postscript font. <code>Null</code> value is ignored.
    */
   public void setPreferredPSFont(PSFont f) { if(f != null) psfont = f; }
   
   private GenericFont altFont = GenericFont.SERIF;
   
   /**
    * Get user's preference for the alternate generic font of a newly created figure.
    * @return The preferred generic font.
    */
   public GenericFont getPreferredAltFont() { return(altFont); }
   
   /**
    * Set user's preference for the alternate generic font of a newly created figure.
    * @param f The preferred generic font. <code>Null</code> value is ignored.
    */
   public void setPreferredAltFont(GenericFont f){ if(f != null) altFont = f; }
   
   private Integer fontSize = 12;
   
   /**
    * Get user's preference for the font size of a newly created figure.
    * @return The preferred font size in typographical points.
    */
   public Integer getPreferredFontSize() { return(fontSize); }
   
   /**
    * Set user's preference for the font size of a newly created figure.
    * @param sz The preferred font size in typographical point. <code>Null</code> value is ignored.
    */
   public void setPreferredFontSize(Integer sz) {  if(sz != null) fontSize = sz; }
   
   private FontStyle fontStyle = FontStyle.PLAIN;

   /**
    * Get user's preference for the font style of a newly created figure.
    * @return The preferred font style.
    */
   public FontStyle getPreferredFontStyle() { return(fontStyle); }
   
   /**
    * Set user's preference for the font style of a newly created figure.
    * @param style The preferred font style. <code>Null</code> value is ignored.
    */
   public void setPreferredFontStyle(FontStyle style) { if(style != null) fontStyle = style; }
   
   private Color fillColor = Color.BLACK;
   
   /**
    * Get user's preference for the text/fill color of a newly created figure.
    * @return The preferred fill color.
    */
   public Color getPreferredFillColor() { return(fillColor); }
   
   /**
    * Set user's preference for the text/fill color of a newly created figure.
    * @param c The preferred fill color. <code>Null</code> value is ignored.
    */
   public void setPreferredFillColor(Color c) { if(c != null) fillColor = c; }
   
   private Color strokeColor = Color.BLACK;
   
   /**
    * Get user's preference for the stroke color of a newly created figure.
    * @return The preferred stroke color.
    */
   public Color getPreferredStrokeColor() { return(strokeColor); }
   
   /**
    * Set user's preference for the stroke color of a newly created figure.
    * @param c The preferred stroke color. <code>Null</code> value is ignored.
    */
   public void setPreferredStrokeColor(Color c) { if(c != null) strokeColor = c; }
   
   private Measure strokeWidth = new Measure(0.01, Measure.Unit.IN)
   ;
   /**
    * Get user's preference for the stroke width of a newly created figure.
    * @return The preferred stroke width.
    */
   public Measure getPreferredStrokeWidth() { return(strokeWidth); }
   
   /**
    * Set user's preference for the stroke width of a newly created figure.
    * @param c The preferred stroke width. <code>Null</code> value is ignored.
    */
   public void setPreferredStrokeWidth(Measure m) { if(m != null) strokeWidth = m; }
   
   private StrokeCap strokeEndcap = StrokeCap.BUTT;
   
   /**
    * Get user's preference for the stroke endcap style of a newly created figure.
    * @return The preferred stroke endcap style.
    */
   public StrokeCap getPreferredStrokeEndcap() { return(strokeEndcap); }
   
   /**
    * Set user's preference for the stroke endcap style of a newly created figure.
    * @param sc The preferred stroke endcap style. <code>Null</code> value is ignored.
    */
   public void setPreferredStrokeEndcap(StrokeCap sc) { if(sc != null) strokeEndcap = sc; }
   
   private StrokeJoin strokeJoin = StrokeJoin.MITER;
   
   /**
    * Get user's preference for the stroke join style of a newly created figure.
    * @return The preferred stroke join style.
    */
   public StrokeJoin getPreferredStrokeJoin() { return(strokeJoin); }
   
   /**
    * Set user's preference for the stroke join style of a newly created figure.
    * @param sj The preferred stroke join style. <code>Null</code> value is ignored.
    */
   public void setPreferredStrokeJoin(StrokeJoin sj) { if(sj != null) strokeJoin = sj; }
   
   private Boolean legendSymbolAtMidPoint = new Boolean(true);
   
   /**
    * Get user's preference on whether symbols should be rendered at the midpoint or endpoints of a legend entry's 
    * line segment.
    * @return <code>Boolean.TRUE</code> if midpoint legend symbols are preferred.
    */
   public Boolean getPreferredLegendSymbolAtMidPoint() { return(legendSymbolAtMidPoint); }
   
   /**
    * Set user's preference on whether symbols should be rendered at the midpoint or endpoints of a legend entry's 
    * line segment.
    * @return atMid If <code>Boolean.TRUE</code>, midpoint legend symbols are preferred. <code>Null</code> is ignored.
    */
   public void setPreferredLegendSymbolAtMidPoint(Boolean atMid) { if(atMid != null) legendSymbolAtMidPoint = atMid; }

   private Measure legendSpacer = new Measure(0.25, Measure.Unit.IN);
   
   /**
    * Get user's preference for the distance between successive entries in a newly created legend.
    * @return The preferred distance between successive legend entries.
    */
   public Measure getPreferredLegendSpacer() { return(legendSpacer); }
   
   /**
    * Set user's preference for the distance between successive entries in a newly created legend.
    * @param m The preferred distance between successive legend entries. <code>Null</code> value is ignored.
    */
   public void setPreferredLegendSpacer(Measure m) { if(m != null) legendSpacer = m; }
   
   private Measure legendLabelOffset = new Measure(0.1, Measure.Unit.IN);
   
   /**
    * Get user's preference for the offset between trace line and text label in each entry of a newly created legend.
    * @return The preferred legend label offset.
    */
   public Measure getPreferredLegendLabelOffset() { return(legendLabelOffset); }
   
   /**
    * Set user's preference for the offset between trace line and text label in each entry of a newly created legend.
    * @param m The preferred legend label offset. Null value is ignored.
    */
   public void setPreferredLegendLabelOffset(Measure m) { if(m != null) legendLabelOffset = m; }
   
   private Measure axisSpacer = new Measure(0.1, Measure.Unit.IN);
   
   /** 
    * Get user's preference for size of spacer between an axis and the nearest edge of the parent graph's data box.
    * @return Preferred value of axis spacer property.
    */
   public Measure getPreferredAxisSpacer() { return(axisSpacer); }
   
   /** 
    * Set user's preference for size of spacer between an axis and the nearest edge of the parent graph's data box.
    * @param m The new preferred value of axis spacer property. <code>Null</code> value is ignored.
    */
   public void setPreferredAxisSpacer(Measure m) { if(m != null) axisSpacer = m; }
   
   private Measure axisLabelOffset = new Measure(0.5, Measure.Unit.IN);
   
   /** 
    * Get user's preference for the perpendicular distance between an axis and its automated label.
    * @return Preferred value of the axis label offset.
    */
   public Measure getPreferredAxisLabelOffset() { return(axisLabelOffset); }
   
   /** 
    * Set user's preference for the perpendicular distance between an axis and its automated label.
    * @param m The new preferred value for the axis label offset. <code>Null</code> value is ignored.
    */
   public void setPreferredAxisLabelOffset(Measure m) { if(m != null) axisLabelOffset = m; }
   
   private Marker calibCap = Marker.LINEDOWN;
   
   /** 
    * Get user's preference for the endpoint adornment of a newly created calibration bar.
    * @return Preferred value of a calibration bar's endpoint adornment.
    */
   public Marker getPreferredCalibCap() { return(calibCap); }
   
   /** 
    * Set user's preference for the endpoint adornment of a newly created calibration bar.
    * @param m The new preferred value for a calibration bar's endpoint adornment. <code>Null</code> value is ignored.
    */
   public void setPreferredCalibCap(Marker m) { if(m != null) calibCap = m; }
   
   private Measure calibCapSize = new Measure(0, Measure.Unit.IN);
   
   /** 
    * Get user's preference for the size of a calibration bar's endpoint adornment.
    * @return Preferred value of endpoint cap size.
    */
   public Measure getPreferredCalibCapSize() { return(calibCapSize); }
   
   /** 
    * Set user's preference for the size of a calibration bar's endpoint adornment.
    * @param m The new preferred value of endpoint cap size. <code>Null</code> value is ignored.
    */
   public void setPreferredCalibCapSize(Measure m) { if(m != null) calibCapSize = m; }
   
   private Marker ebarCap = Marker.BRACKET;
   
   /** 
    * Get user's preference for the endcap adornment attached to error bars in a data trace rendering.
    * @return Preferred value for the error bar endcap adornment.
    */
   public Marker getPreferredEBarCap() { return(ebarCap); }
   
   /** 
    * Set user's preference for the endcap adornment attached to error bars in a data trace rendering.
    * @param m The new preferred value for the error bar endcap adornment. <code>Null</code> value is ignored.
    */
   public void setPreferredEBarCap(Marker m) { if(m != null) ebarCap = m; }
   
   private Measure ebarCapSize = new Measure(0.2, Measure.Unit.IN);
   
   /** 
    * Get user's preference for the size of an error bar's endpoint adornment.
    * @return Preferred value for the error bar endcap size.
    */
   public Measure getPreferredEBarCapSize() { return(ebarCapSize); }
   
   /** 
    * Set user's preference for the size of an error bar's endpoint adornment.
    * @param m The new preferred value for the error bar endcap size. <code>Null</code> value is ignored.
    */
   public void setPreferredEBarCapSize(Measure m) { if(m != null) ebarCapSize = m; }
   
   private LogTickPattern logTickPattern = LogTickPattern.fromString("");
   
   /** 
    * Get user's preference for the per-decade tick pattern along a logarithmic axis.
    * @return Preferred value for the per-decade tick pattern.
    */
   public LogTickPattern getPreferredLogTickPattern() { return(logTickPattern); }
   
   /** 
    * Set user's preference for the per-decade tick pattern along a logarithmic axis.
    * @param m The new preferred value for the per-decade tick pattern. <code>Null</code> value is ignored.
    */
   public void setPreferredLogTickPattern(LogTickPattern p) { if(p != null) logTickPattern = p; }
   
   private TickSetNode.Orientation tickOrientation = TickSetNode.Orientation.OUT;
   
   /** 
    * Get user's preference for the orientation of a tick mark set.
    * @return Preferred value for the tick mark orientation.
    */
   public TickSetNode.Orientation getPreferredTickOrientation()  { return(tickOrientation); }
   
   /** 
    * Set user's preference for the orientation of a tick mark set.
    * @param ori The new preferred value for the tick mark orientation. <code>Null</code> value is ignored.
    */
   public void setPreferredTickOrientation(TickSetNode.Orientation ori) { if(ori!=null) tickOrientation = ori; }
   
   private Measure tickLength = new Measure(0.06, Measure.Unit.IN);
   
   /** 
    * Get user's preference for the length of a tick mark.
    * @return Preferred value for tick mark length.
    */
   public Measure getPreferredTickLength() { return(tickLength); }
   
   /** 
    * Set user's preference for the length of a tick mark.
    * @param m The new preferred value for tick mark length. <code>Null</code> value is ignored.
    */
   public void setPreferredTickLength(Measure m) { if(m != null) tickLength = m; }
   
   private TickSetNode.LabelFormat tickLabelFormat = TickSetNode.LabelFormat.INT;
   
   /** 
    * Get user's preference for a tick mark label's format.
    * @return Preferred value for the tick mark label format.
    */
   public TickSetNode.LabelFormat getPreferredTickLabelFormat() { return(tickLabelFormat); }
   
   /** 
    * Set user's preference for a tick mark label's format.
    * @param fmt The new preferred value for the tick mark label format. <code>Null</code> value is ignored.
    */
   public void setPreferredTickLabelFormat(TickSetNode.LabelFormat fmt) { if(fmt != null) tickLabelFormat = fmt; }
   
   private Measure tickGap = new Measure(0.04, Measure.Unit.IN);
   
   /** 
    * Get user's preference for the gap size between ticks and corresponding labels, for a tick mark set.
    * @return Preferred value for tick-label gap size.
    */
   public Measure getPreferredTickGap() { return(tickGap); }
   
   /** 
    * Set user's preference for the gap size between ticks and corresponding labels, for a tick mark set.
    * @param m The new preferred value for tick-label gap size. <code>Null</code> value is ignored.
    */
   public void setPreferredTickGap(Measure m) { if(m != null) tickGap = m; }
   
   private Boolean enaHMImageSmoothing = new Boolean(false);
   
   /**
    * Get user's preference on whether or not image smoothing should be enabled for a new heat map node.
    * @return True if image smoothing should be enabled initially for a new heat map node, else false.
    */
   public Boolean getPreferredHeatMapImageSmoothingEnable() { return(enaHMImageSmoothing); }
   
   /**
    * Set user's preference on whether or not image smoothing should be enabled for a new heat map node.
    * @return ena True/false to enable/disable image smoothing for a new heat map node. A null value is ignored.
    */
   public void setPreferredHeatMapImageSmoothingEnable(Boolean ena) { if(ena != null) enaHMImageSmoothing = ena; }

   private List<ColorMap> customCMaps = new ArrayList<ColorMap>();
   
   /** 
    * Get the user's custom-defined color maps.
    * 
    * @return List of the user's custom color maps.
    */
   public List<ColorMap> getCustomColorMaps() { return(new ArrayList<ColorMap>(customCMaps)); }
   
   /** 
    * Convenience method returns all available color maps: All built-in maps, followed by any additional custom maps
    * that have been defined by the user and stored in his/her workspace settings.
    * @return Array containing all defined color maps.
    */
   public ColorMap[] getAllAvailableColorMaps()
   {
      List<ColorMap> maps = new ArrayList<ColorMap>();
      for(ColorMap cm : ColorMap.getBuiltins()) maps.add(cm);
      maps.addAll(customCMaps);
      return(maps.toArray(new ColorMap[maps.size()]));
   }
   
   /**
    * Add a color map to the user's list of custom-defined color maps. A color map will be rejected if it has the
    * same name or the same key frame definition as one of the built-in color maps or any of the existing custom maps.
    * 
    * @param cm The color map to add.
    * @return True if color map was added to the user's custom-defined color map list; false if rejected.
    */
   public boolean addCustomColorMap(ColorMap cm)
   {
      if(cm==null || (!cm.isCustom()) || ColorMap.duplicatesBuiltin(cm)) return(false);
      for(ColorMap ccm : customCMaps)
         if(ccm.getName().equals(cm.getName()) || ccm.equals(cm))
            return(false);
      customCMaps.add(cm);
      return(true);
   }
   
   /**
    * Remove the specified color map from the user's list of custom-defined color maps. 
    * 
    * @param cm The color map to remove.
    * @return True if successful; false if specified color map was not found.
    */
   public boolean removeCustomColorMap(ColorMap cm) { return(customCMaps.remove(cm)); }

   
   /** Key for the preferred value of the <i>font family</i> property for a figure. */
   private final static String KEY_FIG_FONT = "figFont";
   /** Key for the preferred value of the <i>Postscript font</i> property for a figure. */
   private final static String KEY_FIG_PSFONT = "figPSFont";
   /** Key for the preferred value of the <i>alternate font</i> property for a figure. */
   private final static String KEY_FIG_ALTFONT = "figAltFont";
   /** Key for the preferred value of the <i>font size</i> property for a figure. */
   private final static String KEY_FIG_FONTSZ = "figFontSz";
   /** Key for the preferred value of the <i>font style</i> property for a figure. */
   private final static String KEY_FIG_FONTSTYLE = "figFontStyle";
   /** Key for the preferred value of the <em>text/fill color</em> property for a figure. */
   private final static String KEY_FIG_FILLC = "figFillC";
   /** Key for the preferred value of the <em>stroke color</em> property for a figure. */
   private final static String KEY_FIG_STROKEC = "figStrokeC";
   /** Key for the preferred value of the <em>stroke width</em> property for a figure. */
   private final static String KEY_FIG_STROKEW = "figStrokeW";
   /** Key for the preferred value of the <em>stroke endcap</em> property for a figure. */
   private final static String KEY_FIG_STROKECAP = "figStrokeCap";
   /** Key for the preferred value of the <em>stroke join</em> property for a figure. */
   private final static String KEY_FIG_STROKEJOIN = "figStrokeJoin";
   /** Key for the preferred value of the <em>mid</em> property for a graph legend. */
   private final static String KEY_LEGEND_MID = "legendMid";
   /** Key for the preferred value of the <em>spacer</em> property for a graph legend. */
   private final static String KEY_LEGEND_SPACER = "legendSpacer";
   /** [As of v4.4.0] Key for the preferred value of the <em>labelOffset</em> property for a graph legend. */
   private final static String KEY_LEGEND_LABELOFFSET = "legendLabelOffset";
   /** Key for the preferred value of the <em>spacer</em> property for a graph axis. */
   private final static String KEY_AXIS_SPACER = "axisSpacer";
   /** Key for the preferred value of the <em>label offset</em> property for a graph axis. */
   private final static String KEY_AXIS_LABELOFFSET = "axisLOffset";
   /** Key for the preferred value of the <em>end cap</em> property for a calibration bar. */
   private final static String KEY_CALIB_CAP = "calibCap";
   /** Key for the preferred value of the <em>end cap size</em> property for a calibration bar. */
   private final static String KEY_CALIB_CAPSZ = "calibCapSz";
   /** Key for the preferred value of the <em>end cap</em> property for an error bar node. */
   private final static String KEY_EBAR_CAP = "ebarCap";
   /** Key for the preferred value of the <em>end cap size</em> property for an error bar node. */
   private final static String KEY_EBAR_CAPSZ = "ebarCapSz";
   /** Key for the preferred value of the <em>perLogIntv</em> property for a tick set. */
   private final static String KEY_TICK_PERLOG = "tickPerLog";
   /** Key for the preferred value of the <em>tick direction</em> property for a tick set. */
   private final static String KEY_TICK_DIR = "tickDir";
   /** Key for the preferred value of the <em>tick length</em> property for a tick set. */
   private final static String KEY_TICK_LEN = "tickLen";
   /** Key for the preferred value of the <em>tick label format</em> property for a tick set. */
   private final static String KEY_TICK_FMT = "tickFmt";
   /** Key for the preferred value of the <em>tick gap</em> property for a tick set. */
   private final static String KEY_TICK_GAP = "tickGap";
   /** (As of v4.5.5) Key for the preferred value of the <i>smooth</i> property for a heat map node. */
   private final static String KEY_HM_SMOOTH = "hmSmooth";
   /** (As of v5.4.3) Key containing list of user's custom color maps. */
   private final static String KEY_CMAPS = "customCMaps";
   /** 
    * (25Jan2011) Key for preferred value of the <i>enable auto-scaling</i> property for a graph axis.
    * (21Mar2013) Deprecated as of version 4.1.0. Property is simply ignored.
    */
   @Deprecated private final static String KEY_AXIS_AUTO = "axisAutoScale";

   /**
    * Load the user-defined style preferences from the workspace settings.
    * @param wsSettings The workspace settings object. If null, no action is taken.
    */
   public void load(Properties wsSettings)
   {
      if(wsSettings == null) return;
      
      String s = wsSettings.getProperty(KEY_FIG_FONT);
      setPreferredFont(s);
      s = wsSettings.getProperty(KEY_FIG_PSFONT);
      setPreferredPSFont(Utilities.getEnumValueFromString(s, PSFont.values()));
      s = wsSettings.getProperty(KEY_FIG_ALTFONT);
      setPreferredAltFont(Utilities.getEnumValueFromString(s, GenericFont.values()));

      s = wsSettings.getProperty(KEY_FIG_FONTSZ);
      try 
      {
         Integer sz = Integer.parseInt(s);
         setPreferredFontSize(sz);
      }
      catch(NumberFormatException nfe) {}
      
      s = wsSettings.getProperty(KEY_FIG_FONTSTYLE);
      setPreferredFontStyle(Utilities.getEnumValueFromString(s, FontStyle.values()));
      s = wsSettings.getProperty(KEY_FIG_FILLC);
      setPreferredFillColor(BkgFill.colorFromHexString(s));
      s = wsSettings.getProperty(KEY_FIG_STROKEC);
      setPreferredStrokeColor(BkgFill.colorFromHexString(s));
      s = wsSettings.getProperty(KEY_FIG_STROKEW);
      setPreferredStrokeWidth(Measure.fromString(s));
      s = wsSettings.getProperty(KEY_FIG_STROKECAP);
      setPreferredStrokeEndcap(Utilities.getEnumValueFromString(s, StrokeCap.values()));
      s = wsSettings.getProperty(KEY_FIG_STROKEJOIN);
      setPreferredStrokeJoin(Utilities.getEnumValueFromString(s, StrokeJoin.values()));
      s = wsSettings.getProperty(KEY_LEGEND_MID);
      setPreferredLegendSymbolAtMidPoint(new Boolean("true".equals(s)));
      s = wsSettings.getProperty(KEY_LEGEND_SPACER);
      setPreferredLegendSpacer(Measure.fromString(s));
      s = wsSettings.getProperty(KEY_LEGEND_LABELOFFSET);
      setPreferredLegendLabelOffset(Measure.fromString(s));
      s = wsSettings.getProperty(KEY_AXIS_SPACER);
      setPreferredAxisSpacer(Measure.fromString(s));
      s = wsSettings.getProperty(KEY_AXIS_LABELOFFSET);
      setPreferredAxisLabelOffset(Measure.fromString(s));
      s = wsSettings.getProperty(KEY_CALIB_CAP);
      setPreferredCalibCap(Utilities.getEnumValueFromString(s, Marker.values()));
      s = wsSettings.getProperty(KEY_CALIB_CAPSZ);
      setPreferredCalibCapSize(Measure.fromString(s));
      s = wsSettings.getProperty(KEY_EBAR_CAP);
      setPreferredEBarCap(Utilities.getEnumValueFromString(s, Marker.values()));
      s = wsSettings.getProperty(KEY_EBAR_CAPSZ);
      setPreferredEBarCapSize(Measure.fromString(s));
      s = wsSettings.getProperty(KEY_TICK_PERLOG);
      setPreferredLogTickPattern(LogTickPattern.fromString(s));
      s = wsSettings.getProperty(KEY_TICK_DIR);
      setPreferredTickOrientation(Utilities.getEnumValueFromString(s, TickSetNode.Orientation.values()));
      s = wsSettings.getProperty(KEY_TICK_LEN);
      setPreferredTickLength(Measure.fromString(s));
      s = wsSettings.getProperty(KEY_TICK_FMT);
      setPreferredTickLabelFormat(Utilities.getEnumValueFromString(s, TickSetNode.LabelFormat.values()));
      s = wsSettings.getProperty(KEY_TICK_GAP);
      setPreferredTickGap(Measure.fromString(s));
      s = wsSettings.getProperty(KEY_HM_SMOOTH);
      setPreferredHeatMapImageSmoothingEnable(new Boolean(s==null || "true".equals(s)));

      // reconstruct custom color maps (comma-separated list of string)
      s = wsSettings.getProperty(KEY_CMAPS);
      customCMaps.clear();
      if(s != null && s.length() > 0)
      {
         String[] tokens = s.split(",");
         for(String token : tokens)
         {
            ColorMap cm = ColorMap.fromString(token);
            if(cm != null) addCustomColorMap(cm);
         }
      }
      
      // remove deprecated preferences from the settings object, just in case.
      wsSettings.remove(KEY_AXIS_AUTO);
   }
   
   /**
    * Store the current user-defined style preferences in the workspace settings.
    * @param wsSettings The workspace settings object. If null, no action is taken.
    */
   public void save(Properties wsSettings)
   {
      wsSettings.setProperty(KEY_FIG_FONT, getPreferredFont());
      wsSettings.setProperty(KEY_FIG_PSFONT, getPreferredPSFont().toString());
      wsSettings.setProperty(KEY_FIG_ALTFONT, getPreferredAltFont().toString());
      wsSettings.setProperty(KEY_FIG_FONTSZ, getPreferredFontSize().toString());
      wsSettings.setProperty(KEY_FIG_FONTSTYLE, getPreferredFontStyle().toString());
      wsSettings.setProperty(KEY_FIG_FILLC, BkgFill.colorToHexString(getPreferredFillColor()));
      wsSettings.setProperty(KEY_FIG_STROKEC, BkgFill.colorToHexString(getPreferredStrokeColor()));
      
      wsSettings.setProperty(KEY_FIG_STROKEW, getPreferredStrokeWidth().toString());
      wsSettings.setProperty(KEY_FIG_STROKECAP, getPreferredStrokeEndcap().toString());
      wsSettings.setProperty(KEY_FIG_STROKEJOIN, getPreferredStrokeJoin().toString());
      wsSettings.setProperty(KEY_LEGEND_MID, getPreferredLegendSymbolAtMidPoint().toString());
      wsSettings.setProperty(KEY_LEGEND_SPACER, getPreferredLegendSpacer().toString());
      wsSettings.setProperty(KEY_LEGEND_LABELOFFSET, getPreferredLegendLabelOffset().toString());
      wsSettings.setProperty(KEY_AXIS_SPACER, getPreferredAxisSpacer().toString());
      wsSettings.setProperty(KEY_AXIS_LABELOFFSET, getPreferredAxisLabelOffset().toString());
      wsSettings.setProperty(KEY_CALIB_CAP, getPreferredCalibCap().toString());
      wsSettings.setProperty(KEY_CALIB_CAPSZ, getPreferredCalibCapSize().toString());
      wsSettings.setProperty(KEY_EBAR_CAP, getPreferredEBarCap().toString());
      wsSettings.setProperty(KEY_EBAR_CAPSZ, getPreferredEBarCapSize().toString());
      wsSettings.setProperty(KEY_TICK_PERLOG, getPreferredLogTickPattern().toString());
      wsSettings.setProperty(KEY_TICK_DIR, getPreferredTickOrientation().toString());
      wsSettings.setProperty(KEY_TICK_LEN, getPreferredTickLength().toString());
      wsSettings.setProperty(KEY_TICK_FMT, getPreferredTickLabelFormat().toString());
      wsSettings.setProperty(KEY_TICK_GAP, getPreferredTickGap().toString());
      wsSettings.setProperty(KEY_HM_SMOOTH, getPreferredHeatMapImageSmoothingEnable().toString());
      
      // all custom color maps are stored as comma-separated string
      StringBuilder sb = new StringBuilder();
      for(ColorMap cm : customCMaps) sb.append(ColorMap.asString(cm)).append(",");
      if(sb.length() > 0) sb.setLength(sb.length()-1);  // get rid of trailing comma
      wsSettings.setProperty(KEY_CMAPS, sb.toString());
   }
}
