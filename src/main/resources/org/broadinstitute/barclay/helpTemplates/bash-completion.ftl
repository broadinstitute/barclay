
####################
# Tab completion file to allow for easy use of this tool with the command-line using Bash.
####################

<#include "bash-completion.macros.ftl"/>

####################################################################################################

# High-level caller/dispatch script information:

CALLER_SCRIPT_NAME="${callerScriptOptions["callerScriptName"]}"

# A description of these variables is below in the main completion function (_masterCompletionFunction)
CS_PREFIX_OPTIONS_ALL_LEGAL_ARGUMENTS=(${callerScriptOptions["callerScriptPrefixLegalArgs"]} <@compress_single_line><@emitToolListForTopLevelComplete tools=tools /></@compress_single_line>)
CS_PREFIX_OPTIONS_NORMAL_COMPLETION_ARGUMENTS=(${callerScriptOptions["callerScriptPrefixLegalArgs"]} <@compress_single_line><@emitToolListForTopLevelComplete tools=tools /></@compress_single_line>)
CS_PREFIX_OPTIONS_ALL_ARGUMENT_VALUE_TYPES=(${callerScriptOptions["callerScriptPrefixArgValueTypes"]} <@compress_single_line><@emitStringsForToolList tools=tools repeatString="\"null\""/></@compress_single_line>)
CS_PREFIX_OPTIONS_MUTUALLY_EXCLUSIVE_ARGS=(${callerScriptOptions["callerScriptPrefixMutexArgs"]})
CS_PREFIX_OPTIONS_SYNONYMOUS_ARGS=(${callerScriptOptions["callerScriptPrefixAliasArgs"]})
CS_PREFIX_OPTIONS_MIN_OCCURRENCES=(${callerScriptOptions["callerScriptPrefixMinOccurrences"]} <@compress_single_line><@emitStringsForToolList tools=tools repeatString="0"/></@compress_single_line>)
CS_PREFIX_OPTIONS_MAX_OCCURRENCES=(${callerScriptOptions["callerScriptPrefixMaxOccurrences"]} <@compress_single_line><@emitStringsForToolList tools=tools repeatString="1"/></@compress_single_line>)

CS_POSTFIX_OPTIONS_ALL_LEGAL_ARGUMENTS=(${callerScriptOptions["callerScriptPostfixLegalArgs"]})
CS_POSTFIX_OPTIONS_NORMAL_COMPLETION_ARGUMENTS=(${callerScriptOptions["callerScriptPostfixLegalArgs"]})
CS_POSTFIX_OPTIONS_ALL_ARGUMENT_VALUE_TYPES=(${callerScriptOptions["callerScriptPostfixArgValueTypes"]})
CS_POSTFIX_OPTIONS_MUTUALLY_EXCLUSIVE_ARGS=(${callerScriptOptions["callerScriptPostfixMutexArgs"]})
CS_POSTFIX_OPTIONS_SYNONYMOUS_ARGS=(${callerScriptOptions["callerScriptPostfixAliasArgs"]})
CS_POSTFIX_OPTIONS_MIN_OCCURRENCES=(${callerScriptOptions["callerScriptPostfixMinOccurrences"]})
CS_POSTFIX_OPTIONS_MAX_OCCURRENCES=(${callerScriptOptions["callerScriptPostfixMaxOccurrences"]})

# Whether we have to worry about these extra script options at all.
HAS_POSTFIX_OPTIONS="${callerScriptOptions["hasCallerScriptPostfixArgs"]}"

# All the tool names we are able to complete:
ALL_TOOLS=(<@compress_single_line><@emitToolListForTopLevelComplete tools=tools /></@compress_single_line>)

####################################################################################################

# Get the name of the tool that we're currently trying to call
_${callerScriptOptions["callerScriptName"]}_getToolName()
{
    # Naively go through each word in the line until we find one that is in our list of tools:
    for word in ${r"${COMP_WORDS[@]}"} ; do
        if ( echo " ${r"${ALL_TOOLS[@]}"} " | grep -q " ${r"${word}"} " ) ; then
            echo "${r"${word}"}"
            break
        fi
    done
}

