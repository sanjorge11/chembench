$(document).ready(function() {
    $(".nav-list li").removeClass("active");
    $("#nav-button-datasets").addClass("active");

    var datasetTypeInput = $("input#datasetType");
    datasetTypeInput.val($(".tab-pane:first-child").find('input[name="dataset-type"]').val());
    var splitTypeInput = $("input#splitType");
    splitTypeInput.val($(".tab-pane:first-child").find('input[name="split-type"]').val());

    // add red background for unfilled file upload fields
    var requiredFileFields = $('input[type="file"]').not(".optional-sdf-select");
    requiredFileFields.each(function() {
        if (!$(this).val()) {
            $(this).parent("div").addClass("bg-danger");
        }
    });

    // change background for file upload fields when file is selected/deselected
    requiredFileFields.change(function() {
        var parent = $(this).parent("div");
        if ($(this).val()) {
            parent.removeClass("bg-danger");
            parent.addClass("bg-success");
        } else {
            parent.removeClass("bg-success");
            parent.addClass("bg-danger");
        }
    });

    // for optional sdf fields, only reveal additional options when an sdf is chosen
    $('input[type="file"].optional-sdf-select').change(function() {
        var container = $(this).parents(".optional-sdf");
        if ($(this).val()) {
            container.removeClass("text-muted").siblings(".optional-sdf-options").show();
        } else {
            container.addClass("text-muted").siblings(".optional-sdf-options").hide();
        }
    });

    $('.descriptor-type input[type="radio"]').click(function() {
        var parent = $(this).parents(".descriptor-type");
        var otherParent = $(parent).siblings(".descriptor-type");

        // disable the other descriptor type
        otherParent.addClass("text-muted");
        otherParent.find('input[type="text"], select').prop("disabled", true);

        // enable this type
        parent.removeClass("text-muted");
        parent.find('input[type="text"], select').prop("disabled", false);
    });

    $('#dataset-type-selection a[data-toggle="tab"]').on("shown.bs.tab", function(e) {
        console.log(e);
        var tabName = e.currentTarget.innerText;
        if (tabName.toLowerCase().indexOf("modeling dataset") >= 0) {
            $("#external-set-settings").show();
        } else if (tabName.toLowerCase().indexOf("prediction dataset") >= 0) {
            $("#external-set-settings").hide();
        }

        var tab = $(e.currentTarget.hash);
        datasetTypeInput.val(tab.find('input[name="dataset-type"]').val());
    });

    $('#external-set-settings a[data-toggle="tab"]').on("shown.bs.tab", function(e) {
        var tab = $(e.currentTarget.hash);
        splitTypeInput.val(tab.find('input[name="split-type"]').val());
    });
});