function res = getdatanavsrc(filename, arg1)
%GETDATANAVSRC Utility that imports data from a Figure Composer-compatible data set source file.
% GETDATANAVSRC(filename [,arg1]) reads into Matlab all or selected data sets stored in a Figure Composer (FC) data 
% source file. Each imported data set is saved in a Matlab structure array with fields similar to those required by the 
% reciprocal function PUTDATANAVSRC(). Alternatively, the function can provide a "table of contents" listing information
% about all data sets stored in the source file.
%
% The primary purpose for this function is to read in data sets prepared in JMWork. This analysis program can perform
% certain basic analysis tasks that process all or some of the Maestro data files in a data directory, saving the task
% results in a FC-compatible data set source file. Use GETDATANAVSRC to read these JMWork task results into Matlab for
% further processing.
%
% FigureComposer is a Java application that helps the user create scientific figures and populate them with real data. 
% For more information, go to the Figure Composer website at https://sites.google.com/a/srscicomp.com/figure-composer.
%
% IMPORTANT USAGE INFO:
% 1) GETDATANAVSRC relies on FC-specific Java code to do its work. The JAR files HHMI-MS-COMMON.JAR and 
% HHMI-MS-DATANAV.JAR must be on Matlab's Java classpath. You can call JAVACLASSPATH(P) on the Matlab command line, 
% where P is the full pathname to a required JAR. More conveniently, include JAVAADDPATH commands for each JAR file in
% your STARTUP.M file.
% 2) If GETDATANAVSRC fails for any reason, it exits with error(), which might wreak havoc if it occurs when the 
% function is called from within another M-file.
%
% USAGE:
% RES = GETDATANAVSRC(F) imports all data sets found in a FC data source file F. F is a character string specifying the
% file system path (absolute or relative to the current directory) to the source file. If any error occurs while reading
% the file, the return value is an empty vector. Otherwise, it is a vector of Matlab structures, one for each data set 
% culled from the file. Each such structure has the following fields:
%
%    'id' : A Matlab character string holding the ID of the data set.
%
%    'fmt' : An integer code specifying the data format. Here's a description of the supported data formats:
%
%      PTSET(=0). A single point set with optional standard deviation data. It may be thought of as a set of zero or 
%      more "tuples" {x y [yStd YE xStd XE]}, representing a single point (x,y) with standard deviations (xStd, yStd). 
%      (XE,YE) are codes that indicate how the x- and y-error bars should be drawn for for that point. An error bar 
%      code of 0  selects a two-sided (+/-1STD) error bar, 1 selects a +1STD bar, and -1 selects a -1STD bar; otherwise,
%      the error bar is not drawn. Of course, an error bar is never drawn if the standard deviation is zero! The only 
%      required members of a tuple are the coordinates of the point (x,y). Thus, tuple length may vary between [2..6], 
%      but all tuples in a given point set instance will have the same length. [NOTE: A tuple of length 3 or 4 
%      corresponds to a data point with an error bar in y only. To get a data point with a horizontal error bar only, 
%      the tuple must have the form {x y 0 0 xStd XE}. Such usage is rare.]
%
%      MSET(=1). A collection of 1+ (usually many) individual point sets, all sharing the same x-coordinates. It may be 
%      thought of as a set of one or more "tuples" of the form {x y1 [y2 y3 ...]}, where (x,y1) is a point in the first 
%      point set, (x,y2) in the second set, and so on. All tuples will have the same length L, where L-1 is the number 
%      of point sets in the collection. In typical usage, each individual point set represents a repeated measure of the 
%      same stochastic phenomenon, so the variation in that phenomenon is captured in the collection. Hence, this data 
%      set format does not include standard deviation data.
%
%      SERIES(=2). A single data series sampled at regular intervals in x, with optional standard deviation data. It may 
%      be thought of as a set of zero or more "tuples" of the form {y [yStd YE]}, where yStd is the standard deviation 
%      in y at the N-th point (x0+N*dx, yN) and YE is the error bar display code -- as described for the PTSET format. 
%      The sample interval dx and the initial value x0 are additional defining parameters for this data set format. 
%      Tuple length can vary between 1 and 3, but all tuples in a given series instance will have the same length.
% 
%      MSERIES(=3). A collection of 1+ (usually many) individual data series, all sampled at regular intervals in x. It 
%      may be thought of as a set of zero or more "tuples" of the form {y1 [y2 y3 ...]}, where (x0 + N*dx, y1[N]) is the 
%      (N+1)-th point in the first series, etcetera. All tuples will have the same length L, which is equal to the 
%      number of individual data series in the collection. As with the MSET format, each individual series typically 
%      represents a repeated measure of the same stochastic phenomenon, so that variation in that phenomenon is captured 
%      by the collection itself. The sample interval dx and the initial value x0 are additional defining parameters for 
%      this data set format.
%
%      RASTER1D(=4). A collection of M rasters in x -- typically used to represent spike trains. The data in this case 
%      is a set of M vectors, each containing 0 or more samples (eg, spike times). In the data file, these vectors 
%      are physically stored one after another in one long array that begins with the M raster lengths (each of which 
%      could be different): {n1 n2 .. nM x1(1..n1) x2(1..n2) ... xM(1..nM)}.
%
%      XYZIMG(=5). This specialized data set format provides a means of representing 3D data of the form {x, y, z(x,y)}, 
%      where one variable is a function of two independent variables. It is rendered as an indexed color image, or "heat
%      map", in Figure Composer. The "data" in this case is really a NxM matrix storing an "intensity" z(x,y) at each 
%      "pixel" (x,y), where x=[1..M] and y=[1..N]. At render time, the intensity image is associated with a colormap by 
%      which each intensity value is mapped to an RGB color; this colormap is not considered part of the data itself. 
%      (Obviously, this sort of presentation is easy to do in Matlab itself!) Additional defining parameters are the 
%      actual range [x0..x1] spanned by the data in x, and the range [y0..y1] spanned in y. [The x- and y-ranges enable 
%      scaling the color image IAW the current dimensions of the FypML graph in which it is embedded at render time.]
%
%      XYZSET(=6). A simple set of points {(x, y, z)} in 3D space. No standard deviation data.
%
%    'params' : A vector holding additional defining parameters for the SERIES, MSERIES, and XYZIMG formats. For the 
%       SERIES and MSERIES formats, this will have two elements [dx, x0], such that the x-coordinates of the data are
%       given by: x0, x0+dx, x0+2*dx, ... For the XYZIMG format, the vector will have four elements [x0, x1, y0, y1] 
%       defining the x- and y-coordinate ranges spanned by the image matrix.  For all other formats, this is empty.
%
%    'sz' : A two-element vector holding the dimensions (N, M) of the actual data. For all formats except RASTER1D,
%       the data is imported as an NxM matrix. The content of the matrix depends on the format -- see below. For 
%       RASTER1D, the data is imported as a Mx1 cell array, where each cell holds a vector containing the samples in an 
%       individual raster. Raster lengths will likely vary; N is the total number of samples across all rasters.
%
%    'data' : The actual data. The content of this field depends on the data format:
%
%  	  PTSET: NxM matrix, where each row is a single M-tuple as described above and N is the number of data points.
%          If the matrix is not empty, M will be in [2..6].
%       MSET: NxM matrix, where each row is a single M-tuple as described above, M-1 is the number of sets in the 
%          collection, and N is the number of data points in each set. If the matrix is not empty, M will be 2 or more.
%       SERIES: NxM matrix, where each row is a single M-tuple as described above and N is the number of samples. If the
%          matrix is not empty, M will be in [1..3].
%       MSERIES: NxM matrix, where each row is a single M-tuple as described abpve, M is the number of series in the 
%          collection, and N is the number of samples in each series. If the matrix is not empty, M will be 1 or more.
%       RASTER1D: A Mx1 cell array, where each cell holds a vector containing the samples in an individual raster.
%       XYZIMG: NxM matrix holding the values z(x,y) for x=[1..M] and y=[1..N].
%       XYZSET: Nx3 matrix in which each of the N rows specifies the coordinates of a point (x, y, z) in the set.
% 
%
% RES = GETDATANAVSRC(F, ''), where the second argument is an empty string, retrieves table-of-contents information from 
% the data source file F. The return value is again a vector of Matlab structures, one for each data set culled from 
% the file. Each structure has all of the fields described above, except the 'data' field. If you are not aware of the 
% contents of a source file, it is a good idea to call this version and review the table of contents before retrieving 
% the actual data. Again, if an error occurs, the return value is an empty vector.
%
%
% RES = GETDATANAVSRC(F, IDSTR), where IDSTR is a Matlab character string, will retrieve a single data set with the 
% specified identifier from the data source file F. The return value is a Matlab structure as described above. If
% an error occurs or a data set with the specified identifier is not found, an empty vector is returned.
%
%
% RES = GETDATANAVSRC(F, IDS), where IDS is a Matlab cell array of character strings, will retrieve from the source 
% file F those data sets identified by the strings in the cell array. The return value is again a vector of Matlab 
% structures, each having the fields described above. The length of this vector may be shorter than the cell array if
% some of the identified data sets were not found in the file, or if any duplicate IDs appear in the cell array. If an
% error occurs or if no identified sets were found, an empty vector is returned.
%
% 
% Scott Ruffner
% sruffner@srscicomp.com
%

