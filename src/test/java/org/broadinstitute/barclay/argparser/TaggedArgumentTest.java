package org.broadinstitute.barclay.argparser;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Tests for taggable arguments, i.e.:
 *
 *     -I:tumor my.bam
 *     -I:tumor,key=value,key2=value2
 */
public class TaggedArgumentTest {

    private static class TaggableArg implements TaggedArgument {

        private String tagName;
        private Map<String, String> tagAttributes = new HashMap<>();
        public String argValue;

        public TaggableArg(final String value) {
            this.argValue = value;
        }

        public TaggableArg(final String value, final String tagName)
        {
            this(value);
            this.tagName = tagName;
        }

        @Override
        public void setTag(final String tagName) {
            this.tagName = tagName;
        }

        @Override
        public String getTag() {
            return tagName;
        }

        @Override
        public void setTagAttributes(final Map<String, String> attributes) {
            this.tagAttributes.putAll(attributes);
        }

        @Override
        public Map<String, String> getTagAttributes() {
            return tagAttributes;
        }

        @Override
        public String toString() { return argValue; }
    }

    @CommandLineProgramProperties(
            summary = "Test tagged aruments",
            oneLineSummary = "Test tagged aruments",
            programGroup = TestProgramGroup.class
    )
    private static class TaggableArguments {

        @PositionalArguments(minElements = 0, maxElements=2)
        public List<String> positionalArgs = new ArrayList<>();

        @Argument(shortName="t", fullName="tFullName", doc="Tag target arg", optional = true)
        public List<TaggableArg> taggableArgList = new ArrayList<>();

        @Argument(shortName="scalar", fullName="tScalar", doc="Tag target arg", optional = true)
        public TaggableArg taggableArgScalar = new TaggableArg("foo", "taggableArgScalar");

        @Argument(shortName="s", fullName="scalarArg", optional = true)
        public Integer nonTaggableInteger = 17;

        @Argument(shortName="n", fullName="notTaggable", optional = true)
        public String notTaggable;
    }

    // Non-generic key-value class for easy addition to statically initialized arrays in data provider
    private static class KeyValuePair{
        private final String key, value;

        public static KeyValuePair of(final String key, final String right) { return new KeyValuePair(key, right); }

        private KeyValuePair(final String left, final String right) { this.key = left; this.value = right; }
        public String getKey() { return this.key; }
        public String getValue() { return this.value; }
    }

