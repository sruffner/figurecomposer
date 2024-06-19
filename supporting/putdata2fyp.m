function putdata2fyp(src, id, x, y, varargin)
%PUTDATA2FYP Utility that injects a data set into a Figure Composer FypML figure document.
% PUTDATA2FYP(F, ID, X, Y) replaces the data set object identified by the character-string ID in the FypML figure 
% document stored in the file F. The vector argument X contains the X-coordinates of the new data, while the vector or 
% matrix argument Y holds the Y coordinates. PUTDATA2FYP handles the details of translating the (X,Y) data into one of 
% the FypML-aupported data formats. If the data is successfully "injected", the function raises a Java dialog in which
% the revised figure is previewed. After examining the results, you may choose to save the figure to file F, or cancel.
%
% PUTDATA2FYP(F, ID, X, Y, C) performs the same task, but the logical scalar C lets but lets you control whether or not 
% the confirmation dialog is raised. If C==false and data injection was successful, the updated figure is saved to file 
% without confirmation.
%
% PUTDATA2FYP(F, ID, X, Y, C, F2), in addition to controlling the confirmation dialog, lets you specify an alternate
% destination file F2 for the updated figure -- so the original figure file F is unaffected.
%
% Figure Composer (FC) is a Java application for composing scientific figures suitable for journal publication. FypML is
% an informally specified XML dialect used to describe a figure in FC. For more information about FC and the FypML file 
% format, go to the Figure Composer online guide at:
%
%     https://sites.google.com/a/srscicomp.com/figure-composer
% 
% The primary motivation for PUTDATA2FYP is to provide a convenient way to update a FypML figure with data generated in
% a Matlab script (or interactively in the Matlab command console). Since it only replaces raw data, the function offers
% a way to update a nicely formatted FypML figure with new or revised data prepared in Matlab WITHOUT changing how that
% data is ultimately rendered. Since you will probably go through an analyze-examine-revise cycle many times as you 
% prepare figures for publication, PUTDATA2FYP could be a helpful tool in your analysis workflow.
%
% To the extent possible, PUTDATA2FYP shields the Matlab user from the details of the FypML-specific data set formats
% ("ptset", "series", "mset", "mseries", "raster1d", "xyzimg", "xyzset"). Essentially, it examines the 2D data specified
% in the arguments (X,Y) and converts that data to one of the FypML-supported formats. It checks the format of the data
% set being replaced (as identified by the ID argument) to guide this conversion process. Here are some examples to help
% explain how the conversion is handled:
%
% -- X and Y are both N-vectors, representing a data set with N (x,y} data points. In general, length(X) must equal the
%    the number of rows in Y, or the data cannot be converted. The one exception: X=[] -- see below.
% -- X is a Nx1 vector and Y is an NxM matrix with M > 1. In this case, it is assumed that each column in Y represents
%    a separate data set. Thus, Y holds a collection of M data sets, all sampled at the same x-coordinate values. This 
%    maps to the FypML "mset" data format.
% -- X=[], Y is an NxM matrix. Whenever X is an empty matrix, it is assumed that Y contains data sampled at regularly
%    spaced values of X, i.e., "series" data. If M=1,2 or 3, there's ambiguity about whether Y should be mapped to a
%    FypML "series" or "mseries" (a collection of "series"). In this case, the format of the data set being replaced
%    governs the choice made. Also the "x0" (first value of X) and "dx" (sample interval in X) parameters of the
%    replaced data series are preserved.
% -- When the data set being replaced in the figure backs a raster element ("raster1d" data format), then the supplied
%    data (X,Y) must be converted to that format. In this case, X is ignored, and all of the data is expected to be
%    contained in Y, in one of two forms:
%    (1) An NxM matrix. In this case, each column is interpreted as a separate raster over N time units. If R(n,m) is
%    nonzero, then an "event occurred" at time (n-1) in the m-th event train. 
%    (2) A 1XM or Mx1 cell array, where each cell R{m} contains a numeric vector V. In this case, each such vector is 
%    interpreted as a separate raster train, with the vector containing the "times" at which an event occured in that
%    train.
% -- When the data set being replaced backs a heatmap element ("xyzimg" data format), then X is again ignored and Y is
%    expected to be an NxM matrix holding the heatmap values z(x,y) for x=[1..M] and y=[1..N]. The "xyzimg" parameters
%    X0,X1,Y0,Y1 -- defining the extent of the heatmap data along the X and Y axes -- are copied from the original set.
% -- When the data set being replaced is a 3D point set -- the "xyzset" format --, then X is again ignored and Y is
%    expected to be an Nx3 matrix where each of the N rows holds the (x,y,z) coordinates of a point in 3D space.
%
% IMPORTANT NOTES AND USAGE INFO: 
% 1) To use PUTDATA2FYP effectively, you must be aware of the character-string identifiers assigned to the data sets in
% your FypML figure. You can specify these in Figure Composer when you initially build that figure. It is also 
% imperative that you understand the difference between a raw data set and a "data presentation node" in FC. There are
% a number of different data presentation nodes in FC -- for example, XY data trace, raster, and heatmap. Each is backed
% by a raw data set, but it is the presentation node that determines how that data is rendered. PUTDATA2FYP only changes
% the raw data set, not its presentation node "container".
% 2) On replacing a data set with an identical copy: When designing a script to inject one or more data sets into a
% figure or figures, it may be that you inject the same data set into a figure that was injected in a prior run. In this
% scenario, PUTDATA2FYP() will not inject the copy since there is no change. However, it will no longer throw an error
% -- as it did in past versions. If you specified an alternate destination file F2, it will still save the "updated"
% figure to that file (effectively, a file copy from F to F2!)
% 3) PUTDATA2FYP relies on FC-specific Java code to do its work. The JAR files XPP3-1.1.3.4.D.JAR, ITEXTPDF-5.5.0.JAR,
% HHMI-MS-COMMON.JAR, and HHMI-MS-DATANAV.JAR must be on Matlab's Java classpath. You can call JAVACLASSPATH(P) on the 
% Matlab command line, where P is the full pathname to a required JAR. More conveniently, include JAVAADDPATH commands 
% for each JAR file in your STARTUP.M file.
% 4) If PUTDATA2FYP fails for any reason, it exits with error(), which might wreak havoc if it occurs when the function
% is called from within another M-file.
%
% 
% Scott Ruffner
% sruffner@srscicomp.com
%

