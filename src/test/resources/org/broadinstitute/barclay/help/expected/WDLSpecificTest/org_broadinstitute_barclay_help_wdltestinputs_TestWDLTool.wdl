version 1.0

# Run TestWDLTool (WDL auto generated from: Tool Version 11.1)
#
# WDL Test Tool to test WDL Generation
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
#    positionalArgs                                     Positional args doc                                         
#    posDictionary                                      Companion resource for positionalArgs                       
#    posIndex                                           Companion resource for positionalArgs                       
#
#  Required Tool Arguments
#    requiredListFileInput                              requiredListFileInput doc                                   
#    requiredListFileInputDictionary                    Companion resource for requiredListFileInput                
#    requiredListFileInputIndex                         Companion resource for requiredListFileInput                
#    requiredListFileOutput                             requiredListFileOutput doc                                  
#    requiredListFileOutputDictionary                   Companion resource for requiredListFileOutput               
#    requiredListFileOutputIndex                        Companion resource for requiredListFileOutput               
#    requiredScalarFileInput                            requiredScalarFileInput doc                                 
#    requiredScalarFileInputDictionary                  Companion resource for requiredScalarFileInput              
#    requiredScalarFileInputIndex                       Companion resource for requiredScalarFileInput              
#    requiredScalarFileInputOptionalCompanion           Optional Companion resource for requiredScalarFileInput              
#    requiredScalarFileOutput                           requiredScalarFileOutput doc                                
#    requiredScalarFileOutputDictionary                 Companion resource for requiredScalarFileOutput             
#    requiredScalarFileOutputIndex                      Companion resource for requiredScalarFileOutput             
#    requiredScalarFileOutputOptionalCompanion          Optional Companion resource for requiredScalarFileOutput             
#
#  Optional Tool Arguments
#    optionaldListFileOutput                            optionalListFileOutput doc                                  
#    optionalListFileOutputDictionary                   Optional Companion resource for optionaldListFileOutput              
#    optionalListFileOutputIndex                        Optional Companion resource for optionaldListFileOutput              
#    optionaldScalarFileOutput                          optionalScalarFileOutput doc                                
#    optionalScalarFileOutputDictionary                 Optional Companion resource for optionaldScalarFileOutput            
#    optionalScalarFileOutputIndex                      Optional Companion resource for optionaldScalarFileOutput            
#    optionalListDoubleInput                            optionalListDoubleInput doc                                 
#    optionalListFileInput                              optionalListFileInput doc                                   
#    optionalListFileInputDictionary                    Optional Companion resource for optionalListFileInput                
#    optionalListFileInputIndex                         Optional Companion resource for optionalListFileInput                
#    optionalListFloatInput                             optionalListFloatInput doc                                  
#    optionalListIntegerInput                           optionalListIntegerInput doc                                
#    optionalListLongInput                              optionalListLongInput doc                                   
#    optionalListStringInput                            optionalListStringInput doc                                 
#    optionalScalarDoubleInput                          optionalScalarDoubleInput doc                               
#    optionalScalarDoublePrimitiveInput                 optionalScalarDoublePrimitiveInput doc                      
#    optionalScalarFileInput                            optionalScalarFileInput doc                                 
#    optionalScalarFileInputDictionary                  Optional Companion resource for optionalScalarFileInput              
#    optionalScalarFileInputIndex                       Optional Companion resource for optionalScalarFileInput              
#    optionalScalarFloatInput                           optionalScalarFloatInput doc                                
#    optionalScalarFloatPrimitiveInput                  optionalScalarFloatPrimitiveInput doc                       
#    optionalScalarIntegerInput                         optionalScalarIntegerInput doc                              
#    optionalScalarIntegerPrimitiveInput                optionalScalarIntegerPrimitiveInput doc                     
#    optionalScalarLongInput                            optionalScalarLongInput doc                                 
#    optionalScalarLongPrimitiveInput                   optionalScalarLongPrimitiveInput doc                        
#    optionalScalarStringInput                          optionalScalarStringInput doc                               
#