import com.srscicomp.fc.data.*

% the data format codes
PTSET=0; MSET=1; SERIES=2; MSERIES=3; RASTER1D=4; XYZIMG=5; XYZSET=6;

% basic argument checks.
res = [];
if(nargin < 1 || nargin > 2) 
	error('Illegal number of arguments');
end
if(~ischar(filename))
    error('Arg FILENAME must be a string');
end
if(nargin == 2)
	if(~(ischar(arg1) || iscell(arg1)))
		error('Second argument must be a string or cell array of strings');
	end
end

% open the data set source file. Fail if it is not a valid source.
factory = DataSrcFactory.getInstance();
dataSrc = factory.getDataSource(java.io.File(filename), 0);
if(~isjava(dataSrc) || isempty(dataSrc)) 
	error('File not found or not recognized as a data source!');
end

% get table of contents: DataSetInfo[]
toc = dataSrc.getSummaryInfo();
if(~isjava(toc) || isempty(toc))
	error('Error reading table of contents, or source file is empty!');
end

% CASE: Table of contents requested only
if(nargin == 2 && ischar(arg1) && isempty(arg1))
   for(i=1:length(toc))
   	  res(i).id = char(toc(i).getID());
   	  dataSetFmt = toc(i).getFormat();
   	  res(i).fmt = dataSetFmt.getIntCode();
   	  nParams = dataSetFmt.getNumberOfParams();
   	  res(i).params = zeros(nParams, 1);
   	  for(j=1:nParams)
   	     res(i).params(j) = toc(i).getParam(j-1);
   	  end
   	  res(i).sz = [toc(i).getDataLength() toc(i).getDataBreadth()];
   end
   return;