# Get the index of the toolname inside COMP_WORDS
_${callerScriptOptions["callerScriptName"]}_getToolNameIndex()
{
    # Naively go through each word in the line until we find one that is in our list of tools:
    local ctr=0
    for word in ${r"${COMP_WORDS[@]}"} ; do
        if ( echo " ${r"${ALL_TOOLS[@]}"} " | grep -q " ${r"${word}"} " ) ; then
            echo $ctr
            break
        fi
        let ctr=$ctr+1
    done
}

# Get all possible tool names for the current command line if the current command is a
# complete command on its own already.
# If there is no complete command yet, then this prints nothing.
_${callerScriptOptions["callerScriptName"]}_getAllPossibleToolNames()
{
# We want to return a list of possible tool names if and only if
# the current word is a valid complete tool name
# AND
# the current word is also a substring in more than one tool name

    local tool count matches toolList

    let count=0
    matches=false
    toolList=()

    # Go through tool names and get what matches and partial matches we have:
    for tool in ${r"${ALL_TOOLS[@]}"} ; do
        if [[ "${r"${COMP_WORDS[COMP_CWORD]}"}" == "${r"${tool}"}" ]] ; then
            matches=true
            let count=$count+1
            ${r"toolList+=($tool)"}
        elif [[ "${r"${tool}"}" == "${r"${COMP_WORDS[COMP_CWORD]}"}"* ]] ; then
            ${r"toolList+=($tool)"}
        fi
    done

    # If we have a complete match, then we print out our partial matches as a space separated string.
    # That way we have a list of all possible full completions for this match.
    # For instance, if there was a tool named "read" and another named "readBetter" this would get both.
    if $matches ; then
        echo "${r"${toolList[@]}"}"
    fi
}

# Gets how many dependent arguments we have left to fill
_${callerScriptOptions["callerScriptName"]}_getDependentArgumentCount()
{
    local depArgCount=0

    for word in ${r"${COMP_LINE}"} ; do
        for depArg in ${r"${DEPENDENT_ARGUMENTS[@]}"} ; do
            if [[ "${r"${word}"}" == "${r"${depArg}"}" ]] ; then
                $((depArgCount++))
            fi
        done
    done

    echo ${r"$depArgCount"}
}

# Resolves the given argument name to its long (normal) name
_${callerScriptOptions["callerScriptName"]}_resolveVarName()
{
    local argName=$1
    if [[ "${r"${SYNONYMOUS_ARGS[@]}"}" == *"${r"${argName}"}"* ]] ; then
        echo "${r"${SYNONYMOUS_ARGS[@]}"}" | sed -e "s#.* \\([a-zA-Z0-9;,_\\-]*${r"${argName}"}[a-zA-Z0-9,;_\\-]*\\).*#\\1#g" -e 's#;.*##g'
    else
        echo "${r"${argName}"}"
    fi
}

# Checks if we need to complete the VALUE for an argument.
# Prints the index in the given argument list of the corresponding argument whose value we must complete.
# Takes as input 1 positional argument: the name of the last argument given to this script
# Otherwise prints -1
_${callerScriptOptions["callerScriptName"]}_needToCompleteArgValue()
{
    if [[ "${r"${prev}"}" != "--" ]] ; then
        local resolved=$( _${callerScriptOptions["callerScriptName"]}_resolveVarName ${r"${prev}"} )

        ${r"for (( i=0 ; i < ${#ALL_LEGAL_ARGUMENTS[@]} ; i++ )) ; do"}
            if [[ "${r"${resolved}"}" == "${r"${ALL_LEGAL_ARGUMENTS[i]}"}" ]] ; then

                # Make sure the argument isn't one that takes no additional value
                # such as a flag.
                if [[ "${r"${ALL_ARGUMENT_VALUE_TYPES[i]}"}" != "null" ]] ; then
                    echo "$i"
                else
                    echo "-1"
                fi
                return 0
            fi
        done
    fi

    echo "-1"
}

