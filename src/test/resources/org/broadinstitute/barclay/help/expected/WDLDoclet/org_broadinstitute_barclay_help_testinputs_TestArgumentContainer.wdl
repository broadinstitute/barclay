version 1.0

# Run TestArgumentContainer (**BETA**) (WDL auto generated from: Tool Version 11.1)
#
# Argument container class for testing documentation generation.
#
#  General Workflow (non-tool) Arguments
#    dockerImage                                        Docker image for this workflow
#    appLocation                                        Location of app to run for this workflow
#    memoryRequirements                                 Runtime memory requirements for this workflow
#    diskRequirements                                   Runtime disk requirements for this workflow
#    cpuRequirements                                    Runtime CPU count for this workflow
#    preemptibleRequirements                            Runtime preemptible count for this workflow
#    bootdisksizegbRequirements                         Runtime boot disk size for this workflow
#
#  Positional Tool Arguments
#    positionalArgs                                     Positional arguments, min = 2, max = 2                      
#
#  Required Tool Arguments
#    enumCollection                                     Undocumented option                                         
#    requiredClpEnum                                    Required Clp enum                                           
#    requiredFileList                                   Required file list                                          
#    companionDictionary                                Companion resource for requiredFileList                     
#    companionIndex                                     Companion resource for requiredFileList                     
#    requiredInputFilesFromArgCollection                Required input files from argument collection               
#    requiredStringInputFromArgCollection               Required string input from argument collection              
#    requiredStringList                                 A required list of strings                                  
#    usesFieldNameForArgName                            Use field name if no name in annotation.                    
#
#  Optional Tool Arguments
#    enumSetLong                                        Some set thing.                                             
#    fullAnonymousArgName                               Test anonymous class arg                                    
#    mutexSourceField                                   Undocumented option                                         
#    mutexTargetField1                                  SAM/BAM/CRAM file(s) with alignment data from the first read of a pair.
#    mutexTargetField2                                  SAM/BAM file(s) with alignment data from the second read of a pair.
#    optionalClpEnum                                    Optional Clp enum                                           
#    optionalDouble                                     Optionals double with initial value 2.15                    
#    optionalDoubleList                                 optionalDoubleList with initial values: 100.0, 99.9, 99.0, 90.0
#    optionalFileList                                   Optional file list                                          
#    optionalFlag                                       Optional flag, defaults to false.                           
#    optionalInputFilesFromArgCollection                Optional input files from argument collection               
#    optionalStringInputFromArgCollection               Optional string input from argument collection              
#    optionalStringList                                 An optional list of strings                                 
#    testPlugin                                         Undocumented option                                         
#

