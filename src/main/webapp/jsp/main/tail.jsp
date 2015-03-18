<script>
  // Chembench-specific JS globals
  var Chembench = {};
  Chembench.MODI_MODELABLE = <s:property value="@edu.unc.ceccr.chembench.global.Constants@MODI_MODELABLE" />;
  Chembench.DATATABLE_OPTIONS = {
    "columnDefs": [{
                     orderable: false,
                     targets: "unsortable"
                   }],
    "paging": false,
    "dom": "lifrtp",
    "infoCallback": function(_, _, _, max, total, _) {
      if (max !== total) {
        return "Showing " + total + " entries (filtered from " + max + " total entries)";
      }
      return "Showing " + max + " entries";
    }
  };
</script>

<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
<script src="https://cdn.datatables.net/1.10.5/js/jquery.dataTables.min.js"></script>
<script src="https://cdn.datatables.net/plug-ins/f2c75b7247b/integration/bootstrap/3/dataTables.bootstrap.js"></script>
<script src="/assets/js/bootstrap.min.js"></script>
<script src="/assets/js/bootbox.min.js"></script>
<script src="/assets/js/common.js"></script>