import com.srscicomp.fc.data.*;

% check arguments
nArgs = nargin;
if(nArgs < 4 || nArgs > 6) 
	error('Invalid number of arguments');
end;

if(~ischar(src))
   error('SRC (arg 1) must be a string, the figure file pathname');
end;
jSrcFile = java.io.File(src);
if(~jSrcFile.isFile())
   error('Source FypML figure file does not exist.');
end;

if(~ischar(id) || ~DataSet.isValidIDString(id))
   error('ID (arg 2) is not a valid FypML data set identifier');
end;

ok = isempty(x) || (isvector(x) && isnumeric(x));
if(~ok)
   error('X (arg 3) must be a numeric vector, possibly empty');
end;

ok = isempty(y) || (iscell(y) && isvector(y)) || (ismatrix(y) && isnumeric(y));
if(~ok)
   error('Y (arg 4) must be a numeric vector or matrix, or a cell vector');
end;

if((~isempty(x)) && ~iscell(y))
   if(length(x) ~= size(y,1))
      error('Length of vector X (arg 3) must match number rows in Y (arg 4)');
   end;
end;

confirm = true;
if(nArgs >= 5)
   confirm = varargin{1};
   if(~(isscalar(confirm) && islogical(confirm)))
      error('Arg 5 invalid -- must be a logical scalar');
   end;
end;

jDstFile = jSrcFile;
altDst = false;
if(nArgs == 6)
   dst = varargin{2};
   if(~ischar(dst))
      error('Arg 6 must be a string, the alternate pathname to which revised figure should be saved');
   end;

   % make sure filename ends in '.fyp'. Tack it on otherwise.
   jDst = java.lang.String(dst);
   if(jDst.length() < 4) 
      jDst = java.lang.String([dst '.fyp']);
   else
      jExt = jDst.substring(jDst.length()-4);
      if(~jExt.equalsIgnoreCase('.fyp'))
         jDst = java.lang.String([dst '.fyp']);
      end;
   end;

   jDstFile = java.io.File(jDst);
   if(~isjava(jDstFile.getParentFile()) || ~jDstFile.getParentFile().isDirectory())
      error('Alternate destination FypML file path is not valid.');
   end;
   altDst = true;
end;


