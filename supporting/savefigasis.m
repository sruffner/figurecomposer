function savefigasis(figHandle, dst)
%SAVEFIGASIS Save an open Matlab figure to a .FIG file, making select changes to improve fidelity when the file is
% imported by Figure Composer.
%
% SAVEFIGASIS(H, F) is a cover for the Matlab built-in function HGSAVE(H, F, '-v7'), intended for those who wish to 
% import their Matlab .FIG files into the Java app FigureComposer. The script makes a few changes to certain HG object
% properties in an effect to ensure that the imported FypML figure will more accurately replicate the original Matlab
% figure. Below is a list of the changes made:
%
%  -- Axis range, tick mark locations, and color limits need to be explicitly stored to ensure that the FypML figure
%  can match these properties. Therefore, for every 'axes' object therein, the following properties are set to 'manual':
%  'XLimMode', 'YLimMode', 'ZLimMode', 'CLimMode', 'XTickMode', 'YTickMode', and 'ZTickMode'. Doing so ensures that the 
%  axis ranges ('XLim', 'YLim', 'ZLim') and tick locations ('XTick', 'YTick', 'ZTick'), and the color limits range 
%  ('CLim'), are explicitly stored in the FIG file. If any of these are set to 'auto' (the Matlab default), then the 
%  corresponding property is not stored.
% 
%  -- Similarly, for every 'scribe.colorbar' object therein, the 'XTickMode' and 'YTickMode' properties are set to 
%  manual. However, if the Matlab version is 2014b or higher, then 'TicksMode' and 'LimitsMode' are each set to 
%  'manual'. The 2014b release includes a major overhaul of Matlab graphics; colorbar objects are no longer 'axes' 
%  objects, and have some different properties.
% 
%  -- (As of Aug 2024, FC 5.5.0) In order to import the title of a 'matlab.graphics.axis.PolarAxes' object in the FypML 
% figure, the polar plot's 'Title' property, a full-fledged Matlab Text object, is converted to a structure via 
% handle2struct() and that structure is saved in the 'UserData' field. When the FIG-to-FypML engine encounters this 
% field, it adds the title as a 'label' or 'textbox' child of the FypML 'pgraph' element. Prior to this change, 
% SAVEFIGASIS only stored the title string and color in a cell array {title, color}, which was placed in 'UserData'.
% (The FIG-to-FypML engine handles either form for the 'UserData' property.)
%
%  -- For colorbar objects (R2014b or later), the axis label is in a Matlab Text object "Label". Since FigureComposer
%  cannot process that object, it would not have access to the label string. As a workaround SAVEFIGASIS stores the
%  label string in the "UserData" field, unless that field is already set to something else.
%
%  -- For bubblechart objects (R2020b or later), the "UserData" field is set to 1x4 vector [bubblesize_min 
%  bubblesize_max bubblelim_min bubblelim_max]. Otherwise this information is not available to FigureComposer.
%
% H is the handle of the figure to be saved, and F is a character string specifying the path to which figure should be 
% saved. If the filename does not end in the '.fig' extension, that extension will be added automatically.
%
% NOTES:
% 1) SAVEFIGASIS() calls HGSAVE with the '-v7' parameter to ensure that the FIG file is saved in a format that 
% FigureComposer can read. The latest MAT/FIG file format (V7.3) uses HDF storage format and cannot be imported by
% FigureComposer.
%
% 2) Use of SAVEFIGASIS() is not required in order to import a Matlab figure into FigureComposer. It merely will help
% to improve the fidelity of the imported figure. An alternative approach is to use the FC-specific Matlab utility
% function MATFIG2FYP(), which converts an open Matlab figure directly into a FypML figure. Since this function runs in
% the Matlab environment, it can query each 'axes' or 'scribe.colorbar' object for the current values of any property.
% 
% Scott Ruffner
% sruffner@srscicomp.com
%

nArgs = nargin;
if(nArgs ~= 2)
   error('Invalid number of arguments');
end

if(~ishandle(figHandle) || ~ischar(dst))
   error('Missing or bad argument');
end

fig = handle2struct(figHandle);
if(~strcmp(fig.type, 'figure'))
   error('Argument not a Matlab figure handle graphics object');
end

% for each 'axes' or 'scribe.colorbar' child of the root figure, ensure 'XLimMode', etc are set to 'manual'
for i=1:length(fig.children)
   child = fig.children(i);
   if(strcmp(child.type, 'axes'))
      set(child.handle, 'XLimMode', 'manual');
      set(child.handle, 'YLimMode', 'manual');
      set(child.handle, 'ZLimMode', 'manual');
      set(child.handle, 'CLimMode', 'manual');
      set(child.handle, 'XTickMode', 'manual');
      set(child.handle, 'YTickMode', 'manual');
      set(child.handle, 'ZTickMode', 'manual');
   elseif(strcmp(child.type, 'scribe.colorbar'))
      if(verLessThan('matlab', '8.4.0'))
         set(child.handle, 'XTickMode', 'manual');
         set(child.handle, 'YTickMode', 'manual');
      else
         set(child.handle, 'TicksMode', 'manual');
         set(child.handle, 'LimitsMode', 'manual');
         labelObj = get(child.handle, 'Label');
         userData = get(child.handle, 'UserData');
         if(isobject(labelObj) && isempty(userData))
            set(child.handle, 'UserData', labelObj.String);
         end
      end
   elseif(strcmp(child.type, 'matlab.graphics.axis.PolarAxes'))
      titleObj = get(child.handle, 'Title');
      if(isobject(titleObj))
         set(child.handle, 'UserData', handle2struct(titleObj));
      end
   end

   % save bubble size and limit info in 'UserData' of each bubblechart object (R2020b or later)
   if((strcmp(child.type, 'axes') || strcmp(child.type, 'matlab.graphics.axes.PolarAxes')) && ...
         ~verLessThan('matlab', '9.9.0'))
      set(figHandle, 'currentaxes', child.handle);
      bubbleinfo = [bubblesize bubblelim];
      for j=1:length(child.children)
         chart = child.children(j);
         if(strcmp(chart.type, 'bubblechart'))
            set(chart.handle, 'UserData', bubbleinfo);
         end
      end
   end

end

% now save the figure to a FIG file. Make sure we don't use V7.3 (HDF)
% format, which FigureComposer cannot read.
hgsave(figHandle, dst, '-v7');

end