# Get the completion word list for the given argument type.
# Prints the completion string to the screen
_${callerScriptOptions["callerScriptName"]}_getCompletionWordList()
{
    # Normalize the type string so it's easier to deal with:
    local argType=$( echo $1 | tr '[A-Z]' '[a-z]')

    local isNumeric=false
    local isFloating=false

    local completionType=""

    [[ "${r"${argType}"}" == *"file"* ]]      && completionType='-A file'
    [[ "${r"${argType}"}" == *"folder"* ]]    && completionType='-A directory'
    [[ "${r"${argType}"}" == *"directory"* ]] && completionType='-A directory'
    [[ "${r"${argType}"}" == *"boolean"* ]]   && completionType='-W true false'

    [[ "${r"${argType}"}" == "int" ]]         && completionType='-W 0 1 2 3 4 5 6 7 8 9'   && isNumeric=true
    [[ "${r"${argType}"}" == *"[int]"* ]]     && completionType='-W 0 1 2 3 4 5 6 7 8 9'   && isNumeric=true
    [[ "${r"${argType}"}" == "long" ]]        && completionType='-W 0 1 2 3 4 5 6 7 8 9'   && isNumeric=true
    [[ "${r"${argType}"}" == *"[long]"* ]]    && completionType='-W 0 1 2 3 4 5 6 7 8 9'   && isNumeric=true

    [[ "${r"${argType}"}" == "double" ]]      && completionType='-W . 0 1 2 3 4 5 6 7 8 9' && isNumeric=true && isFloating=true
    [[ "${r"${argType}"}" == *"[double]"* ]]  && completionType='-W . 0 1 2 3 4 5 6 7 8 9' && isNumeric=true && isFloating=true
    [[ "${r"${argType}"}" == "float" ]]       && completionType='-W . 0 1 2 3 4 5 6 7 8 9' && isNumeric=true && isFloating=true
    [[ "${r"${argType}"}" == *"[float]"* ]]   && completionType='-W . 0 1 2 3 4 5 6 7 8 9' && isNumeric=true && isFloating=true

    # If we have a number, we need to prepend the current completion to it so that we can continue to tab complete:
    if $isNumeric ; then
        completionType=$( echo ${r"${completionType}"} | sed -e "s#\([0-9]\)#$cur\1#g" )

        # If we're floating point, we need to make sure we don't complete a `.` character
        # if one already exists in our number:
        if $isFloating ; then
            echo "$cur" | grep -o '\.' &> /dev/null
            local r=$?

            [[ $r -eq 0 ]] && completionType=$( echo ${r"${completionType}"} | awk '{$2="" ; print}' )
        fi
    fi

    echo "${r"${completionType}"}"
}

