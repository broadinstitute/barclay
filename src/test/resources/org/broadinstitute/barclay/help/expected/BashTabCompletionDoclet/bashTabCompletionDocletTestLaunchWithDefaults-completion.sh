
####################
# Tab completion file to allow for easy use of this tool with the command-line using Bash.
####################


####################################################################################################

# High-level caller/dispatch script information:

CALLER_SCRIPT_NAME="bashTabCompletionDocletTestLaunchWithDefaults"

# A description of these variables is below in the main completion function (_masterCompletionFunction)
CS_PREFIX_OPTIONS_ALL_LEGAL_ARGUMENTS=( TestExtraDocs TestArgumentContainer )
CS_PREFIX_OPTIONS_NORMAL_COMPLETION_ARGUMENTS=( TestExtraDocs TestArgumentContainer )
CS_PREFIX_OPTIONS_ALL_ARGUMENT_VALUE_TYPES=( "null" "null" )
CS_PREFIX_OPTIONS_MUTUALLY_EXCLUSIVE_ARGS=()
CS_PREFIX_OPTIONS_SYNONYMOUS_ARGS=()
CS_PREFIX_OPTIONS_MIN_OCCURRENCES=( 0 0 )
CS_PREFIX_OPTIONS_MAX_OCCURRENCES=( 1 1 )

CS_POSTFIX_OPTIONS_ALL_LEGAL_ARGUMENTS=()
CS_POSTFIX_OPTIONS_NORMAL_COMPLETION_ARGUMENTS=()
CS_POSTFIX_OPTIONS_ALL_ARGUMENT_VALUE_TYPES=()
CS_POSTFIX_OPTIONS_MUTUALLY_EXCLUSIVE_ARGS=()
CS_POSTFIX_OPTIONS_SYNONYMOUS_ARGS=()
CS_POSTFIX_OPTIONS_MIN_OCCURRENCES=()
CS_POSTFIX_OPTIONS_MAX_OCCURRENCES=()

# Whether we have to worry about these extra script options at all.
HAS_POSTFIX_OPTIONS="false"

# All the tool names we are able to complete:
ALL_TOOLS=(TestExtraDocs TestArgumentContainer )

####################################################################################################

# Get the name of the tool that we're currently trying to call
_bashTabCompletionDocletTestLaunchWithDefaults_getToolName()
{
    # Naively go through each word in the line until we find one that is in our list of tools:
    for word in ${COMP_WORDS[@]} ; do
        if ( echo " ${ALL_TOOLS[@]} " | grep -q " ${word} " ) ; then
            echo "${word}"
            break
        fi
    done
}

# Get the index of the toolname inside COMP_WORDS
_bashTabCompletionDocletTestLaunchWithDefaults_getToolNameIndex()
{
    # Naively go through each word in the line until we find one that is in our list of tools:
    local ctr=0
    for word in ${COMP_WORDS[@]} ; do
        if ( echo " ${ALL_TOOLS[@]} " | grep -q " ${word} " ) ; then
            echo $ctr
            break
        fi
        let ctr=$ctr+1
    done
}

# Get all possible tool names for the current command line if the current command is a
# complete command on its own already.
# If there is no complete command yet, then this prints nothing.
_bashTabCompletionDocletTestLaunchWithDefaults_getAllPossibleToolNames()
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
    for tool in ${ALL_TOOLS[@]} ; do
        if [[ "${COMP_WORDS[COMP_CWORD]}" == "${tool}" ]] ; then
            matches=true
            let count=$count+1
            toolList+=($tool)
        elif [[ "${tool}" == "${COMP_WORDS[COMP_CWORD]}"* ]] ; then
            toolList+=($tool)
        fi
    done

    # If we have a complete match, then we print out our partial matches as a space separated string.
    # That way we have a list of all possible full completions for this match.
    # For instance, if there was a tool named "read" and another named "readBetter" this would get both.
    if $matches ; then
        echo "${toolList[@]}"
    fi
}

