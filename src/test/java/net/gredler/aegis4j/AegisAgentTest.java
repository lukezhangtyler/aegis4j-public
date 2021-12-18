/* Copyright (c) 2021, Daniel Gredler. All rights reserved. */

package net.gredler.aegis4j;

import static net.gredler.aegis4j.AegisAgent.toBlockList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.rmi.StubNotFoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NoInitialContextException;
import javax.naming.ldap.LdapName;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.objenesis.SpringObjenesis;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.spi.HttpServerProvider;

import sun.misc.Unsafe;

/**
 * Tests {@link AegisAgent}.
 */
public class AegisAgentTest {

    @BeforeAll
    public static void installAgent() throws Exception {
        TestUtils.installAgent();
    }

    @Test
    public void testParseBlockList() throws Exception {
        assertEquals(Set.of("jndi", "rmi", "process", "httpserver", "serialization", "unsafe"), toBlockList(""));
        assertEquals(Set.of("jndi", "rmi", "process", "httpserver", "serialization", "unsafe"), toBlockList("   "));
        assertEquals(Set.of("jndi", "rmi", "process", "httpserver", "serialization", "unsafe"), toBlockList("blahblah"));
        assertEquals(Set.of("jndi", "rmi", "process", "httpserver", "serialization", "unsafe"), toBlockList("foo=bar"));
        assertEquals(Set.of("jndi", "rmi", "process", "httpserver", "serialization", "unsafe"), toBlockList("unblock=incorrect"));
        assertEquals(Set.of("jndi", "rmi", "process", "httpserver", "unsafe"), toBlockList("unblock=serialization"));
        assertEquals(Set.of("jndi", "rmi", "httpserver", "unsafe"), toBlockList("unblock=serialization,process"));
        assertEquals(Set.of("jndi", "rmi", "httpserver", "unsafe"), toBlockList("UNbloCk=SERIALIZATION,Process"));
        assertEquals(Set.of("jndi", "rmi", "httpserver", "unsafe"), toBlockList(" unblock\t=    serialization      , process\t"));
        assertEquals(Set.of("jndi", "rmi", "httpserver", "unsafe"), toBlockList("unblock=serialization,process,incorrect1,incorrect2"));
        assertEquals(Set.of(), toBlockList("unblock=jndi,rmi,process,httpserver,serialization,unsafe"));
        assertEquals(Set.of("jndi"), toBlockList("block=jndi"));
        assertEquals(Set.of("jndi", "rmi", "process"), toBlockList("block=jndi,rmi,process"));
        assertEquals(Set.of("jndi", "rmi", "process"), toBlockList("block = jndi\t, rmi ,\nprocess"));
        assertEquals(Set.of("jndi", "rmi", "process"), toBlockList("BLOck = JNDI\t, rmi ,\nProcESs"));
    }

    @Test
    public void testJndi() throws Exception {

        String string = "foo";
        Name name = new LdapName("cn=foo");
        Object object = new Object();
        InitialContext initialContext = new InitialContext();

        assertThrowsNICE(() -> initialContext.lookup(string));
        assertThrowsNICE(() -> initialContext.lookup(name));
        assertThrowsNICE(() -> initialContext.bind(string, object));
        assertThrowsNICE(() -> initialContext.bind(name, object));
        assertThrowsNICE(() -> initialContext.rebind(string, object));
        assertThrowsNICE(() -> initialContext.rebind(name, object));
        assertThrowsNICE(() -> initialContext.unbind(string));
        assertThrowsNICE(() -> initialContext.unbind(name));
        assertThrowsNICE(() -> initialContext.rename(string, string));
        assertThrowsNICE(() -> initialContext.rename(name, name));
        assertThrowsNICE(() -> initialContext.list(string));
        assertThrowsNICE(() -> initialContext.list(name));
        assertThrowsNICE(() -> initialContext.listBindings(string));
        assertThrowsNICE(() -> initialContext.listBindings(name));
        assertThrowsNICE(() -> initialContext.destroySubcontext(string));
        assertThrowsNICE(() -> initialContext.destroySubcontext(name));
        assertThrowsNICE(() -> initialContext.createSubcontext(string));
        assertThrowsNICE(() -> initialContext.createSubcontext(name));
        assertThrowsNICE(() -> initialContext.lookupLink(string));
        assertThrowsNICE(() -> initialContext.lookupLink(name));
        assertThrowsNICE(() -> initialContext.getNameParser(string));
        assertThrowsNICE(() -> initialContext.getNameParser(name));
        assertThrowsNICE(() -> initialContext.addToEnvironment(string, object));
        assertThrowsNICE(() -> initialContext.removeFromEnvironment(string));
        assertThrowsNICE(() -> initialContext.getEnvironment());
        assertThrowsNICE(() -> initialContext.getNameInNamespace());
    }

