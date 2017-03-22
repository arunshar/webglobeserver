/* 
 * Copyright (c) 2016 Bruce Schubert <bruce@emxsys.com>.
 * Released under the MIT License
 * http://www.opensource.org/licenses/mit-license.php
 */

/**
 * ServerTemplate content module
 *
 * @param {type} ko
 * @param {type} $
 * @returns {ServerTemplateModel}
 */
define(['knockout',
        'jquery',
        'model/Constants'],
    function (ko, $, constants) {

        /**
         * The view model for the Output panel.
         * @constructor
         */
        function ServerTemplateModel(globe) {
            var self = this;
            var wwd = globe.wwd;

        
            self.onRemoveServer = function() {
                        return true;

            };            
        }
        return ServerTemplateModel;
    }
);



