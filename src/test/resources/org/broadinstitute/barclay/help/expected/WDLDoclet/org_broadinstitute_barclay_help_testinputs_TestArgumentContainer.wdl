version 1.0

# Run TestArgumentContainer (**BETA**) (Tool Version 11.1)
#
# Argument container class for testing documentation generation.
#
# General Workflow (non-tool) Arguments
#    app                                               Location of app run by this workflow
#
# Positional Tool Arguments
#   Array[File] positionalArgs
#
# Required Tool Arguments
#   requiredClpEnum                                    Required Clp enum                                           
#   requiredFileList                                   Required file list                                          
#   requiredInputFilesFromArgCollection                Required input files from argument collection               
#   requiredStringInputFromArgCollection               Required string input from argument collection              
#   requiredStringList                                 A required list of strings                                  
#   usesFieldNameForArgName                            Use field name if no name in annotation.                    
#
# Optional Tool Arguments
#   fullAnonymousArgName                               Test anonymous class arg                                    
#   mutexSourceField                                   Undocumented option                                         
#   mutexTargetField1                                  SAM/BAM/CRAM file(s) with alignment data from the first read of a pair.
#   mutexTargetField2                                  SAM/BAM file(s) with alignment data from the second read of a pair.
#   optionalClpEnum                                    Optional Clp enum                                           
#   optionalDouble                                     Optionals double with initial value 2.15                    
#   optionalDoubleList                                 optionalDoubleList with initial values: 100.0, 99.9, 99.0, 90.0
#   optionalFileList                                   Optional file list                                          
#   optionalFlag                                       Optional flag, defaults to false.                           
#   optionalInputFilesFromArgCollection                Optional input files from argument collection               
#   optionalStringInputFromArgCollection               Optional string input from argument collection              
#   optionalStringList                                 An optional list of strings                                 
#   testPlugin                                         Undocumented option                                         
#

workflow TestArgumentContainer {

  input {
    #Docker to use
    String? docker
    #App location
    String app

    # Positional Arguments
    Array[File] positionalArgs

    # Required Arguments
    String requiredClpEnum
    Array[File] requiredFileList
    Array[File] companionDictionary
    Array[File] companionIndex
    Array[File] requiredInputFilesFromArgCollection
    String requiredStringInputFromArgCollection
    Array[String] requiredStringList
    String usesFieldNameForArgName

    # Optional Tool Arguments
    Array[File]? fullAnonymousArgName
    Array[File]? mutexSourceField
    Array[File]? mutexTargetField1
    Array[File]? mutexTargetField2
    String? optionalClpEnum
    Float? optionalDouble
    Array[Float]? optionalDoubleList
    Array[File]? optionalFileList
    Boolean? optionalFlag
    Array[File]? optionalInputFilesFromArgCollection
    String? optionalStringInputFromArgCollection
    Array[String]? optionalStringList
    Array[String]? testPlugin

  }

  call TestArgumentContainerTask {

    input:

        #App location
        app                                                = app,

        # Positional Arguments
        positionalArgs                                     = positionalArgs,

        # Required Arguments
        requiredClpEnum                                    = requiredClpEnum,
        requiredFileList                                   = requiredFileList,
        companionDictionary                                = companionDictionary
        companionIndex                                     = companionIndex
        requiredInputFilesFromArgCollection                = requiredInputFilesFromArgCollection,
        requiredStringInputFromArgCollection               = requiredStringInputFromArgCollection,
        requiredStringList                                 = requiredStringList,
        usesFieldNameForArgName                            = usesFieldNameForArgName,

        # Optional Tool Arguments
        fullAnonymousArgName                               = fullAnonymousArgName,
        mutexSourceField                                   = mutexSourceField,
        mutexTargetField1                                  = mutexTargetField1,
        mutexTargetField2                                  = mutexTargetField2,
        optionalClpEnum                                    = optionalClpEnum,
        optionalDouble                                     = optionalDouble,
        optionalDoubleList                                 = optionalDoubleList,
        optionalFileList                                   = optionalFileList,
        optionalFlag                                       = optionalFlag,
        optionalInputFilesFromArgCollection                = optionalInputFilesFromArgCollection,
        optionalStringInputFromArgCollection               = optionalStringInputFromArgCollection,
        optionalStringList                                 = optionalStringList,
        testPlugin                                         = testPlugin,

  }

  output {
    # Workflow Outputs                                  
    Array[File] TestArgumentContainerrequiredFileList = TestArgumentContainerTask.TestArgumentContainerTask_requiredFileList
    Array[File] TestArgumentContainercompanionDictionary = TestArgumentContainerTask.TestArgumentContainerTask_companionDictionary
    Array[File] TestArgumentContainercompanionIndex = TestArgumentContainerTask.TestArgumentContainerTask_companionIndex
  }
}

