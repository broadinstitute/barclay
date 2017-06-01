# TestArgumentContainer

Argument container class for testing documentation generation.

## Description

Argument container class for testing documentation generation. Contains an argument
 for each @Argument, @ArgumentCollection, and @DocumentedFeature property that should
 be tested.

 Test custom tag:
 testType

 <p>
 The purpose of this paragraph is to test embedded html formatting.
 <ol>
     <li>This is point number 1</li>
     <li>This is point number 2</li>
 </ol>
 </p>

## Arguments

### Positional Arguments

| Argument name(s) | Type | Default value(s) | Summary |
| :--------------- | :--: | :--------------: | :------ |
| `[NA - Positional]` | List[File] | NA | Positional arguments, min = 2, max = 2 |

### Required Arguments

| Argument name(s) | Type | Default value(s) | Summary |
| :--------------- | :--: | :--------------: | :------ |
| `--requiredClpEnum`,`-requiredClpEnum` | TestEnum | null | Required Clp enum |
| `--requiredFileList`,`-reqFilList` | List[File] | [] | Required file list |
| `--requiredInputFilesFromArgCollection`,`-rRequiredInputFilesFromArgCollection` | List[File] | [] | Required input files from argument collection |
| `--requiredStringInputFromArgCollection`,`-requiredStringInputFromArgCollection` | String | null | Required string input from argument collection |
| `--requiredStringList`,`-reqStrList` | List[String] | [] | A required list of strings |
| `--usesFieldNameForArgName`,`-` | String | null | Use field name if no name in annotation. |

### Optional Arguments

| Argument name(s) | Type | Default value(s) | Summary |
| :--------------- | :--: | :--------------: | :------ |
| `--mutexArg`,`-mutexArg` | List[File] | [] | Undocumented option |
| `--mutexTargetField1`,`-mutexTargetField1` | List[File] | [] | SAM/BAM/CRAM file(s) with alignment data from the first read of a pair. |
| `--mutexTargetField2`,`-mutexTargetField2` | List[File] | [] | SAM/BAM file(s) with alignment data from the second read of a pair. |
| `--optionalClpEnum`,`-optionalClpEnum` | TestEnum | ENUM_VALUE_1 | Optional Clp enum |
| `--optionalDouble`,`-optDouble` | double | 2.15 | Optionals double with initial value 2.15 |
| `--optionalDoubleList`,`-optDoubleList` | List[Double] | [100.0, 99.9, 99.0, 90.0] | optionalDoubleList with initial values: 100.0, 99.9, 99.0, 90.0 |
| `--optionalFileList`,`-optFilList` | List[File] | [] | Optional file list |
| `--optionalFlag`,`-optFlag` | boolean | false | Optional flag, defaults to false. |
| `--optionalInputFilesFromArgCollection`,`-optionalInputFilesFromArgCollection` | List[File] | [] | Optional input files from argument collection |
| `--optionalStringInputFromArgCollection`,`-optionalStringInputFromArgCollection` | String | null | Optional string input from argument collection |
| `--optionalStringList`,`-optStrList` | List[String] | [] | An optional list of strings |
| `--testPlugin`,`-` | List[String] | [] | Undocumented option |

### Advanced Arguments

| Argument name(s) | Type | Default value(s) | Summary |
| :--------------- | :--: | :--------------: | :------ |
| `--advancedOptionalInt`,`-advancedOptInt` | int | 1 | advancedOptionalInt with initial value 1 |

### Deprecated Arguments

| Argument name(s) | Type | Default value(s) | Summary |
| :--------------- | :--: | :--------------: | :------ |
| `--deprecatedString`,`-depStr` | int | 0 | deprecated |


---

*Last updated: 2016/01/01 01:01:01*