workflow TestWDLTool {

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
    Array[File] posDictionary
    Array[File] posIndex

    # Required Arguments
    Array[File] requiredListFileInput
    Array[File] requiredListFileInputDictionary
    Array[File] requiredListFileInputIndex
    Array[String] requiredListFileOutput
    Array[String] requiredListFileOutputDictionary
    Array[String] requiredListFileOutputIndex
    File requiredScalarFileInput
    File requiredScalarFileInputDictionary
    File requiredScalarFileInputIndex
    File? requiredScalarFileInputOptionalCompanion
    String requiredScalarFileOutput
    String requiredScalarFileOutputDictionary
    String requiredScalarFileOutputIndex
    String? requiredScalarFileOutputOptionalCompanion

    # Optional Tool Arguments
    Array[String]? optionaldListFileOutput
    Array[String]? optionalListFileOutputDictionary
    Array[String]? optionalListFileOutputIndex
    String? optionaldScalarFileOutput
    String? optionalScalarFileOutputDictionary
    String? optionalScalarFileOutputIndex
    Array[Float]? optionalListDoubleInput
    Array[File]? optionalListFileInput
    Array[File]? optionalListFileInputDictionary
    Array[File]? optionalListFileInputIndex
    Array[Float]? optionalListFloatInput
    Array[Int]? optionalListIntegerInput
    Array[Int]? optionalListLongInput
    Array[String]? optionalListStringInput
    Float? optionalScalarDoubleInput
    Float? optionalScalarDoublePrimitiveInput
    File? optionalScalarFileInput
    File? optionalScalarFileInputDictionary
    File? optionalScalarFileInputIndex
    Float? optionalScalarFloatInput
    Float? optionalScalarFloatPrimitiveInput
    Int? optionalScalarIntegerInput
    Int? optionalScalarIntegerPrimitiveInput
    Int? optionalScalarLongInput
    Int? optionalScalarLongPrimitiveInput
    String? optionalScalarStringInput

  }

  call TestWDLTool {

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
        posDictionary                                      = posDictionary,
        posIndex                                           = posIndex,

        # Required Arguments
        requiredListFileInput                              = requiredListFileInput,
        requiredListFileInputDictionary                    = requiredListFileInputDictionary,
        requiredListFileInputIndex                         = requiredListFileInputIndex,
        requiredListFileOutput                             = requiredListFileOutput,
        requiredListFileOutputDictionary                   = requiredListFileOutputDictionary,
        requiredListFileOutputIndex                        = requiredListFileOutputIndex,
        requiredScalarFileInput                            = requiredScalarFileInput,
        requiredScalarFileInputDictionary                  = requiredScalarFileInputDictionary,
        requiredScalarFileInputIndex                       = requiredScalarFileInputIndex,
        requiredScalarFileInputOptionalCompanion           = requiredScalarFileInputOptionalCompanion,
        requiredScalarFileOutput                           = requiredScalarFileOutput,
        requiredScalarFileOutputDictionary                 = requiredScalarFileOutputDictionary,
        requiredScalarFileOutputIndex                      = requiredScalarFileOutputIndex,
        requiredScalarFileOutputOptionalCompanion          = requiredScalarFileOutputOptionalCompanion,

        # Optional Tool Arguments
        optionaldListFileOutput                            = optionaldListFileOutput,
        optionalListFileOutputDictionary                   = optionalListFileOutputDictionary,
        optionalListFileOutputIndex                        = optionalListFileOutputIndex,
        optionaldScalarFileOutput                          = optionaldScalarFileOutput,
        optionalScalarFileOutputDictionary                 = optionalScalarFileOutputDictionary,
        optionalScalarFileOutputIndex                      = optionalScalarFileOutputIndex,
        optionalListDoubleInput                            = optionalListDoubleInput,
        optionalListFileInput                              = optionalListFileInput,
        optionalListFileInputDictionary                    = optionalListFileInputDictionary,
        optionalListFileInputIndex                         = optionalListFileInputIndex,
        optionalListFloatInput                             = optionalListFloatInput,
        optionalListIntegerInput                           = optionalListIntegerInput,
        optionalListLongInput                              = optionalListLongInput,
        optionalListStringInput                            = optionalListStringInput,
        optionalScalarDoubleInput                          = optionalScalarDoubleInput,
        optionalScalarDoublePrimitiveInput                 = optionalScalarDoublePrimitiveInput,
        optionalScalarFileInput                            = optionalScalarFileInput,
        optionalScalarFileInputDictionary                  = optionalScalarFileInputDictionary,
        optionalScalarFileInputIndex                       = optionalScalarFileInputIndex,
        optionalScalarFloatInput                           = optionalScalarFloatInput,
        optionalScalarFloatPrimitiveInput                  = optionalScalarFloatPrimitiveInput,
        optionalScalarIntegerInput                         = optionalScalarIntegerInput,
        optionalScalarIntegerPrimitiveInput                = optionalScalarIntegerPrimitiveInput,
        optionalScalarLongInput                            = optionalScalarLongInput,
        optionalScalarLongPrimitiveInput                   = optionalScalarLongPrimitiveInput,
        optionalScalarStringInput                          = optionalScalarStringInput,

  }

  output {
    # Workflow Outputs                                  
    File TestWDLToolrequiredScalarFileOutput = TestWDLTool.TestWDLTool_requiredScalarFileOutput
    File TestWDLToolrequiredScalarFileOutputDictionary = TestWDLTool.TestWDLTool_requiredScalarFileOutputDictionary
    File TestWDLToolrequiredScalarFileOutputIndex = TestWDLTool.TestWDLTool_requiredScalarFileOutputIndex
    File? TestWDLToolrequiredScalarFileOutputOptionalCompanion = TestWDLTool.TestWDLTool_requiredScalarFileOutputOptionalCompanion
    Array[File] TestWDLToolrequiredListFileOutput = TestWDLTool.TestWDLTool_requiredListFileOutput
    Array[File] TestWDLToolrequiredListFileOutputDictionary = TestWDLTool.TestWDLTool_requiredListFileOutputDictionary
    Array[File] TestWDLToolrequiredListFileOutputIndex = TestWDLTool.TestWDLTool_requiredListFileOutputIndex
    File? TestWDLTooloptionaldScalarFileOutput = TestWDLTool.TestWDLTool_optionaldScalarFileOutput
    File? TestWDLTooloptionalScalarFileOutputDictionary = TestWDLTool.TestWDLTool_optionalScalarFileOutputDictionary
    File? TestWDLTooloptionalScalarFileOutputIndex = TestWDLTool.TestWDLTool_optionalScalarFileOutputIndex
    Array[File]? TestWDLTooloptionaldListFileOutput = TestWDLTool.TestWDLTool_optionaldListFileOutput
    Array[File]? TestWDLTooloptionalListFileOutputDictionary = TestWDLTool.TestWDLTool_optionalListFileOutputDictionary
    Array[File]? TestWDLTooloptionalListFileOutputIndex = TestWDLTool.TestWDLTool_optionalListFileOutputIndex
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
    positionalArgs: {
      description: "Positional args doc",
      localization_optional : true 
    }
    posDictionary: {
      description: "Companion resource for positionalArgs",
      localization_optional : true 
    }
    posIndex: {
      description: "Companion resource for positionalArgs",
      localization_optional : true 
    }

    # Required Arguments
    requiredListFileInput: { description: "requiredListFileInput doc" }
    requiredListFileInputDictionary: { description: "Companion resource for requiredListFileInput" }
    requiredListFileInputIndex: { description: "Companion resource for requiredListFileInput" }
    requiredListFileOutput: { description: "requiredListFileOutput doc" }
    requiredListFileOutputDictionary: { description: "Companion resource for requiredListFileOutput" }
    requiredListFileOutputIndex: { description: "Companion resource for requiredListFileOutput" }
    requiredScalarFileInput: {
      description: "requiredScalarFileInput doc",
      localization_optional : true 
    }
    requiredScalarFileInputDictionary: {
      description: "Companion resource for requiredScalarFileInput",
      localization_optional : true 
    }
    requiredScalarFileInputIndex: {
      description: "Companion resource for requiredScalarFileInput",
      localization_optional : true 
    }
    requiredScalarFileInputOptionalCompanion: {
      description: "Companion resource for requiredScalarFileInput",
      localization_optional : true 
    }
    requiredScalarFileOutput: { description: "requiredScalarFileOutput doc" }
    requiredScalarFileOutputDictionary: { description: "Companion resource for requiredScalarFileOutput" }
    requiredScalarFileOutputIndex: { description: "Companion resource for requiredScalarFileOutput" }
    requiredScalarFileOutputOptionalCompanion: { description: "Companion resource for requiredScalarFileOutput" }

    # Optional Tool Arguments
    optionaldListFileOutput: { description: "optionalListFileOutput doc" }
    optionalListFileOutputDictionary: { description: "Companion resource for optionaldListFileOutput" }
    optionalListFileOutputIndex: { description: "Companion resource for optionaldListFileOutput" }
    optionaldScalarFileOutput: { description: "optionalScalarFileOutput doc" }
    optionalScalarFileOutputDictionary: { description: "Companion resource for optionaldScalarFileOutput" }
    optionalScalarFileOutputIndex: { description: "Companion resource for optionaldScalarFileOutput" }
    optionalListDoubleInput: { description: "optionalListDoubleInput doc" }
    optionalListFileInput: { description: "optionalListFileInput doc" }
    optionalListFileInputDictionary: { description: "Companion resource for optionalListFileInput" }
    optionalListFileInputIndex: { description: "Companion resource for optionalListFileInput" }
    optionalListFloatInput: { description: "optionalListFloatInput doc" }
    optionalListIntegerInput: { description: "optionalListIntegerInput doc" }
    optionalListLongInput: { description: "optionalListLongInput doc" }
    optionalListStringInput: { description: "optionalListStringInput doc" }
    optionalScalarDoubleInput: { description: "optionalScalarDoubleInput doc" }
    optionalScalarDoublePrimitiveInput: { description: "optionalScalarDoublePrimitiveInput doc" }
    optionalScalarFileInput: { description: "optionalScalarFileInput doc" }
    optionalScalarFileInputDictionary: { description: "Companion resource for optionalScalarFileInput" }
    optionalScalarFileInputIndex: { description: "Companion resource for optionalScalarFileInput" }
    optionalScalarFloatInput: { description: "optionalScalarFloatInput doc" }
    optionalScalarFloatPrimitiveInput: { description: "optionalScalarFloatPrimitiveInput doc" }
    optionalScalarIntegerInput: { description: "optionalScalarIntegerInput doc" }
    optionalScalarIntegerPrimitiveInput: { description: "optionalScalarIntegerPrimitiveInput doc" }
    optionalScalarLongInput: { description: "optionalScalarLongInput doc" }
    optionalScalarLongPrimitiveInput: { description: "optionalScalarLongPrimitiveInput doc" }
    optionalScalarStringInput: { description: "optionalScalarStringInput doc" }
  }
}

