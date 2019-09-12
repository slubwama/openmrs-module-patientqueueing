<%
    ui.includeJavascript("patientqueueing", "patientqueue.js")
%>

<script type="text/javascript">
    jq(document).ready(function () {
    });
</script>

<div class="modal fade" id="complete_patient_queue" tabindex="-1" role="dialog" aria-labelledby="exampleModalLabel"
     aria-hidden="true">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <form method="get" id="patient-search-form" onsubmit="return false">
                <div class="modal-header">
                    ${ui.message("patientqueueing.completequeue.title.label")}
                </div>

                <div class="modal-body">
                    <div>
                        <p class="center" style="text-align: center;font-weight: bolder">
                            <input type="hidden" name="patientQueueId" value="">
                            Are you sure you want to complete Patient Session.
                        </p>
                    </div>
                </div>

                <div class="modal-footer  form">
                    <button class="cancel" id="">${ui.message("patientqueueing.close.label")}</button>
                    <input type="submit" class="confirm" value="${ui.message("patientqueueing.ok.label")}">
                </div>
            </form>
        </div>
    </div>
</div>

