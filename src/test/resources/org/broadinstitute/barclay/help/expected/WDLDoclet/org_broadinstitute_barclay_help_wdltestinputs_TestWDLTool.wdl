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
#    posDictionary                                      Companion resource for: Positional args doc                 
#    posIndex                                           Companion resource for: Positional args doc                 
#
#  Required Tool Arguments
#    requiredListFileInput                              requiredListFileInput doc                                   
#    requiredListFileInputDictionary                    Companion resource for: requiredListFileInput doc           
#    requiredListFileInputIndex                         Companion resource for: requiredListFileInput doc           
#    requiredListFileOutput                             requiredListFileOutput doc                                  
#    requiredListFileOutputDictionary                   Companion resource for: requiredListFileOutput doc          
#    requiredListFileOutputIndex                        Companion resource for: requiredListFileOutput doc          
#    requiredScalarFileInput                            requiredScalarFileInput doc                                 
#    requiredScalarFileInputDictionary                  Companion resource for: requiredScalarFileInput doc         
#    requiredScalarFileInputIndex                       Companion resource for: requiredScalarFileInput doc         
#    requiredScalarFileOutput                           requiredScalarFileOutput doc                                
#    requiredScalarFileOutputDictionary                 Companion resource for: requiredScalarFileOutput doc        
#    requiredScalarFileOutputIndex                      Companion resource for: requiredScalarFileOutput doc        
#
#  Optional Tool Arguments
#    optionaldListFileOutput                            optionalListFileOutput doc                                  
#    optionalListFileOutputDictionary                   Companion resource for: optionalListFileOutput doc          
#    optionalListFileOutputIndex                        Companion resource for: optionalListFileOutput doc          
#    optionaldScalarFileOutput                          optionalScalarFileOutput doc                                
#    optionalScalarFileOutputDictionary                 Companion resource for: optionalScalarFileOutput doc        
#    optionalScalarFileOutputIndex                      Companion resource for: optionalScalarFileOutput doc        
#    optionalListDoubleInput                            optionalListDoubleInput doc                                 
#    optionalListFileInput                              optionalListFileInput doc                                   
#    optionalListFileInputDictionary                    Companion resource for: optionalListFileInput doc           
#    optionalListFileInputIndex                         Companion resource for: optionalListFileInput doc           
#    optionalListFloatInput                             optionalListFloatInput doc                                  
#    optionalListIntegerInput                           optionalListIntegerInput doc                                
#    optionalListLongInput                              optionalListLongInput doc                                   
#    optionalListStringInput                            optionalListStringInput doc                                 
#    optionalScalarDoubleInput                          optionalScalarDoubleInput doc                               
#    optionalScalarDoublePrimitiveInput                 optionalScalarDoublePrimitiveInput doc                      
#    optionalScalarFileInput                            optionalScalarFileInput doc                                 
#    optionalScalarFileInputDictionary                  Companion resource for: optionalScalarFileInput doc         
#    optionalScalarFileInputIndex                       Companion resource for: optionalScalarFileInput doc         
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
    String requiredScalarFileOutput
    String requiredScalarFileOutputDictionary
    String requiredScalarFileOutputIndex

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
        requiredScalarFileOutput                           = requiredScalarFileOutput,
        requiredScalarFileOutputDictionary                 = requiredScalarFileOutputDictionary,
        requiredScalarFileOutputIndex                      = requiredScalarFileOutputIndex,

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
    Array[File] TestWDLToolrequiredListFileOutput = TestWDLTool.TestWDLTool_requiredListFileOutput
    Array[File] TestWDLToolrequiredListFileOutputDictionary = TestWDLTool.TestWDLTool_requiredListFileOutputDictionary
    Array[File] TestWDLToolrequiredListFileOutputIndex = TestWDLTool.TestWDLTool_requiredListFileOutputIndex
    File TestWDLTooloptionaldScalarFileOutput = TestWDLTool.TestWDLTool_optionaldScalarFileOutput
    File TestWDLTooloptionalScalarFileOutputDictionary = TestWDLTool.TestWDLTool_optionalScalarFileOutputDictionary
    File TestWDLTooloptionalScalarFileOutputIndex = TestWDLTool.TestWDLTool_optionalScalarFileOutputIndex
    Array[File] TestWDLTooloptionaldListFileOutput = TestWDLTool.TestWDLTool_optionaldListFileOutput
    Array[File] TestWDLTooloptionalListFileOutputDictionary = TestWDLTool.TestWDLTool_optionalListFileOutputDictionary
    Array[File] TestWDLTooloptionalListFileOutputIndex = TestWDLTool.TestWDLTool_optionalListFileOutputIndex
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
    String requiredScalarFileOutput
    String requiredScalarFileOutputDictionary
    String requiredScalarFileOutputIndex
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
    File TestWDLTool_requiredScalarFileOutput = "${requiredScalarFileOutput}"
    File TestWDLTool_requiredScalarFileOutputDictionary = "${requiredScalarFileOutputDictionary}"
    File TestWDLTool_requiredScalarFileOutputIndex = "${requiredScalarFileOutputIndex}"
    Array[File] TestWDLTool_requiredListFileOutput = "${requiredListFileOutput}"
    Array[File] TestWDLTool_requiredListFileOutputDictionary = "${requiredListFileOutputDictionary}"
    Array[File] TestWDLTool_requiredListFileOutputIndex = "${requiredListFileOutputIndex}"
    File TestWDLTool_optionaldScalarFileOutput = "${optionaldScalarFileOutput}"
    File TestWDLTool_optionalScalarFileOutputDictionary = "${optionalScalarFileOutputDictionary}"
    File TestWDLTool_optionalScalarFileOutputIndex = "${optionalScalarFileOutputIndex}"
    Array[File] TestWDLTool_optionaldListFileOutput = "${optionaldListFileOutput}"
    Array[File] TestWDLTool_optionalListFileOutputDictionary = "${optionalListFileOutputDictionary}"
    Array[File] TestWDLTool_optionalListFileOutputIndex = "${optionalListFileOutputIndex}"
  }
 }

