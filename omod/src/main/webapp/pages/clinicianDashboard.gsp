<%
    ui.decorateWith("appui", "standardEmrPage")
%>
<script type="text/javascript">
    jq(document).ready(function () {

        jq("#okay").click(function () {
            patientqueue.createReadMessageDialog();
        });
    });
</script>

<% if (includeFragments) {

    includeFragments.each {
        // create a base map from the fragmentConfig if it exists, otherwise just create an empty map
        def configs = [:];
        if (it.extensionParams.fragmentConfig != null) {
            configs = it.extensionParams.fragmentConfig;
        } %>
${ui.includeFragment(it.extensionParams.provider, it.extensionParams.fragment, configs)}
<% }
} %>

<div class="dashboard clear">
    <div class="row">
        <div  class="col-12">
            <!-- only show the title div if a title has been defined in the messages.properties -->
            <% if (ui.message(dashboard + ".custom.title") != dashboard + ".custom.title") { %>
            <div class="title">
                <h3>${ui.message(dashboard + ".custom.title")}</h3>
            </div>
            <% } %>
        </div>
    </div>

    <div class="row">
        <div id="right-column" class="col-9">
            <% if (rightColumnFragments) {
                rightColumnFragments.each {
                    // create a base map from the fragmentConfig if it exists, otherwise just create an empty map
                    def configs = [:];
                    if (it.extensionParams.fragmentConfig != null) {
                        configs = it.extensionParams.fragmentConfig;
                    }
            %>
            ${ui.includeFragment(it.extensionParams.provider, it.extensionParams.fragment, configs)}
            <% }
            } %>

        </div>

        <div id="left-column" class="col-3">
            <% if (leftColumnFragments) {
                leftColumnFragments.each {
                    // create a base map from the fragmentConfig if it exists, otherwise just create an empty map
                    def configs = [:];
                    if (it.extensionParams.fragmentConfig != null) {
                        configs = it.extensionParams.fragmentConfig;
                    }
            %>
            ${ui.includeFragment(it.extensionParams.provider, it.extensionParams.fragment, configs)}
            <% }
            } %>

        </div>
    </div>
</div>
