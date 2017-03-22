/* 
 * Copyright (c) 2016 Varun Chandola <chandola@buffalo.edu>.
 * Released under the MIT License
 * http://www.opensource.org/licenses/mit-license.php
 */

/**
 * Dataset View Model
 * 
 * @param {type}
 *            ko
 * @param {type} $
 * @returns {DatasetsViewModel}
 */
define(
		[ 'knockout', 'jquery', 'model/Constants' ],
		function(ko, $, constants) {

			/**
			 * The datasets viewing model
			 * 
			 * @param {Globe}
			 *            globe The globe that provides the supported
			 *            projections
			 * @param {LogViewModel}
			 *            logger The logging module for console based outputs
			 * @constructor
			 */
			function DatasetsViewModel(globe, logger) {
				var self = this;
				layerManager = globe.layerManager;
				self.availableDatasets = ko.observableArray([]);
				self.selectedDataset = ko.observable();
				self.selectedDatasetAnalysis = ko.observable();
				self.fields = ko.observableArray([]);
				self.analysisFields = ko.observableArray([]);
				self.analysisMethods = ko
						.observableArray([ "Change Detection" ]);
				self.infoActive = false;
				self.openActive = false;
				self.openAnalysis = false;
				self.downloading = false;
				self.submitting = false;

				/*
				 * Populate available datasets from the database
				 */

				self.populateDatasets = function() {
					self.availableDatasets.removeAll();
					var webGlobeServer = constants.WEBGLOBE_SERVER;
					$
							.ajax(
									{
										url : webGlobeServer
												+ 'GetDatasetDetails',
										cache : false,
										type : 'POST',
										contentType : 'application/json; charset=utf-8',
										success : function(dataJSON) {
											for (var i = 0; i < dataJSON.count.value; i++) {
												var datasetInfo = 'dataset' + i;
												var id = dataJSON[datasetInfo].id;
												var url = dataJSON[datasetInfo].url;
												var name = dataJSON[datasetInfo].name;
												if (dataJSON[datasetInfo].available == 'all')
													user_data = false;
												else
													user_data = true;

												var info = dataJSON[datasetInfo].info;
												var info_url = dataJSON[datasetInfo].info_url;
												var fieldcount = dataJSON[datasetInfo].fieldcount;
												var fields = [];
												for (var j = 0; j < fieldcount; j++) {
													var fieldInfo = 'field' + j;
													var fieldName = dataJSON[datasetInfo][fieldInfo];
													fields.push(fieldName);
												}
												var datasetLayer = layerManager.createDatasetLayer(name);
												
												self.availableDatasets.push({
													'id' : id,
													'url' : url,
													'name' : name,
													'user_data' : user_data,
													'info' : info,
													'info_url' : info_url,
													'fields' : fields,
													'enabled' : false,
													'layer' : datasetLayer,
													'images': ko.observableArray(),
													'imagesAddress': "",
													'variableAddress' : ""
												});
											}

										}
									})
							.fail(
									function(xhr, textStatus, err) {
										logger
												.log(
														"Error getting dataset information from the server",
														"alert-danger");
										return 'NA';
									});
				}

				self.showDatasetPanel = function(dataset) {
					if (!self.openActive) {
						self.selectedDataset = dataset;
						for (var i = 0; i < dataset.fields.length; i++) {
							self.fields.push(dataset.fields[i]);
						}
						$("#datasetOpenPanel").show();
						$("#selectTimePanel").hide();
						self.openActive = true;
						self.selectedDataset.layer.enabled = true;
					} else {
						self.selectedDataset.layer.enabled = false;
						self.selectedDataset = null;
						self.fields.removeAll();
						
						$("#datasetOpenPanel").hide();
						self.openActive = false;
					}
				}

				self.showAnalysisPanel = function(dataset) {
					if (!self.openAnalysis) {
						self.selectedDatasetAnalysis = dataset;
						for (var i = 0; i < dataset.fields.length; i++) {
							self.analysisFields.push(dataset.fields[i]);
						}
						$("#analysisPanel").show();
						self.openAnalysis = true;
					} else {
						self.selectedDatasetAnalysis = null;
						self.analysisFields.removeAll();
						$("#analysisPanel").hide();
						self.openAnalysis = false;
					}
				}

				self.displayTimePanel = function() {
					var fieldname = $("#fieldSelect :selected").text();
					self.selectedDataset.fieldname = fieldname;
					var url = self.selectedDataset.url;
					var webGlobeServer = constants.WEBGLOBE_SERVER;
					
					$.ajax({
                        url: webGlobeServer + 'LoadNetcdfDataset',
                        cache: false,
                        type: 'POST',
                        contentType: 'application/json; charset=utf-8',
                        data: JSON.stringify({
                            url: url,
                            fieldname: fieldname
                        }),
                        success: function (dataJSON) {
                        	self.selectedDataset.imagesAddress = dataJSON.variable.imagesAddress;
                        	self.selectedDataset.variableAddress = dataJSON.variable.address;

                            $('#create-start-date').attr({
                                "max" : dataJSON.variable.maxDate,
                                "min" : dataJSON.variable.minDate
                            });
                            $('#create-start-date').val(dataJSON.variable.minDate);
                            
                            $('#create-end-date').attr({
                                "max" : dataJSON.variable.maxDate,
                                "min" : dataJSON.variable.minDate
                            });
                            $('#create-end-date').val(dataJSON.variable.maxDate);                        	
                        	
                            $('#load-start-date').attr({
                                "max" : dataJSON.variable.imageMaxDate,
                                "min" : dataJSON.variable.imageMinDate
                            });
                            $('#load-start-date').val(dataJSON.variable.imageMinDate);
                            
                            $('#load-end-date').attr({
                                "max" : dataJSON.variable.imageMaxDate,
                                "min" : dataJSON.variable.imageMinDate
                            });
                            $('#load-end-date').val(dataJSON.variable.imageMaxDate);

                        }
                    }).fail(function (xhr, textStatus, err) {
                        alert(err);
                    });

					$("#selectTimePanel").show();
				}
				
				self.createImages = function() {
					if (self.downloading) {
						logger
								.log(
										"Please wait till the current creating images job completes.",
										"alert-warning")
						return;
					}

                    if (self.selectedDataset.variableAddress  !== "") { 
                    	var webGlobeServer = constants.WEBGLOBE_SERVER;

                    	self.downloading = true;
    					logger.log("Creating images for <a href=\"" + self.selectedDataset.url + "\">"
    							+ self.selectedDataset.name + ":" + self.selectedDataset.fieldname
    							+ "</a>", "alert-info");

                        $.ajax({
                            url: webGlobeServer + 'CreateImages',
                            cache: false,
                            contentType: 'application/json; charset=utf-8',
                            type: 'POST',
                            data: JSON.stringify({
                                url: self.selectedDataset.variableAddress,
                                fieldname: self.selectedDataset.fieldname,
                                from: $('#create-start-date').val(),
                                to: $('#create-end-date').val()
                            }),
                            success: function (data) {
                                // alert('added');
                            	self.selectedDataset.imagesAddress = data.imagesAddress;

                                $('#load-start-date').attr({
                                    "max" : data.imageMaxDate,
                                    "min" : data.imageMinDate
                                });
                                $('#load-start-date').val(data.imageMinDate);
                                
                                $('#load-end-date').attr({
                                    "max" : data.imageMaxDate,
                                    "min" : data.imageMinDate
                                });
                                $('#load-end-date').val(data.imageMaxDate);
                                self.downloading = false;
            					logger.log("Completed the creating images job.", "alert-info");                               
                            }
                        }).fail(function (xhr, textStatus, err) {
                            alert(err);
                        });                            
                    } else{
                        alert('No URL entered.');
                    }
					
				}
				
				self.loadImages = function() {
					var webGlobeServer = constants.WEBGLOBE_SERVER;
					
                    if (self.selectedDataset.imagesAddress  !== "") {
                        
                        $.ajax({
                            url: webGlobeServer + 'LoadImages',
                            cache: false,
                            type: 'POST',
                            data: {
                                url: self.selectedDataset.imagesAddress,
                                from: $('#load-start-date').val(),
                                to: $('#load-end-date').val()
                            },
                            success: function (data) {
                                // alert('added');
                                var imageUrls = data.split(",");
                                                                                                                            
                                var len = imageUrls.length - 1;

                                self.selectedDataset.images.removeAll();
                                for (var i = 0; i < len; ++i) {
                                	self.selectedDataset.images.push(new WorldWind.SurfaceImage(
                                        new WorldWind.Sector(-90, 90, -180,180),imageUrls[i]));
                                }

                                $('#index-of-show-date').attr({
                                    "max" : len-1,
                                    "min" : 0
                                });
                                
                                $('#index-of-show-date').val(0);
                                $('#index-of-show-date').change();
                            }
                        }).fail(function (xhr, textStatus, err) {
                            alert(err);
                        });                            
                    } else{
                        alert('No URL entered.');
                    }
					
				}
				
				self.onInputShowDate = function() {
                    var index = parseInt($('#index-of-show-date').val());
                    var imagesource = self.selectedDataset.images()[index]._imageSource;
                    $('#show-date').val(imagesource.substring(imagesource.lastIndexOf('/')+1));
                    
                    self.selectedDataset.layer.removeAllRenderables();
                    self.selectedDataset.layer.addRenderable(self.selectedDataset.images()[index]);
                    globe.redraw();					
				}
				
//				self.loadDataset = function() {
//					if (self.downloading) {
//						logger
//								.log(
//										"Please wait till the current download completes.",
//										"alert-warning")
//						return;
//					}
//					var fieldname = $("#fieldSelect :selected").text();
//					var url = self.selectedDataset.url;
//					logger.log("Loading <a href=\"" + url + "\">"
//							+ self.selectedDataset.name + ":" + fieldname
//							+ "</a>", "alert-info");
//					self.downloading = true;
//					$("#selectTimePanel").show();
//					// start downloading
//					// DINH -- Add your downloading code here
//					// finish downloading
//					// self.downloading=false;
//				}

				self.analyzeDataset = function() {
					if (self.submitting) {
						logger
								.log(
										"Please wait till the current job is submitted.",
										"alert-warning")
						return;
					}
					var fieldname = $("#fieldAnalysisSelect :selected").text();
					var analysisname = $("#analysisSelect :selected").text();
					var url = self.selectedDatasetAnalysis.url;
					logger.log("Submitting " + analysisname + " <a href=\""
							+ url + "\">" + self.selectedDatasetAnalysis.name
							+ ":" + fieldname + "</a>", "alert-info");
					// start submitting
					//DINH -- Add your analysis submission code here
					//add an entry to the submitted_jobs table
					//add dataset details to the netcdf_datasets table and netcdf_dataset_fields table
					var webGlobeServer = constants.WEBGLOBE_SERVER;
					
					$.ajax({
                        url: webGlobeServer + 'RunJob',
                        cache: false,
                        type: 'POST',
                        data: JSON.stringify({
                        	datasetid: self.selectedDatasetAnalysis.id,
                        	datasetname: self.selectedDatasetAnalysis.name,
                            url: url,
                            analysisname: analysisname,
                            fieldname: fieldname
                        }),
                        success: function (data) {
                            
                        }
                    }).fail(function (xhr, textStatus, err) {
                        alert(err);
                    });                            

					//finish submitting 
				}

				self.showInfo = function(dataset) {
					if (!self.infoActive) {
						$("#datasetInfo").show().html(dataset.info);
						self.infoActive = true;
					} else {
						$("#datasetInfo").hide();
						self.infoActive = false;
					}
				}
				self.populateDatasets();

			}
			return DatasetsViewModel;
		});
