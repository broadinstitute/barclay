package org.broadinstitute.barclay.argparser;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;
import java.util.function.Predicate;

/**
 * Test command line parser plugin functionality.
 */
public class CommandLinePluginUnitTest {

    public static class TestPluginBase {
    }

    public static class TestPluginWithOptionalArg extends TestPluginBase {
        public static final String optionalArgName = "optionalStringArgForPlugin";

        @Argument(fullName=optionalArgName,
                optional=true)
        String optionalArg;
    }

    public static class TestPluginWithRequiredArg extends TestPluginBase {
        public static final String requiredArgName = "requiredStringArgForPlugin";

        @Argument(fullName=requiredArgName, optional=false)
        String requiredArg;
    }

    public static class TestPlugin extends TestPluginBase {
        final static String argumentName = "optionalIntegerArgForPlugin";
        @Argument(fullName = argumentName,
                shortName="optionalStringShortName",
                optional=true)
        Integer argumentForTestPlugin;
    }

    public static class TestDefaultPlugin extends TestPluginBase {
        final static String argumentName = "optionalIntegerArgForTestDefaultPlugin";
        @Argument(fullName = argumentName, optional=true)
        Integer argumentForDefaultTestPlugin;
    }

    public static class TestPluginDescriptor extends CommandLinePluginDescriptor<TestPluginBase> {

        private static final String pluginPackageName = "org.broadinstitute.barclay.argparser";
        private final Class<?> pluginBaseClass = TestPluginBase.class;

        public static final String testPluginArgumentName = "testPlugin";

        @Argument(fullName = testPluginArgumentName, optional=true)
        public final List<String> userPluginNames = new ArrayList<>(); // preserve order

        private List<TestPluginBase> defaultPlugins = new ArrayList<>();

        // Map of plugin (simple) class names to the corresponding discovered plugin instance
        private Map<String, TestPluginBase> testPlugins = new HashMap<>();

        // Set of dependent args for which we've seen values (requires predecessor)
        private Set<String> requiredPredecessors = new HashSet<>();

        public TestPluginDescriptor(final List<TestPluginBase> defaultPlugins) {
            this.defaultPlugins = defaultPlugins;
        }

        /////////////////////////////////////////////////////////
        // TestCommandLinePluginDescriptor implementation methods

        /**
         * Return a display name to identify this plugin to the user
         * @return A short user-friendly name for this plugin.
         */
        @Override
        public String getDisplayName() { return "testPlugin"; }

        /**
         * @return the class object for the base class of all plugins managed by this descriptor
         */
        @Override
        public Class<?> getPluginClass() {return pluginBaseClass;}

        /**
         * A list of package names which will be searched for plugins managed by the descriptor.
         * @return
         */
        @Override
        public List<String> getPackageNames() {return Collections.singletonList(pluginPackageName);};

        @Override
        public Predicate<Class<?>> getClassFilter() {
            return c -> { // don't use the Plugin base class
                return !c.getName().equals(this.getPluginClass().getName());
            };
        }

        // Instantiate a new ReadFilter derived object and save it in the list
        @Override
        public Object getInstance(final Class<?> pluggableClass) throws IllegalAccessException, InstantiationException {
            TestPluginBase testPluginBase = null;
            final String simpleName = pluggableClass.getSimpleName();

            if (testPlugins.containsKey(simpleName)) {
                // we found a plugin class with a name that collides with an existing class;
                // plugin names must be unique even across packages
                throw new IllegalArgumentException(
                        String.format("A plugin class name collision was detected (%s/%s). " +
                                        "Simple names of plugin classes must be unique across packages.",
                                pluggableClass.getName(),
                                testPlugins.get(simpleName).getClass().getName())
                );
            } else {
                testPluginBase = (TestPluginBase) pluggableClass.newInstance();
                testPlugins.put(simpleName, testPluginBase);
            }
            return testPluginBase;
        }

        @Override
        public boolean isDependentArgumentAllowed(final Class<?> dependentClass) {
            // make sure the predecessor for this dependent class was either specified
            // on the command line or is a tool default, otherwise reject it
            String predecessorName = dependentClass.getSimpleName();
            boolean isAllowed = userPluginNames.contains(predecessorName);
            if (isAllowed) {
                // keep track of the ones we allow so we can validate later that they
                // weren't subsequently disabled
                requiredPredecessors.add(predecessorName);
            }
            return isAllowed;
        }

