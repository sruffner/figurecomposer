package com.srscicomp.fc.uibase;

import java.io.File;
import java.io.FileFilter;

import javax.swing.Icon;

import com.srscicomp.common.util.Utilities;

/** 
 * Enumeration of all <i>Figure Composer</i>-supported file types.
 * @author sruffner
 */
public enum FCFileType implements FileFilter
{
   /** A <i>FypML</i> figure definition file. */ 
   FYP(new String[] {"fyp"}, "FypML figure definition (.fyp)", FCIcons.V4_FIGURE_16),
   /** A <i>Matlab</i> figure, to be imported as a <i>FypML</i> figure (file-open only). */
   FIG(new String[] {"fig"}, "Matlab figure (.fig)", FCIcons.V4_MATFIG_16),
   /** Any <i>Figure Composer</i> figure source -- includes both FypML and Matlab figures (file-open only). */
   FGX(new String[] {"fyp", "fig"}, "FypML or Matlab figure (.fyp, .fig)", FCIcons.V4_MATFIG_16),
   /** A Postscript version of a <i>FypML</i> figure (file-save only). */ 
   PS(new String[] {"ps"}, "Postscript file (.ps)", FCIcons.V4_PSPDF_16),
   /** A Portable Document Format (PDF) version of a <i>FypML</i> figure (file-save only). */ 
   PDF(new String[] {"pdf"}, "Portable Document format (.pdf)", FCIcons.V4_PSPDF_16),
   /** A JPEG image of a <i>FypML</i> figure (file-save only). */ 
   JPEG(new String[] {"jpg","jpeg"}, "JPEG image (.jpg, .jpeg)", FCIcons.V4_PNGJPG_16),
   /** A Portable Network Graphics image of a <i>FypML</i> figure (save only). */ 
   PNG(new String[] {"png"}, "Portable Network Graphics image (.png)", FCIcons.V4_PNGJPG_16), 
   /** Supported image file formats for the source image of a <i>FypML</i> "image" element. */
   IMG(new String[] {"png", "jpg", "jpeg"}, "Image file (.png, .jpg, .jpeg)", FCIcons.V4_PNGJPG_16),
   /** A Scalable Vector Graphics image of a <i>FypML</i> figure (save only). */ 
   SVG(new String[] {"svg"}, "Scalable Vector Graphics (.svg)", FCIcons.V4_PNGJPG_16),
   /** A <i>Figure Composer</i>-compatible binary data source. */ 
   DNB(new String[] {"dnr", "dnb"}, " Figure Composer binary dataset source (.dnr, .dnb)", FCIcons.V4_DATA_16),
   /** A <i>Figure Composer</i>-compatible annotated plain-text data source. */ 
   DNA(new String[] {"dna", "txt"}, "Figure Composer annotated text data source (.dna, .txt)", FCIcons.V4_DATA_16),
   /** Any <i>Figure Composer</i>-compatible data source. */ 
   DNX(new String[0], "Figure Composer data source (*.*)", FCIcons.V4_DATA_16);
   
   private FCFileType(String[] exts, String desc, Icon icon) 
   { 
      this.validExts = exts;
      this.fileDescription = desc;
   }
   
   /** Supported file extensions for this file type. If zero-length, any extension is considered valid. */
   private String[] validExts;
   /** A short description of this file type. */
   private String fileDescription;
   /** A 16x16 representative icon. */
   private Icon fileIcon;
   
   @Override public String toString() { return(fileDescription); }

   /** 
    * Get a small (16x16) icon representing this <i>Figure Composer</i>-supported file type. 
    * @return The icon.
    */
   public Icon getRepresentativeIcon() { return(fileIcon); }
   
   /**
    * Does the specified filename string include a valid extension for this <i>Figure Composer</i>-supported file type? 
    * The method requires that the extension be in lower case only.
    * @param fname The filename to test.
    * @return True if filename has a valid extension; false if it has an invalid extension or lacks one. Returns true 
    * always if this file type allows any filename extension.
    */
   public boolean hasValidExtension(String fname)
   {
      if(validExts.length == 0) return(true);
      String ext = Utilities.getExtension(fname);
      if(ext == null) return(false);
      
      for(int i=0; i<validExts.length; i++) if(validExts[i].equals(ext)) return(true);
      
      return(false);
   }
   
