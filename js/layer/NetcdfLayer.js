define(['worldwind'],
    function (ww) {
        "use strict";

        /**
         * @alias NetcdfLayer 
         * @constructor
         * @augments RenderableLayer
         * @classdesc Displays a netcdf data set. 
         */
        //var NetcdfLayer = function () {
        function NetcdfLayer() {
	  var self = this;
            WorldWind.RenderableLayer.call(this, "NetcdfLayer");

            //var surfaceImage = new WorldWind.SurfaceImage(WorldWind.Sector.FULL_SPHERE,
            //    "http://199.109.195.187:8000//user/ubuntu/BCSD/tasmax/variable/tasmax/2014-01-29.png");

            //this.addRenderable(surfaceImage);

            this.pickEnabled = false;
            this.minActiveAltitude = 3e6;
	    self.currentIndex = 0;
	    self.images = []; 
	    self.imageDates = [];
	    self.textAttributes = new WorldWind.TextAttributes(null);
	    self.textAttributes.color = WorldWind.Color.RED;
	    self.screenText;

	    self.populate = function(imgUrls,imgDates){
	      for(var i = 0; i < imgUrls.length; i++){
		var surfaceImage = new WorldWind.SurfaceImage(WorldWind.Sector.FULL_SPHERE,imgUrls[i]);
		self.images.push(surfaceImage);
		self.imageDates.push(imgDates[i]);
	      }
	    }
	    self.empty = function(){
	      self.images = [];
	    }
	    self.showNext = function(){
	      self.removeAllRenderables();
	      self.addRenderable(self.images[self.currentIndex]);
	      self.addScreenText();
	      self.currentIndex++;
	      if(self.currentIndex == self.images.length)
		self.currentIndex = 0;
	    }
	    self.showPrevious = function(){
	      self.removeAllRenderables();
	      self.addRenderable(self.images[self.currentIndex]);
	      self.addScreenText();
	      self.currentIndex--;
	      if(self.currentIndex < 0)
		self.currentIndex = self.images.length-1;;
	    }

	    self.showFirst = function(){
	      self.removeAllRenderables();
	      self.currentIndex = 0;
	      self.addRenderable(self.images[self.currentIndex]);
	      self.addScreenText();
	    }

	    self.showLast = function(){
	      self.removeAllRenderables();
	      self.currentIndex = self.images.length-1;
	      self.addRenderable(self.images[self.currentIndex]);
	      self.addScreenText();
	    }

	    self.addScreenText = function(){
	      self.screenText = new WorldWind.ScreenText(
		              new WorldWind.Offset(WorldWind.OFFSET_FRACTION, 0.5, WorldWind.OFFSET_FRACTION, 0.5), self.imageDates[self.currentIndex]);
	      self.screenText.attributes = self.textAttributes;
	      self.addRenderable(self.screenText);
	    }
	}

	NetcdfLayer.prototype = Object.create(WorldWind.RenderableLayer.prototype);

	return NetcdfLayer;
    });