# Function to handle the completion tasks once we have populated our arg variables
# When passed an argument handles the case for the caller script.
_${callerScriptOptions["callerScriptName"]}_handleArgs()
{
    # Argument offset index is used in the special case where we are past the " -- " delimiter.
    local argOffsetIndex=0

    # We handle the beginning differently if this function was called with an argument
    if [[ $# -eq 0 ]] ; then
        # Get the number of arguments we have input so far:
        local toolNameIndex=$(_${callerScriptOptions["callerScriptName"]}_getToolNameIndex)
        local numArgs=$((COMP_CWORD-toolNameIndex-1))

        # Now we check to see what kind of argument we are on right now
        # We handle each type separately by order of precedence:
        ${r"if [[ ${numArgs} -lt ${NUM_POSITIONAL_ARGUMENTS} ]] ; then"}
            # We must complete a positional argument.
            # Assume that positional arguments are all FILES:
            COMPREPLY=( ${r"$(compgen -A file -- $cur"}) )
            return 0
        fi

        # Dependent arguments must come right after positional arguments
        # We must check to see how many dependent arguments we've gotten so far:
        local numDepArgs=${r"$"}( _${callerScriptOptions["callerScriptName"]}_getDependentArgumentCount )

        ${r"if [[ $numDepArgs -lt ${#DEPENDENT_ARGUMENTS[@]} ]] ; then"}
            # We must complete a dependent argument next.
            COMPREPLY=( ${r"$(compgen -W '${DEPENDENT_ARGUMENTS[@]}' -- $cur"}) )
            return 0
        fi
    elif [[ "${r"${1}"}" == "POSTFIX_OPTIONS" ]] ; then
        # Get the index of the special delimiter.
        # we ignore everything up to and including it.
        for (( i=0; i < COMP_CWORD ; i++ )) ; do
            if [[ "${r"${COMP_WORDS[i]}"}" == "--" ]] ; then
                let argOffsetIndex=$i+1
            fi
        done
    fi
    # NOTE: We don't need to worry about the prefix options case.
    #       The caller will specify it and it skips the two special cases above.

    # First we must resolve all arguments to their full names
    # This is necessary to save time later because of short argument names / synonyms
    local resolvedArgList=()
    for (( i=argOffsetIndex ; i < COMP_CWORD ; i++ )) ; do
        prevArg=${r"${COMP_WORDS[i]}"}

        # Skip the current word to be completed:
        [[ "${r"${prevArg}"}" == "${r"${cur}"}" ]] && continue

        # Check if this has synonyms:
        if [[ "${r"${SYNONYMOUS_ARGS[@]}"}" == *"${r"${prevArg}"}"* ]] ; then

            local resolvedArg=$( _${callerScriptOptions["callerScriptName"]}_resolveVarName "${r"${prevArg}"}" )
            ${r"resolvedArgList+=($resolvedArg)"}

        # Make sure this is an argument:
        elif [[ "${r"${ALL_LEGAL_ARGUMENTS[@]}"}" == *"${r"${prevArg}"}"* ]] ; then
            ${r"resolvedArgList+=($prevArg)"}
        fi
    done

    # Check to see if the last thing we typed was a complete argument.
    # If so, we must complete the VALUE for the argument, not the
    # argument itself:
    # Note: This is shorthand for last element in the array:
    local argToComplete=$( _${callerScriptOptions["callerScriptName"]}_needToCompleteArgValue )

    if [[ $argToComplete -ne -1 ]] ; then
        # We must complete the VALUE for an argument.

        # Get the argument type.
        local valueType=${r"${ALL_ARGUMENT_VALUE_TYPES[argToComplete]}"}

        # Get the correct completion string for the type:
        local completionString=$( _${callerScriptOptions["callerScriptName"]}_getCompletionWordList "${r"${valueType}"}" )

        if [[ ${r"${#completionString}"} -eq 0 ]] ; then
            # We don't have any information on the type to complete.
            # We use the default SHELL behavior:
            COMPREPLY=()
        else
            # We have a completion option.  Let's plug it in:
            local compOperator=$( echo "${r"${completionString}"}" | awk '{print $1}' )
            local compOptions=$( echo "${r"${completionString}"}" | awk '{$1="" ; print}' )

            case ${r"${compOperator}"} in
                -A) COMPREPLY=( ${r"$(compgen -A ${compOptions} -- $cur"}) ) ;;
                -W) COMPREPLY=( ${r"$(compgen -W '${compOptions}' -- $cur"}) ) ;;
                 *) COMPREPLY=() ;;
            esac

        fi
        return 0
    fi

    # We must create a list of the valid remaining arguments:

    # Create a list of all arguments that are
    # mutually exclusive with arguments we have already specified
    local mutex_list=""
    for prevArg in ${r"${resolvedArgList[@]}"} ; do
        if [[ "${r"${MUTUALLY_EXCLUSIVE_ARGS[@]}"}" == *"${r"${prevArg}"};"* ]] ; then
            local mutexArgs=$( echo "${r"${MUTUALLY_EXCLUSIVE_ARGS[@]}"}" | sed -e "s#.*${r"${prevArg}"};\([a-zA-Z0-9_,\-]*\) .*#\1#g" -e "s#,# --#g" -e "s#^#--#g" )
            mutex_list="${r"${mutex_list}${mutexArgs}"}"
        fi
    done

    local remaining_legal_arguments=()
    for (( i=0; i < ${r"${#NORMAL_COMPLETION_ARGUMENTS[@]}"} ; i++ )) ; do
        local legalArg=${r"${NORMAL_COMPLETION_ARGUMENTS[i]}"}
        local okToAdd=true

        # Get the number of times this has occurred in the arguments already:
        local numPrevOccurred=$( grep -o -- "${r"${legalArg}"}" <<< "${r"${resolvedArgList[@]}"}" | wc -l | awk '{print $1}' )

        if [[ $numPrevOccurred -lt "${r"${MAX_OCCURRENCES[i]}"}" ]] ; then

            # Make sure this arg isn't mutually exclusive to another argument that we've already had:
            if [[ "${r"${mutex_list}"}" ==    "${r"${legalArg}"} "* ]] ||
               [[ "${r"${mutex_list}"}" ==  *" ${r"${legalArg}"} "* ]] ||
               [[ "${r"${mutex_list}"}" ==  *" ${r"${legalArg}"}"  ]] ; then
                okToAdd=false
            fi

            # Check if we're still good to add in the argument:
            if $okToAdd ; then
                # Add in the argument:
                ${r"remaining_legal_arguments+=($legalArg)"}

                # Add in the synonyms of the argument:
                if [[ "${r"${SYNONYMOUS_ARGS[@]}"}" == *"${r"${legalArg}"}"* ]] ; then
                    local synonymString=$( echo "${r"${SYNONYMOUS_ARGS[@]}"}" | sed -e "s#.*${r"${legalArg}"};\([a-zA-Z0-9_,\-]*\).*#\1#g" -e "s#,# #g"  )
                    ${r"remaining_legal_arguments+=($synonymString)"}
                fi
            fi
        fi

    done

    # Add in the special option "--" which separates tool options from meta-options if they're necessary:
    if $HAS_POSTFIX_OPTIONS ; then
        if [[ $# -eq 0 ]] || [[ "${r"${1}"}" == "PREFIX_OPTIONS"  ]] ; then
            remaining_legal_arguments+=("--")
        fi
    fi

    COMPREPLY=( ${r"$(compgen -W '${remaining_legal_arguments[@]}' -- $cur"}) )
    return 0
}

