/* 
 * Copyright (c) 2016 Bruce Schubert.
 * The MIT License
 * http://www.opensource.org/licenses/mit-license
 */

/*global require, requirejs, WorldWind */

/**
 * Require.js bootstrapping javascript
 */
requirejs.config({
// Path mappings for the logical module names
    paths: {
        'knockout': 'libs/knockout/knockout-3.4.0.debug',
        'jquery': 'libs/jquery/jquery-2.1.3',
        'jqueryui': 'libs/jquery-ui/jquery-ui-1.11.4',
        'jquery-growl': 'libs/jquery-plugins/jquery.growl',
        'bootstrap': 'libs/bootstrap/v3.3.6/bootstrap',
        'worldwind': 'libs/webworldwind/worldwindlib',
        'model': 'model' // root application path
    },
    // Shim configuration for Bootstrap's JQuery dependency
    shim: {
        "bootstrap": {
            deps: ["jquery"],
            exports: "$.fn.popover"
        }
    }
});

/**
 * A top-level require call executed by the Application.
 */
require(['knockout', 'jquery', 'bootstrap', 'worldwind',
    'model/Config',
    'model/Constants',
    'model/Explorer',
    'model/globe/Globe',
    'views/GlobeViewModel',
    'views/HeaderViewModel',
    'views/LayersViewModel',
    'views/DatasetsViewModel',
    'views/ProjectionSwitchModel',
    'server/AccountManagerModel',
    'logging/LogManagerModel',
    'model/globe/layers/UsgsContoursLayer',
    'model/globe/layers/UsgsImageryTopoBaseMapLayer',
    'model/globe/layers/UsgsTopoBaseMapLayer'],
        function (ko, $, bootstrap, ww,
                config,
                constants,
                explorer,
                Globe,
                GlobeViewModel,
                HeaderViewModel,
                LayersViewModel,
		DatasetsViewModel,
                ProjectionSwitchModel,
		AccountManagerModel,
		LogManagerModel,
                UsgsContoursLayer,
                UsgsImageryTopoBaseMapLayer,
                UsgsTopoBaseMapLayer) { // this callback gets executed when all required modules are loaded
            "use strict";
            // ----------------
            // Setup the globe
            // ----------------
            WorldWind.Logger.setLoggingLevel(WorldWind.Logger.LEVEL_WARNING);
            WorldWind.configuration.baseUrl = ww.WWUtil.currentUrlSansFilePart() + "/" + constants.WORLD_WIND_PATH;

            // Define the configuration for the primary globe
            var globeOptions = {
                showBackground: true,
                showReticule: true,
                showViewControls: true,
		showWidgets: false,
                includePanControls: config.showPanControl,
                includeRotateControls: true,
                includeTiltControls: true,
                includeZoomControls: true,
                includeExaggerationControls: config.showExaggerationControl,
                includeFieldOfViewControls: config.showFieldOfViewControl
            },
            globe;

	    // Create the explorer's primary globe that's associated with the specified HTML5 canvas
	    globe = new Globe(new WorldWind.WorldWindow("canvasOne"), globeOptions);

	    // Defined the Globe's layers and layer options
	    globe.layerManager.addBaseLayer(new WorldWind.BMNGLayer(), {enabled: true, hideInMenu: true, detailHint: config.imageryDetailHint});
	    /*globe.layerManager.addBaseLayer(new WorldWind.BMNGLandsatLayer(), {enabled: false, detailHint: config.imageryDetailHint});
	    globe.layerManager.addBaseLayer(new WorldWind.BingAerialWithLabelsLayer(null), {enabled: false, detailHint: config.imageryDetailHint});
	    globe.layerManager.addBaseLayer(new UsgsImageryTopoBaseMapLayer(), {enabled: false, detailHint: config.imageryDetailHint});
	    globe.layerManager.addBaseLayer(new UsgsTopoBaseMapLayer(), {enabled: false, detailHint: config.imageryDetailHint});
	    globe.layerManager.addBaseLayer(new WorldWind.BingRoadsLayer(null), {enabled: false, opacity: 0.7, detailHint: config.imageryDetailHint});
	    //globe.layerManager.addBaseLayer(new WorldWind.OpenStreetMapImageLayer(null), {enabled: false, opacity: 0.7, detailHint: config.imageryDetailHint});
*/
	    // Initialize the Explorer object
	    explorer.initialize(globe);
	    return;
	    var logger = new LogManagerModel();

	    // --------------------------------------------------------
	    // Bind view models to the corresponding HTML elements
	    // --------------------------------------------------------
	    var dvModel = new DatasetsViewModel(globe,logger);

	    ko.applyBindings(new LayersViewModel(globe), document.getElementById('layers'));
	    ko.applyBindings(dvModel,document.getElementById('datasets'));
	    ko.applyBindings(dvModel,document.getElementById('upload'));
	    ko.applyBindings(new ProjectionSwitchModel(globe),document.getElementById('projectionSwitch'));
	    ko.applyBindings(new AccountManagerModel(logger),document.getElementById('account'));

	    // -----------------------------------------------------------
	    // Add handlers to auto-expand/collapse the menus
	    // -----------------------------------------------------------
	    // Auto-expand menu section-bodies when not small
	    $(window).resize(function () {
	      if ($(window).width() >= 768) {
		$('.section-body').collapse('show');
	      }
	    });
	    // Auto-collapse navbar when sts tab items are clicked
	    $('.navbar-collapse a[role="tab"]').click(function () {
	      $('.navbar-collapse').collapse('hide');
	    });
	    // Auto-scroll-into-view expanded dropdown menus
	    $('.dropdown').on('shown.bs.dropdown', function (event) {
	      event.target.scrollIntoView(false); // align to bottom
	    });

	    // ------------------------------------------------------------
	    // Add handlers to save/restore the session
	    // -----------------------------------------------------------
	    window.onbeforeunload = function () {
	      explorer.saveSession();
	      // Return null to close quietly on Chrome and FireFox.
	      return null;
	    };

	    // Now that MVC is set up, restore the model from the previous session.
	    explorer.restoreSession();
	}
);