task TestArgumentContainerTask {

  input {
    String app
    Array[File] positionalArgs
    String requiredClpEnum
    Array[File] requiredFileList
    Array[File] companionDictionary
    Array[File] companionIndex
    Array[File] requiredInputFilesFromArgCollection
    String requiredStringInputFromArgCollection
    Array[String] requiredStringList
    String usesFieldNameForArgName
    Array[File]? fullAnonymousArgName
    Array[File]? mutexSourceField
    Array[File]? mutexTargetField1
    Array[File]? mutexTargetField2
    String? optionalClpEnum
    Float? optionalDouble
    Array[Float]? optionalDoubleList
    Array[File]? optionalFileList
    Boolean? optionalFlag
    Array[File]? optionalInputFilesFromArgCollection
    String? optionalStringInputFromArgCollection
    Array[String]? optionalStringList
    Array[String]? testPlugin

  }

  command <<<
    ~{app} TestArgumentContainer \
        ~{sep=' ' positionalArgs} \
        --requiredClpEnum ~{sep=' --requiredClpEnum ' requiredClpEnum} \
        --requiredFileList ~{sep=' --requiredFileList ' requiredFileList} \
        --requiredInputFilesFromArgCollection ~{sep=' --requiredInputFilesFromArgCollection ' requiredInputFilesFromArgCollection} \
        --requiredStringInputFromArgCollection ~{sep=' --requiredStringInputFromArgCollection ' requiredStringInputFromArgCollection} \
        --requiredStringList ~{sep=' --requiredStringList ' requiredStringList} \
        --usesFieldNameForArgName ~{sep=' --usesFieldNameForArgName ' usesFieldNameForArgName} \
        ~{true='--fullAnonymousArgName ' false='' defined(fullAnonymousArgName)}~{sep=' --fullAnonymousArgName ' fullAnonymousArgName} \
        ~{true='--mutexSourceField ' false='' defined(mutexSourceField)}~{sep=' --mutexSourceField ' mutexSourceField} \
        ~{true='--mutexTargetField1 ' false='' defined(mutexTargetField1)}~{sep=' --mutexTargetField1 ' mutexTargetField1} \
        ~{true='--mutexTargetField2 ' false='' defined(mutexTargetField2)}~{sep=' --mutexTargetField2 ' mutexTargetField2} \
        ~{true='--optionalClpEnum ' false='' defined(optionalClpEnum)}~{sep=' --optionalClpEnum ' optionalClpEnum} \
        ~{true='--optionalDouble ' false='' defined(optionalDouble)}~{sep=' --optionalDouble ' optionalDouble} \
        ~{true='--optionalDoubleList ' false='' defined(optionalDoubleList)}~{sep=' --optionalDoubleList ' optionalDoubleList} \
        ~{true='--optionalFileList ' false='' defined(optionalFileList)}~{sep=' --optionalFileList ' optionalFileList} \
        ~{true='--optionalFlag ' false='' defined(optionalFlag)}~{sep=' --optionalFlag ' optionalFlag} \
        ~{true='--optionalInputFilesFromArgCollection ' false='' defined(optionalInputFilesFromArgCollection)}~{sep=' --optionalInputFilesFromArgCollection ' optionalInputFilesFromArgCollection} \
        ~{true='--optionalStringInputFromArgCollection ' false='' defined(optionalStringInputFromArgCollection)}~{sep=' --optionalStringInputFromArgCollection ' optionalStringInputFromArgCollection} \
        ~{true='--optionalStringList ' false='' defined(optionalStringList)}~{sep=' --optionalStringList ' optionalStringList} \
        ~{true='--testPlugin ' false='' defined(testPlugin)}~{sep=' --testPlugin ' testPlugin} \
  >>>

  runtime {
          docker:
          memory: "3G"
          disks: "local-disk 20 HDD"
  }

  output {
    # Task Outputs                                      
    Array[File] TestArgumentContainerTask_requiredFileList = "${requiredFileList}"
    Array[File] TestArgumentContainercompanionDictionary = "${companionDictionary}"
    Array[File] TestArgumentContainercompanionIndex = "${companionIndex}"
  }
 }