# Gets how many dependent arguments we have left to fill
_bashTabCompletionDocletTestLaunchWithDefaults_getDependentArgumentCount()
{
    local depArgCount=0

    for word in ${COMP_LINE} ; do
        for depArg in ${DEPENDENT_ARGUMENTS[@]} ; do
            if [[ "${word}" == "${depArg}" ]] ; then
                $((depArgCount++))
            fi
        done
    done

    echo $depArgCount
}

# Resolves the given argument name to its long (normal) name
_bashTabCompletionDocletTestLaunchWithDefaults_resolveVarName()
{
    local argName=$1
    if [[ "${SYNONYMOUS_ARGS[@]}" == *"${argName}"* ]] ; then
        echo "${SYNONYMOUS_ARGS[@]}" | sed -e "s#.* \\([a-zA-Z0-9;,_\\-]*${argName}[a-zA-Z0-9,;_\\-]*\\).*#\\1#g" -e 's#;.*##g'
    else
        echo "${argName}"
    fi
}

# Checks if we need to complete the VALUE for an argument.
# Prints the index in the given argument list of the corresponding argument whose value we must complete.
# Takes as input 1 positional argument: the name of the last argument given to this script
# Otherwise prints -1
_bashTabCompletionDocletTestLaunchWithDefaults_needToCompleteArgValue()
{
    if [[ "${prev}" != "--" ]] ; then
        local resolved=$( _bashTabCompletionDocletTestLaunchWithDefaults_resolveVarName ${prev} )

        for (( i=0 ; i < ${#ALL_LEGAL_ARGUMENTS[@]} ; i++ )) ; do
            if [[ "${resolved}" == "${ALL_LEGAL_ARGUMENTS[i]}" ]] ; then

                # Make sure the argument isn't one that takes no additional value
                # such as a flag.
                if [[ "${ALL_ARGUMENT_VALUE_TYPES[i]}" != "null" ]] ; then
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
_bashTabCompletionDocletTestLaunchWithDefaults_getCompletionWordList()
{
    # Normalize the type string so it's easier to deal with:
    local argType=$( echo $1 | tr '[A-Z]' '[a-z]')

    local isNumeric=false
    local isFloating=false

    local completionType=""

    [[ "${argType}" == *"file"* ]]      && completionType='-A file'
    [[ "${argType}" == *"folder"* ]]    && completionType='-A directory'
    [[ "${argType}" == *"directory"* ]] && completionType='-A directory'
    [[ "${argType}" == *"boolean"* ]]   && completionType='-W true false'

    [[ "${argType}" == "int" ]]         && completionType='-W 0 1 2 3 4 5 6 7 8 9'   && isNumeric=true
    [[ "${argType}" == *"[int]"* ]]     && completionType='-W 0 1 2 3 4 5 6 7 8 9'   && isNumeric=true
    [[ "${argType}" == "long" ]]        && completionType='-W 0 1 2 3 4 5 6 7 8 9'   && isNumeric=true
    [[ "${argType}" == *"[long]"* ]]    && completionType='-W 0 1 2 3 4 5 6 7 8 9'   && isNumeric=true

    [[ "${argType}" == "double" ]]      && completionType='-W . 0 1 2 3 4 5 6 7 8 9' && isNumeric=true && isFloating=true
    [[ "${argType}" == *"[double]"* ]]  && completionType='-W . 0 1 2 3 4 5 6 7 8 9' && isNumeric=true && isFloating=true
    [[ "${argType}" == "float" ]]       && completionType='-W . 0 1 2 3 4 5 6 7 8 9' && isNumeric=true && isFloating=true
    [[ "${argType}" == *"[float]"* ]]   && completionType='-W . 0 1 2 3 4 5 6 7 8 9' && isNumeric=true && isFloating=true

    # If we have a number, we need to prepend the current completion to it so that we can continue to tab complete:
    if $isNumeric ; then
        completionType=$( echo ${completionType} | sed -e "s#\([0-9]\)#$cur\1#g" )

        # If we're floating point, we need to make sure we don't complete a `.` character
        # if one already exists in our number:
        if $isFloating ; then
            echo "$cur" | grep -o '\.' &> /dev/null
            local r=$?

            [[ $r -eq 0 ]] && completionType=$( echo ${completionType} | awk '{$2="" ; print}' )
        fi
    fi

    echo "${completionType}"
}

# Function to handle the completion tasks once we have populated our arg variables
# When passed an argument handles the case for the caller script.
_bashTabCompletionDocletTestLaunchWithDefaults_handleArgs()
{
    # Argument offset index is used in the special case where we are past the " -- " delimiter.
    local argOffsetIndex=0

    # We handle the beginning differently if this function was called with an argument
    if [[ $# -eq 0 ]] ; then
        # Get the number of arguments we have input so far:
        local toolNameIndex=$(_bashTabCompletionDocletTestLaunchWithDefaults_getToolNameIndex)
        local numArgs=$((COMP_CWORD-toolNameIndex-1))

        # Now we check to see what kind of argument we are on right now
        # We handle each type separately by order of precedence:
        if [[ ${numArgs} -lt ${NUM_POSITIONAL_ARGUMENTS} ]] ; then
            # We must complete a positional argument.
            # Assume that positional arguments are all FILES:
            COMPREPLY=( $(compgen -A file -- $cur) )
            return 0
        fi

        # Dependent arguments must come right after positional arguments
        # We must check to see how many dependent arguments we've gotten so far:
        local numDepArgs=$( _bashTabCompletionDocletTestLaunchWithDefaults_getDependentArgumentCount )

        if [[ $numDepArgs -lt ${#DEPENDENT_ARGUMENTS[@]} ]] ; then
            # We must complete a dependent argument next.
            COMPREPLY=( $(compgen -W '${DEPENDENT_ARGUMENTS[@]}' -- $cur) )
            return 0
        fi
    elif [[ "${1}" == "POSTFIX_OPTIONS" ]] ; then
        # Get the index of the special delimiter.
        # we ignore everything up to and including it.
        for (( i=0; i < COMP_CWORD ; i++ )) ; do
            if [[ "${COMP_WORDS[i]}" == "--" ]] ; then
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
        prevArg=${COMP_WORDS[i]}

        # Skip the current word to be completed:
        [[ "${prevArg}" == "${cur}" ]] && continue

        # Check if this has synonyms:
        if [[ "${SYNONYMOUS_ARGS[@]}" == *"${prevArg}"* ]] ; then

            local resolvedArg=$( _bashTabCompletionDocletTestLaunchWithDefaults_resolveVarName "${prevArg}" )
            resolvedArgList+=($resolvedArg)

        # Make sure this is an argument:
        elif [[ "${ALL_LEGAL_ARGUMENTS[@]}" == *"${prevArg}"* ]] ; then
            resolvedArgList+=($prevArg)
        fi
    done

    # Check to see if the last thing we typed was a complete argument.
    # If so, we must complete the VALUE for the argument, not the
    # argument itself:
    # Note: This is shorthand for last element in the array:
    local argToComplete=$( _bashTabCompletionDocletTestLaunchWithDefaults_needToCompleteArgValue )

    if [[ $argToComplete -ne -1 ]] ; then
        # We must complete the VALUE for an argument.

        # Get the argument type.
        local valueType=${ALL_ARGUMENT_VALUE_TYPES[argToComplete]}

        # Get the correct completion string for the type:
        local completionString=$( _bashTabCompletionDocletTestLaunchWithDefaults_getCompletionWordList "${valueType}" )

        if [[ ${#completionString} -eq 0 ]] ; then
            # We don't have any information on the type to complete.
            # We use the default SHELL behavior:
            COMPREPLY=()
        else
            # We have a completion option.  Let's plug it in:
            local compOperator=$( echo "${completionString}" | awk '{print $1}' )
            local compOptions=$( echo "${completionString}" | awk '{$1="" ; print}' )

            case ${compOperator} in
                -A) COMPREPLY=( $(compgen -A ${compOptions} -- $cur) ) ;;
                -W) COMPREPLY=( $(compgen -W '${compOptions}' -- $cur) ) ;;
                 *) COMPREPLY=() ;;
            esac

        fi
        return 0
    fi

    # We must create a list of the valid remaining arguments:

    # Create a list of all arguments that are
    # mutually exclusive with arguments we have already specified
    local mutex_list=""
    for prevArg in ${resolvedArgList[@]} ; do
        if [[ "${MUTUALLY_EXCLUSIVE_ARGS[@]}" == *"${prevArg};"* ]] ; then
            local mutexArgs=$( echo "${MUTUALLY_EXCLUSIVE_ARGS[@]}" | sed -e "s#.*${prevArg};\([a-zA-Z0-9_,\-]*\) .*#\1#g" -e "s#,# --#g" -e "s#^#--#g" )
            mutex_list="${mutex_list}${mutexArgs}"
        fi
    done

    local remaining_legal_arguments=()
    for (( i=0; i < ${#NORMAL_COMPLETION_ARGUMENTS[@]} ; i++ )) ; do
        local legalArg=${NORMAL_COMPLETION_ARGUMENTS[i]}
        local okToAdd=true

        # Get the number of times this has occurred in the arguments already:
        local numPrevOccurred=$( grep -o -- "${legalArg}" <<< "${resolvedArgList[@]}" | wc -l | awk '{print $1}' )

        if [[ $numPrevOccurred -lt "${MAX_OCCURRENCES[i]}" ]] ; then

            # Make sure this arg isn't mutually exclusive to another argument that we've already had:
            if [[ "${mutex_list}" ==    "${legalArg} "* ]] ||
               [[ "${mutex_list}" ==  *" ${legalArg} "* ]] ||
               [[ "${mutex_list}" ==  *" ${legalArg}"  ]] ; then
                okToAdd=false
            fi

            # Check if we're still good to add in the argument:
            if $okToAdd ; then
                # Add in the argument:
                remaining_legal_arguments+=($legalArg)

                # Add in the synonyms of the argument:
                if [[ "${SYNONYMOUS_ARGS[@]}" == *"${legalArg}"* ]] ; then
                    local synonymString=$( echo "${SYNONYMOUS_ARGS[@]}" | sed -e "s#.*${legalArg};\([a-zA-Z0-9_,\-]*\).*#\1#g" -e "s#,# #g"  )
                    remaining_legal_arguments+=($synonymString)
                fi
            fi
        fi

    done

    # Add in the special option "--" which separates tool options from meta-options if they're necessary:
    if $HAS_POSTFIX_OPTIONS ; then
        if [[ $# -eq 0 ]] || [[ "${1}" == "PREFIX_OPTIONS"  ]] ; then
            remaining_legal_arguments+=("--")
        fi
    fi

    COMPREPLY=( $(compgen -W '${remaining_legal_arguments[@]}' -- $cur) )
    return 0
}

####################################################################################################

_bashTabCompletionDocletTestLaunchWithDefaults_masterCompletionFunction()
{
    # Set up global variables for the functions that do completion:
    prev=${COMP_WORDS[COMP_CWORD-1]}
    cur=${COMP_WORDS[COMP_CWORD]}

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
    # <Main argument>;<Mutex Argument 1>[,<Mutex Argument 2>,...]
    MUTUALLY_EXCLUSIVE_ARGS=()

    # Alternate names of arguments.
    # These are listed here as arguments concatenated together with delimiters.
    # <Main argument>;<Synonym Argument 1>[,<Synonym Argument 2>,...]
    SYNONYMOUS_ARGS=()

    # The minimum number of times an argument can occur.
    MIN_OCCURRENCES=()

    # The maximum number of times an argument can occur.
    MAX_OCCURRENCES=()

    # Set up locals for this function:
    local toolName=$( _bashTabCompletionDocletTestLaunchWithDefaults_getToolName )

    # Get possible tool matches:
    local possibleToolMatches=$( _bashTabCompletionDocletTestLaunchWithDefaults_getAllPossibleToolNames )

    # Check if we have postfix options
    # and if we now need to go through them:
    if $HAS_POSTFIX_OPTIONS && [[ "${COMP_WORDS[@]}" == *" -- "* ]] ; then
        NUM_POSITIONAL_ARGUMENTS=0
        POSITIONAL_ARGUMENT_TYPE=()
        DEPENDENT_ARGUMENTS=()
        NORMAL_COMPLETION_ARGUMENTS=("${CS_POSTFIX_OPTIONS_NORMAL_COMPLETION_ARGUMENTS[@]}")
        MUTUALLY_EXCLUSIVE_ARGS=("${CS_POSTFIX_OPTIONS_MUTUALLY_EXCLUSIVE_ARGS[@]}")
        SYNONYMOUS_ARGS=("${CS_POSTFIX_OPTIONS_SYNONYMOUS_ARGS[@]}")
        MIN_OCCURRENCES=("${CS_POSTFIX_OPTIONS_MIN_OCCURRENCES[@]}")
        MAX_OCCURRENCES=("${CS_POSTFIX_OPTIONS_MAX_OCCURRENCES[@]}")
        ALL_LEGAL_ARGUMENTS=("${CS_POSTFIX_OPTIONS_ALL_LEGAL_ARGUMENTS[@]}")
        ALL_ARGUMENT_VALUE_TYPES=("${CS_POSTFIX_OPTIONS_ALL_ARGUMENT_VALUE_TYPES[@]}")

        # Complete the arguments for the base script:
        # Strictly speaking, what the argument to this function is doesn't matter.
        _bashTabCompletionDocletTestLaunchWithDefaults_handleArgs POSTFIX_OPTIONS

    # Check if we have a complete tool match that may match more than one tool:
    elif [[ ${#possibleToolMatches} -ne 0 ]] ; then

        # Set our reply as a list of the possible tool matches:
        COMPREPLY=( $(compgen -W '${possibleToolMatches[@]}' -- $cur) )

    elif [[ ${toolName} == "TestExtraDocs" ]] ; then

        # Set up the completion information for this tool:
        DEPENDENT_ARGUMENTS=()
        NORMAL_COMPLETION_ARGUMENTS=(--extraDocsArgument )
        MUTUALLY_EXCLUSIVE_ARGS=()
        SYNONYMOUS_ARGS=("--extraDocsArgument;-extDocArg" )
        MIN_OCCURRENCES=(0 )
        MAX_OCCURRENCES=(2147483647 )
        ALL_LEGAL_ARGUMENTS=(--extraDocsArgument )
        ALL_ARGUMENT_VALUE_TYPES=("String" )

        # Complete the arguments for this tool:
        _bashTabCompletionDocletTestLaunchWithDefaults_handleArgs
    elif [[ ${toolName} == "TestArgumentContainer" ]] ; then

        # Set up the completion information for this tool:
        NUM_POSITIONAL_ARGUMENTS=2
        POSITIONAL_ARGUMENT_TYPE=("List[File]")
        DEPENDENT_ARGUMENTS=()
        NORMAL_COMPLETION_ARGUMENTS=(--requiredClpEnum --requiredFileList --requiredInputFilesFromArgCollection --requiredStringInputFromArgCollection --requiredStringList --usesFieldNameForArgName --enumSetLong --fullAnonymousArgName --mutexSourceField --mutexTargetField1 --mutexTargetField2 --optionalClpEnum --optionalDouble --optionalDoubleList --optionalFileList --optionalFlag --optionalInputFilesFromArgCollection --optionalStringInputFromArgCollection --optionalStringList --testPlugin --advancedOptionalInt --deprecatedString )
        MUTUALLY_EXCLUSIVE_ARGS=("--mutexSourceField;mutexTargetField1,mutexTargetField2" "--mutexTargetField1;mutexSourceField" "--mutexTargetField2;mutexSourceField" )
        SYNONYMOUS_ARGS=("--requiredClpEnum;-requiredClpEnum" "--requiredFileList;-reqFilList" "--requiredInputFilesFromArgCollection;-rRequiredInputFilesFromArgCollection" "--requiredStringInputFromArgCollection;-requiredStringInputFromArgCollection" "--requiredStringList;-reqStrList" "--enumSetLong;-ES" "--fullAnonymousArgName;-anonymousClassArg" "--mutexSourceField;-mutexSourceField" "--mutexTargetField1;-mutexTargetField1" "--mutexTargetField2;-mutexTargetField2" "--optionalClpEnum;-optionalClpEnum" "--optionalDouble;-optDouble" "--optionalDoubleList;-optDoubleList" "--optionalFileList;-optFilList" "--optionalFlag;-optFlag" "--optionalInputFilesFromArgCollection;-optionalInputFilesFromArgCollection" "--optionalStringInputFromArgCollection;-optionalStringInputFromArgCollection" "--optionalStringList;-optStrList" "--advancedOptionalInt;-advancedOptInt" "--deprecatedString;-depStr" )
        MIN_OCCURRENCES=(0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 )
        MAX_OCCURRENCES=(2147483647 2147483647 2147483647 2147483647 2147483647 2147483647 2147483647 2147483647 2147483647 2147483647 2147483647 2147483647 2147483647 2147483647 2147483647 2147483647 2147483647 2147483647 2147483647 2147483647 2147483647 2147483647 )
        ALL_LEGAL_ARGUMENTS=(--requiredClpEnum --requiredFileList --requiredInputFilesFromArgCollection --requiredStringInputFromArgCollection --requiredStringList --usesFieldNameForArgName --enumSetLong --fullAnonymousArgName --mutexSourceField --mutexTargetField1 --mutexTargetField2 --optionalClpEnum --optionalDouble --optionalDoubleList --optionalFileList --optionalFlag --optionalInputFilesFromArgCollection --optionalStringInputFromArgCollection --optionalStringList --testPlugin --advancedOptionalInt --deprecatedString )
        ALL_ARGUMENT_VALUE_TYPES=("TestEnum" "List[File]" "List[File]" "String" "List[String]" "String" "EnumSet[TestEnum]" "List[File]" "List[File]" "List[File]" "List[File]" "TestEnum" "double" "List[Double]" "List[File]" "boolean" "List[File]" "String" "List[String]" "List[String]" "int" "int" )

        # Complete the arguments for this tool:
        _bashTabCompletionDocletTestLaunchWithDefaults_handleArgs

    # We have no postfix options or tool options.
    # We now must complete any prefix options and the tools themselves.
    # These are defined at the top.
    else
        NUM_POSITIONAL_ARGUMENTS=0
        POSITIONAL_ARGUMENT_TYPE=()
        DEPENDENT_ARGUMENTS=()
        NORMAL_COMPLETION_ARGUMENTS=("${CS_PREFIX_OPTIONS_NORMAL_COMPLETION_ARGUMENTS[@]}")
        MUTUALLY_EXCLUSIVE_ARGS=("${CS_PREFIX_OPTIONS_MUTUALLY_EXCLUSIVE_ARGS[@]}")
        SYNONYMOUS_ARGS=("${CS_PREFIX_OPTIONS_SYNONYMOUS_ARGS[@]}")
        MIN_OCCURRENCES=("${CS_PREFIX_OPTIONS_MIN_OCCURRENCES[@]}")
        MAX_OCCURRENCES=("${CS_PREFIX_OPTIONS_MAX_OCCURRENCES[@]}")
        ALL_LEGAL_ARGUMENTS=("${CS_PREFIX_OPTIONS_ALL_LEGAL_ARGUMENTS[@]}")
        ALL_ARGUMENT_VALUE_TYPES=("${CS_PREFIX_OPTIONS_ALL_ARGUMENT_VALUE_TYPES[@]}")

        # Complete the arguments for the prefix arguments and tools:
        _bashTabCompletionDocletTestLaunchWithDefaults_handleArgs PREFIX_OPTIONS
    fi
}

complete -o default -F _bashTabCompletionDocletTestLaunchWithDefaults_masterCompletionFunction ${CALLER_SCRIPT_NAME}



