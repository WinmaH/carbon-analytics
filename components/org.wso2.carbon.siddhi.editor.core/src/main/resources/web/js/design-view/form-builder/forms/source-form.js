/**
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

define(['log', 'jquery', 'lodash', 'sourceOrSinkAnnotation', 'mapAnnotation', 'payloadOrAttribute',
    'jsonValidator', 'handlebar', 'designViewUtils', 'constants'],
    function (log, $, _, SourceOrSinkAnnotation, MapAnnotation, PayloadOrAttribute, JSONValidator, Handlebars,
        DesignViewUtils, Constants) {

        /**
         * @class SourceForm Creates a forms to collect data from a source
         * @constructor
         * @param {Object} options Rendering options for the view
         */
        var SourceForm = function (options) {
            if (options !== undefined) {
                this.configurationData = options.configurationData;
                this.application = options.application;
                this.formUtils = options.formUtils;
                this.consoleListManager = options.application.outputController;
                var currentTabId = this.application.tabController.activeTab.cid;
                this.designViewContainer = $('#design-container-' + currentTabId);
                this.toggleViewButton = $('#toggle-view-button-' + currentTabId);
            }
        };

        /**
         * Function to get the options of the selected source/map type
         * @param {String} selectedType Selected source/map type
         * @param {object} types Predefined source/map types
         * @return {object} options
         */
        var getSelectedTypeOptions = function (selectedType, types) {
            var options = [];
            for (type of types) {
                if (type.name.toLowerCase() == selectedType.toLowerCase()) {
                    if (type.parameters) {
                        options = type.parameters;
                    }
                    break;

                }
            }
            return options;
        };

        /**
         * Function to render the options for the selected map/source type using handlebars
         * @param {Object} optionsArray Saved options
         * @param {Object} customizedMapperOptions Options typed by the user which aren't one of the predefined option
         * @param {String} id Id for the div to embed the options
         */
        var renderOptions = function (optionsArray, customizedOptions, id) {
            optionsArray.sort(function (val1, val2) {
                if (val1.optional && !val2.optional) return 1;
                else if (!val1.optional && val2.optional) return -1;
                else return 0;
            });
            var optionsTemplate = Handlebars.compile($('#source-sink-store-options-template').html());
            var wrappedHtml = optionsTemplate({
                id: id,
                options: optionsArray,
                customizedOptions: customizedOptions
            });
            $('#' + id + '-options-div').html(wrappedHtml);
            changeCustOptDiv();
        };

        /**
         * Function to render the select options for the map type using handlebars
         * @param {Object} predefinedSourceMaps Predefined map annotations
         */
        var renderMap = function (predefinedSourceMaps) {
            if (!$.trim($('#define-map').html()).length) {
                var mapFormTemplate = Handlebars.compile($('#type-selection-form-template').html());
                var wrappedHtml = mapFormTemplate({ id: "map", types: predefinedSourceMaps });
                $('#define-map').html(wrappedHtml);
                $('#define-map #map-type').val('passThrough');
                $('#define-map #map-type option:contains("' + Constants.DEFAULT_MAPPER_TYPE + '")').text('passThrough (default)');
            }
        };

        /**
         * Function to map the option values from the source view to the option object
         * @param {Object} predefinedOptions Predefined options of a particular source/map annotation type
         * @param {Object} savedOptions Saved options
         * @return {Object} options
         */
        var mapUserOptionValues = function (predefinedOptions, savedOptions) {
            var options = [];
            _.forEach(predefinedOptions, function (predefinedOption) {
                var foundPredefinedOption = false;
                for (var savedOption of savedOptions) {
                    var optionKey = savedOption.split('=')[0].trim();
                    var optionValue = savedOption.split('=')[1].trim();
                    optionValue = optionValue.substring(1, optionValue.length - 1);
                    if (optionKey.toLowerCase() == predefinedOption.name.toLowerCase()) {
                        foundPredefinedOption = true;
                        options.push({
                            key: predefinedOption.name, value: optionValue, description: predefinedOption
                                .description, optional: predefinedOption.optional,
                            defaultValue: predefinedOption.defaultValue
                        });
                        break;
                    }
                }
                if (!foundPredefinedOption) {
                    options.push({
                        key: predefinedOption.name, value: "", description: predefinedOption
                            .description, optional: predefinedOption.optional, defaultValue: predefinedOption.defaultValue
                    });
                }
            });
            return options;
        };

        /**
         * Function to render the html to display the select options for attribute mapping
         */
        var renderAttributeMapping = function () {
            if (!$.trim($('#define-attribute').html()).length) {
                var attributeDiv = $('<div class="clearfix"> <label id="attribute-map-label">' +
                    '<input type="checkbox" id="attributeMap-checkBox"> Map Attribute As Key/Value Pairs ' +
                    '</label> </div>');
                $('#define-attribute').html(attributeDiv);
            }
        };

        /**
         * Function to obtain the customized option entered by the user in the source view
         * @param {Object} predefinedOptions Predefined options of a particular source/map annotation type
         * @param {Object} savedOptions Options defined by the user in the source view
         * @return {Object} customizedOptions
         */
        var getCustomizedOptions = function (predefinedOptions, savedOptions) {
            var customizedOptions = [];
            _.forEach(savedOptions, function (savedOption) {
                var foundSavedOption = false;
                var optionKey = savedOption.split('=')[0];
                var optionValue = savedOption.split('=')[1].trim();
                optionValue = optionValue.substring(1, optionValue.length - 1);
                for (var predefinedOption of predefinedOptions) {
                    if (predefinedOption.name.toLowerCase() == optionKey.toLowerCase().trim()) {
                        foundSavedOption = true;
                        break;
                    }
                }
                if (!foundSavedOption) {
                    customizedOptions.push({ key: optionKey, value: optionValue });
                }
            });
            return customizedOptions;
        };

        /**
         * Function to create option object with an additional empty value attribute
         * @param {Object} optionArray Predefined options without the attribute 'value'
         * @return {Object} options
         */
        var createOptionObjectWithValues = function (optionArray) {
            var options = [];
            _.forEach(optionArray, function (option) {
                options.push({
                    key: option.name, value: "", description: option.description, optional: option.optional,
                    defaultValue: option.defaultValue
                });
            });
            return options;
        };

        /**
         * Function to create attribute-map object
         * @param {Object} savedMapperAttributes Saved attribute-map
         * @param {Objects} streamAttributes Attributes of the connected stream
         * @return {Object} attributes
         */
        var createAttributeObjectList = function (savedMapperAttributes, streamAttributes) {
            var attributeType;
            var attributes = [];
            if (!savedMapperAttributes) {
                attributeType = "none";
            } else {
                attributeType = savedMapperAttributes.getType().toLowerCase();
                var attributeValues = savedMapperAttributes.getValue();
            }
            if (attributeType === Constants.LIST) {
                for (streamAttribute of streamAttributes) {
                    attributes.push({ key: streamAttribute.key, value: "" });
                }
                var i = 0;
                for (var attribute in attributeValues) {
                    if (i < streamAttributes.length) {
                        attributes[i].value = attributeValues[attribute];
                        i++;
                    }
                }
            } else if (attributeType === Constants.MAP) {
                for (streamAttribute of streamAttributes) {
                    attributes.push({ key: streamAttribute.key, value: "" });
                }
                for (var mappedAttribute of attributes) {
                    for (var attribute in attributeValues) {
                        if (mappedAttribute.key === attribute) {
                            mappedAttribute.value = attributeValues[attribute]
                            break;
                        }
                    }
                }
            } else {
                for (var streamAttribute of streamAttributes) {
                    attributes.push({ key: streamAttribute.key, value: "" });
                }
            }
            return attributes;
        };

        /**
         * Function to add the error class
         * @param {Object} id object where the errors needs to be displayed
         */
        var addErrorClass = function (id) {
            $(id)[0].scrollIntoView();
            $(id).addClass('required-input-field')
        };

        /**
         * Function to render the attribute-map div using handlebars
         * @param {Object} attributes which needs to be mapped on to the template
         */
        var renderAttributeMappingContent = function (attributes) {
            var attributeMapFormTemplate = Handlebars.compile($('#source-sink-map-attribute-template').html());
            var wrappedHtml = attributeMapFormTemplate({ id: Constants.SOURCE, attributes: attributes });
            $('#attribute-map-content').html(wrappedHtml);
        };

        /**
         * Function to obtain the connected stream's attributes
         * @param {Object} streamList List of all stream objects
         * @param {String} connectedElement source's connected element's name
         * @return {Object} streamAttributes
         */
        var getConnectStreamAttributes = function (streamList, connectedElement) {
            var streamAttributes = [];
            for (var stream of streamList) {
                if (stream.name == connectedElement) {
                    var attributeList = stream.getAttributeList();
                    _.forEach(attributeList, function (attribute) {
                        streamAttributes.push({ key: attribute.getName(), value: "" });
                    })
                    break;
                }
            }
            return streamAttributes;
        };

        /**
         * Function to validate the customized options
         * @param {Object} selectedOptions options which needs to be saved
         * @param {String} id to identify the div in the html to traverse
         * @return {boolean} isError
         */
        var validateCustomizedOptions = function (id) {
            var isError = false;
            if ($('#customized-' + id + ' ul').has('li').length != 0) {
                $('#customized-' + id + ' .option').each(function () {
                    var custOptName = $(this).find('.cust-option-key').val().trim();
                    var custOptValue = $(this).find('.cust-option-value').val().trim();
                    if ((custOptName != "") || (custOptValue != "")) {
                        if (custOptName == "") {
                            $(this).find('.error-message').text('Option key is required.');
                            addErrorClass($(this).find('.cust-option-key'))
                            isError = true;
                            return false;
                        } else if (custOptValue == "") {
                            $(this).find('.error-message').text('Option value is required.');
                            addErrorClass($(this).find('.cust-option-value'));
                            isError = true;
                            return false;
                        }
                    }
                });
            }
            return isError;
        };

        /**
         * Function to obtain a particular option from predefined option
         * @param {String} optionName option which needs to be found
         * @param {Object} predefinedOptions set of predefined option
         * @return {Object} option
         */
        var getOption = function (optionName, predefinedOptions) {
            var option = null;
            for (var predefinedOption of predefinedOptions) {
                if (predefinedOption.name.toLowerCase() == optionName.toLowerCase()) {
                    option = predefinedOption;
                    break;
                }
            }
            return option;
        };

        /**
         * Function to validate the data type of the options
         * @param {String} dataType data-type of the option
         * @param {String} optionValue value of the option
         * @return {boolean} invalidDataType
         */
        var validateDataType = function (dataType, optionValue) {
            var invalidDataType = false;
            intLongRegexMatch = /^[-+]?\d+$/;
            doubleFloatRegexMatch = /^[+-]?([0-9]*[.])?[0-9]+$/;

            if (dataType === "INT" || dataType === "LONG") {
                if (!optionValue.match(intLongRegexMatch)) {
                    invalidDataType = true;
                }
            } else if (dataType === "DOUBLE" || dataType === "FLOAT") {
                if (!optionValue.match(doubleFloatRegexMatch)) {
                    invalidDataType = true;
                }
            } else if (dataType === "BOOL") {
                if (!(optionValue.toLowerCase() === "false" || optionValue.toLowerCase() === "true")) {
                    invalidDataType = true;
                }
            }
            return invalidDataType;
        };


        /** Function to change the heading and the button text of the customized options div */
        var changeCustOptDiv = function () {
            var sourceCustOptionList = $('.source-sink-map-options #customized-source-options').
                find('.cust-options li');
            var sourceDivParent = $('.source-sink-map-options #customized-source-options');
            if (sourceCustOptionList.length > 0) {
                sourceDivParent.find('h3').show();
                sourceDivParent.find('.btn-add-options').html('Add more');
            } else {
                sourceDivParent.find('h3').hide();
                sourceDivParent.find('.btn-add-options').html('Add customized option');
            }
            var mapperCustOptionList = $('.source-sink-map-options #customized-mapper-options').
                find('.cust-options li');
            var mapperDivParent = $('.source-sink-map-options #customized-mapper-options');
            if (mapperCustOptionList.length > 0) {
                mapperDivParent.find('h3').show();
                mapperDivParent.find('.btn-add-options').html('Add more');
            } else {
                mapperDivParent.find('h3').hide();
                mapperDivParent.find('.btn-add-options').html('Add customized option');
            }
        };

        /**
         * Function to validate the predefined options
         * @param {Object} predefinedOptions
         * @param {String} id to identify the div in the html to traverse
         * @return {boolean} isError
         */
        var validateOptions = function (predefinedOptions, id) {
            var isError = false;
            $('.source-sink-map-options #' + id + ' .option').each(function () {
                var optionName = $(this).find('.option-name').text().trim();
                var optionValue = $(this).find('.option-value').val().trim();
                var predefinedOptionObject = getOption(optionName, predefinedOptions);
                if (!predefinedOptionObject.optional) {
                    if (!checkOptionValue(optionValue, predefinedOptionObject, this)) {
                        isError = true;
                        return false;
                    }
                } else {
                    if ($(this).find('.option-checkbox').is(":checked")) {
                        if (!checkOptionValue(optionValue, predefinedOptionObject, this)) {
                            isError = true;
                            return false;
                        }
                    }
                }
            });
            return isError;
        };

        /**
         * Function to check if a particular option value is valid
         * @param {String} optionValue value which needs to be validated
         * @param {Object} predefinedOptionObject predefined object of the option
         * @param {Object} parent div of the particular option
         */
        var checkOptionValue = function (optionValue, predefinedOptionObject, parent) {
            if (optionValue == "") {
                $(parent).find('.error-message').text('Option value is required.');
                addErrorClass($(parent).find('.option-value'));
                return false;
            } else {
                var dataType = predefinedOptionObject.type[0];
                if (validateDataType(dataType, optionValue)) {
                    $(parent).find('.error-message').text('Invalid data-type. ' + dataType + ' required.');
                    addErrorClass($(parent).find('.option-value'));
                    return false;
                }
            }
            return true;
        };

        /**
         * Function to build the options
         * @param {Object} predefinedOptions predefined options
         * @param {Object} selectedOptions array to add the built option
         * @param {String} id div of the options which needs to be built [mapper or source]
         */
        var buildOptions = function (predefinedOptions, selectedOptions, id) {
            var option;
            $('.source-sink-map-options #' + id + ' .option').each(function () {
                var optionName = $(this).find('.option-name').text().trim();
                var optionValue = $(this).find('.option-value').val().trim();
                var predefinedOptionObject = getOption(optionName, predefinedOptions);
                if (!predefinedOptionObject.optional) {
                    option = optionName + " = \"" + optionValue + "\"";
                    selectedOptions.push(option);
                } else {
                    if ($(this).find('.option-checkbox').is(":checked")) {
                        option = optionName + " = \"" + optionValue + "\"";
                        selectedOptions.push(option);
                    }
                }
            });
        };

        /**
         * Function to build the customized options
         * @param {Object} selectedOptions array to add the built option
         * @param {String} id div of the options which needs to be built [mapper or source]
         */
        var buildCustomizedOption = function (selectedOptions, id) {
            var option = "";
            if ($('#customized-' + id + ' ul').has('li').length != 0) {
                $('#customized-' + id + ' .option').each(function () {
                    var custOptName = $(this).find('.cust-option-key').val().trim();
                    var custOptValue = $(this).find('.cust-option-value').val().trim();
                    if ((custOptName != "") && (custOptValue != "")) {
                        option = custOptName + " = \"" + custOptValue + "\"";
                        selectedOptions.push(option);
                    }
                });
            }
        };

        /**
         * @function generate properties form for a source
         * @param element selected element(source)
         * @param formConsole Console which holds the form
         * @param formContainer Container which holds the form
         */
        SourceForm.prototype.generatePropertiesForm = function (element, formConsole, formContainer) {
            var self = this;
            var id = $(element).parent().attr('id');
            var clickedElement = self.configurationData.getSiddhiAppConfig().getSource(id);

            var isSourceConnected = true;
            if ($('#' + id).hasClass('error-element')) {
                isSourceConnected = false;
                DesignViewUtils.prototype.errorAlert("Please connect to a stream");
            } else if (!JSONValidator.prototype.validateSourceOrSinkAnnotation(clickedElement, Constants.SOURCE, true)) {
                // perform JSON validation to check if source contains a connectedElement.
                isSourceConnected = false;
            }
            if (!isSourceConnected) {
                // close the form window
                self.consoleListManager.removeFormConsole(formConsole);
                self.designViewContainer.removeClass('disableContainer');
                self.toggleViewButton.removeClass('disableContainer');
            } else {
                $('#' + id).addClass('selected-element');
                $(".overlayed-container").fadeTo(200, 1);
                var streamList = self.configurationData.getSiddhiAppConfig().getStreamList();
                var connectedElement = clickedElement.connectedElementName;
                var predefinedSources = _.orderBy(this.configurationData.rawExtensions["source"], ['name'], ['asc']);
                var predefinedSourceMaps = _.orderBy(this.configurationData.rawExtensions["sourceMaps"], ['name'], ['asc']);
                var streamAttributes = getConnectStreamAttributes(streamList, connectedElement);

                var propertyDiv = $('<div class="source-sink-form-container source-div"><div id="define-source"></div>' +
                    '<div class = "source-sink-map-options" id="source-options-div"></div>' +
                    '<button type="submit" id ="btn-submit" class="btn toggle-view-button"> Submit </button>' +
                    '<button id="btn-cancel" type="button" class="btn btn-default"> Cancel </button> </div>' +
                    '<div class="source-sink-form-container mapper-div"> <div id="define-map"> </div> ' +
                    '<div class="source-sink-map-options" id="mapper-options-div">' +
                    '</div> </div> <div class= "source-sink-form-container attribute-map-div">' +
                    '<div id="define-attribute"> </div> <div id="attribute-map-content"></div> </div>');
                formContainer.append(propertyDiv);
                self.designViewContainer.addClass('disableContainer');
                self.toggleViewButton.addClass('disableContainer');

                //declaration of variables
                var sourceOptions = [];
                var sourceOptionsWithValues = [];
                var customizedSourceOptions = [];
                var mapperOptions = [];
                var mapperOptionsWithValues = [];
                var customizedMapperOptions = [];
                var attributes = [];

                // event listener to show option description
                $('.source-sink-map-options').on('mouseover', '.option-desc', function () {
                    $(this).find('.option-desc-content').show();
                });

                //event listener to hide option description
                $('.source-sink-map-options').on('mouseout', '.option-desc', function () {
                    $(this).find('.option-desc-content').hide();
                });

                //event listener when the option checkbox is changed
                $('.source-sink-map-options').on('change', '.option-checkbox', function () {
                    if ($(this).is(':checked')) {
                        $(this).parents(".option").find(".option-value").show();
                    } else {
                        $(this).parents(".option").find(".option-value").hide();
                        $(this).parents(".option").find(".option-value").removeClass("required-input-field");
                        $(this).parents(".option").find(".error-message").text("");
                    }
                });

                var customizedOptDiv = '<li class="option">' +
                    '<div class = "clearfix"> <label>option.key</label> <input type="text" class="cust-option-key"' +
                    'value=""> </div> <div class="clearfix"> <label>option.value</label> ' +
                    '<input type="text" class="cust-option-value" value="">' +
                    '<a class = "btn-del btn-del-option"><i class="fw fw-delete"></i></a></div>' +
                    '<label class = "error-message"></label></li>';

                //onclick to add customized source option
                $('#source-options-div').on('click', '#btn-add-source-options', function () {
                    $('#customized-source-options .cust-options').append(customizedOptDiv);
                    changeCustOptDiv();
                });

                //onclick to add customized mapper option
                $('#mapper-options-div').on('click', '#btn-add-mapper-options', function () {
                    $('#customized-mapper-options .cust-options').append(customizedOptDiv);
                    changeCustOptDiv();
                });

                //onclick to delete customized option
                $('.source-sink-form-container').on('click', '.btn-del-option', function () {
                    $(this).closest('li').remove();
                    changeCustOptDiv();
                });

                //event listener for attribute-map checkbox
                $('#define-attribute').on('change', '#attributeMap-checkBox', function () {
                    if ($(this).is(':checked')) {
                        var attributes = createAttributeObjectList(savedMapperAttributes, streamAttributes);
                        $('#attribute-map-content').show();
                        renderAttributeMappingContent(attributes)
                    } else {
                        $('#attribute-map-content').hide();
                    }
                });

                //get the clicked element's information
                var type = clickedElement.getType();
                var savedSourceOptions = clickedElement.getOptions();
                var map = clickedElement.getMap();

                //render the template to select the source type
                var sourceFormTemplate = Handlebars.compile($('#type-selection-form-template').html());
                var wrappedHtml = sourceFormTemplate({ id: Constants.SOURCE, types: predefinedSources });
                $('#define-source').html(wrappedHtml);

                //onchange of the source-type selection
                $('#source-type').change(function () {
                    sourceOptions = getSelectedTypeOptions(this.value, predefinedSources);
                    if (type && (type.toLowerCase() == this.value.toLowerCase()) && savedSourceOptions) {
                        //if the selected type is same as the saved source-type
                        sourceOptionsWithValues = mapUserOptionValues(sourceOptions, savedSourceOptions);
                        customizedSourceOptions = getCustomizedOptions(sourceOptions, savedSourceOptions);
                    } else {
                        sourceOptionsWithValues = createOptionObjectWithValues(sourceOptions);
                        customizedSourceOptions = [];
                    }
                    renderOptions(sourceOptionsWithValues, customizedSourceOptions, Constants.SOURCE);
                    if (!map) {
                        renderMap(predefinedSourceMaps);
                        customizedMapperOptions = [];
                        mapperOptions = getSelectedTypeOptions(Constants.DEFAULT_MAPPER_TYPE, predefinedSourceMaps);
                        mapperOptionsWithValues = createOptionObjectWithValues(mapperOptions);
                        renderOptions(mapperOptionsWithValues, customizedMapperOptions, Constants.MAPPER)
                        renderAttributeMapping();
                    }
                });

                if (type) {
                    //if source object is already edited
                    $('#define-source').find('#source-type option').filter(function () {
                        return ($(this).val().toLowerCase() == (type.toLowerCase()));
                    }).prop('selected', true);
                    sourceOptions = getSelectedTypeOptions(type, predefinedSources);
                    if (savedSourceOptions) {
                        //get the savedSourceoptions values and map it
                        sourceOptionsWithValues = mapUserOptionValues(sourceOptions, savedSourceOptions);
                        customizedSourceOptions = getCustomizedOptions(sourceOptions, savedSourceOptions);
                    } else {
                        //create option object with empty values
                        sourceOptionsWithValues = createOptionObjectWithValues(sourceOptions);
                        customizedSourceOptions = [];
                    }
                    renderOptions(sourceOptionsWithValues, customizedSourceOptions, Constants.SOURCE);
                    if (!map) {
                        renderMap(predefinedSourceMaps);
                        customizedMapperOptions = [];
                        mapperOptions = getSelectedTypeOptions(Constants.DEFAULT_MAPPER_TYPE, predefinedSourceMaps);
                        mapperOptionsWithValues = createOptionObjectWithValues(mapperOptions);
                        renderOptions(mapperOptionsWithValues, customizedMapperOptions, Constants.MAPPER)
                        renderAttributeMapping();
                    }
                }

                //if map is defined
                if (map) {
                    renderMap(predefinedSourceMaps);
                    renderAttributeMapping();
                    var mapperType = map.getType();
                    var savedMapperOptions = map.getOptions();
                    var savedMapperAttributes = map.getPayloadOrAttribute();

                    if (mapperType) {
                        $('#define-map').find('#map-type option').filter(function () {
                            return ($(this).val().toLowerCase() == (mapperType.toLowerCase()));
                        }).prop('selected', true);
                        mapperOptions = getSelectedTypeOptions(mapperType, predefinedSourceMaps);
                        if (savedMapperOptions) {
                            //get the savedMapoptions values and map it
                            mapperOptionsWithValues = mapUserOptionValues(mapperOptions, savedMapperOptions);
                            customizedMapperOptions = getCustomizedOptions(mapperOptions, savedMapperOptions);
                        } else {
                            //create option object with empty values
                            mapperOptionsWithValues = createOptionObjectWithValues(mapperOptions);
                            customizedMapperOptions = [];
                        }
                        renderOptions(mapperOptionsWithValues, customizedMapperOptions, Constants.MAPPER);
                    }
                    if (savedMapperAttributes) {
                        $('#define-attribute #attributeMap-checkBox').prop('checked', true);
                        attributes = createAttributeObjectList(savedMapperAttributes, streamAttributes);
                        renderAttributeMappingContent(attributes);
                    }
                }

                //onchange of map type selection
                $('#define-map').on('change', '#map-type', function () {

                    mapperOptions = getSelectedTypeOptions(this.value, predefinedSourceMaps);
                    if ((map) && (mapperType) && (mapperType.toLowerCase() == this
                        .value.toLowerCase()) && savedMapperOptions) {
                        //if the selected type is same as the saved map type
                        mapperOptionsWithValues = mapUserOptionValues(mapperOptions, savedMapperOptions);
                        customizedMapperOptions = getCustomizedOptions(mapperOptions, savedMapperOptions);
                    } else {
                        mapperOptionsWithValues = createOptionObjectWithValues(mapperOptions);
                        customizedMapperOptions = [];
                    }
                    renderOptions(mapperOptionsWithValues, customizedMapperOptions, Constants.MAPPER)
                    if (!map || (map && !savedMapperAttributes)) {
                        //if saved mapper attributes are undefined
                        renderAttributeMapping();
                    } else if (map && savedMapperAttributes) {
                        //if saved mapper attributes are defined
                        renderAttributeMapping();
                        $('#define-attribute #attributeMap-checkBox').prop('checked', true);
                        attributes = createAttributeObjectList(savedMapperAttributes, streamAttributes);
                        renderAttributeMappingContent(attributes);
                    }
                });

                //onclick of submit
                var submitButtonElement = $(formContainer).find('#btn-submit')[0];
                submitButtonElement.addEventListener('click', function () {

                    //clear the errors
                    $('.error-message').text("")
                    $('.required-input-field').removeClass('required-input-field');
                    var isErrorOccurred = false;

                    var selectedSourceType = $('#define-source #source-type').val();
                    if (selectedSourceType === null) {
                        DesignViewUtils.prototype.errorAlert("Select a source type to submit.");
                        isErrorOccurred = true;
                        return;
                    } else {
                        if (validateOptions(sourceOptions, "source-options")) {
                            isErrorOccurred = true;
                            return;
                        }
                        if (validateCustomizedOptions("source-options")) {
                            isErrorOccurred = true;
                            return;
                        }
                        var selectedMapType = $('#define-map #map-type').val();
                        var mapperAnnotationOptions = [];
                        if (validateOptions(mapperOptions, "mapper-options")) {
                            isErrorOccurred = true;
                            return;
                        }
                        if (validateCustomizedOptions("mapper-options")) {
                            isErrorOccurred = true;
                            return;
                        }

                        if ($('#define-attribute #attributeMap-checkBox').is(":checked")) {
                            //if attribute section is checked
                            var mapperAttributeValuesArray = {};
                            $('#mapper-attributes .attribute').each(function () {
                                //validate mapper  attributes if value is not filled
                                var key = $(this).find('.attr-key').val().trim();
                                var value = $(this).find('.attr-value').val().trim();
                                if (value == "") {
                                    $(this).find('.error-message').text('Attribute Value is required.');
                                    addErrorClass($(this).find('.attr-value'));
                                    isErrorOccurred = true;
                                    return false;
                                } else {
                                    mapperAttributeValuesArray[key] = value;
                                }
                            });
                        }
                    }

                    if (!isErrorOccurred) {
                        clickedElement.setType(selectedSourceType);
                        var textNode = $('#' + id).find('.sourceNameNode');
                        textNode.html(selectedSourceType);

                        var annotationOptions = [];
                        buildOptions(sourceOptions, annotationOptions, "source-options");
                        buildCustomizedOption(annotationOptions, "source-options");
                        if (annotationOptions.length == 0) {
                            clickedElement.setOptions(undefined);
                        } else {
                            clickedElement.setOptions(annotationOptions);
                        }

                        var mapper = {};
                        var mapperAnnotationOptions = [];
                        buildOptions(mapperOptions, mapperAnnotationOptions, "mapper-options");
                        buildCustomizedOption(mapperAnnotationOptions, "mapper-options");
                        _.set(mapper, 'type', selectedMapType);
                        if (mapperAnnotationOptions.length == 0) {
                            _.set(mapper, 'options', undefined);
                        } else {
                            _.set(mapper, 'options', mapperAnnotationOptions);
                        }

                        if ($('#define-attribute #attributeMap-checkBox').is(":checked")) {
                            payloadOrAttributeOptions = {};
                            _.set(payloadOrAttributeOptions, 'annotationType', 'ATTRIBUTES');
                            _.set(payloadOrAttributeOptions, 'type', Constants.MAP);
                            _.set(payloadOrAttributeOptions, 'value', mapperAttributeValuesArray);
                            var payloadOrAttributeObject = new PayloadOrAttribute(payloadOrAttributeOptions);
                            _.set(mapper, 'payloadOrAttribute', payloadOrAttributeObject);
                        } else {
                            _.set(mapper, 'payloadOrAttribute', undefined);
                        }

                        var mapperObject = new MapAnnotation(mapper);
                        clickedElement.setMap(mapperObject);

                        $('#' + id).removeClass('incomplete-element');
                        //Send source element to the backend and generate tooltip
                        var sourceToolTip = self.formUtils.getTooltip(clickedElement, Constants.SOURCE);
                        $('#' + id).prop('title', sourceToolTip);

                        // set the isDesignViewContentChanged to true
                        self.configurationData.setIsDesignViewContentChanged(true);

                        self.designViewContainer.removeClass('disableContainer');
                        self.toggleViewButton.removeClass('disableContainer');

                        // close the form window
                        self.consoleListManager.removeFormConsole(formConsole);
                    }
                });

                // 'Cancel' button action
                var cancelButtonElement = $(formContainer).find('#btn-cancel')[0];
                cancelButtonElement.addEventListener('click', function () {
                    self.designViewContainer.removeClass('disableContainer');
                    self.toggleViewButton.removeClass('disableContainer');
                    // close the form window
                    self.consoleListManager.removeFormConsole(formConsole);
                });
            }
        };
        return SourceForm;
    });
