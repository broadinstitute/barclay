<?php
    include '../../../common/include/common.php';
?>

<div class='row-fluid' id="top">

	<#include "common.html.ftl"/>

	<#macro flagDeprecated arg>
		<#if arg.deprecated == true> (Deprecated)</#if>
	</#macro>

	<#macro flagDeprecatedWithReason arg>
		<#if arg.deprecated == true>
			<#if arg.deprecationDetail??>
				This argument is deprecated. ${arg.deprecationDetail}</br>
			<#else>
				This argument is deprecated.</br>
			</#if>
		</#if>
	</#macro>

	<#macro argumentlist name myargs>
		<#if myargs?size != 0>
			<tr>
				<th colspan="4" id="row-divider">${name}</th>
			</tr>
			<#list myargs as arg>
				<tr>
					<td><a href="#${arg.name}">${arg.name}</a><br />
						<#if arg.synonyms != "NA">
							<#if arg.name[2..] != arg.synonyms[1..]>
								&nbsp;<em>${arg.synonyms}</em>
							</#if>
						</#if>
					</td>
					<!--<td>${arg.type}</td> -->
					<td>${arg.defaultValue!"NA"}</td>
					<td><@flagDeprecated arg=arg/>${arg.summary}</td>
				</tr>
			</#list>
		</#if>
	</#macro>

	<#macro argumentDetails arg>
		<hr style="border-bottom: dotted 1px #C0C0C0;" />
		<h3><a name="${arg.name}">${arg.name} </a>
			<#if arg.synonyms != "NA"> / <small>${arg.synonyms}</small></#if>
		</h3>
		<p class="args">
			<b><@flagDeprecated arg=arg/>${arg.summary}</b><br />
			${arg.fulltext}
		</p>
		<#if arg.otherArgumentRequired != "NA">
			<p><b>Dependency:</b> This argument requires that you also specify <code>${arg.otherArgumentRequired}</code>.</p>
		</#if>
		<#if arg.exclusiveOf != "NA">
			<p><b>Exclusion:</b> This argument cannot be used at the same time as <code>${arg.exclusiveOf}</code>.</p>
		</#if>
		<#if arg.options?has_content>
			<p>
		<#if arg.collection>
				The ${arg.name} argument is a list of an enumerated type (${arg.type}), which can accept one or more of the following values:
		<#else>
				The ${arg.name} argument is an enumerated type (${arg.type}), which can accept the following values:
		</#if>
			<dl class="enum">
				<#list arg.options as option>
					<dt class="enum">${option.name}</dt>
					<dd class="enum">${option.summary}</dd>
				</#list>
			</dl>
			</p>
		</#if>
		<p><#if arg.required != "NA">
			<#if arg.required == "yes">
				<span class="badge badge-important">R</span>
			</#if>
		</#if>
			<span class="label label-info ">${arg.type}</span>
			<#if arg.defaultValue?has_content>
				&nbsp;<span class="label">${arg.defaultValue}</span>
			</#if>
			<#if arg.minValue?is_number>
				&nbsp;<span class="label label-warning">[ [ ${arg.minValue}</span>
			</#if>
			<#if arg.minRecValue?is_number>
				&nbsp;<span class="label label-success">[ ${arg.minRecValue}</span>
			</#if>
			<#if arg.maxRecValue?is_number>
				&nbsp;<span class="label label-success">${arg.maxRecValue} ]</span>
			</#if>
			<#if arg.maxValue?is_number>
				&nbsp;<span class="label label-warning">${arg.maxValue} ] ]</span>
			</#if>
		</p>
	</#macro>

	<#macro relatedByType name type>
		<#list relatedDocs as relatedDoc>
			<#if relatedDoc.relation == type>
				<h3>${name}</h3>
				<ul>
					<#list relatedDocs as relatedDoc>
						<#if relatedDoc.relation == type>
							<li><a href="${relatedDoc.filename}">${relatedDoc.name}</a> is a ${relatedDoc.relation}</li>
						</#if>
					</#list>
				</ul>
				<#break>
			</#if>
		</#list>
	</#macro>

	<?php $group = '${group}'; ?>

	<section class="span4">
		<aside class="well">
			<a href="index"><h4><i class='fa fa-chevron-left'></i> Back to Tool Docs Index</h4></a>
		</aside>
		<aside class="well">
			<h2>Categories</h2>
			<@getCategories groups=groups />
		</aside>
		<?php getForumPosts( '${name}' ) ?>

	</section>

	<div class="span8">

		<#if beta?? && beta == true>
			<h1>${name} **BETA**</h1>
		<#elseif experimental?? && experimental == true>
			<h1>${name} **EXPERIMENTAL**</h1>
		<#elseif deprecated?? && deprecated == true>
			<h1>${name} **DEPRECATED** ${deprecationDetail}</h1>
		<#else>
			<h1>${name}</h1>
		</#if>

		<p class="lead">${summary}</p>

		<#if group?? >
			<h3>Category
				<small> ${group}</small>
			</h3>
		</#if>
		<hr>
		<h2>Overview</h2>
		${description}

		<#-- Create references to additional capabilities if appropriate -->
			<#if extradocs?size != 0 || arguments.all?size != 0>
				<hr>
				<h2>Command-line Arguments</h2>
				<p></p>
			</#if>
			<#if extradocs?size != 0>
				<h3>Additional References</h3>
				<p>See these additional references for more information.</p>
				<ul>
					<#list extradocs as extradoc>
						<li><a href="${extradoc.filename}">${extradoc.name}</a></li>
					</#list>
				</ul>
			</#if>

			<#-- Create the argument summary -->
			<#if arguments.all?size != 0>
				<h3>${name} specific arguments</h3>
				<p>This table summarizes the command-line arguments that are specific to this tool. For more details on each argument, see the list further down below the table or click on an argument name to jump directly to that entry in the list.</p>
				<table class="table table-striped table-bordered table-condensed">
					<thead>
					<tr>
						<th>Argument name(s)</th>
						<th>Default value</th>
						<th>Summary</th>
					</tr>
					</thead>
					<tbody>
					<@argumentlist name="Positional Arguments" myargs=arguments.positional/>
					<@argumentlist name="Required Arguments" myargs=arguments.required/>
					<@argumentlist name="Optional Tool Arguments" myargs=arguments.optional/>
					<@argumentlist name="Optional Common Arguments" myargs=arguments.common/>
					<@argumentlist name="Dependent Arguments" myargs=arguments.dependent/>
					<@argumentlist name="Advanced Arguments" myargs=arguments.advanced/>
					<@argumentlist name="Hidden Arguments" myargs=arguments.hidden/>
					<@argumentlist name="Deprecated Arguments" myargs=arguments.deprecated/>
					</tbody>
				</table>
			</#if>

			<#-- List all of the things -->
			<#if arguments.all?size != 0>
				<#-- Create the argument details -->
					<h3>Argument details</h3>
					<p>Arguments in this list are specific to this tool. Keep in mind that other arguments are available that are shared with other tools (e.g. command-line GATK arguments); see Inherited arguments above.</p>
					<#list arguments.all as arg>
						<@argumentDetails arg=arg/>
					</#list>
			</#if>

			<@footerInfo />
			<@footerClose />

	</div>

	<?php printFooter($module); ?>