####################################################################################################

_${callerScriptOptions["callerScriptName"]}_masterCompletionFunction()
{
    # Set up global variables for the functions that do completion:
    prev=${r"${COMP_WORDS[COMP_CWORD-1]}"}
    cur=${r"${COMP_WORDS[COMP_CWORD]}"}

    # How many positional arguments a tool will have.
    # These positional arguments must come directly after a tool name.
    NUM_POSITIONAL_ARGUMENTS=0

    # The types of the positional arguments, in the order in which they must be specified
    # on the command-line.
    POSITIONAL_ARGUMENT_TYPE=()

    # The set of legal arguments that aren't dependent arguments.
    # (A dependent argument is an argument that must occur immediately after
    # all positional arguments.)
    NORMAL_COMPLETION_ARGUMENTS=()

    # The set of ALL legal arguments
    # Corresponds by index to the type of those arguments in ALL_ARGUMENT_VALUE_TYPES
    ALL_LEGAL_ARGUMENTS=()

    # The types of ALL legal arguments
    # Corresponds by index to the names of those arguments in ALL_LEGAL_ARGUMENTS
    ALL_ARGUMENT_VALUE_TYPES=()

    # Arguments that are mutually exclusive.
    # These are listed here as arguments concatenated together with delimiters:
    # ${r"<"}Main argument${r">"};${r"<"}Mutex Argument 1${r">"}[,${r"<"}Mutex Argument 2${r">"},...]
    MUTUALLY_EXCLUSIVE_ARGS=()

    # Alternate names of arguments.
    # These are listed here as arguments concatenated together with delimiters.
    # ${r"<"}Main argument${r">"};${r"<"}Synonym Argument 1${r">"}[,${r"<"}Synonym Argument 2${r">"},...]
    SYNONYMOUS_ARGS=()

    # The minimum number of times an argument can occur.
    MIN_OCCURRENCES=()

    # The maximum number of times an argument can occur.
    MAX_OCCURRENCES=()

    # Set up locals for this function:
    local toolName=$( _${callerScriptOptions["callerScriptName"]}_getToolName )

    # Get possible tool matches:
    local possibleToolMatches=$( _${callerScriptOptions["callerScriptName"]}_getAllPossibleToolNames )

    # Check if we have postfix options
    # and if we now need to go through them:
    if $HAS_POSTFIX_OPTIONS && [[ "${r"${COMP_WORDS[@]}"}" == *" -- "* ]] ; then
        NUM_POSITIONAL_ARGUMENTS=0
        POSITIONAL_ARGUMENT_TYPE=()
        DEPENDENT_ARGUMENTS=()
        NORMAL_COMPLETION_ARGUMENTS=("${r"${CS_POSTFIX_OPTIONS_NORMAL_COMPLETION_ARGUMENTS[@]}"}")
        MUTUALLY_EXCLUSIVE_ARGS=("${r"${CS_POSTFIX_OPTIONS_MUTUALLY_EXCLUSIVE_ARGS[@]}"}")
        SYNONYMOUS_ARGS=("${r"${CS_POSTFIX_OPTIONS_SYNONYMOUS_ARGS[@]}"}")
        MIN_OCCURRENCES=("${r"${CS_POSTFIX_OPTIONS_MIN_OCCURRENCES[@]}"}")
        MAX_OCCURRENCES=("${r"${CS_POSTFIX_OPTIONS_MAX_OCCURRENCES[@]}"}")
        ALL_LEGAL_ARGUMENTS=("${r"${CS_POSTFIX_OPTIONS_ALL_LEGAL_ARGUMENTS[@]}"}")
        ALL_ARGUMENT_VALUE_TYPES=("${r"${CS_POSTFIX_OPTIONS_ALL_ARGUMENT_VALUE_TYPES[@]}"}")

        # Complete the arguments for the base script:
        # Strictly speaking, what the argument to this function is doesn't matter.
        _${callerScriptOptions["callerScriptName"]}_handleArgs POSTFIX_OPTIONS

    # Check if we have a complete tool match that may match more than one tool:
    elif [[ ${r"${#possibleToolMatches}"} -ne 0 ]] ; then

        # Set our reply as a list of the possible tool matches:
        COMPREPLY=( ${r"$(compgen -W '${possibleToolMatches[@]}' -- $cur"}) )