        /**
         * Get the list of default plugins that were passed to this instance.
         * @return
         */
        public List<Object> getDefaultInstances() {
            ArrayList<Object> defaultList = new ArrayList<>(defaultPlugins.size());
            defaultList.addAll(defaultPlugins);
            return defaultList;
        }

        /**
         * Pass back the list of ReadFilter instances that were actually seen on the
         * command line in the same order they were specified. This list does not
         * include the tool defaults.
         */
        @Override
        public List<TestPluginBase> getAllInstances() {
            // Add the instances in the order they were specified on the command line
            //
            final ArrayList<TestPluginBase> filters = new ArrayList<>(userPluginNames.size());
            userPluginNames.forEach(s -> filters.add(testPlugins.get(s)));
            return filters;
        }

        public Class<?> getClassForInstance(final String pluginName) { return testPlugins.get(pluginName).getClass();};

        // Return the allowable values for readFilterNames/disableReadFilter
        @Override
        public Set<String> getAllowedValuesForDescriptorArgument(final String longArgName) {
            if (longArgName.equals(testPluginArgumentName)) {
                return testPlugins.keySet();
            }
            throw new IllegalArgumentException("Allowed values request for unrecognized string argument: " + longArgName);
        }

        /**
         * Validate the list of arguments and reduce the list of plugins to those
         * actually seen on the command line. This is called by the command line parser
         * after all arguments have been parsed.
         */
        @Override
        public void validateArguments() {
            Set<String> seenNames = new HashSet<>();
            seenNames.addAll(userPluginNames);

            Set<String> validNames = new HashSet<>();
            validNames.add(org.broadinstitute.barclay.argparser.CommandLinePluginUnitTest.TestPluginWithRequiredArg.class.getSimpleName());
            validNames.add(org.broadinstitute.barclay.argparser.CommandLinePluginUnitTest.TestPluginWithOptionalArg.class.getSimpleName());
            validNames.add(org.broadinstitute.barclay.argparser.CommandLinePluginUnitTest.TestPlugin.class.getSimpleName());

            if (seenNames.retainAll(validNames)) {
                throw new CommandLineException.BadArgumentValue("Illegal command line plugin specified");
            }
            userPluginNames.retainAll(seenNames);
        }

    }

    @CommandLineProgramProperties(
            summary = "Plugin Test",
            oneLineSummary = "Plugin test",
            programGroup = TestProgramGroup.class
    )
    public class PlugInTestObject {
    }

    @DataProvider(name="pluginTests")
    public Object[][] pluginTests() {
        return new Object[][]{
                {new String[0], 0},
                {new String[]{"--" + TestPluginDescriptor.testPluginArgumentName, TestPlugin.class.getSimpleName()}, 1}
        };
    }

    @Test(dataProvider = "pluginTests")
    public void testBasicPlugin(final String[] args, final int expectedInstanceCount){

        PlugInTestObject plugInTest = new PlugInTestObject();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(
                plugInTest,
                Collections.singletonList(new TestPluginDescriptor(Collections.singletonList(new TestDefaultPlugin()))),
                Collections.emptySet());

        Assert.assertTrue(clp.parseArguments(System.err, args));

        TestPluginDescriptor pid = clp.getPluginDescriptor(TestPluginDescriptor.class);
        Assert.assertNotNull(pid);

        List<TestPluginBase> pluginBases = pid.getAllInstances();

        Assert.assertEquals(pluginBases.size(), expectedInstanceCount);
    }

    @Test
    public void testPluginUsage() {
        PlugInTestObject plugInTest = new PlugInTestObject();
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(
                plugInTest,
                Collections.singletonList(new TestPluginDescriptor(Collections.singletonList(new TestDefaultPlugin()))),
                Collections.emptySet());
        final String out = clp.usage(true, false); // with common args, without hidden

        TestPluginDescriptor pid = clp.getPluginDescriptor(TestPluginDescriptor.class);
        Assert.assertNotNull(pid);

        // Make sure TestPlugin.argumentName is listed as conditional
        final int condIndex = out.indexOf("Conditional Arguments");
        Assert.assertTrue(condIndex > 0);
        final int argIndex = out.indexOf(TestPlugin.argumentName);
        Assert.assertTrue(argIndex > condIndex);
    }


