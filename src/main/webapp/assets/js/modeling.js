(function() {
    'use strict';

    function estimateRunningTime() {
        var moment = window.moment();
        var modelType = $('#modelingType').val();
        var numSplits = ($('#splitType').val() === "RANDOM") ? $('#numSplitsInternalRandom').val() :
                $('#numSplitsInternalSphere').val();
        var numCompounds = $('#num-compounds').val();

        // Time estimates were generated by taking the results from around 200 jobs
        // then removing outliers and plotting a trendline in Excel
        if (modelType === 'RANDOMFOREST') {
            moment.add(numCompounds * 0.003 - 5.718, 'minutes');
        } else if (modelType === 'KNN-GA' || modelType === 'KNN-SA') {
            var activeTab = $('#model-type-section').find('.tab-pane.active');
            var minDesc = activeTab.find('input[name="knnMinNumDescriptors"]').val();
            var maxDesc = activeTab.find('input[name="knnMaxNumDescriptors"]').val();
            var descSteps = activeTab.find('input[name="knnDescriptorStepSize"]').val();
            var numDifferentDescriptors = 1;
            if (descSteps > 0) {
                numDifferentDescriptors += Math.floor((maxDesc - minDesc) / descSteps);
            }
            var baseFactor = numCompounds * numSplits * numDifferentDescriptors;

            if (modelType === 'KNN-GA') {
                moment.add(baseFactor * 0.05, 'minutes');
            } else { // KNN-SA
                // depends on numRuns and needs a factor for convergence parameters (temperature etc).
                var numRuns = $('#saNumRuns').val();
                var numBest = $('#saNumBestModels').val();
                moment.add(baseFactor * numRuns * numBest * 0.018, 'minutes');
            }
        } else if (modelType === 'SVM') {
            var numDifferentCosts = 1 + Math.floor(($('#svmCostTo').val() - $('#svmCostFrom').val()) /
                                                   $('#svmCostStep').val());
            var numDifferentNus = 1 + Math.floor(($('#svmNuTo').val() - $('#svmNuFrom').val()) / $('#svmNuStep').val());
            var numDifferentPEpsilons = 1 + Math.floor(($('#svmPEpsilonTo').val() - $('#svmPEpsilonFrom').val()) /
                                                       $('#svmPEpsilonStep').val());

            var svmId = $('input[name="svmType' + $('#activity-type').val().toProperCase() + '"]:radio:checked').attr(
                    'id');
            var svmType = $('label[for="' + svmId + '"]').text();
            var numModelsPerSplit = 1;
            if (svmType === 'C-SVC') {
                numModelsPerSplit *= numDifferentCosts;
            } else if (svmType === 'nu-SVC') {
                numModelsPerSplit *= numDifferentNus;
            } else if (svmType === 'epsilon-SVR') {
                numModelsPerSplit *= numDifferentPEpsilons;
            } else if (svmType === 'nu-SVR') {
                numModelsPerSplit *= numDifferentCosts * numDifferentNus;
            }

            var numDifferentDegrees = 1 + Math.floor(($('#svmDegreeTo').val() - $('#svmDegreeFrom').val()) /
                                                     $('#svmDegreeStep').val());
            var numDifferentGammas = 1 + Math.floor(($('#svmGammaTo').val() - $('#svmGammaFrom').val()) /
                                                    $('#svmGammaStep').val());
            var kernelType = $('label[for="' + $('input[name="svmKernel"]:checked').attr('id') + '"]').text();
            if (kernelType === 'polynomial') {
                numModelsPerSplit *= numDifferentDegrees * numDifferentGammas;
            } else if (kernelType === 'radial basis function' || kernelType === 'sigmoid') {
                numModelsPerSplit *= numDifferentGammas;
            }
            moment.add(numCompounds * numSplits * numModelsPerSplit * 0.00022, 'minutes');
        }

        $('#time-estimate').html('This job will take about <strong>' + moment.toNow(true) + '</strong>.');
    }

    function updateModelingForm(dataset) {
        showSections();
        $('#uploaded-descriptors-scaled').val(dataset.hasBeenScaled);
        $('#num-compounds').val(dataset.numCompound);
        $('#activity-type').val(dataset.modelType);
        $('input[name="selectedDatasetId"]').val(dataset.id);

        var availableDescriptors = dataset.availableDescriptors.trim().split(/\s+/);
        if (availableDescriptors.contains(Chembench.Constants.UPLOADED)) {
            $('input[name="descriptorGenerationType"][value="' + Chembench.Constants.UPLOADED +
              '"]').parents('label').append('<span id="uploaded-descriptor-type">("' + dataset.uploadedDescriptorType +
                                            '")</span>');
        } else {
            $('#uploaded-descriptor-type').remove();
        }

        // enable only available descriptors in Descriptor Set selection
        $('input[name="descriptorGenerationType"]').prop('disabled', true).removeAttr('checked').parent().addClass(
                'text-muted');
        $.each(availableDescriptors, function(i, descriptor) {
            $('input[name="descriptorGenerationType"][value="' + descriptor + '"]').removeAttr('disabled').parent(
                    'label').removeClass('text-muted');
        });

        if (dataset.numCompound < 40) {
            $('#small-dataset-warning').show();
        } else {
            $('#small-dataset-warning').hide();
        }

        // pre-select the default descriptor set, if available
        var defaultDescriptor = $('input[type="hidden"]#defaultDescriptorGenerationType').val();
        var defaultDescriptorOption = $('input[name="descriptorGenerationType"][value="' + defaultDescriptor + '"]');
        if (!defaultDescriptorOption.prop('disabled')) {
            defaultDescriptorOption.prop('checked', true);
        }

        // enable the correct svm type based on the selected dataset's activity value type
        $('#svm-type-continuous, #svm-type-category').hide();
        if (dataset.modelType === Chembench.Constants.CONTINUOUS) {
            $('#svm-type-continuous').show();
            $('input[name="svmTypeContinuous"]:radio:checked').trigger('change');
        } else if (dataset.modelType === Chembench.Constants.CATEGORY) {
            $('#svm-type-category').show();
            $('input[name="svmTypeCategory"]:radio:checked').trigger('change');
        }

        // select the correct internal split type depending on the number of compounds in the dataset
        if (dataset.numCompound < 300) {
            $('a[href="#sphere-exclusion"]').tab('show');
        } else {
            $('a[href="#random-split"]').tab('show');
        }

        var internalSplitSection = $('#internal-split-type-section').closest('.panel');
        // ... but hide the div containing internal split options if random forest is selected
        // (random forest doesn't use internal splitting)
        if ($('#model-type-section').find('.nav').children('.active').children('a').attr('href') === '#random-forest') {
            internalSplitSection.hide();
        } else {
            internalSplitSection.show();
        }
    }

    function hideSections() {
        $('#dataset-info, form#createModelingJob .panel:not(#dataset-selection-section)').hide();
        $('#dataset-info-help').show();
        $('#view-dataset-detail').addClass('disabled');
        $('#descriptor-types').find('label').addClass('text-muted').find('input').prop('disabled', true).prop('checked',
                false);
    }

    function showSections() {
        $('#dataset-info, form#createModelingJob .panel').show();
        $('#dataset-info-help').hide();
        $('#view-dataset-detail').removeClass('disabled');
    }

    $(document).ready(function() {
        $('.nav-list li').removeClass('active');
        $('#nav-button-modeling').addClass('active');

        $('#descriptor-types').find('label').addClass('text-muted').find('input').prop('disabled', true);
        hideSections();
        $('#degree-settings, #gamma-settings').hide();

        var modelTypeSection = $('#model-type-section');
        var splitTypeSection = $('#internal-split-type-section');
        var activeModelType = $(modelTypeSection.find('.active').children('a').attr('href')).find(
                'input[name="modelingTypeConstant"]').val();
        var activeSplitType = $(splitTypeSection.find('.active').children('a').attr('href')).find(
                'input[name="splitTypeConstant"]').val();
        $('#modelingType').val(activeModelType);
        $('#splitType').val(activeSplitType);

        modelTypeSection.find('a[data-toggle="tab"]').on('shown.bs.tab', function(e) {
            var tab = $($(e.target).attr('href'));
            var tabId = tab.find('input[name="modelingTypeConstant"]').val();
            var splitTypeSection = $('#internal-split-type-section').closest('.panel');
            if (tabId === 'RANDOMFOREST') {
                splitTypeSection.hide();
            } else {
                splitTypeSection.show();
            }
            $('#modelingType').val(tabId);
            estimateRunningTime();
        });

        splitTypeSection.find('a[data-toggle="tab"]').on('shown.bs.tab', function(e) {
            var tab = $($(e.target).attr('href'));
            var tabId = tab.find('input[name="splitTypeConstant"]').val();
            $('#splitType').val(tabId);
        });

        modelTypeSection.add(splitTypeSection).find('input').change(function() {
            estimateRunningTime();
        });

        $('input[name="svmKernel"]').change(function(e) {
            var degreeSettings = $('#degree-settings');
            var gammaSettings = $('#gamma-settings');
            degreeSettings.hide();
            gammaSettings.hide();

            var selectedRadioId = $(e.target).attr('id');
            var selectedKernel = $('label[for="' + selectedRadioId + '"]').text();
            if (selectedKernel === 'polynomial') {
                degreeSettings.show();
                gammaSettings.show();
            } else if (selectedKernel === 'radial basis function' || selectedKernel === 'sigmoid') {
                gammaSettings.show();
            }
        });

        $('input[name="svmKernel"]:radio:checked').trigger('change');

        $('input[name="svmTypeCategory"], input[name="svmTypeContinuous"]').change(function(e) {
            var costSettings = $('#cost-settings');
            var nuSettings = $('#nu-settings');
            var epsilonSettings = $('#epsilon-settings');
            var weightSettings = $('#csvm-weight-settings');
            costSettings.hide();
            nuSettings.hide();
            epsilonSettings.hide();
            weightSettings.hide();

            var selectedRadioId = $(e.target).attr('id');
            var selectedSvm = $('label[for="' + selectedRadioId + '"]').text();
            if (selectedSvm === 'C-SVC') {
                costSettings.show();
                weightSettings.show();
            } else if (selectedSvm === 'nu-SVC') {
                nuSettings.show();
            } else if (selectedSvm === 'epsilon-SVR') {
                epsilonSettings.show();
            } else if (selectedSvm === 'nu-SVR') {
                costSettings.show();
                nuSettings.show();
            }
        });

        $('.advanced-settings-toggle').click(function(e) {
            e.preventDefault();
            var container = $(this).parents('.advanced-settings-group');
            container.find('.glyphicon').toggleClass('glyphicon-chevron-up glyphicon-chevron-down');
            container.find('.advanced-settings').slideToggle();
        });

        $('input[name="descriptorGenerationType"]').change(function() {
            var scalingTypes = $('#scaling-types');
            var infoBox = $('#already-scaled-info');
            if ($(this).val() === Chembench.Constants.UPLOADED &&
                $('input[name="uploaded-descriptors-scaled"]').val() === 'true') {
                scalingTypes.addClass('text-muted').find('input').prop('disabled', true);
                infoBox.show();
                scalingTypes.find('[value="NOSCALING"]').prop('checked', true);
            } else {
                scalingTypes.removeClass('text-muted').find('input').prop('disabled', false);
                infoBox.hide();
                var defaultScalingType = $('input[type="hidden"]#defaultScalingType').val();
                $('input[name="scalingType"][value="' + defaultScalingType + '"]').prop('checked', true);
            }
        });

        var table = $('#dataset-selection-section').find('table.datatable');
        table.DataTable().on('init', function() {
            table.find('input[type="radio"]').change(function() {
                var rowSelector = $(this).closest('tr');
                var dataset = rowSelector.closest('table').DataTable().row(rowSelector).data();
                updateModelingForm(dataset);
            });
        }).on('draw', function() {
            // XXX don't use find('tbody').find('tr'), or non-active pages won't be modified
            table.DataTable().rows().nodes().to$().click(function() {
                $(this).find('input[type="radio"]').prop('checked', 'checked').change();
            });

            table.find('input[type="radio"]').change(function() {
                var radio = $(this);
                var radioRow = radio.closest('tr');

                table.DataTable().rows().every(function() {
                    var row = $(this.node());
                    row.removeClass('info');
                    if (row.data('oldClass')) {
                        row.addClass(row.data('oldClass'));
                    }
                    if (!row.is(radioRow)) {
                        row.find('input[type="radio"]').prop('checked', false);
                    }
                });

                var match = /(danger|warning|success)/.exec(radioRow.attr('class'));
                if (match !== null) {
                    var color = match[1];
                    radioRow.data('oldClass', color);
                    radioRow.removeClass(color);
                }
                radioRow.addClass('info');
            });
        });
    });
})();
