package org.broadinstitute.barclay.argparser;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * Base class for a descriptor that defines a set of command line plugin classes that can be dynamically
 * discovered by the command line parser. Plugin instances contain argument fields that extend the
 * set of command line arguments accepted by the parser.
 * </p>
 * <p>
 * Each plugin descriptor passed to the command line parser is queried to determine the class and
 * package names to be searched for plugin classes controlled by that descriptor. The descriptor
 * object itself is added to the parser's list of argument sources, and for each matching plugin class
 * discovered in the packages list defined by the descriptor, the parser delegates back to the descriptor
 * to obtain an instance of that plugin. Each of these plugin objects are then also added to the parsers's
 * list of argument sources.
 * </p>
 * <p>
 * Descriptor implementations should have at least one @Argument field used to accumulate the plugins that
 * are specified on the command line by the user. Allowed values for this argument are taken from the
 * plugins that have been dynamically discovered.
 * </p>
 * <p>
 * Descriptor implementations should also maintain a list of the instances that are returned to callers by
 * {@link #createInstanceForPlugin} so they can be returned through {@link #getResolvedInstances} once
 * validation is complete.
 * </p>
 * <p>
 * A descriptor implementation may optionally choose to provide a constructor that specifies a list of
 * default plugin instances that are automatically included along with any plugins that are specified
 * manually by the user. It is the descriptor implementation's job to resolve and validate any command
 * line arguments with these default instances for purposes of ultimately rendering a fully resolved
 * list of instances to be returned by {@link #getResolvedInstances}. Implementers should be aware of
 * https://github.com/broadinstitute/barclay/issues/23 when implementing the resolution policy.</li>
 * </p>
 * <p>
 * Each plugin class controlled by a descriptor:
 * </p>
 * <ul>
 * <li> Must live in one of the packages returned by the {@link #getPackageNames} method. </li>
 * <li> Must have a type that is a subclass of the plugin base class returned by the {@link #getPluginBaseClass()}
 *      method </li>
 * <li> Must have a unique simple class name in order to avoid command line name collisions </li>
 * <li> May contain one or more @Argument annotated fields for any arguments used with that plugin. Plugin arguments
 *      may be optional or required. If required, the arguments are actually "conditionally required", in that they
 *      are only required contingent on the predecessor plugin being specified on the command line, or included in the
 *      default plugins (as determined by the command line parser via a call to isDependentArgumentAllowed).
 *      Likewise, the same conditions must apply for any such dependent argument to be accepted as valid by the
 *      descriptor.</li>
 * <li> NOTE: plugin class @Arguments that are marked "optional=false" should not have a primitive type, and
 *      should not have an initial value, as the command line parser will interpret such arguments as having been set even
 *      if they have not been specified on the command line. Conversely, @Arguments that are optional=true should
 *      have an initial value, since the parser will not require them to be set in the command line. </li>
 * </ul>
 * <p>
 * The command line parser calls the descriptor methods based on the following descriptor lifecycle:
 * </p>
 * <ol>
 * <li> During command line parser initialization (before plugin discovery or command line parsing begins): </li>
 * <ul>
 * <li> {@link #getPackageNames} </li>
 * <li> {@link #getPluginBaseClass} </li>
 * </ul>
 * <li> During plugin discovery (before argument parsing begins). Once for each plugin class discovered: </li>
 * <ul>
 * <li> {@link #includePluginClass} </li>
 * <li> {@link #createInstanceForPlugin} </li>
 * </ul>
 * <li> During command line argument parsing: </li>
 * <ul>
 * <li> {@link #isDependentArgumentAllowed} - once for each argument that has been seen on the command line that is
 *      contained within a plugin class that is controlled by this descriptor. The descriptor should decide whether
 *      the dependent is allowed argument by determining if the predecessor class is valid (implementations should
 *      recognize that the predecessor class may have been provided as a default plugin, even if not specified by
 *      the user on the command line) usually based on what plugin name arguments and default instances have been
 *      provided for this descriptor.
 * </li>
 * </ul>
 * <li> When command line argument parsing is complete (all command line arguments have been processed), but
 * before the parser returns control to the caller, a one time call: </li>
 * <ul>
 * <li> {@link #validateAndResolvePlugins} </li>
 * </ul>
 * <li> After the command line parser returns from argument parsing. These methods are generally called by
 * the application that consumes the plugins: </li>
 * <ul>
 * <li> {@link #getDefaultInstances} - whenever a plugin consumer wants the list of plugin instances that were
 *      specified as defaults. Note that is possible that the list returned by this method will be different
 *      after a call to {@link #validateAndResolvePlugins}, since the resolution policy may allow the user to
 *      override and disable default plugins.</li>
 * <li> {@link #getResolvedInstances} - whenever a plugin consumer wants the final list of resolved of plugin
 *      instances (including defaults) that were specified on the command line, or otherwise result from the
 *      the resolution policy determined by {@link #validateAndResolvePlugins}.
 * </ul>
 * <li> When generating command line help and during documentation generation: </li>
 * <ul>
 * <li> {@link #getAllowedValuesForDescriptorHelp} - only called when the command line parser is constructing
 *      a help/usage message for an @Argument field </li>
 * </ul>
 */
public abstract class CommandLinePluginDescriptor<T> {

    /**
     * Return a display name to identify this plugin to the user
     * @return A short user-friendly name for this plugin.
     */
    public String getDisplayName() { return getPluginBaseClass().getSimpleName(); }

    /**
     * List of package names from which to load command line plugin classes.
     *
     * Note that the simple name of each plugin class within these packages must be unique, even across packages.
     * @return List of package names.
     */
    public abstract List<String> getPackageNames();

    /**
     * Base class for all command line plugin classes managed by this descriptor. Subclasses of
     * this class in any of the packages returned by {@link #getPackageNames} will be command line
     * accessible.
     *
     * @return a class that is upper bounded by type {@code T}
     */
    public abstract Class<?> getPluginBaseClass();

    /**
     * Determine if a plugin class should be retained for this descriptor. Return true if the
     * descriptor wants this class to be included in from the list of plugins discovered dynamically.
     *
     * @param c plugin class, upper bounded by {@code T}
     * @return false if the plugin class shouldn't be used, otherwise true
     */
    public boolean includePluginClass(Class<?> c) { return getPluginBaseClass().isAssignableFrom(c);}

    /**
     * Return an instance of the specified plugin class. The descriptor should instantiate or otherwise
     * obtain (possibly by having been provided a default instance through the descriptor's constructor)
     * an instance of this plugin class. The descriptor should maintain a list of these instances so
     * they can later be retrieved by {@link #getResolvedInstances}.
     *
     * In addition, implementations should recognize and reject any attempt to instantiate a second
     * instance of a plugin that has the same simple class name as another plugin controlled by this
     * descriptor (which can happen if they have different qualified names within the base package
     * used by the descriptor) since the user has no way to disambiguate these on the command line).
     *
     * @param pluginClass a plugin class discovered by the command line parser that
     *                       was not rejected by {@link #includePluginClass}, and is upper bounded by {@link T}
     * @return the instantiated object that will be used by the command line parser
     * as an argument source
     * @throws IllegalAccessException if thrown when calling the {@code pluginClass} constructor
     * @throws InstantiationException if thrown when calling the {@code pluginClass} constructor
     */
    public abstract T createInstanceForPlugin(Class<?> pluginClass)
            throws IllegalAccessException, InstantiationException;

    /**
     * Called by the command line parser when an argument value contained in the class specified
     * by {@code predecessorClass} has been seen on the command line.
     *
     * Return true if the argument is allowed (i.e., this name of {@code predecessorClass} was
     * specified as a predecessor on the command line), otherwise false.
     *
     * This method can be used by both the command line parser and the descriptor class for
     * determining when to issue error messages for "dangling" arguments (dependent arguments
     * for which a value has been supplied on the command line, but for which the predecessor
     * plugin was not supplied).
     *
     * When this method returns "false", the parser will issue an error message.
     *
     * @param predecessorClass class which contains the argument that is the subject of the query
     * @return true if the plugin for this class was specified on the command line, or the
     * values in this class may be set by the user, otherwise false
     */
    public abstract boolean isDependentArgumentAllowed(Class<?> predecessorClass);

    /**
     * This method is called after all command line arguments have been processed to allow
     * the descriptor to validate and resolve the plugin arguments that have been specified, and to
     * establish any state necessary to implement {@link #getResolvedInstances}.
     *
     * It is the descriptor's job to maintain a list of all plugins discovered by the command
     * line parser (signaled by calls to {@link #createInstanceForPlugin}), and an @Argument
     * field to contain name of each plugin specified by the user on the command line. This
     * method gives the descriptor a chance to validate and resolve those the user's arguments,
     * and to derive a reduced list that contains only those instances that actually
     * should be enabled, based on whatever resolution policy is in place for the descriptor.
     *
     * Implementations of this method should minimally validate that all values that have
     * been specified on the command line have a corresponding plugin instance (this will
     * detect a user-specified value for which there is no corresponding plugin class).
     *
     * @throws CommandLineException if a plugin value has been specified that has no corresponding
     * plugin instance (i.e., the plugin class corresponding to the name was not discovered)
     * or the plugins otherwise cannot be resolved per the descriptor's argument resolution policy.
     */
    public abstract void validateAndResolvePlugins() throws CommandLineException;

    /**
     * @return the default plugins enabled for this descriptor as a list of Object. Used for
     * help/documentation generation for this descriptor.
     *
     * NOTE: calling this method after argument parsing (and thus after {@link #validateAndResolvePlugins}
     * is called) may return a different list than retruned when calling it before parsing, since the resolution
     * policy is implementation-dependent and may depend on the actual argument specified by the user
     * on the command line. For example, the user may have specified that a plugin originally included
     * in the list of defaults be disabled. The corresponding plugin class would be returned by this
     * method BEFORE parsing, but not after, since it would have been removed by
     * {@link #validateAndResolvePlugins}.
     */
    public abstract List<T> getDefaultInstances();

    /**
     * @return an ordered list containing the plugins that result from resolving all command line
     * arguments with any default plugins that have been provided to this descriptor. This list
     * represents the plugins that will actually be used by the consumer. The resolution policy is
     * descriptor-dependent.
     *
     * NOTE: calling this method before argument parsing (and thus before {@link #validateAndResolvePlugins}
     * has been called) may return a different list than calling it after parsing, since the resolution
     * policy is implementation-dependent and may depend on the actual arguments specified by the user
     * on the command line.
     */
    public abstract List<T> getResolvedInstances();

    /**
     * Return the allowable values for the String argument of this plugin descriptor
     * that is specified by {@code longArgName}. Used for help/documentation generation for this descriptor.
     * If the value is unrecognized, the implementation should return null.
     *
     * @param longArgName
     * @return Set<String> of allowable values, empty set if any value is allowed,
     *         or null if the argument doesn't belong to the descriptor
     */
    public abstract Set<String> getAllowedValuesForDescriptorHelp(String longArgName);

    /**
     * Return the class object for the plugin with simple class name {@code pluginName}
     * Used for help/usage and documentation generation.
     *
     * @param pluginName Name of the plugin requested
     * @return Class object for the plugin instance requested, upper bounded by type {@code T}
     */
    public abstract Class<?> getClassForPluginHelp(final String pluginName);

}