   /** 
    * Append to the specified filename a valid extension for this <i>Figure Composer</i>-supported file type.
    * @param fname The filename to be augmented.
    * @return If the filename already has a valid extension, it is returned unchanged. If it lacks an extension or has
    * an invalid one, a valid extension is appended (note that this results in filenames like "fname.bad.good"). 
    * However, if the extension is an incomplete version of a valid one, then the partial extension is simply 
    * completed ("fname.go" becomes "fname.good").
    */
   public String addValidExtension(String fname) 
   {
      if(hasValidExtension(fname)) return(fname);
      String ext = Utilities.getExtension(fname);
      if(ext == null || ext.length() == 0) return( fname + (fname.endsWith(".") ? "" : ".") + validExts[0]);
      
      String base = fname.substring(0, fname.lastIndexOf('.'));
      for(int i=0; i<validExts.length; i++) if(validExts[i].startsWith(ext)) return(base + "." + validExts[i]);
      
      return(fname + "." + validExts[0]);
   }
   
   /**
    * Does the specified pathname include a valid extension for this <i>Figure Composer</i>-supported file type? The 
    * method requires that the extension be in lower case only.
    * @param f An abstract pathname.
    * @return True if pathname includes a valid extension; false if it has an invalid extension or lacks one. Returns 
    * true always if this file type allows any extension.
    */
   public boolean hasValidExtension(File f)
   {
      if(validExts.length == 0) return(true);
      String ext = Utilities.getExtension(f);
      if(ext == null) return(false);
      
      for(int i=0; i<validExts.length; i++) if(validExts[i].equals(ext)) return(true);
      
      return(false);
   }
   
   /**
    * Fix the extension of a pathname so that it is valid for this <i>Figure Composer</i>-supported file type.
    * @param f An abstract pathname. Must not be null.
    * @return If argument already has a valid extension, it is returned. Else, method returns a new file path with a 
    * valid extension replacing the invalid one. It is appended if original filename lacked an extension.
    */
   public File fixExtension(File f)
   {
      if(f == null) throw new IllegalArgumentException();
      if(hasValidExtension(f)) return(f);
      
      String fName = f.getName();
      int iDot = fName.lastIndexOf('.');
      String adjFName = (iDot < 0) ? fName : fName.substring(0, iDot);
      if(adjFName.length() == 0) adjFName = "Untitled";
      adjFName += "." + validExts[0];
      
      return(new File(f.getParentFile(), adjFName));
   }
   
   /**
    * Accepts any pathname that is not considered a hidden file -- i.e., {@link File#getName()} starts with a period
    * -- and that is a directory or a non-directory with a valid extension for this file type.
    * @see FileFilter#accept(File)
    */
   public boolean accept(File f) 
   { 
      return((f==null) ? false : ((!f.getName().startsWith(".")) && (f.isDirectory() || hasValidExtension(f)))); 
   }
   
   /**
    * Get the <i>Figure Composer</i>-supported file type that recognizes the extension of the specified file.
    * @param f An abstract pathname.
    * @return The corresponding <i>Figure Composer</i>-supported file type. If the pathname has no recognized extension,
    * then this method will always return {@link #DNX}; this file type is a catch-all for any <i>Figure Composer</i> 
    * data set source file, including the now-deprecated <i>Phyplot</i>-era source files; there was no defined file 
    * extension for the <i>Phyplot</i> data files.
    */
   public static FCFileType getFileType(File f)
   {
      String ext = Utilities.getExtension(f);
      if(ext == null) return(DNX);
      for(FCFileType type : FCFileType.values()) if(type != DNX)
      {
         if(type.hasValidExtension(f)) return(type);
      }
      return(DNX);
   }
   
   /**
    * Does specified file type represent one of the file formats to which a <i>FypML</i> figure may be exported?
    * @param ft The file type.
    * @return True if file type is {@link #PS}, {@link #PDF}, {@link #JPEG}, {@link #PNG}, or {@link #SVG}.
    */
   public static boolean isFigureExportFileType(FCFileType ft)
   {
      return(ft == PS || ft == PDF || ft == JPEG || ft == PNG || ft == SVG);
   }
}
