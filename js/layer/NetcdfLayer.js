define(['worldwind','heatmap'],
    function (ww,h337) {
        "use strict";

        /**
         * @alias NetcdfLayer 
         * @constructor
         * @augments RenderableLayer
         * @classdesc Displays a netcdf data set. 
         */
        //var NetcdfLayer = function () {
        function NetcdfLayer(name) {
	  var self = this;
	  WorldWind.RenderableLayer.call(this, "NetcdfLayer");


	  this.pickEnabled = true;
	  this.minActiveAltitude = 3e6;
	  self.name = name;
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

	  self.populateJSON = function(bounds,data,shape,dates,limits,blur,radius){
	    self.empty();
	    //populate a analytical surface
	    var numY = Number(shape[0]);//lat
	    var numX = Number(shape[1]);//lon
	    var numT = Number(shape[2]);//time
	    var canvas = document.getElementById("canvas1");
	    var height = $('#canvas1').height();
	    var width = $('#canvas1').width();
	    
	    
	    var yPixel = Math.ceil(height/numY);
	    var xPixel = Math.ceil(width/numX);
	    for(var t = 0; t < numT; t++){
	      var heatmapInstance = h337.create({
		container: canvas,
		opacity: 1,
		blur: blur,
		radius: radius 
	      });
	      var points = [];
	      var min = Number(limits[0]);
	      var max = Number(limits[1]);
	      var diff = max - min;
	      var indices = [];
	      for(var i = 0; i < numY; i++){
		for(var j = 0; j < numX; j++){
		  var index = Number(i*numX*numT + j*numT + t);
		  indices.push(index);
		  //var value = Math.floor(Math.random()*diff + min);
		  var value = data[index];
		  var point = {
		    x: j*xPixel,
		    y: i*yPixel,
		    value: value
		  };
		  points.push(point);
		}
	      }

	      // heatmap data format
	      var heatmapdata = { 
		min: min,
		max: max, 
		data: points 
	      };
	      heatmapInstance.setData(heatmapdata);

	      var surfaceImage = new WorldWind.SurfaceImage(WorldWind.Sector.FULL_SPHERE,heatmapInstance.getDataURL());
	      self.images.push(surfaceImage);
	      self.imageDates.push(dates[t]);
	      //run one round of animation in the background
	    }
	  }

	  self.empty = function(){
	    self.removeAllRenderables();
	    self.images = [];
	    self.imageDates = [];
	  }

	  self.showAt = function(i){
	    self.removeAllRenderables();
	    self.addRenderable(self.images[i]);
	    self.opacity = 0.8;
	    self.addScreenText(i);
	  }

	  self.showNext = function(){
	    self.showAt(self.currentIndex);
	    self.currentIndex++;
	    if(self.currentIndex == self.images.length)
	      self.currentIndex = 0;
	  }
	  self.showPrevious = function(){
	    self.showAt(self.currentIndex);
	    self.currentIndex--;
	    if(self.currentIndex < 0)
	      self.currentIndex = self.images.length-1;;
	  }

	  self.showFirst = function(){
	    self.currentIndex = 0;
	    self.showAt(self.currentIndex);
	  }

	  self.showLast = function(){
	    self.currentIndex = self.images.length-1;
	    self.showAt(self.currentIndex);
	  }

	  self.addScreenText = function(i){
	    self.screenText = new WorldWind.ScreenText(
		new WorldWind.Offset(WorldWind.OFFSET_FRACTION, 1.0-0.05, WorldWind.OFFSET_FRACTION, 0), self.imageDates[i]);
	    self.screenText.attributes = self.textAttributes;
	    self.addRenderable(self.screenText);
	  }
	}

	NetcdfLayer.prototype = Object.create(WorldWind.RenderableLayer.prototype);

	return NetcdfLayer;
    });