task TestWDLTool {

  input {
    String dockerImage
    String appLocation
    String memoryRequirements
    String diskRequirements
    String cpuRequirements
    String preemptibleRequirements
    String bootdisksizegbRequirements
    Array[File] positionalArgs
    Array[File] Positional_posDictionary
    Array[File] Positional_posIndex
    Array[File] requiredListFileInput
    Array[File] requiredListFileInputDictionary
    Array[File] requiredListFileInputIndex
    Array[String] requiredListFileOutput
    Array[String] requiredListFileOutputDictionary
    Array[String] requiredListFileOutputIndex
    File requiredScalarFileInput
    File requiredScalarFileInputDictionary
    File requiredScalarFileInputIndex
    File? requiredScalarFileInputOptionalCompanion
    String requiredScalarFileOutput
    String requiredScalarFileOutputDictionary
    String requiredScalarFileOutputIndex
    String? requiredScalarFileOutputOptionalCompanion
    Array[String]? optionaldListFileOutput
    Array[String]? optionalListFileOutputDictionary
    Array[String]? optionalListFileOutputIndex
    String? optionaldScalarFileOutput
    String? optionalScalarFileOutputDictionary
    String? optionalScalarFileOutputIndex
    Array[Float]? optionalListDoubleInput
    Array[File]? optionalListFileInput
    Array[File]? optionalListFileInputDictionary
    Array[File]? optionalListFileInputIndex
    Array[Float]? optionalListFloatInput
    Array[Int]? optionalListIntegerInput
    Array[Int]? optionalListLongInput
    Array[String]? optionalListStringInput
    Float? optionalScalarDoubleInput
    Float? optionalScalarDoublePrimitiveInput
    File? optionalScalarFileInput
    File? optionalScalarFileInputDictionary
    File? optionalScalarFileInputIndex
    Float? optionalScalarFloatInput
    Float? optionalScalarFloatPrimitiveInput
    Int? optionalScalarIntegerInput
    Int? optionalScalarIntegerPrimitiveInput
    Int? optionalScalarLongInput
    Int? optionalScalarLongPrimitiveInput
    String? optionalScalarStringInput

  }

  command <<<
    ~{appLocation} TestWDLTool \
    ~{sep=' ' positionalArgs} \
    --requiredListFileInput ~{sep=' --requiredListFileInput ' requiredListFileInput} \
    --requiredListFileOutput ~{sep=' --requiredListFileOutput ' requiredListFileOutput} \
    --requiredScalarFileInput ~{sep=' --requiredScalarFileInput ' requiredScalarFileInput} \
    --requiredScalarFileOutput ~{sep=' --requiredScalarFileOutput ' requiredScalarFileOutput} \
    ~{true='--optionaldListFileOutput ' false='' defined(optionaldListFileOutput)}~{sep=' --optionaldListFileOutput ' optionaldListFileOutput} \
    ~{true='--optionaldScalarFileOutput ' false='' defined(optionaldScalarFileOutput)}~{sep=' --optionaldScalarFileOutput ' optionaldScalarFileOutput} \
    ~{true='--optionalListDoubleInput ' false='' defined(optionalListDoubleInput)}~{sep=' --optionalListDoubleInput ' optionalListDoubleInput} \
    ~{true='--optionalListFileInput ' false='' defined(optionalListFileInput)}~{sep=' --optionalListFileInput ' optionalListFileInput} \
    ~{true='--optionalListFloatInput ' false='' defined(optionalListFloatInput)}~{sep=' --optionalListFloatInput ' optionalListFloatInput} \
    ~{true='--optionalListIntegerInput ' false='' defined(optionalListIntegerInput)}~{sep=' --optionalListIntegerInput ' optionalListIntegerInput} \
    ~{true='--optionalListLongInput ' false='' defined(optionalListLongInput)}~{sep=' --optionalListLongInput ' optionalListLongInput} \
    ~{true='--optionalListStringInput ' false='' defined(optionalListStringInput)}~{sep=' --optionalListStringInput ' optionalListStringInput} \
    ~{true='--optionalScalarDoubleInput ' false='' defined(optionalScalarDoubleInput)}~{sep=' --optionalScalarDoubleInput ' optionalScalarDoubleInput} \
    ~{true='--optionalScalarDoublePrimitiveInput ' false='' defined(optionalScalarDoublePrimitiveInput)}~{sep=' --optionalScalarDoublePrimitiveInput ' optionalScalarDoublePrimitiveInput} \
    ~{true='--optionalScalarFileInput ' false='' defined(optionalScalarFileInput)}~{sep=' --optionalScalarFileInput ' optionalScalarFileInput} \
    ~{true='--optionalScalarFloatInput ' false='' defined(optionalScalarFloatInput)}~{sep=' --optionalScalarFloatInput ' optionalScalarFloatInput} \
    ~{true='--optionalScalarFloatPrimitiveInput ' false='' defined(optionalScalarFloatPrimitiveInput)}~{sep=' --optionalScalarFloatPrimitiveInput ' optionalScalarFloatPrimitiveInput} \
    ~{true='--optionalScalarIntegerInput ' false='' defined(optionalScalarIntegerInput)}~{sep=' --optionalScalarIntegerInput ' optionalScalarIntegerInput} \
    ~{true='--optionalScalarIntegerPrimitiveInput ' false='' defined(optionalScalarIntegerPrimitiveInput)}~{sep=' --optionalScalarIntegerPrimitiveInput ' optionalScalarIntegerPrimitiveInput} \
    ~{true='--optionalScalarLongInput ' false='' defined(optionalScalarLongInput)}~{sep=' --optionalScalarLongInput ' optionalScalarLongInput} \
    ~{true='--optionalScalarLongPrimitiveInput ' false='' defined(optionalScalarLongPrimitiveInput)}~{sep=' --optionalScalarLongPrimitiveInput ' optionalScalarLongPrimitiveInput} \
    ~{true='--optionalScalarStringInput ' false='' defined(optionalScalarStringInput)}~{sep=' --optionalScalarStringInput ' optionalScalarStringInput} \

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
    File TestWDLTool_requiredScalarFileOutput = requiredScalarFileOutput
    File TestWDLTool_requiredScalarFileOutputDictionary = requiredScalarFileOutputDictionary
    File TestWDLTool_requiredScalarFileOutputIndex = requiredScalarFileOutputIndex
    File? TestWDLTool_requiredScalarFileOutputOptionalCompanion = requiredScalarFileOutputOptionalCompanion
    Array[File] TestWDLTool_requiredListFileOutput = requiredListFileOutput
    Array[File] TestWDLTool_requiredListFileOutputDictionary = requiredListFileOutputDictionary
    Array[File] TestWDLTool_requiredListFileOutputIndex = requiredListFileOutputIndex
    File? TestWDLTool_optionaldScalarFileOutput = optionaldScalarFileOutput
    File? TestWDLTool_optionalScalarFileOutputDictionary = optionalScalarFileOutputDictionary
    File? TestWDLTool_optionalScalarFileOutputIndex = optionalScalarFileOutputIndex
    Array[File]? TestWDLTool_optionaldListFileOutput = optionaldListFileOutput
    Array[File]? TestWDLTool_optionalListFileOutputDictionary = optionalListFileOutputDictionary
    Array[File]? TestWDLTool_optionalListFileOutputIndex = optionalListFileOutputIndex
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
    positionalArgs: {
      description: "Positional args doc",
      localization_optional : true 
    }
    posDictionary: {
      description: "Companion resource for positionalArgs",
      localization_optional : true 
    }
    posIndex: {
      description: "Companion resource for positionalArgs",
      localization_optional : true 
    }

    # Required Arguments
    requiredListFileInput: { description: "requiredListFileInput doc" }
    requiredListFileInputDictionary: { description: "Companion resource for requiredListFileInput" }
    requiredListFileInputIndex: { description: "Companion resource for requiredListFileInput" }
    requiredListFileOutput: { description: "requiredListFileOutput doc" }
    requiredListFileOutputDictionary: { description: "Companion resource for requiredListFileOutput" }
    requiredListFileOutputIndex: { description: "Companion resource for requiredListFileOutput" }
    requiredScalarFileInput: {
      description: "requiredScalarFileInput doc",
      localization_optional : true 
    }
    requiredScalarFileInputDictionary: {
      description: "Companion resource for requiredScalarFileInput",
      localization_optional : true 
    }
    requiredScalarFileInputIndex: {
      description: "Companion resource for requiredScalarFileInput",
      localization_optional : true 
    }
    requiredScalarFileInputOptionalCompanion: {
      description: "Companion resource for requiredScalarFileInput",
      localization_optional : true 
    }
    requiredScalarFileOutput: { description: "requiredScalarFileOutput doc" }
    requiredScalarFileOutputDictionary: { description: "Companion resource for requiredScalarFileOutput" }
    requiredScalarFileOutputIndex: { description: "Companion resource for requiredScalarFileOutput" }
    requiredScalarFileOutputOptionalCompanion: { description: "Companion resource for requiredScalarFileOutput" }

    # Optional Tool Arguments
    optionaldListFileOutput: { description: "optionalListFileOutput doc" }
    optionalListFileOutputDictionary: { description: "Companion resource for optionaldListFileOutput" }
    optionalListFileOutputIndex: { description: "Companion resource for optionaldListFileOutput" }
    optionaldScalarFileOutput: { description: "optionalScalarFileOutput doc" }
    optionalScalarFileOutputDictionary: { description: "Companion resource for optionaldScalarFileOutput" }
    optionalScalarFileOutputIndex: { description: "Companion resource for optionaldScalarFileOutput" }
    optionalListDoubleInput: { description: "optionalListDoubleInput doc" }
    optionalListFileInput: { description: "optionalListFileInput doc" }
    optionalListFileInputDictionary: { description: "Companion resource for optionalListFileInput" }
    optionalListFileInputIndex: { description: "Companion resource for optionalListFileInput" }
    optionalListFloatInput: { description: "optionalListFloatInput doc" }
    optionalListIntegerInput: { description: "optionalListIntegerInput doc" }
    optionalListLongInput: { description: "optionalListLongInput doc" }
    optionalListStringInput: { description: "optionalListStringInput doc" }
    optionalScalarDoubleInput: { description: "optionalScalarDoubleInput doc" }
    optionalScalarDoublePrimitiveInput: { description: "optionalScalarDoublePrimitiveInput doc" }
    optionalScalarFileInput: { description: "optionalScalarFileInput doc" }
    optionalScalarFileInputDictionary: { description: "Companion resource for optionalScalarFileInput" }
    optionalScalarFileInputIndex: { description: "Companion resource for optionalScalarFileInput" }
    optionalScalarFloatInput: { description: "optionalScalarFloatInput doc" }
    optionalScalarFloatPrimitiveInput: { description: "optionalScalarFloatPrimitiveInput doc" }
    optionalScalarIntegerInput: { description: "optionalScalarIntegerInput doc" }
    optionalScalarIntegerPrimitiveInput: { description: "optionalScalarIntegerPrimitiveInput doc" }
    optionalScalarLongInput: { description: "optionalScalarLongInput doc" }
    optionalScalarLongPrimitiveInput: { description: "optionalScalarLongPrimitiveInput doc" }
    optionalScalarStringInput: { description: "optionalScalarStringInput doc" }
  }
}
