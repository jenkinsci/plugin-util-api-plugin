package io.jenkins.plugins.util;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import edu.hm.hafner.util.TreeString;
import edu.hm.hafner.util.TreeStringBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.XmlFile;
import hudson.util.XStream2;

/**
 * Base class that provides the basic setup to read and write entities of a given type using {@link XStream}.
 *
 * @param <T>
 *         type of the entities
 *
 * @author Ullrich Hafner
 */
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS", justification = "Filename is always safe")
public abstract class AbstractXmlStream<T> {
    private static final Logger LOGGER = Logger.getLogger(AbstractXmlStream.class.getName());

    private final Class<T> type;

    /**
     * Creates a new instance of {@link AbstractXmlStream}.
     *
     * @param type
     *         the type of the elements that are stored and retrieved
     */
    protected AbstractXmlStream(final Class<T> type) {
        this.type = type;
    }

    /**
     * Returns the default value that should be returned if the XML file is broken.
     *
     * @return the default value
     */
    protected abstract T createDefaultValue();

    protected final XStream2 createStream() {
        var xStream2 = new XStream2();
        xStream2.registerConverter(new TreeStringConverter());
        configureXStream(xStream2);
        return xStream2;
    }

    /**
     * Configures the {@link XStream} instance with custom converters or alias definitions. This default implementation
     * is empty.
     *
     * @param xStream the {@link XStream} instance
     */
    protected void configureXStream(final XStream2 xStream) {
        // empty default implementation
    }

    /**
     * Reads the specified {@code file} and creates a new instance of the given type.
     *
     * @param file
     *         path to the file
     *
     * @return the created instance
     */
    public T read(final Path file) {
        return readXml(createFile(file), createDefaultValue());
    }

    /**
     * Writes the specified instance to the given {@code file}.
     *
     * @param file
     *         path to the file
     * @param entity
     *         the entity to write to the file
     */
    public void write(final Path file, final T entity) {
        try {
            createFile(file).write(entity);
        }
        catch (IOException exception) {
            LOGGER.log(Level.SEVERE, "Failed to write entity to file " + file, exception);
        }
    }

    private XmlFile createFile(final Path file) {
        return new XmlFile(createStream(), file.toFile());
    }

    private T readXml(final XmlFile dataFile, final T defaultValue) {
        try {
            var restored = dataFile.read();

            if (type.isInstance(restored)) {
                LOGGER.log(Level.FINE, "Loaded data file " + dataFile);

                return type.cast(restored);
            }
            LOGGER.log(Level.SEVERE, "Failed to load " + dataFile + ", wrong type: " + restored);
        }
        catch (IOException exception) {
            LOGGER.log(Level.SEVERE, "Failed to load " + dataFile, exception);
        }
        return defaultValue; // fallback
    }

    /**
     * Default {@link Converter} implementation for XStream that does intern scoped to one unmarshalling.
     */
    private static final class TreeStringConverter implements Converter {
        @Override
        public void marshal(final Object source, final HierarchicalStreamWriter writer,
                final MarshallingContext context) {
            writer.setValue(source == null ? null : source.toString());
        }

        @Override
        public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
            var builder = (TreeStringBuilder) context.get(TreeStringBuilder.class);
            if (builder == null) {
                builder = new TreeStringBuilder();
                context.put(TreeStringBuilder.class, builder);
                context.addCompletionCallback(builder::dedup, 0);
            }
            return builder.intern(reader.getValue());
        }

        @Override
        @SuppressWarnings("rawtypes")
        public boolean canConvert(final Class type) {
            return type == TreeString.class;
        }
    }
}