    @SuppressWarnings("unchecked")
    @DataProvider(name = "GoodTaggedArguments")
    public Object[][] goodTaggedArguments() {
        return new Object[][][]{
                //TODO: file args, tagged enum
                {
                        // single value in a collection, no tag, no attributes, short name
                        new String[]{"--t", "tumor.bam"},
                        new KeyValuePair[]{ KeyValuePair.of(null, "tumor.bam")}, // tag is long arg name
                        new KeyValuePair[][]{{}}
                },
                {
                        // same as above, with "-" instead of "--"
                        new String[]{"-t", "tumor.bam"},
                        new KeyValuePair[]{ KeyValuePair.of(null, "tumor.bam")}, // tag is long arg name
                        new KeyValuePair[][]{{}}
                },
                {
                        // single value in a collection, no attributes, short name
                        new String[]{"--t:tumor", "tumor.bam"},
                        new KeyValuePair[]{KeyValuePair.of("tumor", "tumor.bam")},
                        new KeyValuePair[][]{{}}
                },
                {
                        // single value in a collection, no attributes, short name
                        new String[]{"--t:tumor", "/tumor.bam"},
                        new KeyValuePair[]{KeyValuePair.of("tumor", "/tumor.bam")},
                        new KeyValuePair[][]{{}}
                },
                {
                        // single tagged value in a collection, no attributes, short name
                        new String[]{"--t:tumor", "gcs://my/tumor.bam"},
                        new KeyValuePair[]{KeyValuePair.of("tumor", "gcs://my/tumor.bam")},
                        new KeyValuePair[][]{{}}
                },
                {
                        // single tagged value in a collection, no attributes, short name
                        new String[]{"--t:tumor", "gendb://mydb"},
                        new KeyValuePair[]{KeyValuePair.of("tumor", "gendb://mydb")},
                        new KeyValuePair[][]{{}}
                },
                {
                        // same as above but with "-"
                        new String[]{"-t:tumor", "gcs://my/tumor.bam"},
                        new KeyValuePair[]{KeyValuePair.of("tumor", "gcs://my/tumor.bam")},
                        new KeyValuePair[][]{{}}
                },
                {
                        // single tagged value in a collection, no attributes, short name
                        new String[]{"-t:tumor", "hostName:1234/tumor.bam"},
                        new KeyValuePair[]{KeyValuePair.of("tumor", "hostName:1234/tumor.bam")},
                        new KeyValuePair[][]{{}}
                },
                {
                        // single tagged value in a collection, no attributes, short name
                        new String[]{"-t:tumor", "http://hostName:1234/tumor.bam"},
                        new KeyValuePair[]{KeyValuePair.of("tumor", "http://hostName:1234/tumor.bam")},
                        new KeyValuePair[][]{{}}
                },
                {
                        // single value in a collection, no attributes, full name
                        new String[]{"--tFullName:tumor", "tumor.bam"},
                        new KeyValuePair[]{KeyValuePair.of("tumor", "tumor.bam")},
                        new KeyValuePair[][]{{}}
                },
                {
                        // single tagged value in a collection, two attributes, short name
                        new String[]{"--t:tumor,truth=true,training=false", "tumor.bam"},
                        new KeyValuePair[]{KeyValuePair.of("tumor", "tumor.bam")},
                        new KeyValuePair[][]{
                                {
                                        KeyValuePair.of("truth", "true"),
                                        KeyValuePair.of("training", "false")
                                }
                        }
                },
                {
                        // same as above, but with a single attribute
                        new String[]{"--t:tumor,truth=false", "tumor.bam"},
                        new KeyValuePair[]{KeyValuePair.of("tumor", "tumor.bam")},
                        new KeyValuePair[][]{
                                {
                                        KeyValuePair.of("truth", "false"),
                                }
                        }
                },
                {
                        // single tagged value in a collection, two attributes, short name
                        new String[] {"--t:tumor,truth=false,training=true", "tumor.bam"},
                        new KeyValuePair[]{KeyValuePair.of("tumor", "tumor.bam")},
                        new KeyValuePair[][]{
                                {
                                        KeyValuePair.of("truth", "false"),
                                        KeyValuePair.of("training", "true")
                                }
                        }
                },
                {
                        // same as above, but with "-"
                        new String[] {"-t:tumor,truth=false,training=true", "tumor.bam"},
                        new KeyValuePair[]{KeyValuePair.of("tumor", "tumor.bam")},
                        new KeyValuePair[][]{
                                {
                                        KeyValuePair.of("truth", "false"),
                                        KeyValuePair.of("training", "true")
                                }
                        }
                },
                {
                        // two tagged values in a collection, no attributes, short name
                        new String[]{"--t:tumor", "tumor.bam", "--t:normal", "normal.bam"},
                        new KeyValuePair[]{
                                KeyValuePair.of("tumor", "tumor.bam"),
                                KeyValuePair.of("normal", "normal.bam")
                        },
                        new KeyValuePair[][]{{}}
                },
                {
                        // two tagged values in a collection, with attributes, short name
                        new String[] {"--t:tumor,truth=false,training=true", "tumor.bam", "--t:normal,truth=true,training=false", "normal.bam"},
                        new KeyValuePair[]{
                                KeyValuePair.of("tumor", "tumor.bam"),
                                KeyValuePair.of("normal", "normal.bam")
                        },
                        new KeyValuePair[][]{
                                {
                                        KeyValuePair.of("truth", "false"),
                                        KeyValuePair.of("training", "true")
                                },
                                {
                                        KeyValuePair.of("truth", "true"),
                                        KeyValuePair.of("training", "false")
                                }
                        }
                },
                {
                        // two tagged values in a collection, with attributes, one short name, one long name
                        new String[] {"--t:tumor,truth=false,training=true", "tumor.bam", "--tFullName:normal,truth=true,training=false", "normal.bam"},
                        new KeyValuePair[]{
                                KeyValuePair.of("tumor", "tumor.bam"),
                                KeyValuePair.of("normal", "normal.bam")
                        },
                        new KeyValuePair[][]{
                                {
                                        KeyValuePair.of("truth", "false"),
                                        KeyValuePair.of("training", "true")
                                },
                                {
                                        KeyValuePair.of("truth", "true"),
                                        KeyValuePair.of("training", "false")
                                }
                        }
                },
                {
                        // intermix positional with two tagged values in a collection, with attributes, one short name, one long name
                        new String[] {
                                "positional1", // note this test does not verify the positional values
                                "positional2",
                                "--t:tumor,truth=false,training=true",
                                "tumor.bam",
                                "--tFullName:normal,truth=true,training=false",
                                "normal.bam"},
                        new KeyValuePair[]{
                                KeyValuePair.of("tumor", "tumor.bam"),
                                KeyValuePair.of("normal", "normal.bam")
                        },
                        new KeyValuePair[][]{
                                {
                                        KeyValuePair.of("truth", "false"),
                                        KeyValuePair.of("training", "true")
                                },
                                {
                                        KeyValuePair.of("truth", "true"),
                                        KeyValuePair.of("training", "false")
                                }
                        }
                }
        };
    }