<@emitGroupToolCheckConditional tools=tools/>

    # We have no postfix options or tool options.
    # We now must complete any prefix options and the tools themselves.
    # These are defined at the top.
    else
        NUM_POSITIONAL_ARGUMENTS=0
        POSITIONAL_ARGUMENT_TYPE=()
        DEPENDENT_ARGUMENTS=()
        NORMAL_COMPLETION_ARGUMENTS=("${r"${CS_PREFIX_OPTIONS_NORMAL_COMPLETION_ARGUMENTS[@]}"}")
        MUTUALLY_EXCLUSIVE_ARGS=("${r"${CS_PREFIX_OPTIONS_MUTUALLY_EXCLUSIVE_ARGS[@]}"}")
        SYNONYMOUS_ARGS=("${r"${CS_PREFIX_OPTIONS_SYNONYMOUS_ARGS[@]}"}")
        MIN_OCCURRENCES=("${r"${CS_PREFIX_OPTIONS_MIN_OCCURRENCES[@]}"}")
        MAX_OCCURRENCES=("${r"${CS_PREFIX_OPTIONS_MAX_OCCURRENCES[@]}"}")
        ALL_LEGAL_ARGUMENTS=("${r"${CS_PREFIX_OPTIONS_ALL_LEGAL_ARGUMENTS[@]}"}")
        ALL_ARGUMENT_VALUE_TYPES=("${r"${CS_PREFIX_OPTIONS_ALL_ARGUMENT_VALUE_TYPES[@]}"}")

        # Complete the arguments for the prefix arguments and tools:
        _${callerScriptOptions["callerScriptName"]}_handleArgs PREFIX_OPTIONS
    fi
}

${r"complete -o default -F _"}${callerScriptOptions["callerScriptName"]}${r"_masterCompletionFunction ${CALLER_SCRIPT_NAME}"}