    @Test
    public void testRmi() throws Exception {

        int integer = 9090;
        String string = "foo";
        RMIClientSocketFactory clientSocketFactory = null;
        RMIServerSocketFactory serverSocketFactory = null;

        assertThrowsSNFE(() -> LocateRegistry.getRegistry(integer));
        assertThrowsSNFE(() -> LocateRegistry.getRegistry(string));
        assertThrowsSNFE(() -> LocateRegistry.getRegistry(string, integer));
        assertThrowsSNFE(() -> LocateRegistry.getRegistry(string, integer, clientSocketFactory));
        assertThrowsSNFE(() -> LocateRegistry.createRegistry(integer));
        assertThrowsSNFE(() -> LocateRegistry.createRegistry(integer, clientSocketFactory, serverSocketFactory));
    }

    @Test
    public void testProcess() throws Exception {

        Runtime runtime = Runtime.getRuntime();
        String string = "foo";
        String[] array = new String[] { "foo" };
        File file = new File(".");

        assertThrowsIOE(() -> runtime.exec(string));
        assertThrowsIOE(() -> runtime.exec(array));
        assertThrowsIOE(() -> runtime.exec(string, array));
        assertThrowsIOE(() -> runtime.exec(array, array));
        assertThrowsIOE(() -> runtime.exec(string, array, file));
        assertThrowsIOE(() -> runtime.exec(array, array, file));

        assertThrowsIOE(() -> new ProcessBuilder(string).start());
        assertThrowsIOE(() -> new ProcessBuilder(array).start());
        assertThrowsIOE(() -> new ProcessBuilder(List.of()).start());
        assertThrowsIOE(() -> ProcessBuilder.startPipeline(List.of()));
    }

    @Test
    public void testHttpServer() throws Exception {
        assertThrowsRE(() -> HttpServer.create(), "HTTP server provider lookup blocked by aegis4j");
        assertThrowsRE(() -> HttpServer.create(null, 0), "HTTP server provider lookup blocked by aegis4j");
        assertThrowsRE(() -> HttpServerProvider.provider(), "HTTP server provider lookup blocked by aegis4j");
    }

    @Test
    public void testSerialization() throws Exception {

        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
        assertThrowsRE(() -> new ObjectInputStream(bais), "Java deserialization blocked by aegis4j");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        assertThrowsRE(() -> new ObjectOutputStream(baos), "Java serialization blocked by aegis4j");
    }

