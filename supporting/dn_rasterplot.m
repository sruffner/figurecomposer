function dn_rasterplot(rasterData, varargin)
%DN_RASTERPLOT Display series of discrete-time events as "spike trains".
%
% A sequence of occurrence events -- such as action potentials recorded from a neural unit -- is often rendered in a
% 2D graph as a series of short, disjoint vertical lines sharing the same baseline Y=A. Furthermore, if there are N
% such event sequences -- or "trains" -- each train is typically drawn on a different baseline: Y=1, Y=2, ... Y=N.
%
% DN_RASTERPLOT displays the supplied discrete-time event data in this manner, which is sometimes called a "raster 
% plot". It prepares vectors X and Y representing a single lineseries that renders the raster plot as described, 
% invoking PLOT(X,Y) to draw it into the current axes. Each vertical line in the raster plot is represented by a
% sequence of three consecutive data points in the line series: (x, y), (x, y+H), (NaN, NaN), where H is the height of
% the line in data units. The undefined data point ensures that the individual lines are not connected when the line
% series plot is drawn.
%
% This function is provided as a convenient means of creating plots of spike train rasters which can be easily converted
% to a DataNav/FigureComposer FypML figure via the Matlab utility MATFIG2FYP(). When MATFIG2FYP encounters a line series
% plot object with XData and YData content conforming to the format described above, that plot object is translated into
% a FypML "raster node" when the Matlab figure is converted to FypML format.
%
% DN_RASTERPLOT(R) plots the raster data in R, which must take on one of two forms:
%    -- An NxM matrix. In this case, each column is interpreted as a separate raster over N time units. If R(n,m) is
%    nonzero, then an "event occurred" at time (n-1) in the m-th event train. For each such event, the triplet of points
%    (n-1, m-1), (n-1, m-1 + 0.75), (NaN, NaN) is appended to the line series that is eventually plotted.
%    -- A 1XM or Mx1 cell array, where each cell R{m} contains a numeric vector V. In this case, each such vector is 
%    interpreted as a separate raster train, with the vector containing the "times" at which an event occured in that
%    train. For each such event, a triplet of points (V(i), m-1), (V(i), m-1 +0.75), (NaN, NaN) is appended to the line
%    series.
%
% DN_RASTERPLOT(R, Y0) plots the raster data in R, but offsets the Y-coordinate values of every point in the line series
%    by the real scalar value Y0.
%
% DN_RASTERPLOT(R, Y0, T): As above, except that the X-coordinate values of every point in the line series are 
%    multiplied by the real scalar value T. Typically, this form is most useful when R is in matrix form, in which case
%    the event times are really row indices. If T is the event sample interval in seconds, e.g., then the raster data
%    can be plotted on the actual time scale.
%
% DN_RASTERPLOT(R, Y0, T, C): As above, except C is a single-character string indicating the color with which the
%    raster plot should be stroked: 'r'=red, 'g'=green, 'b'=blue, 'c'=cyan, 'm'=magenta, 'y'=yellow, 'k'=black, and
%    'w'=white. If C is not one of these strings, the raster will be stroked in black.
%
% DN_RASTERPLOT(R, Y0, T, C, H): As above, except H is the handle of the axes into which the raster plot should be 
%    drawn. In all of the above cases, the raster plot is drawn into the current axes (creating a new figure if none
%    exists).
%
% 
% Scott Ruffner
% sruffner@srscicomp.com
%

nArgs = nargin;
if(nArgs < 1 || nArgs > 5)
   error('Invalid number of arguments');
end;

% defaults
yOffset = 0;
tScale = 1;
plotColor = 'k';

% validate the one required input argument. Note that if it is empty, there's nothing to do!
if(isempty(rasterData))
   disp('No raster data to plot!');
   return;
end;

ok = 0;
if(iscell(rasterData))
   ok = isvector(rasterData);
   if(ok)
      for i=1:length(rasterData)
         v = rasterData{i};    % NOTE: the curly brackets are KEY!
         ok = isempty(v) || (isvector(v) && isnumeric(v) && isreal(v));
         if(~ok) 
            break;
         end;
      end;
   end;
elseif(ismatrix(rasterData))
   ok = (ndims(rasterData) == 2) && (islogical(rasterData) || (isnumeric(rasterData) && isreal(rasterData)));
end;
if(~ok)
   error('Invalid raster data specified in first argument');
end;
         
% process any additional input arguments
if(nArgs == 5)
   axesH = varargin{4};
   if(~ishandle(axesH) || ~strcmp('axes', get(axesH, 'Type')))
      error('Arg 5 invalid -- must be a valid axes handle');
   end;
else
   axesH = gca;
end;

if(nArgs >= 4)
   % if this argument is not one of the 8 single-character color codes, then black is used
   clr = varargin{3};
   if(ischar(clr) && any(strcmp(clr, {'r', 'g', 'b', 'c', 'm', 'y', 'k', 'w'})))
      plotColor = clr;
   end;
end;

if(nArgs >= 3)
   tScale = varargin{2};
   if(~(isscalar(tScale) && isreal(tScale)))
      error('Arg 3 invalid -- must be a real scalar value');
   end;
end;

if(nArgs >= 2)
   yOffset = varargin{1};
   if(~(isscalar(yOffset) && isreal(yOffset)))
      error('Arg 2 invalid -- must be a real scalar value');
   end;
end;


% prepare X and Y vectors for the line series that will render the raster plot
if(iscell(rasterData))
   % first get a count of the total # of vertical marks, aka "events", there will be in the raster plot. We have to
   % check for NaN and Inf in each vector of event "times".
   nEvents = 0;
   for i=1:length(rasterData)
      nEvents = nEvents + sum(isfinite(rasterData{i}));   % NOTE: The curly brackets are KEY!
   end;
   if(nEvents == 0)
      disp('No raster data to plot!');
      return;
   end;
   
   x = nan(nEvents*3, 1);
   y = nan(nEvents*3, 1);
   iEvent = 0;
   for i=1:length(rasterData)
      aRaster = rasterData{i};   % NOTE: The curly brackets are KEY!
      for j=1:length(aRaster)
         if(isfinite(aRaster(j)))
            x(iEvent + 1) = tScale * aRaster(j);
            y(iEvent + 1) = i-1 + yOffset;
            x(iEvent + 2) = x(iEvent + 1);
            y(iEvent + 2) = y(iEvent + 1) + 0.75;
            iEvent = iEvent + 3;
         end;
      end;
   end;
else
   % get the row,col indices of all nonzero elements in matrix. If all entries are zero, there's nothing to draw!
   [idxRows, idxCols] = find(rasterData ~= 0);
   if(isempty(idxRows))
      disp('No raster data to plot!');
      return;
   end;

   x = nan(length(idxRows)*3, 1);
   y = nan(length(idxRows)*3, 1);
   for i=1:length(idxRows)
      x(i*3 - 2) = tScale * (idxRows(i) - 1);
      y(i*3 - 2) = idxCols(i)-1 + yOffset;
      x(i*3 - 1) = x(i*3 - 2);
      y(i*3 - 1) = y(i*3 - 2) + 0.75;
   end;
end;

plot(axesH, x, y, plotColor);
end

