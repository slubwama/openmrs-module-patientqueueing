<%
    ui.includeJavascript("patientqueueing", "patientqueue.js")
%>

<script type="text/javascript">
    jq(document).ready(function () {
    });
</script>
<style>
.modal-header {
    background: #000;
    color: #ffffff;
}
</style>

<div class="modal fade" id="add_patient_to_queue_dialog" tabindex="-1" role="dialog" aria-labelledby="exampleModalLabel"
     aria-hidden="true">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <form method="post">
                <div class="modal-header">
                    ${ui.message("patientqueueing.task.addPatientToQueue.label")}<i class="icon-remove-sign"></i>
                </div>

                <div class="modal-body">
                    <span id="add_to_queue-container">

                    </span>
                    <input type="hidden" id="patient_id" name="patientId" value="">

                    <div class="form-group">
                        <label for="locationId">${ui.message("patientqueueing.location.label")}</label>
                        <select class="form-control" id="locationId" name="locationId">
                            <option value="">${ui.message("patientqueueing.location.selectTitle")}</option>
                            <% if (locationList != null) {
                                locationList.each { %>
                            <option value="${it.uuid}">${it.name}</option>
                            <%
                                    }
                                }
                            %>
                        </select>
                        <span class="field-error" style="display: none;"></span>
                        <% if (locationList == null) { %>
                        <div><${ui.message("patientqueueing.select.error")}</div>
                        <% } %>
                    </div>

                    <div class="form-group">
                        <label for="locationId">${ui.message("patientqueueing.provider.label")}</label>
                        <select class="form-control" id="locationId" name="locationId">
                            <option value="">${ui.message("patientqueueing.provider.selectTitle")}</option>
                            <% if (providerList != null) {
                                providerList.each {
                                    if (it.getName() != null) { %><option
                                value="${it.providerId}">${it.getName()}</option><% }
                        }
                        } %>
                        </select>
                        <span class="field-error" style="display: none;"></span>
                        <% if (locationList == null) { %>
                        <div><${ui.message("patientqueueing.select.error")}</div>
                        <% } %>
                    </div>
                </div>
                <div class="modal-footer form">
                    <input type="submit" class="confirm" value="${ui.message("patientqueueing.send.label")}">
                </div>
            </form>
        </div>
    </div>
</div>