    @Test
    public void testUnsafe() throws Exception {

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Unsafe unsafe = (Unsafe) f.get(null);

        String msg = "Unsafe blocked by aegis4j";

        assertThrowsRE(() -> Unsafe.getUnsafe(), msg);
        assertThrowsRE(() -> unsafe.addressSize(), msg);
        assertThrowsRE(() -> unsafe.allocateInstance(null), msg);
        assertThrowsRE(() -> unsafe.allocateMemory(1), msg);
        assertThrowsRE(() -> unsafe.arrayBaseOffset(null), msg);
        assertThrowsRE(() -> unsafe.arrayIndexScale(null), msg);
        assertThrowsRE(() -> unsafe.compareAndSwapInt(null, 0, 0, 0), msg);
        assertThrowsRE(() -> unsafe.compareAndSwapLong(null, 0, 0, 0), msg);
        assertThrowsRE(() -> unsafe.compareAndSwapObject(null, 0, null, null), msg);
        assertThrowsRE(() -> unsafe.copyMemory(0, 0, 0), msg);
        assertThrowsRE(() -> unsafe.copyMemory(null, 0, null, 0, 0), msg);
        assertThrowsRE(() -> unsafe.defineAnonymousClass(null, null, null), msg);
        assertThrowsRE(() -> unsafe.ensureClassInitialized(null), msg);
        assertThrowsRE(() -> unsafe.freeMemory(0), msg);
        assertThrowsRE(() -> unsafe.fullFence(), msg);
        assertThrowsRE(() -> unsafe.getAddress(0), msg);
        assertThrowsRE(() -> unsafe.getAndAddInt(null, 0, 0), msg);
        assertThrowsRE(() -> unsafe.getAndAddLong(null, 0, 0), msg);
        assertThrowsRE(() -> unsafe.getAndSetInt(null, 0, 0), msg);
        assertThrowsRE(() -> unsafe.getAndSetLong(null, 0, 0), msg);
        assertThrowsRE(() -> unsafe.getAndSetObject(null, 0, null), msg);
        assertThrowsRE(() -> unsafe.getBoolean(null, 0), msg);
        assertThrowsRE(() -> unsafe.getBooleanVolatile(null, 0), msg);
        assertThrowsRE(() -> unsafe.getByte(0), msg);
        assertThrowsRE(() -> unsafe.getByte(null, 0), msg);
        assertThrowsRE(() -> unsafe.getByteVolatile(null, 0), msg);
        assertThrowsRE(() -> unsafe.getChar(0), msg);
        assertThrowsRE(() -> unsafe.getChar(null, 0), msg);
        assertThrowsRE(() -> unsafe.getCharVolatile(null, 0), msg);
        assertThrowsRE(() -> unsafe.getDouble(0), msg);
        assertThrowsRE(() -> unsafe.getDouble(null, 0), msg);
        assertThrowsRE(() -> unsafe.getDoubleVolatile(null, 0), msg);
        assertThrowsRE(() -> unsafe.getFloat(0), msg);
        assertThrowsRE(() -> unsafe.getFloat(null, 0), msg);
        assertThrowsRE(() -> unsafe.getFloatVolatile(null, 0), msg);
        assertThrowsRE(() -> unsafe.getInt(0), msg);
        assertThrowsRE(() -> unsafe.getInt(null, 0), msg);
        assertThrowsRE(() -> unsafe.getIntVolatile(null, 0), msg);
        assertThrowsRE(() -> unsafe.getLoadAverage(null, 0), msg);
        assertThrowsRE(() -> unsafe.getLong(0), msg);
        assertThrowsRE(() -> unsafe.getLong(null, 0), msg);
        assertThrowsRE(() -> unsafe.getLongVolatile(null, 0), msg);
        assertThrowsRE(() -> unsafe.getObject(null, 0), msg);
        assertThrowsRE(() -> unsafe.getObjectVolatile(null, 0), msg);
        assertThrowsRE(() -> unsafe.getShort(0), msg);
        assertThrowsRE(() -> unsafe.getShort(null, 0), msg);
        assertThrowsRE(() -> unsafe.getShortVolatile(null, 0), msg);
        assertThrowsRE(() -> unsafe.invokeCleaner(null), msg);
        assertThrowsRE(() -> unsafe.loadFence(), msg);
        assertThrowsRE(() -> unsafe.objectFieldOffset(null), msg);
        assertThrowsRE(() -> unsafe.pageSize(), msg);
        assertThrowsRE(() -> unsafe.park(false, 0), msg);
        assertThrowsRE(() -> unsafe.putAddress(0, 0), msg);
        assertThrowsRE(() -> unsafe.putBoolean(null, 0, false), msg);
        assertThrowsRE(() -> unsafe.putBooleanVolatile(null, 0, false), msg);
        assertThrowsRE(() -> unsafe.putByte(0, (byte) 0), msg);
        assertThrowsRE(() -> unsafe.putByte(null, 0, (byte) 0), msg);
        assertThrowsRE(() -> unsafe.putByteVolatile(null, 0, (byte) 0), msg);
        assertThrowsRE(() -> unsafe.putChar(0, 'x'), msg);
        assertThrowsRE(() -> unsafe.putChar(null, 0, 'x'), msg);
        assertThrowsRE(() -> unsafe.putCharVolatile(null, 0, 'x'), msg);
        assertThrowsRE(() -> unsafe.putDouble(0, 0), msg);
        assertThrowsRE(() -> unsafe.putDouble(null, 0, 0), msg);
        assertThrowsRE(() -> unsafe.putDoubleVolatile(null, 0, 0), msg);
        assertThrowsRE(() -> unsafe.putFloat(0, 0), msg);
        assertThrowsRE(() -> unsafe.putFloat(null, 0, 0), msg);
        assertThrowsRE(() -> unsafe.putFloatVolatile(null, 0, 0), msg);
        assertThrowsRE(() -> unsafe.putInt(0, 0), msg);
        assertThrowsRE(() -> unsafe.putInt(null, 0, 0), msg);
        assertThrowsRE(() -> unsafe.putIntVolatile(null, 0, 0), msg);
        assertThrowsRE(() -> unsafe.putLong(0, 0), msg);
        assertThrowsRE(() -> unsafe.putLong(null, 0, 0), msg);
        assertThrowsRE(() -> unsafe.putLongVolatile(null, 0, 0), msg);
        assertThrowsRE(() -> unsafe.putObject(null, 0, null), msg);
        assertThrowsRE(() -> unsafe.putObjectVolatile(null, 0, null), msg);
        assertThrowsRE(() -> unsafe.putOrderedInt(null, 0, 0), msg);
        assertThrowsRE(() -> unsafe.putOrderedLong(null, 0, 0), msg);
        assertThrowsRE(() -> unsafe.putOrderedObject(null, 0, null), msg);
        assertThrowsRE(() -> unsafe.putShort(0, (short) 0), msg);
        assertThrowsRE(() -> unsafe.putShort(null, 0, (short) 0), msg);
        assertThrowsRE(() -> unsafe.putShortVolatile(null, 0, (short) 0), msg);
        assertThrowsRE(() -> unsafe.reallocateMemory(0, 0), msg);
        assertThrowsRE(() -> unsafe.setMemory(0, 0, (byte) 0), msg);
        assertThrowsRE(() -> unsafe.setMemory(null, 0, 0, (byte) 0), msg);
        assertThrowsRE(() -> unsafe.shouldBeInitialized(null), msg);
        assertThrowsRE(() -> unsafe.staticFieldBase(null), msg);
        assertThrowsRE(() -> unsafe.staticFieldOffset(null), msg);
        assertThrowsRE(() -> unsafe.storeFence(), msg);
        assertThrowsRE(() -> unsafe.throwException(null), msg);
        assertThrowsRE(() -> unsafe.unpark(null), msg);

        // Spring should still work with Unsafe disabled
        SpringObjenesis so = new SpringObjenesis();
        assertInstanceOf(SerializablePojo.class, so.newInstance(SerializablePojo.class));
        assertInstanceOf(TestUtils.class, so.newInstance(TestUtils.class));
        assertInstanceOf(LocalDate.class, so.newInstance(LocalDate.class));
    }

    private static void assertThrowsNICE(Task task) throws Exception {
        assertThrows(task, NoInitialContextException.class, "JNDI context creation blocked by aegis4j");
    }

    private static void assertThrowsSNFE(Task task) throws Exception {
        assertThrows(task, StubNotFoundException.class, "RMI registry creation blocked by aegis4j");
    }

    private static void assertThrowsIOE(Task task) throws Exception {
        assertThrows(task, IOException.class, "Process execution blocked by aegis4j");
    }

    private static void assertThrowsRE(Task task, String msg) throws Exception {
        assertThrows(task, RuntimeException.class, msg);
    }

    private static void assertThrows(Task task, Class< ? extends Exception > exceptionType, String msg) throws Exception {
        try {
            task.run();
            fail("Exception expected");
        } catch (Exception e) {
            Throwable root = getRootCause(e);
            assertInstanceOf(exceptionType, root);
            assertEquals(msg, root.getMessage());
        }
    }

    private static Throwable getRootCause(Exception e) {
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }
}
