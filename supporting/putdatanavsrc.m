function putdatanavsrc(filename, id, fmt, params, data, replace, plaintext)
%PUTDATANAVSRC Utility that writes a Figure Composer-compatible data set to file.
% PUTDATANAVSRC(filename, id, fmt, params, data [, replace, plaintext]) saves data prepared in MATLAB in a file that can
% be subsequently processed by the Java application Figure Composer (FC).
%
% Figure Composer is a Java application that helps the user construct scientific figures for ournal publication. For 
% more information, see the Figure Composer online guide at https://sites.google.com/a/srscicomp.com/figure-composer.
%
% 
% IMPORTANT USAGE INFO:
% 1) PUTDATANAVSRC relies on FC-specific Java code to do its work. The JAR files HHMI-MS-COMMON.JAR and 
% HHMI-MS-DATANAV.JAR must be on Matlab's Java classpath. You can call JAVACLASSPATH(P) on the Matlab command line, 
% where P is the full pathname to a required JAR. More conveniently, include JAVAADDPATH commands for each JAR file in
% your STARTUP.M file.
% 2) If PUTDATANAVSRC fails for any reason, it exits with error(), which might wreak havoc if it occurs when the 
% function is called from within another M-file.
%
% Figure Composer supports seven basic data set formats:
%
% PTSET. A single point set with optional standard deviation data. It may be thought of as a set of zero or more 
% "tuples" {x y [yStd YE xStd XE]}, representing a single point (x,y) with standard deviations (xStd, yStd). (XE,YE) are
% codes that indicate how the x- and y-error bars should be drawn for that point. An error bar code of 0 selects a 
% two-sided (+/-1STD) error bar, 1 selects a +1STD bar, and -1 selects a -1STD bar; otherwise, the error bar is not 
% drawn. Of course, an error bar is never drawn if the standard deviation is zero! The only required members of a tuple 
% are the coordinates of the point (x,y). Thus, tuple length may vary between [2..6], but all tuples in a given point 
% set instance will have the same length. [NOTE: A tuple of length 3 or 4 corresponds to a data point with an error bar 
% in y only. To get a data point with a horizontal error bar only, the tuple must have the form {x y 0 0 xStd XE}. Such 
% usage is rare.
%
% MSET. A collection of 1+ (usually many) individual point sets, all sharing the same x-coordinates. It may be thought 
% of as a set of one or more "tuples" of the form {x y1 [y2 y3 ...]}, where (x,y1) is a point in the first point set, 
% (x,y2) in the second set, and so on. All tuples will have the same length L, where L-1 is the number of point sets in 
% the collection. In typical usage, each individual point set represents a repeated measure of the same stochastic 
% phenomenon, so the variation in that phenomenon is captured in the collection. Hence, this data set format does not 
% include standard deviation data.
%
% SERIES. A single data series sampled at regular intervals in x, with optional standard deviation data. It may be 
% thought of as a set of zero or more "tuples" of the form {y [yStd YE]}, where yStd is the standard deviation in y at 
% the N-th point (x0+N*dx, yN) and YE is the error bar display code -- as described for the PTSET format. The sample 
% interval dx and the initial value x0 are additional defining parameters for this data set format. Tuple length can 
% vary between 1 and 3, but all tuples in a given series instance will have the same length.
%
% MSERIES. A collection of 1+ (usually many) individual data series, all sampled at regular intervals in x. It may be 
% thought of as a set of zero or more "tuples" of the form <em>{y1 [y2 y3 ...]}</em>, where (x0 + N*dx, y1[N]) is the 
% (N+1)-th point in the first series, etcetera. All tuples will have the same length L, which is equal to the number of 
% individual data series in the collection. As with the MSET format, each individual series typically represents a 
% repeated measure of the same stochastic phenomenon, so that variation in that phenomenon is captured by the collection
% itself. The sample interval dx and the initial value x0 are additional defining parameters for this data set format.
%
% RASTER1D. A collection of M rasters in x -- typically used to represent spike trains. The data in this case is a set 
% of M vectors, each containing 0 or more samples (eg, spike times). In the FC data source file, these vectors are 
% physically stored one after another in one long array that begins with the M raster lengths (each of which could be 
% different): {n1 n2 .. nM x1(1..n1) x2(1..n2) ... xM(1..nM)}.
%
% XYZIMG. This specialized data set format provides a means of representing 3D data of the form {x, y, z(x,y)}, where 
% one variable is a function of two independent variables. It is rendered as an indexed color image in Figure Composer. 
% The "data" in this case is really a NxM matrix storing an "intensity" z(x,y) at each "pixel" (x,y), where 
% x=[1..M] and y=[1..N]. At render time, the intensity image is associated with a colormap by which each intensity value
% is mapped to an RGB color; this colormap is not considered part of the data itself. Additional defining parameters are
% the actual range [x0..x1] spanned by the data in x, and the range [y0..y1] spanned in y. The x- and y-ranges enable 
% scaling the color image IAW the current dimensions of the FC graph in which it is embedded at render time.
%
% XYZSET. A simple set of points {(x, y, z)} in 3D space.
%
%
% REQUIRED ARGS:
% FILENAME - Character string holding the path of the destination file.
% ID - Character string holding the dataset identifier. This ID must be no more than 40 characters long and can only 
% contain selected US-ASCII characters: any alphanumeric, or any of these $_[](){}+-^!= punctuation marks. No whitespace 
% is permitted. If the ID is not valid, the function fails.
% FMT - A scalar value indicating the data format: 0=PTSET, 1=MSET, 2=SERIES, 3=MSERIES, 4=RASTER1D, 5=XYZIMG, 6=XYZSET.
% PARAMS - A vector holding additional defining parameters for the SERIES, MSERIES, and XYZIMG formats. For the SERIES 
% and MSERIES formats, this should be a two-element array [dx, x0]. Note that dx cannot be zero. For the XYZIMG format, 
% PARAMS should be a four-element array [x0, x1, y0, y1] defining the x- and y-coordinate ranges spanned by the image 
% matrix. Note that x0 cannot equal x1 and y0 cannot equal y1. If PARAMS is not empty, the function fails if the 
% parameter values are invalid. If it is empty, PUTDATANAVSRC assumes [dx x0] = [1 0] and [x0 x1 y0 y1] = [-1 1 -1 1].
% DATA - The actual data. The expected form of this argument depends on the data format. The function fails if the 
% argument does not meet these requirements.
%   PTSET: NxM matrix, where each row is a single M-tuple as described earlier and N is the number of data points. If 
%   matrix is not empty, M = [2..6].
%   MSET: NxM matrix, where each row is a single M-tuple as described earlier, M-1 is the number of sets in the 
%   collection, and N is the number of data points in each set. If the matrix is not empty, M must be 2 or greater.
%   SERIES: NxM matrix, where each row is a single M-tuple as described earlier and N is the number of samples. If 
%   matrix is not empty, M = [1..3].
%   MSERIES: NxM matrix, where each row is a single M-tuple as described above, M is the number of series in the 
%   collection, and N is the number of samples in each set. If the matrix is not empty, M >= 1.
%   RASTER1D: A Mx1 cell array, where each cell holds a vector containing the samples in an individual raster.
%   XYZIMG: NxM matrix holding the values z(x,y) for x=[1..M] and y=[1..N].
%   XYZSET: Nx3 matrix where each row defines the coordinates (x,y,z) of a point in 3D space.
%
% Note that all data will be converted to single-precision floating-point when it is saved to the file. NaN is accepted 
% as a valid datum value. 
%
% OPTIONAL ARGS:
% REPLACE - (default = 1) A scalar flag. If the destination file already contains a data set with the specified ID, the 
%    method fails UNLESS this flag is set, in which case the new data set replaces the old one.
% PLAINTEXT - (default = 0) A scalar flag applicable only if the destination file does not exist. If nonzero, the file 
%    is saved in a plain US-ASCII text format; otherwise, it is saved in binary format. The latter is significantly
%    more compact and efficient and should be used whenever possible.
%
% 
% Scott Ruffner
% sruffner@srscicomp.com
%

import com.srscicomp.fc.data.*

% the data format codes
PTSET=0; MSET=1; SERIES=2; MSERIES=3; RASTER1D=4; XYZIMG=5; XYZSET=6;

if(nargin < 5 || nargin > 7) 
	error('Invalid number of arguments!');
end
if(~ischar(filename))
    error('Arg FILENAME must be a string');
end
if(~ischar(id) || isempty(id) || length(id) > 40)
    error('Arg ID is not a string or has an invalid length');
end
if(~isscalar(fmt) || fmt < 0 || fmt > 6)
    error('Invalid data format FMT');
end

% use default parameter values if PARAMS is empty
if(isempty(params))
	if(fmt == SERIES || fmt == MSERIES)
		params = [1 0];
	elseif(fmt == XYZIMG)
		params = [-1 1 -1 1];
	end
elseif(~isvector(params))
	error('Arg PARAMS must be a vector');
end

% check dimensions of DATA for 2D formats
if(ndims(data) ~= 2)
    error('Arg DATA must have exactly two dimensions');
elseif(~isempty(data))
	[n,m] = size(data);
	if(fmt==PTSET) 
		ok = (m >= 2) && (m <= 6);
	elseif(fmt==MSET)
		ok = (m >= 2);
	elseif(fmt==SERIES)
		ok = (m >= 1) && (m <= 3);
	elseif(fmt==MSERIES)
		ok = (m >= 1);
   elseif(fmt==XYZSET)
      ok = (m == 3);
	else
		ok = 1;
	end
	if(~ok)
		error('Number columns in DATA not compatible with data format specified!');
	end
end

% check optional arguments. Set default values if not provided
if(nargin >= 6)
	if(~isscalar(replace))
		error('Arg REPLACE must be a scalar');
	end
else
	replace = 1;
end
if(nargin == 7)
	if(~isscalar(plaintext))
		error('Arg PLAINTEXT must be a scalar');
	end
else
	plaintext = 0;
end

% prepare DataSet object 
if(fmt == RASTER1D)
	if(~iscell(data))
		error('Arg DATA must be a cell array of vectors for the RASTER1D (4) format');
	end
	ncols = length(data);
	nrows = 0;
	rasterdata = zeros(1,ncols);
	for i=1:length(data)
		if(~(isempty(data{i}) || isvector(data{i})))
			error(sprintf('DATA{%d} invalid; must be empty or a numeric vector for RASTER1D format', i));
		end
		rasterdata(i) = length(data{i});
		nrows = nrows + length(data{i});
	end
	
	for i=1:length(data)
        if(isempty(data{i}))
            continue;
        end;
	    [n,m] = size(data{i});
        if(n == 1)
            rasterdata = [rasterdata data{i}];
		else
			rasterdata = [rasterdata data{i}'];
		end
	end
elseif(isempty(data))
	nrows = 0;
	ncols = 0;
else
	[nrows, ncols] = size(data);
end

dsinfo = DataSetInfo.createDataSetInfo(id, fmt, nrows, ncols, params);
if(~isjava(dsinfo) || isempty(dsinfo))
	error('Unable to create dataset info; probably illegal ID, bad PARAMS, or invalid DATA dimensions');
end

if(fmt == RASTER1D)
	ds = DataSet.createDataSet(dsinfo, rasterdata);
else
	ds = DataSet.createDataSet(dsinfo, reshape(data', nrows*ncols, 1));
end
if(~isjava(ds) || isempty(ds))
	error('Unable to create dataset; data inconsistent with specified data format?');
end

% create BinarySrcFile and use it to write the dataset created
factory = DataSrcFactory.getInstance();
dataSrc = factory.getDataSource(java.io.File(filename), plaintext);
if(~isjava(dataSrc) || isempty(dataSrc)) 
	error('Existing file not recognized as a data source!');
end
ok = dataSrc.writeData(ds, (replace ~= 0));
if(~ok)
	msg = strcat('Unable to write dataset to file: ', char(dataSrc.getLastError()));
	error(msg);
end