end

% CASE: One, some, or all datasets requested
full = 0;
if(nargin == 2 && ischar(arg1))
   reqIDs = cell(1,1);
   reqIDs{1} = arg1;
elseif(nargin == 2)
   reqIDs = arg1;
   for(i=1:length(reqIDs))
      if(~ischar(reqIDs{i}))
         error('Second arg must be a string or cell array of strings!');
      end
   end
else
   full = 1;
   reqIDs = cell(length(toc), 1);
   for(i=1:length(toc))
      reqIDs{i} = char(toc(i).getID());
   end
end

% extract the datasets one at a time (NOTE: not particularly efficient for a full extraction!)
for(i=1:length(reqIDs))
   % read next dataset. If we're reading all datasets and this fails, then abort. Otherwise, we simply move on to
   % ID of the next dataset to extract
   dataSet = dataSrc.getDataByID(reqIDs{i});
   if(~isjava(dataSet) || isempty(dataSet))
      if(full)
         res = [];
         error('Problem occurred while reading source file?!');
      end
      continue;
   end
   
   % populate fields in result structure
   res(i).id = char(dataSet.getID());
   dataSetFmt = dataSet.getFormat();
   res(i).fmt = dataSetFmt.getIntCode();
   res(i).params = dataSet.getParams();
   nrows = dataSet.getDataLength();
   ncols = dataSet.getDataBreadth();
   res(i).sz = [nrows ncols];
   
   fData = dataSet.copyRawData();
   if(res(i).fmt == RASTER1D)
      res(i).data = cell(ncols, 1);
      j = ncols+1;
      for(m=1:ncols)
         rasterlen = fData(m);
         if(rasterlen == 0)
            res(i).data{m} = [];
         else
            res(i).data{m} = fData([j:j+rasterlen-1]);
         end
         j = j+rasterlen;
      end
   elseif(nrows == 0 || ncols == 0)
      res(i).data = [];
   else
      res(i).data = reshape(fData, ncols, nrows)';
   end
   
   clear fData;
end
