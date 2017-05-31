<#--
        This file contains part of the theming used to present Barclay docs on a website. Styling is separated
        out, so pages will be minimalistic html unless replacement styling is provided.
        -->

    <#macro footerInfo>
        <hr>
        <p><a href='#top'><i class='fa fa-chevron-up'></i> Return to top</a></p>
        <hr>
        <p class="version">Barclay version ${version} built at ${timestamp}.
        <#-- closing P tag in next macro -->
    </#macro>
    
    <#macro footerClose>
    	<#-- ugly little hack to enable adding tool-specific info inline -->
        </p>
    </#macro>

    <#macro getCategories groups>
        <style>
            #sidenav .accordion-body a {
                color : gray;
            }

            .accordion-body li {
                list-style : none;
            }
        </style>
        <ul class="nav nav-pills nav-stacked" id="sidenav">
			<#list groups?sort_by("name") as group>
				<li><a data-toggle="collapse" data-parent="#sidenav" href="#${group.id}">${group.name}</a>
					<div id="${group.id}"
					<?php echo ($group == '${group.name}')? 'class="accordion-body collapse in"'.chr(62) : 'class="accordion-body collapse"'.chr(62);?>
					<ul>
						<#list data as datum>
							<#if datum.group == group.name>
								<li>
									<a href="${datum.filename}">${datum.name}</a>
								</li>
							</#if>
						</#list>
					</ul>
					</div>
				</li>
			</#list>
        </ul>
    </#macro>