    @DataProvider(name="pluginsWithRequiredArguments")
    public Object[][] pluginsWithRequiredArguments(){
        return new Object[][]{
                { TestPluginWithRequiredArg.class.getSimpleName(), TestPluginWithRequiredArg.requiredArgName, "fakeArgValue" }
        };
    }

    // fail if a plugin with required arguments is specified without the corresponding required arguments
    @Test(dataProvider = "pluginsWithRequiredArguments", expectedExceptions = CommandLineException.MissingArgument.class)
    public void testRequiredDependentArguments(
            final String plugin,
            final String argName,   //unused
            final String argValue)  //unused
    {
        CommandLineParser clp = new CommandLineArgumentParser(new Object(),
                Collections.singletonList(new TestPluginDescriptor(Collections.singletonList(new TestDefaultPlugin()))),
                Collections.emptySet());
        String[] args = {
                "--" + TestPluginDescriptor.testPluginArgumentName, plugin  // no args, just enable plugin
        };

        clp.parseArguments(System.out, args);
    }

    @DataProvider(name="pluginsWithArguments")
    public Object[][] pluginsWithArguments(){
        return new Object[][]{
                { TestPluginWithRequiredArg.class.getSimpleName(), TestPluginWithRequiredArg.requiredArgName, "fakeArgValue" },
                { TestPluginWithOptionalArg.class.getSimpleName(), TestPluginWithOptionalArg.optionalArgName, "fakeArgValue" }
        };
    }

    // fail if a plugin's arguments are passed but the plugin itself is not specified
    @Test(dataProvider = "pluginsWithArguments", expectedExceptions = CommandLineException.class)
    public void testDanglingFilterArguments(
            final String filter, // unused
            final String argName,
            final String argValue)
    {
        CommandLineParser clp = new CommandLineArgumentParser(new Object(),
                Collections.singletonList(new TestPluginDescriptor(Collections.singletonList(new TestDefaultPlugin()))),
                Collections.emptySet());

        String[] args = { argName, argValue }; // plugin args are specified but no plugin actually specified

        clp.parseArguments(System.out, args);
    }

    @Test
    public void testNoPluginsSpecified() {
        CommandLineParser clp = new CommandLineArgumentParser(new Object(),
                Collections.singletonList(new TestPluginDescriptor(Collections.singletonList(new TestDefaultPlugin()))),
                Collections.emptySet());
        clp.parseArguments(System.out, new String[]{});

        // get the command line read plugins
        final TestPluginDescriptor pluginDescriptor = clp.getPluginDescriptor(TestPluginDescriptor.class);
        final List<org.broadinstitute.barclay.argparser.CommandLinePluginUnitTest.TestPluginBase> plugins = pluginDescriptor.getAllInstances();
        Assert.assertEquals(plugins.size(), 0);
    }

    @Test
    public void testEnableMultiplePlugins() {
        CommandLineParser clp = new CommandLineArgumentParser(new Object(),
                Collections.singletonList(new TestPluginDescriptor(Collections.singletonList(new TestDefaultPlugin()))),
                Collections.emptySet());
        String[] args = {
                "--" + TestPluginDescriptor.testPluginArgumentName, TestPluginWithRequiredArg.class.getSimpleName(),
                "--" + TestPluginWithRequiredArg.requiredArgName, "fake",
                "--" + TestPluginDescriptor.testPluginArgumentName, TestPluginWithOptionalArg.class.getSimpleName(),
                "--" + TestPluginWithOptionalArg.optionalArgName, "alsofake"
        };
        clp.parseArguments(System.out, args);

        // get the command line plugins
        final TestPluginDescriptor pluginDescriptor = clp.getPluginDescriptor(TestPluginDescriptor.class);
        final List<org.broadinstitute.barclay.argparser.CommandLinePluginUnitTest.TestPluginBase> plugins = pluginDescriptor.getAllInstances();
        Assert.assertEquals(plugins.size(), 2);
        Assert.assertEquals(plugins.get(0).getClass().getSimpleName(), TestPluginWithRequiredArg.class.getSimpleName());
        Assert.assertEquals(plugins.get(1).getClass().getSimpleName(), TestPluginWithOptionalArg.class.getSimpleName());
    }

    @Test(expectedExceptions = CommandLineException.class)
    public void testEnableNonExistentPlugin() {
        CommandLineParser clp = new CommandLineArgumentParser(new Object(),
                Collections.singletonList(new TestPluginDescriptor(Collections.singletonList(new TestDefaultPlugin()))),
                Collections.emptySet());
        clp.parseArguments(System.out, new String[] {"--" + TestPluginDescriptor.testPluginArgumentName, "nonExistentPlugin"});
    }

