<?php

    include '../../../common/include/common.php';
    include_once '../../config.php';
    printHeader($module, "Tool Documentation Index", "Guide");
?>

<div class='row-fluid'>

<div class='span9'>

<#include "common.html.ftl"/>

<#macro emitGroup group>
    <div class="accordion-group">
        <div class="accordion-heading">
            <a class="accordion-toggle" data-toggle="collapse" data-parent="#index" href="#${group.id}">
                <h4>${group.name}</h4>
            </a>
        </div>
        <div class="accordion-body collapse" id="${group.id}">
            <div class="accordion-inner">
                <p class="lead">${group.summary}</p>
                <table class="table table-striped table-bordered table-condensed">
                    <tr>
                        <th>Name</th>
                        <th>Summary</th>
                    </tr>
                    <#list data as datum>
                        <#if datum.group == group.name>
                            <tr>
                                <#if datum.beta?? && datum.beta == "true">
                                    <td><a href="${datum.filename}">${datum.name} **BETA**</a></td>
                                <#elseif datum.experimental?? && datum.experimental == "true">
                                    <td><a href="${datum.filename}">${datum.name} **EXPERIMENTAL**</a></td>
                                <#else>
                                    <td><a href="${datum.filename}">${datum.name}</a></td>
                                </#if>
                                <td>${datum.summary}</td>
                            </tr>
                        </#if>
                    </#list>
                </table>
            </div>
        </div>
    </div>
</#macro>

<h1 id="top">Tool Documentation Index
    <small>${version}</small>
</h1>
<div class="accordion" id="index">
    <#assign seq = ["engine", "tools", "other", "utilities"]>
	<#list seq as supercat>
		<br />
		<#list groups?sort_by("name") as group>
			<#if group.supercat == supercat>
				<@emitGroup group=group/>
			</#if>
		</#list>
	</#list>
</div>

<@footerInfo />
<@footerClose />

</div></div>

<?php

    printFooter($module);

?>