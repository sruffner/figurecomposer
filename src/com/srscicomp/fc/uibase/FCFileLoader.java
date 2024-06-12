package com.srscicomp.fc.uibase;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.SwingWorker;

import com.srscicomp.common.g2dviewer.RenderableModel;
import com.srscicomp.fc.data.DataSet;
import com.srscicomp.fc.data.DataSetInfo;
import com.srscicomp.fc.data.IDataSrc;
import com.srscicomp.fc.fig.FGModelSchemaConverter;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicModel;
import com.srscicomp.fc.fig.ImageNode;
import com.srscicomp.fc.fig.Measure;
import com.srscicomp.fc.matlab.MatlabFigureImporter;

/**
 * A background worker thread that can load a <i>Figure Composer</i> figure, data set, or image from a compatible source
 * file.
 * 
 * <p>A {@link BufferedImage} may be loaded from a valid JPEG or PNG source file; a {@link DataSet} can be extracted 
 * from any <i>FC</i>-compatible data set source file; and a {@link FGraphicModel} may be loaded from a valid 
 * <i>FypML</i> figure definition file OR imported from a <i>Matlab</i> figure (.FIG) file. In the latter case, the 
 * import could fail if the .FIG file cannot be read, or if it does not contain a valid Matlab Handle Graphics figure 
 * hierarchy. Alternatively, the import could succeed, but the resulting <i>FypML<i> figure may not be a faithful 
 * reproduction of the original Matlab figure.</p>
 * <p>Regardless the type of object loaded, <b>FCFileLoader</b> prepares a {@link RenderableModel} of the object to 
 * serve as a "preview" of that object, intended for display in the graphic viewer canvas used extensively in <i>FC</i>.
 * If the figure graphic or rendered data set model is already available in the current user's workspace model cache, 
 * the cached version is used -- thereby avoiding the time-consuming task of opening and parsing the source file.</p>
 * 
 * <p>Once the task is completed, the file loader will send an event (on the event dispatch thread) to the action 
 * listener passed in its constructor. If the task was successful, the action listener can retrieve the graphic model 
 * prepared; if not, it can get a brief error message.</p>
 * 
 * @author sruffner
 */
class FCFileLoader extends SwingWorker<Object, Object>
{
   /**
    * Construct a file loader to load an image or a <i>FypML</i> figure. 
    * <p>If the file is a JPEG or PNG image file, the image is loaded and embedded in a simple <i>FypML</i> figure. If
    * the file is a valid <i>FypML</i> figure definition file (".fyp"), the figure model is parsed from the file's 
    * contents. Finally, if the file is a valid <i>Matlab</i> .FIG file, its content is imported as a <i>FypML</i>
    * figure that reproduces the original <i>Matlab</i> figure to the extent possible. For details on how <i>Matlab</i> 
    * figures are imported to <i>FypML</i>, see {@link MatlabFigureImporter}.
    * 
    * @param f The source file. Must be a valid <i>FypML</i> figure file, <i>Matlab</i> .FIG file, JPEG image file, or
    * PNG image file. If not, the file loader task will abort immediately.
    * @param l Action listener to be notified (on event dispatch thread) when task is finished. Cannot be null -- else
    * the file loader task will abort immediately.
    */
   FCFileLoader(File f, ActionListener l)
   {
      this.srcFile = f;
      this.listener = l;
   }
   
   /**
    * Construct a file loader to load a single data set from a <i>Figure Composer</i>-compatible data set source.
    * @param src The data set source.
    * @param dsID The ID of the requested data set. If null, the first available data set in the source is extracted.
    * @param l Action listener to be notified (on event dispatch thread) when task is finished. Cannot be null -- else
    * the file loader task will abort immediately.
    */
   FCFileLoader(IDataSrc src, String dsID, ActionListener l)
   {
      this.dataSrc = src;
      this.dataSetID = dsID;
      this.listener = l;
   }
   
   /**
    * Get the renderable model offering a "preview" of the target object loaded by this file loader task. The nature of
    * the graphic model depends on the target object loaded:
    * <ul>
    * <li>A <i>FypML</i> figure: {@link FGraphicModel}.</li>
    * <li>A <i>FypML</i> data set: {@link DataSetPreview}.</li>
    * <li>A JPEG or PNG image: A simple {@link FGraphicModel} containing a single <i>image</i> element in which the 
    * loaded image is embedded.</li>
    * </ul>
    * 
    * @return The prepared model. If task failed or is still in progress, method returns null.
    */
   public RenderableModel getModel() { return(done ? model : null); }
   
   /**
    * Get the image that was loaded by this file loader task.
    * @return The image. Returns null if task is still in progress, an error occurred, OR the target to be loaded was 
    * not a 2D image.
    */
   public BufferedImage getImageLoaded() { return(done ? bi : null); }
   
   /**
    * Get a brief description of the error encountered if file loader task did not complete successfully.
    * @return A short error message, or null if no error occurred or task is still in progress.
    */
   public String getErrorMessage() { return(done ? errMsg : null); }