    @Test(dataProvider="GoodTaggedArguments")
    public void testGoodTaggedArguments(
            final String argv[],
            final KeyValuePair expectedTagNameValuePairs[],
            final KeyValuePair expectedAttributePairArrays[][])
    {
        final TaggableArguments taggable = new TaggableArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(taggable);
        clp.parseArguments(System.err, argv);

        // All list entries are always populated
        Assert.assertEquals(taggable.taggableArgList.size(), expectedTagNameValuePairs.length);

        // validate that the tags match the right values
        int i = 0;
        for (final KeyValuePair tagNameValuePair : expectedTagNameValuePairs) {
            TaggableArg taggedArg = taggable.taggableArgList.get(i++);
            Assert.assertEquals(taggedArg.getTag(), tagNameValuePair.getKey());
            Assert.assertEquals(taggedArg.argValue, tagNameValuePair.getValue());
        }

        // validate attributes
        i = 0;
        for (KeyValuePair attributePairArray[] : expectedAttributePairArrays) {
            Map<String, String> attributes = taggable.taggableArgList.get(i++).getTagAttributes();
            for (KeyValuePair attributePair : attributePairArray) {
                Assert.assertEquals(attributes.get(attributePair.getKey()), attributePair.getValue());
            }
        }
    }

    @Test
    public void testTaggedScalarArgument() {
        final TaggableArguments taggable = new TaggableArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(taggable);
        String argv[] = new String[] {
                "-tScalar:ScalarTag,aScalar=27", "tumor.bam"
        };
        clp.parseArguments(System.err, argv);

        Assert.assertEquals(taggable.taggableArgScalar.argValue, "tumor.bam");
        Assert.assertEquals(taggable.taggableArgScalar.getTag(), "ScalarTag");
        Assert.assertEquals(taggable.taggableArgScalar.getTagAttributes().get("aScalar"), "27");
    }

    @Test
    public void testMixedTagCollectionArgument() {
        final TaggableArguments taggable = new TaggableArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(taggable);
        String argv[] = new String[] {
                "--t:tumor,truth=false,training=true", "tumor.bam", "--t", "normal.bam"
        };
        clp.parseArguments(System.err, argv);

        // first in collection is tagged.
        TaggableArg ta = taggable.taggableArgList.get(0);
        Assert.assertEquals(ta.argValue, "tumor.bam");

        Assert.assertEquals(ta.getTag(), "tumor");
        Assert.assertEquals(ta.getTagAttributes().get("truth"), "false");
        Assert.assertEquals(ta.getTagAttributes().get("training"), "true");

        // second in collection is not
        ta = taggable.taggableArgList.get(1);
        Assert.assertEquals(ta.argValue, "normal.bam");
        Assert.assertTrue(ta.getTagAttributes().isEmpty());
    }

