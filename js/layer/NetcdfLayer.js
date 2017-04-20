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
	    self.images = []; 
	    self.populate = function(imgUrls){
	      for(var i = 0; i < imgUrls.length; i++){
		var surfaceImage = new WorldWind.SurfaceImage(WorldWind.Sector.FULL_SPHERE,imgUrls[i]);
		self.images.push(surfaceImage);
	      }
	    }
	    self.empty = function(){
	      self.images = [];
	    }
	    self.showAt = function(index){
	      alert('Comin in showat');
	      self.removeAllRenderables();
	      self.addRenderable(self.images[index]);
	    }
	}

        NetcdfLayer.prototype = Object.create(WorldWind.RenderableLayer.prototype);

        return NetcdfLayer;
    });