   /** 
    * Get the source file (a FypML file, a Matlab .FIG file, a data set source file, or a JPEG/PNG image file) for this 
    * file loader.
    * @return Abstract pathname of source file.
    */
   File getSourceFile()
   {
      return((srcFile != null) ? srcFile : (dataSrc != null ? dataSrc.getSourceFile() : null));
   }
   
   /**
    * Get the data set source from which a data set was extracted by this file loader task, if applicable.
    * @return The data source, or null if loader was configured to load a figure or image instead.
    */
   IDataSrc getDatasetSource() { return(dataSrc); }
   
   /** 
    * Abort the operation immediately. Depending on what the file loader is doing, it make take some time before the 
    * worker thread actually dies. Regardless, once the abort flag is set, it will not inform the registered action 
    * listener (unless it has already done so).
    */
   void abort() { abort = true; done = true; cancel(false); }
   
   
   @Override protected Object doInBackground() throws Exception
   {
      FCWorkspace workspace = FCWorkspace.getInstance();
      
      if(abort || listener == null || (srcFile == null && dataSrc == null))
      {
         listener = null;
         return(null);
      }

      if(srcFile != null)
      {
         model = workspace.getModelFromCache(srcFile, null);
         if(model == null)
         {
            StringBuffer errBuf = new StringBuffer();
            FCFileType fType = FCFileType.getFileType(srcFile);
            if(fType == FCFileType.FYP)
               model = FGModelSchemaConverter.fromXML(srcFile, errBuf);
            else if(fType == FCFileType.FIG)
               model = MatlabFigureImporter.importMatlabFigureFromFile(srcFile, errBuf);
            else if(fType == FCFileType.PNG || fType == FCFileType.JPEG)
            {
               try
               {
                  bi = ImageIO.read(srcFile);
               }
               catch(IOException ioe)
               {
                  errBuf.append("Unable to load image: " + ioe.getMessage());
               }
               
               if(bi != null) buildImagePreview();
            }
            else
               errBuf.append("Source is not a supported image file, FypML figure definition, or Matlab FIG file!");
            
            if(model == null) errMsg = errBuf.toString();
            else if(fType == FCFileType.FYP || fType == FCFileType.FIG)
            {
               workspace.putModelToCache(srcFile, null, model);
               workspace.addToWorkspace(srcFile);
            }
         }
      }
      else
      {
         if(dataSetID == null)
         {
            DataSetInfo[] dsInfo = dataSrc.getSummaryInfo();
            if(dsInfo == null)
               errMsg = "Cannot read data source file";
            else if(dsInfo.length == 0)
               errMsg = "Data source is empty";
            else
               dataSetID = dsInfo[0].getID();
            if(errMsg != null) return(null);
         }
         model = workspace.getModelFromCache(dataSrc.getSourceFile(), dataSetID);
         if(model == null)
         {
            DataSet ds = dataSrc.getDataByID(dataSetID);
            if(ds == null) errMsg = "(Cannot retrieve dataset)";
            else
            {
               DataSetPreview dsp = new DataSetPreview();
               dsp.setDataSet(ds);
               model = dsp;
               workspace.putModelToCache(dataSrc.getSourceFile(), dataSetID, model);
               workspace.addToWorkspace(dataSrc.getSourceFile());
            }               
         }
      }
      return(null);
   }

   /**
    * Helper method embeds the image loaded from a PNG or JPEG source file into a simple <i>FypML</i> figure consisting
    * of a single <i>image</i> element that covers the figure's bounding box. This figure becomes the rendered model
    * returned via {@link #getModel()}.
    */
   private void buildImagePreview()
   {
      FGraphicModel fgm = new FGraphicModel();
      if(fgm.insertNode(fgm.getRoot(), FGNodeType.IMAGE, 0))
      {
         ImageNode img = (ImageNode) fgm.getRoot().getChildAt(0);
         img.setWidth(new Measure(100, Measure.Unit.PCT));
         img.setHeight(new Measure(100, Measure.Unit.PCT));
         img.setImage(bi);
         model = fgm;
      }
   }
   
   @Override protected void done()
   {
      done = true;
      if(abort || listener == null) return;
      listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
   }
   
   
   /** 
    * Source file for a <i>FypML</i> figure (.fyp or Matlab .fig) or an image (.jpg or .png); null if loading a data 
    * set from a <i>Figure Composer</i>-compatible data set source file. 
    */
   private File srcFile = null;

   /** The data set source. Null if not applicable. */
   private IDataSrc dataSrc = null;
   
   /** The ID of the data set to retrieve from data set source; if null, first data set is retrieved. */
   private String dataSetID = null;
   
   /** The listener to be notified once this file loader task is completed. */
   private ActionListener listener = null;
   
   /** The renderable graphic model of the target object, prepared by this file loader task. Null until done loading. */
   private RenderableModel model = null;
   
   /** The image loaded from an image source file (JPEG or PNG). Null if loading in progress, or not applicable */
   private BufferedImage bi = null;
   
   /** If an error occurs while loading target object, this is a brief error description; else it is null.*/
   private String errMsg = null;
   
   /** Flag set to abort the operation. */
   private boolean abort = false;
   
   /** Flag set once file loader has completed its task, successfully or not. */
   private boolean done = false;
}