    ////////////////////////////////////////////
    //Begin plugin argument name collision tests

    public static class TestPluginArgCollisionBase {
    }

    public static class TestPluginArgCollision1 extends TestPluginArgCollisionBase {
        public final static String argumentName = "argumentForTestCollisionPlugin";
        @Argument(fullName = argumentName, optional=true)
        Integer argumentForTestPlugin;
    }

    // This class isn't explicitly referenced anywhere, but it needs to be here so the command line parser
    // will find it on behalf of the TestPluginArgCollisionDescriptor when running the collision test. This
    // will result in an argument namespace collision, which is what we're testing.
    public static class TestPluginArgCollision2 extends TestPluginArgCollisionBase {

        //deliberately create an arg name collision with TestPluginArgCollision1
        @Argument(fullName = TestPluginArgCollision1.argumentName, optional=true)
        Integer argumentForTestPlugin;
    }

    // This descriptor should only be used for the namespace collision tests since it has a...namespace collision
    public static class TestPluginArgCollisionDescriptor extends CommandLinePluginDescriptor<TestPluginArgCollisionBase> {

        final String collisionPluginArgName = "collisionPluginName";

        @Argument(fullName=collisionPluginArgName, optional = true)
        Set<String> pluginNames = new HashSet<>();

        // Map of plugin names to the corresponding instance
        public Map<String, TestPluginArgCollisionBase> pluginInstances = new HashMap<>();

        public TestPluginArgCollisionDescriptor() {}

        @Override
        public Class<?> getPluginClass() {
            return TestPluginArgCollisionBase.class;
        }

        @Override
        public List<String> getPackageNames() {
            return Collections.singletonList("org.broadinstitute.barclay.argparser");
        }

        @Override
        public Predicate<Class<?>> getClassFilter() {
            return c -> {
                // don't use the TestPlugin base class
                return !c.getName().equals(this.getPluginClass().getName());
            };
        }

        @Override
        public Object getInstance(Class<?> pluggableClass) throws IllegalAccessException, InstantiationException {
            final TestPluginArgCollisionBase plugin = (TestPluginArgCollisionBase) pluggableClass.newInstance();
            pluginInstances.put(pluggableClass.getSimpleName(), plugin);
            return plugin;
        }

        @Override
        public Set<String> getAllowedValuesForDescriptorArgument(String longArgName) {
            if (longArgName.equals(collisionPluginArgName) ){
                return pluginInstances.keySet();
            }
            throw new IllegalArgumentException("Allowed values request for unrecognized string argument: " + longArgName);

        }
        @Override
        public boolean isDependentArgumentAllowed(Class<?> targetPluginClass) {
            return true;
        }

        @Override
        public void validateArguments() {
            // remove the un-specified plugin instances
            Map<String, TestPluginArgCollisionBase> requestedPlugins = new HashMap<>();
            pluginNames.forEach(s -> {
                TestPluginArgCollisionBase trf = pluginInstances.get(s);
                if (null == trf) {
                    throw new CommandLineException("Unrecognized test plugin name: " + s);
                }
                else {
                    requestedPlugins.put(s, trf);
                }
            });
            pluginInstances = requestedPlugins;

            // now validate that each plugin specified is valid (has a corresponding instance)
            Assert.assertEquals(pluginNames.size(), pluginInstances.size());
        }

        /**
         * Get the list of default plugins that were passed to this instance.
         * @return
         */
        @Override
        public List<Object> getDefaultInstances() { return null; }

        @Override
        public List<TestPluginArgCollisionBase> getAllInstances() {
            List<TestPluginArgCollisionBase> pluginList = new ArrayList<>();
            pluginList.addAll(pluginInstances.values());
            return pluginList;
        }

        public Class<?> getClassForInstance(final String pluginName) { return pluginInstances.get(pluginName).getClass();};
    }

    @Test(expectedExceptions=CommandLineException.CommandLineParserInternalException.class)
    public void testPluginArgumentNameCollision(){
        PlugInTestObject PlugInTestObject = new PlugInTestObject();
        // just the act of passing this descriptor to the parser should cause the collision
        new CommandLineArgumentParser(
                PlugInTestObject,
                Collections.singletonList(new TestPluginArgCollisionDescriptor()),
                Collections.emptySet());
    }

}