    @Test
    public void testMixedTaggedExpansionFile() throws IOException {
        // test that tags on a tagged argument populated with an expansion file have the tags propagated to all values
        final TaggableArguments taggable = new TaggableArguments();

        final File expansionFile = createTemporaryExpansionFile();

        final CommandLineArgumentParser clp = new CommandLineArgumentParser(taggable);
        String argv[] = new String[] {
                "--t:tumor,truth=false,training=true", "tumor-false-true.bam",
                "--t:tumor,truth=true,training=true", expansionFile.getAbsolutePath(),
                "--t:tumor,truth=true,training=false", "tumor-true-false.bam",
                "--t", "normal.bam"
        };
        clp.parseArguments(System.err, argv);

        Assert.assertEquals(taggable.taggableArgList.size(), 6);

        TaggableArg ta = taggable.taggableArgList.get(0);
        Assert.assertEquals(ta.argValue, "tumor-false-true.bam");
        Assert.assertEquals(ta.getTagAttributes().get("truth"), "false");
        Assert.assertEquals(ta.getTagAttributes().get("training"), "true");

        ta = taggable.taggableArgList.get(1);
        Assert.assertEquals(ta.argValue, "value1");
        Assert.assertEquals(ta.getTagAttributes().get("truth"), "true");
        Assert.assertEquals(ta.getTagAttributes().get("training"), "true");

        ta = taggable.taggableArgList.get(2);
        Assert.assertEquals(ta.argValue, "value2");
        Assert.assertEquals(ta.getTagAttributes().get("truth"), "true");
        Assert.assertEquals(ta.getTagAttributes().get("training"), "true");

        ta = taggable.taggableArgList.get(3);
        Assert.assertEquals(ta.argValue, "value3");
        Assert.assertEquals(ta.getTagAttributes().get("truth"), "true");
        Assert.assertEquals(ta.getTagAttributes().get("training"), "true");

        ta = taggable.taggableArgList.get(4);
        Assert.assertEquals(ta.argValue, "tumor-true-false.bam");
        Assert.assertEquals(ta.getTagAttributes().get("truth"), "true");
        Assert.assertEquals(ta.getTagAttributes().get("training"), "false");

        ta = taggable.taggableArgList.get(5);
        Assert.assertEquals(ta.argValue, "normal.bam");
        Assert.assertTrue(ta.getTagAttributes().isEmpty());
    }

    private File createTemporaryExpansionFile() throws IOException {
        final File expansionFile = File.createTempFile("clp.", ".args");
        expansionFile.deleteOnExit();
        try (final PrintWriter writer = new PrintWriter(expansionFile)) {
            writer.println("value1");
            writer.println("value2");
            writer.println("value3");
        }

        return expansionFile;
    }