workflow TestArgumentContainer {

  input {
    #Docker to use
    String dockerImage
    #App location
    String appLocation
    #Memory to use
    String memoryRequirements
    #Disk requirements for this workflow
    String diskRequirements
    #CPU requirements for this workflow
    String cpuRequirements
    #Preemptible requirements for this workflow
    String preemptibleRequirements
    #Boot disk size requirements for this workflow
    String bootdisksizegbRequirements

    # Positional Arguments
    Array[File] positionalArgs

    # Required Arguments
    Array[String] enumCollection
    String requiredClpEnum
    Array[String] requiredFileList
    Array[String] companionDictionary
    Array[String] companionIndex
    Array[File] requiredInputFilesFromArgCollection
    String requiredStringInputFromArgCollection
    Array[String] requiredStringList
    String usesFieldNameForArgName

    # Optional Tool Arguments
    Array[String]? enumSetLong
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

  call TestArgumentContainer {

    input:

        #Docker
        dockerImage                                        = dockerImage,
        #App location
        appLocation                                        = appLocation,
        #Memory to use
        memoryRequirements                                 = memoryRequirements,
        #Disk requirements for this workflow
        diskRequirements                                   = diskRequirements,
        #CPU requirements for this workflow
        cpuRequirements                                    = cpuRequirements,
        #Preemptible requirements for this workflow
        preemptibleRequirements                            = preemptibleRequirements,
        #Boot disk size requirements for this workflow
        bootdisksizegbRequirements                         = bootdisksizegbRequirements,


        # Positional Arguments
        positionalArgs                                     = positionalArgs,

        # Required Arguments
        enumCollection                                     = enumCollection,
        requiredClpEnum                                    = requiredClpEnum,
        requiredFileList                                   = requiredFileList,
        companionDictionary                                = companionDictionary,
        companionIndex                                     = companionIndex,
        requiredInputFilesFromArgCollection                = requiredInputFilesFromArgCollection,
        requiredStringInputFromArgCollection               = requiredStringInputFromArgCollection,
        requiredStringList                                 = requiredStringList,
        usesFieldNameForArgName                            = usesFieldNameForArgName,

        # Optional Tool Arguments
        enumSetLong                                        = enumSetLong,
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
    Array[File] TestArgumentContainerrequiredFileList = TestArgumentContainer.TestArgumentContainer_requiredFileList
    Array[File] TestArgumentContainercompanionDictionary = TestArgumentContainer.TestArgumentContainer_companionDictionary
    Array[File] TestArgumentContainercompanionIndex = TestArgumentContainer.TestArgumentContainer_companionIndex
  }

  parameter_meta {
    dockerImage: { description: "Docker image for this task" }
    appLocation: { description: "Location of app to run for this task" }
    memoryRequirements: { description: "Runtime memory requirements for this task" }
    diskRequirements: { description: "Runtime disk requirements for this task" }
    cpuRequirements: { description: "Runtime CPU count for this task" }
    preemptibleRequirements: { description: "Runtime preemptible count for this task" }
    bootdisksizegbRequirements: { description: "Runtime boot disk size for this task" }

    # Positional Arguments
    positionalArgs: { description: "Positional arguments, min = 2, max = 2" }

    # Required Arguments
    enumCollection: { description: "Undocumented option" }
    requiredClpEnum: { description: "Required Clp enum" }
    requiredFileList: { description: "Required file list" }
    companionDictionary: { description: "Companion resource for requiredFileList" }
    companionIndex: { description: "Companion resource for requiredFileList" }
    requiredInputFilesFromArgCollection: { description: "Required input files from argument collection" }
    requiredStringInputFromArgCollection: { description: "Required string input from argument collection" }
    requiredStringList: { description: "A required list of strings" }
    usesFieldNameForArgName: { description: "Use field name if no name in annotation." }

    # Optional Tool Arguments
    enumSetLong: { description: "Some set thing." }
    fullAnonymousArgName: { description: "Test anonymous class arg" }
    mutexSourceField: { description: "Undocumented option" }
    mutexTargetField1: { description: "SAM/BAM/CRAM file(s) with alignment data from the first read of a pair." }
    mutexTargetField2: { description: "SAM/BAM file(s) with alignment data from the second read of a pair." }
    optionalClpEnum: { description: "Optional Clp enum" }
    optionalDouble: { description: "Optionals double with initial value 2.15" }
    optionalDoubleList: { description: "optionalDoubleList with initial values: 100.0, 99.9, 99.0, 90.0" }
    optionalFileList: { description: "Optional file list" }
    optionalFlag: { description: "Optional flag, defaults to false." }
    optionalInputFilesFromArgCollection: { description: "Optional input files from argument collection" }
    optionalStringInputFromArgCollection: { description: "Optional string input from argument collection" }
    optionalStringList: { description: "An optional list of strings" }
    testPlugin: { description: "Undocumented option" }
  }
}

task TestArgumentContainer {

  input {
    String dockerImage
    String appLocation
    String memoryRequirements
    String diskRequirements
    String cpuRequirements
    String preemptibleRequirements
    String bootdisksizegbRequirements
    Array[File] positionalArgs
    Array[String] enumCollection
    String requiredClpEnum
    Array[String] requiredFileList
    Array[String] companionDictionary
    Array[String] companionIndex
    Array[File] requiredInputFilesFromArgCollection
    String requiredStringInputFromArgCollection
    Array[String] requiredStringList
    String usesFieldNameForArgName
    Array[String]? enumSetLong
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
    ~{appLocation} TestArgumentContainer \
    ~{sep=' ' positionalArgs} \
    --enumCollection ~{sep=' --enumCollection ' enumCollection} \
    --requiredClpEnum ~{sep=' --requiredClpEnum ' requiredClpEnum} \
    --requiredFileList ~{sep=' --requiredFileList ' requiredFileList} \
    --requiredInputFilesFromArgCollection ~{sep=' --requiredInputFilesFromArgCollection ' requiredInputFilesFromArgCollection} \
    --requiredStringInputFromArgCollection ~{sep=' --requiredStringInputFromArgCollection ' requiredStringInputFromArgCollection} \
    --requiredStringList ~{sep=' --requiredStringList ' requiredStringList} \
    --usesFieldNameForArgName ~{sep=' --usesFieldNameForArgName ' usesFieldNameForArgName} \
    ~{true='--enumSetLong ' false='' defined(enumSetLong)}~{sep=' --enumSetLong ' enumSetLong} \
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
      docker: dockerImage
      memory: memoryRequirements
      disks: diskRequirements
      cpu: cpuRequirements
      preemptible: preemptibleRequirements
      bootDiskSizeGb: bootdisksizegbRequirements
  }

  output {
    # Task Outputs                                      
    Array[File] TestArgumentContainer_requiredFileList = requiredFileList
    Array[File] TestArgumentContainer_companionDictionary = companionDictionary
    Array[File] TestArgumentContainer_companionIndex = companionIndex
  }

  parameter_meta {
    dockerImage: { description: "Docker image for this task" }
    appLocation: { description: "Location of app to run for this task" }
    memoryRequirements: { description: "Runtime memory requirements for this task" }
    diskRequirements: { description: "Runtime disk requirements for this task" }
    cpuRequirements: { description: "Runtime CPU count for this task" }
    preemptibleRequirements: { description: "Runtime preemptible count for this task" }
    bootdisksizegbRequirements: { description: "Runtime boot disk size for this task" }

    # Positional Arguments
    positionalArgs: { description: "Positional arguments, min = 2, max = 2" }

    # Required Arguments
    enumCollection: { description: "Undocumented option" }
    requiredClpEnum: { description: "Required Clp enum" }
    requiredFileList: { description: "Required file list" }
    companionDictionary: { description: "Companion resource for requiredFileList" }
    companionIndex: { description: "Companion resource for requiredFileList" }
    requiredInputFilesFromArgCollection: { description: "Required input files from argument collection" }
    requiredStringInputFromArgCollection: { description: "Required string input from argument collection" }
    requiredStringList: { description: "A required list of strings" }
    usesFieldNameForArgName: { description: "Use field name if no name in annotation." }

    # Optional Tool Arguments
    enumSetLong: { description: "Some set thing." }
    fullAnonymousArgName: { description: "Test anonymous class arg" }
    mutexSourceField: { description: "Undocumented option" }
    mutexTargetField1: { description: "SAM/BAM/CRAM file(s) with alignment data from the first read of a pair." }
    mutexTargetField2: { description: "SAM/BAM file(s) with alignment data from the second read of a pair." }
    optionalClpEnum: { description: "Optional Clp enum" }
    optionalDouble: { description: "Optionals double with initial value 2.15" }
    optionalDoubleList: { description: "optionalDoubleList with initial values: 100.0, 99.9, 99.0, 90.0" }
    optionalFileList: { description: "Optional file list" }
    optionalFlag: { description: "Optional flag, defaults to false." }
    optionalInputFilesFromArgCollection: { description: "Optional input files from argument collection" }
    optionalStringInputFromArgCollection: { description: "Optional string input from argument collection" }
    optionalStringList: { description: "An optional list of strings" }
    testPlugin: { description: "Undocumented option" }
  }
}