% read in figure file
ebuf = java.lang.StringBuffer();
jFig = javaMethod('fromXML', 'com.srscicomp.fc.fig.FGModelSchemaConverter', jSrcFile, ebuf);
if(~isjava(jFig) || isempty(jFig))
   msg = strcat('Unable to read source FypML file: ', char(ebuf.toString()));
   error(msg);
end;

% retrieve the data set that is being replaced
jOldDS = jFig.getDataset(id);
if(~isjava(jOldDS) || isempty(jOldDS))
   error('ID (arg 2) does not identify an existing data set in source figure');
end;

% prepare the replacement data set, then inject it into the source figure
jNewDS = prepareDataSet(x, y, jOldDS);
if(~isjava(jNewDS) || isempty(jNewDS))
   error('Unable to generate replacement data set from X,Y args; not compatible with set identified by ID?');
end;

% inject the replacement data set into the source figure. IF the replacement set is identical to the original, no
% action is taken. In this scenario, the figure is saved to file ONLY if an alternate destination file was specified.
identical = jNewDS.equals(jOldDS);
if(~identical)
   ok = jFig.replaceDataSetInUse(jNewDS);
   if(~ok)
      error('Unable to inject new data into source figure');
   end;
end;

% raise confirmation dialog, unless user chose not to.
ok = true;
if(confirm)
   ok = javaMethodEDT('raiseConfirmSaveDialog', 'com.srscicomp.fc.matlab.DNFigureSaveDlg', jFig, jDstFile);
end;

% if confirmed, save the converted figure to file. Don't save if replacement set was identical to original, unless
% we're saving the figure to an alternate destination.
if(ok && (altDst || ~identical))
   emsg = javaMethod('toXML', 'com.srscicomp.fc.fig.FGModelSchemaConverter', jFig, jDstFile);
   if(ischar(emsg))
      error(['Error saving FypML figure to file: ', char(emsg)]);
   end;