    @DataProvider(name = "BadTaggedArguments")
    public Object[][] badTaggedArguments() {
        return new Object[][]{
                // short name, mix of "-" and "--"
                {new String[]{"--t"}},                             // taggable, no tagname, but no arg
                {new String[]{"-t"}},                              // taggable, no tagname, but no arg
                {new String[]{"--t:"}},                            // taggable, tagname missing, no arg
                {new String[]{"-t:"}},                             // taggable, tagname missing, no arg
                {new String[]{"--t:", "tumor1.bam"}},              // taggable, tagname missing, with arg
                {new String[]{"-t:", "tumor1.bam"}},               // taggable, tagname missing, with arg
                {new String[]{"--t:tagName"}},                     // taggable, with tagname, but no arg
                {new String[]{"-t:tagName"}},                      // taggable, with tagname, but no arg

                // full name, mix of "-" and "--"
                {new String[]{"--tFullName"}},                     // taggable, but no tagname, no arg
                {new String[]{"-tFullName"}},                      // taggable, but no tagname, no arg
                {new String[]{"--tFullName:"}},                    // taggable, tagname missing, no arg
                {new String[]{"-tFullName:"}},                     // taggable, tagname missing, no arg
                {new String[]{"--tFullName:", "tumor1.bam"}},      // taggable, tagname missing, with arg
                {new String[]{"-tFullName:", "tumor1.bam"}},       // taggable, tagname missing, with arg
                {new String[]{"--tFullName:tagName"}},             // taggable, with tagname, but no arg
                {new String[]{"-tFullName:tagName"}},              // taggable, with tagname, but no arg

                // not taggable, mix of "-" and "--"
                {new String[]{"--n:tagName"}},                     // not taggable, with tagname
                {new String[]{"-n:tagName"}},                      // not taggable, with tagname
                {new String[]{"--n:key=value"}},                   // not taggable, with attributes
                {new String[]{"-n:key=value"}},                    // not taggable, with attributes
                {new String[]{"--n:tagName,key=value"}},           // not taggable, with tagname and attributes
                {new String[]{"-n:tagName,key=value"}},            // not taggable, with tagname and attributes
                {new String[]{"--n:tagName,key=value argValue"}},  // not taggable, with tagname, attributes and value
                {new String[]{"-n:tagName,key=value argValue"}},   // not taggable, with tagname, attributes and value

                // missing value, mix of "-" and "--"
                {new String[]{"--t:tagName,key=value"}},            // taggable, with tagname, attributes, missing value at end
                {new String[]{"-t:tagName,key=value"}},             // taggable, with tagname, attributes, missing value at end
                {new String[]{"--t:tagName,key=value", "-s", "28"}},// taggable, with tagname, attributes, missing value and second argument
                {new String[]{"-t:tagName,key=value", "-s", "28"}}, // taggable, with tagname, attributes, missing value and second argument
                {new String[]{ "-s", "28", "--t:tagName,key=value"}},// second argument taggable with tagname, attributes, missing value
                {new String[]{ "-s", "28", "-t:tagName,key=value"}},// second argument taggable with tagname, attributes, missing value

                // Malformed attribute strings
                {new String[]{"--t:tumor,truth", "tumor.bam"}},                    // attribute name with missing value
                {new String[]{"--t:tumor,truth=", "tumor.bam"}},                   // attribute name with missing value
                {new String[]{"--t:tumor,truth=true,truth=false", "tumor.bam"}},   // duplicate attribute value
                {new String[]{"--t:tumor,", "tumor.bam"}},                         // dangling comma

                // actually ok - we haven't placed any restrictions on tagnames
                //{new String[]{"--t:tag:tag", "value"}},
                //{new String[]{"--t::", "value"}},
                {new String[]{"--t:tag,key=value=foo", "value"}},
                {new String[]{"--t:tag,,", "value"}},
                {new String[]{"--t:tag,,key=value", "value"}},
                {new String[]{"--t:,key=value", "value"}},
                {new String[]{"--t:,,key=value", "value"}},

                {new String[]{"--,tumor:key=value", "value"}},
                {new String[]{"--t\"", "value"}},
                {new String[]{"--t:,", "value"}},
                {new String[]{"--t,:", "value"}},
                {new String[]{"--t,,", "value"}},
                {new String[]{"--t:,,key=value", "value"}},
                {new String[]{"--t,tumor:key=value", "value"}},
                {new String[]{"--t,tumor:key=:value", "value"}},
                {new String[]{"--t,tumor:key:value", "value"}},
                {new String[]{"--t,tumor,key:value", "value"}},
                {new String[]{"--t,tumor,=key:value", "value"}},
                {new String[]{"--t,tumor,=key=value", "value"}},
                {new String[]{"--t,tumor,=value", "value"}},
                {new String[]{"--t,tumor,=:value", "value"}},
                {new String[]{"--t,tumor:value", "value"}},
                {new String[]{"--t,tumor:value:", "gendb://mydb"}},

                // reject hybrid picard/posix syntax
                {new String[]{"-C=hybrid"}},
                {new String[]{"-C=hybrid:"}},
                {new String[]{"-C=hybrid: "}},
                {new String[]{"-C=hybrid: comment"}},
                {new String[]{"-C=hybrid: comment", "-C=hybrid: comment"}},
        };
    }

    @Test(dataProvider="BadTaggedArguments", expectedExceptions = CommandLineException.class)
    public void testBadTaggedArguments(final String argv[]) {
        final TaggableArguments taggables = new TaggableArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(taggables);
        clp.parseArguments(System.err, argv);
    }

    @DataProvider(name = "GetCommandLineTaggedArguments")
    public Object[][] taggedGetCommandLine() {
        return new Object[][]{
                {new String[]{"--t:tumor", "gcs://my/tumor.bam"},
                        "TaggableArguments --tFullName:tumor gcs://my/tumor.bam --tScalar:taggableArgScalar foo --scalarArg 17"
                },
                {new String[]{"--t:tumor,truth=false,training=true", "tumor.bam", "--tFullName:normal,truth=true,training=false", "normal.bam"},
                        "TaggableArguments --tFullName:tumor,training=true,truth=false tumor.bam --tFullName:normal,training=false,truth=true normal.bam --tScalar:taggableArgScalar foo --scalarArg 17"
                }
        };
    }

    @Test(dataProvider="GetCommandLineTaggedArguments")
    public void testGetCommandLineTagged(final String[] argv, final String expectedCommandLine) {
        final TaggableArguments taggables = new TaggableArguments();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(taggables);
        clp.parseArguments(System.err, argv);
        final String commandLine = clp.getCommandLine();
        Assert.assertEquals(commandLine, expectedCommandLine);
    }

    //TODO Add a tagNameAndAttributesAreEqual test here
}
