<#-- Removes all occurrences of delim between the two nested tags -->
<#macro removeDelimiter delim><#local captured><#nested></#local>${ captured?replace(delim, "", "rm") }</#macro>

<#-- Compress all text between opening and closing compress_single_line tags into a single line. -->
<#macro compress_single_line><#local captured><#nested></#local>${ captured?replace("\\n|\\r", "", "rm") }</#macro>

<#-- Print out the names of all arguments of the given type from the given argument map. -->
<#macro printArgNames argumentMap argType >
    <#list argumentMap[argType]?sort_by("name") as args>
${args["name"]} <#nt>
    </#list>
</#macro>

<#-- Print out the types of all arguments of the given type from the given argument map. -->
<#macro printArgTypes argumentMap argType >
    <#list argumentMap[argType]?sort_by("name") as args>
"${args["type"]}" <#nt>
    </#list>
</#macro>

<#-- Print out the given field from the list of all argument types in the given argument map. -->
<#macro printArgFieldList argumentMap fieldName>
    <#list ["required", "common", "optional", "advanced", "deprecated"] as argType >
        <#if argumentMap[argType]?size gt 0>
            <#list argumentMap[argType]?sort_by("name") as args>
                <#if args[fieldName]?length gt 0 >
${args[fieldName]} <#nt>
                </#if>
            </#list>
        </#if>
    </#list>
</#macro>

<#-- Print out a string of of argument sets that are mutually exclusive.
 Argument sets are separated by spaces.
 The argument itself is first in the list and separated between the mutex
 arguments with a semicolon.
 The mutex arguments are comma separated.-->
<#macro printDelimitedArgSet argumentMap argName>
    <#list ["required", "common", "optional", "advanced", "deprecated"] as argType >
        <#if argumentMap[argType]?size gt 0>
            <#list argumentMap[argType]?sort_by("name") as args>
                <#if args[argName]?length gt 0 >
                    <#if args[argName] != "NA" >
"${args["name"]};${args[argName]?replace(" ", "")}" <#nt>
                    </#if>
                </#if>
            </#list>
        </#if>
    </#list>
</#macro>

<#-- Print the minimum occurrences of the arguments in the given map -->
<#macro printMinOccurrences argumentMap>
    <@printArgFieldList argumentMap "minElements" />
</#macro>

<#macro emitGroupToolCheckConditional tools>
    <#list tools?keys as toolName>
    elif ${r"[[ ${toolName}"} == "${toolName}" ${r"]]"} ; then

        <#assign arguments = tools[toolName].arguments>
        # Set up the completion information for this tool:
        <#if arguments["positional"]?size gt 0 >
        <#-- We know that there will only be one positional argument in the list because of how they are declared: -->
        NUM_POSITIONAL_ARGUMENTS=${arguments["positional"]?first["minElements"]}
        POSITIONAL_ARGUMENT_TYPE=("${arguments["positional"]?first["type"]}")
        </#if>
        <@compress_single_line>
        DEPENDENT_ARGUMENTS=(<@printArgNames arguments "dependent" />)
        </@compress_single_line>

        <@compress_single_line>
        NORMAL_COMPLETION_ARGUMENTS=(<@printArgFieldList arguments "name"/>)
        </@compress_single_line>

        <@compress_single_line>
        MUTUALLY_EXCLUSIVE_ARGS=(<@printDelimitedArgSet arguments "exclusiveOf"/>)
        </@compress_single_line>

        <@compress_single_line>
        SYNONYMOUS_ARGS=(<@printDelimitedArgSet arguments "synonyms"/>)
        </@compress_single_line>

        <@compress_single_line>
        MIN_OCCURRENCES=(<@removeDelimiter ","><@printArgFieldList arguments "minElements"/></@removeDelimiter>)
        </@compress_single_line>

        <@compress_single_line>
        MAX_OCCURRENCES=(<@removeDelimiter ","><@printArgFieldList arguments "maxElements"/></@removeDelimiter>)
        </@compress_single_line>

        <@compress_single_line>
        ALL_LEGAL_ARGUMENTS=(<#list ["required", "common", "optional", "dependent", "advanced", "deprecated"] as argType><@printArgNames arguments argType/></#list>)
        </@compress_single_line>

        <@compress_single_line>
        ALL_ARGUMENT_VALUE_TYPES=(<#list ["required", "common", "optional", "dependent", "advanced", "deprecated"] as argType><@printArgTypes arguments argType/></#list>)
        </@compress_single_line>


        # Complete the arguments for this tool:
        _${callerScriptOptions["callerScriptName"]}_handleArgs
    </#list>
</#macro>

<#-- Print out the list of all tools. -->
<#macro emitToolListForTopLevelComplete tools>
    <#list tools?keys as toolName>
${toolName} <#nt>
    </#list>
</#macro>

<#-- Print out repeatString for each tool in the list of all tools. -->
<#macro emitStringsForToolList tools repeatString>
    <#list tools?keys as toolName>
${repeatString} <#nt>
    </#list>
</#macro>