end;


   %=== prepareDataSet(x, y, jOld) ====================================================================================
   % Nested function handles the details of converting Matlab 2D data (X,Y) into a FypML data set that can replace the
   % specified FypML data set object, jOld. If successful, it returns the replacement data set -- an instance of the
   % Java class com.srscicomp.fc.data.DataSet; otherwise, it returns an empty matrix.
   %
   function jRes = prepareDataSet(x, y, jOld)
      import com.srscicomp.fc.data.DataSet;

      jRes = [];

      % the data format codes
      PTSET=0; MSET=1; SERIES=2; MSERIES=3; RASTER1D=4; XYZIMG=5; XYZSET=6;

      % the format of the data set being replaced guides our conversion of X,Y into a FypML-supported data format
      fmt = jOld.getFormat().getIntCode();

      % raster1d -- X ignored, Y must be an NxM matrix or a cell vector
      if(fmt == RASTER1D)
         % rasterdata = [];
         if(ismatrix(y) && isnumeric(y))
            ncols = size(y,2);
            nrows = 0;
            for i=1:ncols
               nrows = nrows + length(find(y(:, i) ~= 0));
            end;
            rasterdata = zeros(1, ncols + nrows);

            start = ncols + 1;
            for i=1:ncols
               train = find(y(:,i) ~= 0)';
               n = length(train);
               rasterdata(i) = n;
               if(n > 0)
                  rasterdata(start:start+n-1) = train;
                  start = start + n;
               end;
            end;
         elseif(iscell(y) && (isempty(y) || isvector(y)))
            ncols = length(y);
            nrows = 0;
            for i=1:ncols
               train = y{i};
               if(~(isempty(train) || (isnumeric(train) && isvector(train))))
                  return;
               end;
               nrows = nrows + length(train);
            end;
            rasterdata = zeros(1, ncols + nrows);

            start = ncols + 1;
            for i=1:ncols 
               train = y{i};
               if(size(train,1) > size(train,2))
                  train = train';
               end;
               n = length(train);
               rasterdata(i) = n;
               if(n > 0)
                  rasterdata(start:start+n-1) = train;
                  start = start + n;
               end;
            end;
         else
            % bad Y argument. Fail.
            return;
         end;

         if(isempty(rasterdata))
            % special case: an empty raster set
            jRes = DataSet.createEmptySet(jOld.getFormat(), jOld.getID());
         else
            jRes = DataSet.createDataSet(jOld.getID(), RASTER1D, [], nrows, ncols, rasterdata);
         end;

         return;
      end;

      % xyzimg -- X ignored, Y must be an NxM matrix. We adopt the params of the data set being replaced. Empty case
      % is handled specially because Matlab maps [] to null, in which case createDataSet() fails...
      if(fmt == XYZIMG && ismatrix(y) && isnumeric(y))
         if(isempty(y))
            jRes = DataSet.createEmptySet(jOld.getFormat(), jOld.getID());
         else
            [nrows, ncols] = size(y);
            jRes = DataSet.createDataSet(jOld.getID(),XYZIMG,jOld.getParams(), nrows,ncols, reshape(y',nrows*ncols,1));
         end;
         return;
      end;
      
      % XYZSET -- X ignored, Y must be an Nx3 matrix holding (x,y,z) coordinates of N points in 3D space. If Y does not
      % have exactly 3 columns, then operation fails.
      if(fmt == XYZSET && ismatrix(y) && isnumeric(y))
         if(isempty(y))
            jRes = DataSet.createEmptySet(jOld.getFormat(), jOld.getID());
         else
            [nrows, ncols] = size(y);
            if(ncols == 3)
               jRes = DataSet.createDataSet(jOld.getID(),XYZSET,[], nrows,ncols, reshape(y',nrows*ncols,1));
            end;
         end;
         return;
      end;
      
      % the remaining formats are all compatible
      if(fmt == PTSET || fmt == MSET || fmt == SERIES || fmt == MSERIES)
         if(~(ismatrix(y) && isnumeric(y) && (isempty(x) || (length(x) == size(y,1)))))
            return;
         end;
      
         % handle special case: empty set
         if(isempty(y))
            jRes = DataSet.createEmptySet(jOld.getFormat(), jOld.getID());
            return;
         end;

         % first, decide whether we'll use a series-type format. If so, determine params = [dx x0]
         params = [];
         isSeries = false;
         if(isempty(x))
            % in this case, new data set must be series-type. If the old set was a series, we use its [dx, x0]; else,
            % we assume dx = 1, x0 = 0.
            isSeries = true;
            if(fmt == SERIES || fmt == MSERIES)
               params = jOld.getParams();
            else
               params = [1 0];
            end;
         elseif(length(x) > 2)
            % since X vector is specified, we can scan it to see if contains regularly spaced values. Since we're doing
            % floating-pt comparison in this test, we introduce a tolerance of 0.0001. If X passes the test, we can
            % compute values for dx and x0 and use a series-type format.
            x0 = x(1);
            dx = x(2) - x(1);
            
            isSeries = true;
            for i=2:length(x)
               xTest = x0 + dx * (i-1);
               if(abs(xTest-x(i)) > 0.0001)
                  isSeries = false;
                  break;
               end;
            end;

            if(isSeries)
               params = [dx x0];
            end;
         end;

         if(isSeries)
            % for series data, everything is in the Y argument. If the number of columns in Y is in [1..3], then the
            % target format could be SERIES or MSERIES. In that case, if the old set was a series-type, we use its 
            % format; else we assume SERIES.
            [nrows, ncols] = size(y);
            if(ncols >= 1 && ncols <= 3)
               if(~(fmt == SERIES || fmt == MSERIES))
                  fmt = SERIES;
               end;
            else
               fmt = MSERIES;
            end;

            jRes = DataSet.createDataSet(jOld.getID(), fmt, params, nrows, ncols, reshape(y',nrows*ncols,1));
         else
            % for non-series data, we need to incorporate the X vector in the raw data array. If the number of columns
            % in [x, y] is in [2..6], then the target format could be PTSET or MSET. In that case, if the old set was 
            % one of these two formats, we leave the format unchanged; else, we assume PTSET.
            raw = [x, y];
            [nrows, ncols] = size(raw);
            if(ncols >= 2 && ncols <= 6)
               if(~(fmt == PTSET || fmt == MSET))
                  fmt = PTSET;
               end;
            else
               fmt = MSET;
            end;

            jRes = DataSet.createDataSet(jOld.getID(), fmt, [], nrows, ncols, reshape(raw', nrows*ncols, 1));
         end;
      end;
   end
   %=== end of nested function prepareDataSet(x, y, jOld) =============================================================
   

end
%=== end of primary function putdata2fyp(src,id,x,y) ==================================================================
