require(['knockout', 'jquery', 'bootstrap', 'worldwind'],
    function (RenderableLayer,
              Sector,
              SurfaceImage,
              WWUtil) {
        "use strict";

        /**
         * @alias NetcdfLayer 
         * @constructor
         * @augments RenderableLayer
         * @classdesc Displays a netcdf data set. 
         */
        var NetcdfLayer = function () {
            RenderableLayer.call(this, "NetcdfLayer");

            var surfaceImage = new SurfaceImage(Sector.FULL_SPHERE,
                "http://199.109.195.187:8000//user/ubuntu/BCSD/tasmax/variable/tasmax/2014-01-29.png");

            this.addRenderable(surfaceImage);

            this.pickEnabled = false;
            this.minActiveAltitude = 3e6;
        };

        NetcdfLayer.prototype = Object.create(RenderableLayer.prototype);

        return NetcdfLayer;
    });
