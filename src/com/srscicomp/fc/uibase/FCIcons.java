package com.srscicomp.fc.uibase;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.srscicomp.common.ui.GUIUtilities;

/**
 * A singleton class which loads image icons and cursors used in the <i>Figure Composer</i> GUI and provides access to 
 * them. All icons are 16x16 unless otherwise noted. Cursors are sized IAW the best cursor size reported by the 
 * default AWT toolkit.
 * 
 * <p><b>NOTE: Do NOT use the file system-like parent directory shortcut "../" to specify a relative path to a resource.
 * It does not work when the app is packaged as an executable JAR!</b></p>
 * @author sruffner
 */
public class FCIcons
{
   /** All icon image files are expected to be found in this package, or icon loading will fail miserably. */
   private final static String RESPATH = "/com/srscicomp/fc/resources/";
   
   /** A standard pixel gap size for spacing adjacent components on the user interface. */
   public final static int UIGAPSZ = 3;

   // FigureComposer application icon at different sizes.
   public final static ImageIcon FC_APP16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "fc_app16.png", "");
   public final static ImageIcon FC_APP32 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "fc_app32.png", "");
   public final static ImageIcon FC_APP48 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "fc_app48.png", "");
   public final static ImageIcon FC_APP128 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "fc_app128.png", "");

   public final static ImageIcon V4_FWD_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_forward_22.png", "");
   public final static ImageIcon V4_BACK_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_back_22.png", "");
   public final static ImageIcon V4_ADD_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_add_22.png", "");
   public final static ImageIcon V4_DELETE_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_delete_22.png", "");
   public final static ImageIcon V4_DELETE_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_delete_16.png", "");
   public final static ImageIcon V4_DEL_DISABLED_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_del_disabled_16.png", "");
   public final static ImageIcon V4_CLOSE_24 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_close_24.png", "");

   public final static ImageIcon V4_DATA_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_data_16.png", "");
   public final static ImageIcon V4_BROKEN = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_broken.png", "");
   public final static ImageIcon V4_OK_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_ok_16.png", "");
   public final static ImageIcon V4_NOTOK_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_notok_16.png", "");
   public final static ImageIcon V4_HELP_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_help_16.png", "");

   public final static ImageIcon V4_QUAD1 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_quad1.png", "");
   public final static ImageIcon V4_QUAD2 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_quad2.png", "");
   public final static ImageIcon V4_QUAD3 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_quad3.png", "");
   public final static ImageIcon V4_QUAD4 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_quad4.png", "");
   public final static ImageIcon V4_ALLQUAD = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_allquad.png", "");
   
   public final static ImageIcon V4_NEW_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_new_22.png", "");
   public final static ImageIcon V4_DUPE_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_dupe_22.png", "");
   public final static ImageIcon V4_OPEN_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_open_22.png", "");
   public final static ImageIcon V4_OPENRECENT_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_openrecent_22.png", "");
   public final static ImageIcon V4_SAVE_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_save_22.png", "");
   public final static ImageIcon V4_SAVEAS_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_saveas_22.png", "");
   public final static ImageIcon V4_EXPORT_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_export_22.png", "");
   public final static ImageIcon V4_PAGESETUP_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_pagesetup_22.png", "");
   public final static ImageIcon V4_PRINT_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_print_22.png", "");

   public final static ImageIcon V4_SCALETOFITON_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_scaletofiton_22.png", "");
   public final static ImageIcon V4_SCALETOFITOFF_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_scaletofitoff_22.png", "");
   public final static ImageIcon V4_RESETZOOM_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_resetzoom_22.png", "");
   public final static ImageIcon V4_REFRESH_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_refresh_22.png", "");
   public final static ImageIcon V4_INSCHAR_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_inschar_22.png", "");
   public final static ImageIcon V4_ENTER_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_enter_22.png", "");

   public final static ImageIcon V4_UNDO_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_undo_16.png", "");
   public final static ImageIcon V4_UNDO_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_undo_22.png", "");
   public final static ImageIcon V4_REDO_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_redo_22.png", "");
   public final static ImageIcon V4_CUT_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_cut_22.png", "");
   public final static ImageIcon V4_COPY_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_copy_16.png", "");
   public final static ImageIcon V4_COPY_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_copy_22.png", "");
   public final static ImageIcon V4_PASTE_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_paste_22.png", "");
   public final static ImageIcon V4_COPYSTYLE_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_copystyle_22.png", "");
   public final static ImageIcon V4_PASTESTYLE_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_pastestyle_22.png", "");
   public final static ImageIcon V4_EXTDATA_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_extdata_22.png", "");
   public final static ImageIcon V4_INJDATA_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_injdata_22.png", "");

   public final static ImageIcon V4_MATFIG_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_matlabfig_16.png", "");
   public final static ImageIcon V4_FIGFOLDER_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_figfolder_16.png", "");

   public final static ImageIcon V4_FIGURE_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_figure_16.png", "");
   public final static ImageIcon V4_FIGURE_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_figure_22.png", "");
   public final static ImageIcon V4_FIGURE_32 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_figure_32.png", "");
   public final static ImageIcon V4_GRAPH_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_graph_22.png", "");
   public final static ImageIcon V4_GRAPH_32 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_graph_32.png", "");
   public final static ImageIcon V4_LABEL_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_label_22.png", "");
   public final static ImageIcon V4_LABEL_32 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_label_32.png", "");
   public final static ImageIcon V4_TEXTBOX_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_textbox_22.png", "");
   public final static ImageIcon V4_TEXTBOX_32 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_textbox_32.png", "");
   public final static ImageIcon V4_SHAPE_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_shape_22.png", "");
   public final static ImageIcon V4_SHAPE_32 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_shape_32.png", "");
   public final static ImageIcon V4_IMAGE_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_image_22.png", "");
   public final static ImageIcon V4_IMAGE_32 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_image_32.png", "");
   public final static ImageIcon V4_LINE_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_line_22.png", "");
   public final static ImageIcon V4_LINE_32 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_line_32.png", "");
   public final static ImageIcon V4_TRACE_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_trace_16.png", "");
   public final static ImageIcon V4_TRACE_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_trace_22.png", "");
   public final static ImageIcon V4_TRACE_32 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_trace_32.png", "");
   public final static ImageIcon V4_FUNCTION_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_function_22.png", "");
   public final static ImageIcon V4_FUNCTION_32 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_function_32.png", "");
   public final static ImageIcon V4_RASTER_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_raster_16.png", "");
   public final static ImageIcon V4_RASTER_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_raster_22.png", "");
   public final static ImageIcon V4_RASTER_32 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_raster_32.png", "");
   public final static ImageIcon V4_HEATMAP_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_heatmap_16.png", "");
   public final static ImageIcon V4_HEATMAP_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_heatmap_22.png", "");
   public final static ImageIcon V4_HEATMAP_32 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_heatmap_32.png", "");
   public final static ImageIcon V4_BAR_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_bar_22.png", "");
   public final static ImageIcon V4_BAR_32 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_bar_32.png", "");
   public final static ImageIcon V4_BOX_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_box_22.png", "");
   public final static ImageIcon V4_BOX_32 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_box_32.png", "");
   public final static ImageIcon V4_SCATTER_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_scatter_22.png", "");
   public final static ImageIcon V4_SCATTER_32 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_scatter_32.png", "");
   public final static ImageIcon V4_AREA_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_area_22.png", "");
   public final static ImageIcon V4_AREA_32 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_area_32.png", "");
   public final static ImageIcon V4_PIE_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_pie_22.png", "");
   public final static ImageIcon V4_PIE_32 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_pie_32.png", "");
   public final static ImageIcon V4_CALIB_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_calib_22.png", "");
   public final static ImageIcon V4_CALIB_32 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_calib_32.png", "");
   public final static ImageIcon V4_AXIS_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_axis_22.png", "");
   public final static ImageIcon V4_ZAXIS_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_zaxis_22.png", "");
   public final static ImageIcon V4_LEGEND_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_legend_22.png", "");
   
   public final static ImageIcon V4_GRAPH3D_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_graph3d_22.png", "");
   public final static ImageIcon V4_GRAPH3D_32 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_graph3d_32.png", "");
   public final static ImageIcon V4_SCATTER3D_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_scatter3d_22.png", "");
   public final static ImageIcon V4_SCATTER3D_32 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_scatter3d_32.png", "");
   public final static ImageIcon V4_SURFACE_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_surface_22.png", "");
   public final static ImageIcon V4_SURFACE_32 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_surface_32.png", "");

   public final static ImageIcon V4_POLARG_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_polarg_22.png", "");
   public final static ImageIcon V4_POLARG_32 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_polarg_32.png", "");


   /** The "zoom-in" cursor for the figure composer canvas. */
   public final static Cursor V4_ZOOMCURSOR;
   /** The "image crop" cursor for the figure composer canvas. */
   public final static Cursor V4_CROPCURSOR;
   /** The "move node" cursor for the figure composer canvas. */
   public final static Cursor V4_MOVECURSOR;
   /** A non-directional "resize node" cursor for the figure composer canvas. */
   public final static Cursor V4_RESIZECURSOR;
   /** The "rotate 3D graph" cursor for the figure composer canvas. */
   public final static Cursor V4_ROT3DCURSOR;

   static
   {
      Toolkit tk = Toolkit.getDefaultToolkit();
      Dimension best = tk.getBestCursorSize(16, 16);
      BufferedImage sizedTransBim = new BufferedImage(best.width, best.height, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = sizedTransBim.createGraphics();
      ImageIcon icon = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_zoomcursor16.png", "");
      g.drawImage(icon.getImage(), 0, 0, null);
      V4_ZOOMCURSOR = tk.createCustomCursor(sizedTransBim, new Point(8,5), "zoom"); 
      g.dispose();
      
      sizedTransBim = new BufferedImage(best.width, best.height, BufferedImage.TYPE_INT_ARGB);
      g = sizedTransBim.createGraphics();
      icon = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_cropcursor16.png", "");
      g.drawImage(icon.getImage(), 0, 0, null);
      V4_CROPCURSOR = tk.createCustomCursor(sizedTransBim, new Point(3,3), "crop"); 
      g.dispose();
      
      sizedTransBim = new BufferedImage(best.width, best.height, BufferedImage.TYPE_INT_ARGB);
      g = sizedTransBim.createGraphics();
      icon = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_movecursor16.png", "");
      g.drawImage(icon.getImage(), 0, 0, null);
      V4_MOVECURSOR = tk.createCustomCursor(sizedTransBim, new Point(8,8), "move"); 
      g.dispose();
      
      sizedTransBim = new BufferedImage(best.width, best.height, BufferedImage.TYPE_INT_ARGB);
      g = sizedTransBim.createGraphics();
      icon = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_resizecursor16.png", "");
      g.drawImage(icon.getImage(), 0, 0, null);
      V4_RESIZECURSOR = tk.createCustomCursor(sizedTransBim, new Point(8,8), "resize"); 
      g.dispose();

      sizedTransBim = new BufferedImage(best.width, best.height, BufferedImage.TYPE_INT_ARGB);
      g = sizedTransBim.createGraphics();
      icon = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_rotcursor_16.png", "");
      g.drawImage(icon.getImage(), 0, 0, null);
      V4_ROT3DCURSOR = tk.createCustomCursor(sizedTransBim, new Point(8,8), "rotate"); 
      g.dispose();
   }

   
   public final static ImageIcon V4_ALIGNLEFT_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_alignleft_16.png", "");
   public final static ImageIcon V4_ALIGNRIGHT_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_alignright_16.png", "");
   public final static ImageIcon V4_ALIGNCENTER_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_aligncenter_16.png", "");
   public final static ImageIcon V4_VALIGNTOP_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_valigntop_16.png", "");
   public final static ImageIcon V4_VALIGNBOT_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_valignbot_16.png", "");
   public final static ImageIcon V4_VALIGNMID_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_valignmid_16.png", "");
   public final static ImageIcon V4_FSPLAIN_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_fsplain_16.png", "");
   public final static ImageIcon V4_FSITALIC_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_fsitalic_16.png", "");
   public final static ImageIcon V4_FSBOLD_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_fsbold_16.png", "");
   public final static ImageIcon V4_FSBOLDITALIC_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_fsbolditalic_16.png", "");
   public final static ImageIcon V4_NORMSCRIPT_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_normscript.png", "");
   public final static ImageIcon V4_SUBSCRIPT_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_subscript.png", "");
   public final static ImageIcon V4_SUPERSCRIPT_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_superscript.png", "");
   public final static ImageIcon V4_CAPBUTT_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_capbutt_16.png", "");
   public final static ImageIcon V4_CAPSQUARE_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_capsquare_16.png", "");
   public final static ImageIcon V4_CAPROUND_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_capround_16.png", "");
   public final static ImageIcon V4_JOINMITER_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_joinmiter_16.png", "");
   public final static ImageIcon V4_JOINROUND_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_joinround_16.png", "");
   public final static ImageIcon V4_JOINBEVEL_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_joinbevel_16.png", "");
   public final static ImageIcon V4_MIDPT_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_midpt_16.png", "");
   public final static ImageIcon V4_ENDPT_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_endpt_16.png", "");
   public final static ImageIcon V4_TICKOUT_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_tickout_16.png", "");
   public final static ImageIcon V4_TICKIN_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_tickin_16.png", "");
   public final static ImageIcon V4_TICKTHRU_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_tickthru_16.png", "");

   public final static ImageIcon V4_PNGJPG_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_pngjpg_16.png", "");
   public final static ImageIcon V4_PSPDF_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_pspdf_16.png", "");
   public final static ImageIcon V4_BLANKDOC_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_blankdoc_16.png", "");

   public final static ImageIcon V4_LOADDATASET_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_loaddataset_16.png", "");
   public final static ImageIcon V4_EXPDATASET_16 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_expdataset_16.png", "");
   public final static ImageIcon V4_INSROW_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_insrow_22.png", "");
   public final static ImageIcon V4_INSCOL_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_inscol_22.png", "");
   public final static ImageIcon V4_APPENDROW_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_appendrow_22.png", "");
   public final static ImageIcon V4_APPENDCOL_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_appendcol_22.png", "");
   public final static ImageIcon V4_INSPASTE_22 = GUIUtilities.createImageIcon(FCIcons.class, RESPATH + "v4_inspaste_22.png", "");

   /** A 80x50 iconic image depicting some of the layout parameters for a graph legend. */
   public final static Icon LEGEND_LAYOUT = GUIUtilities.createImageIcon(FCIcons.class,RESPATH + "legend_layout.png", "");
}
